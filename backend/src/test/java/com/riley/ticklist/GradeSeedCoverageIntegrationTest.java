package com.riley.ticklist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Coverage contract between {@link GradeParser} and the seeded grade-mappings
 * table: every real-world grade string the parser can classify must resolve,
 * through the REAL path ({@code applyGradeMapping} -> normalization -> seeded
 * H2 lookup), to an active {@link GradeMapping} row.
 *
 * Why this exists: the parser and the seed CSV are edited independently, and
 * when the parser learns a form the CSV doesn't price, the failure mode is a
 * tick silently stranded at {@code difficultyScore = null} (see "Sub-6A Font
 * letter grades" in docs/grade-mapping-review.md — lowercase "5c+" on a boulder
 * resolved FONT but had no seed row). This test turns that drift into a red
 * build instead of a silent null: if you widen a GradeParser regex, add the new
 * forms to the enumeration here, and the test forces matching seed rows.
 *
 * Scope — the enumeration is the intersection of two sets, both deliberate:
 *  - Parser-recognizable: forms the seed prices but the parser cannot yet
 *    classify are EXCLUDED (numeric Font "4+"/"5", plain French "4"/"5+",
 *    WI/AI/M/E range grades like "WI3-4", protection suffixes "V0 R", bare
 *    "E2", UK adjectival "VS"). Those are known parser gaps tracked in
 *    docs/grade-mapping-review.md; when the parser learns one, add it here.
 *  - Real-world: the regexes over-accept beyond the actual ladders (V18+,
 *    Font "9A+"/"9C+", French "9c+", "WI9", AI plus/minus forms). Those are
 *    not real grades (or, for WI9, deliberately unseeded: the contested
 *    Helmcken spray-ice extension jumps WI8 -> WI10), so they are EXCLUDED
 *    rather than seeded.
 *
 * AID rows bind a mapping but intentionally carry no difficultyScore (aid
 * grades measure risk/time, not free-climbing difficulty), so for AID this
 * asserts only that the mapping itself resolves.
 */
@SpringBootTest
@ActiveProfiles("test")
class GradeSeedCoverageIntegrationTest {

    @Autowired
    private GradeMappingService gradeMappingService;

    private record GradeCase(String grade, Discipline discipline, GradeSystem expectedSystem) {
    }

    @Test
    @DisplayName("every parser-recognizable real-world grade resolves to a seeded mapping")
    void everyParserRecognizableGradeResolvesToASeededMapping() {
        List<String> failures = new ArrayList<>();

        for (GradeCase gradeCase : allCases()) {
            GradeSystem parsedSystem = GradeParser.parseGradeSystem(gradeCase.grade(), gradeCase.discipline());
            if (parsedSystem != gradeCase.expectedSystem()) {
                failures.add("%s on %s: parsed as %s, expected %s".formatted(
                    gradeCase.grade(), gradeCase.discipline(), parsedSystem, gradeCase.expectedSystem()));
                continue;
            }

            // The same write path every tick create/update/import goes through.
            Tick tick = new Tick();
            tick.setGrade(gradeCase.grade());
            tick.setDiscipline(gradeCase.discipline());
            gradeMappingService.applyGradeMapping(tick);

            if (tick.getGradeMapping() == null) {
                failures.add("%s on %s (%s): no seed row for normalized lookup".formatted(
                    gradeCase.grade(), gradeCase.discipline(), parsedSystem));
                continue;
            }
            if (parsedSystem != GradeSystem.AID && tick.getDifficultyScore() == null) {
                failures.add("%s on %s (%s): mapping bound but difficultyScore is null".formatted(
                    gradeCase.grade(), gradeCase.discipline(), parsedSystem));
            }
        }

        assertThat(failures)
            .as("parser-recognizable grades with no usable seed mapping (add rows to "
                + "docs/gen_grade_mappings.py and regenerate grade-mappings.csv)")
            .isEmpty();
    }

    private static List<GradeCase> allCases() {
        List<GradeCase> cases = new ArrayList<>();

        for (String grade : vScaleGrades()) {
            cases.add(new GradeCase(grade, Discipline.BOULDER, GradeSystem.V_SCALE));
            cases.add(new GradeCase(grade, Discipline.GYM, GradeSystem.V_SCALE));
        }

        for (String grade : fontLetterGrades()) {
            cases.add(new GradeCase(grade, Discipline.BOULDER, GradeSystem.FONT));
            // 8a.nu exports boulder grades lowercase; discipline must still win.
            cases.add(new GradeCase(grade.toLowerCase(Locale.ROOT), Discipline.BOULDER, GradeSystem.FONT));
            // Uppercase on GYM keeps the case-based Font guess (GYM Font ladder).
            cases.add(new GradeCase(grade, Discipline.GYM, GradeSystem.FONT));
        }

        for (String grade : frenchLetterGrades()) {
            cases.add(new GradeCase(grade, Discipline.SPORT, GradeSystem.FRENCH_SPORT));
            // Uppercase on a rope discipline: discipline beats letter case.
            cases.add(new GradeCase(grade.toUpperCase(Locale.ROOT), Discipline.SPORT, GradeSystem.FRENCH_SPORT));
            cases.add(new GradeCase(grade, Discipline.TRAD, GradeSystem.FRENCH_SPORT));
            // Lowercase on GYM keeps the case-based French guess (GYM route ladder).
            cases.add(new GradeCase(grade, Discipline.GYM, GradeSystem.FRENCH_SPORT));
        }

        for (String grade : ydsGrades()) {
            cases.add(new GradeCase(grade, Discipline.SPORT, GradeSystem.YDS));
            cases.add(new GradeCase(grade, Discipline.GYM, GradeSystem.YDS));
            cases.add(new GradeCase(grade, Discipline.TRAD, GradeSystem.YDS));
        }

        for (String grade : iceGrades()) {
            cases.add(new GradeCase(grade, Discipline.ICE, GradeSystem.ICE_WI));
        }
        for (String grade : mixedGrades()) {
            cases.add(new GradeCase(grade, Discipline.MIXED, GradeSystem.MIXED_M));
        }
        for (String grade : aidGrades()) {
            cases.add(new GradeCase(grade, Discipline.AID, GradeSystem.AID));
        }
        for (String grade : E_GRADE_FORMS) {
            cases.add(new GradeCase(grade, Discipline.TRAD, GradeSystem.E_Grade));
        }

        cases.addAll(CASE_VARIANT_SPOT_CHECKS);
        return cases;
    }

    // VB/V-easy, V0..V17 with minus, plus, and both range spellings (the seed
    // stores dash ranges; the slash form must normalize onto them).
    private static List<String> vScaleGrades() {
        List<String> grades = new ArrayList<>(List.of("VB", "V-easy"));
        for (int number = 0; number <= 17; number++) {
            grades.add("V" + number);
            grades.add("V" + number + "-");
            if (number < 17) {
                grades.add("V" + number + "+");
                grades.add("V" + number + "-" + (number + 1));
                grades.add("V" + number + "/" + (number + 1));
            }
        }
        return grades;
    }

    // Font letters 3A..8C+ plus the 9A terminal (hardest boulder grade yet
    // proposed; 9A+ and up are parser-accepted shapes, not real grades).
    // Sub-6A letters (3A..5C+) are the 8a.nu/UK forms that fold onto the
    // numeric Font rungs — the exact gap this test exists to keep closed.
    private static List<String> fontLetterGrades() {
        List<String> grades = new ArrayList<>();
        for (int number = 3; number <= 8; number++) {
            for (String letter : List.of("A", "B", "C")) {
                grades.add(number + letter);
                grades.add(number + letter + "+");
            }
        }
        grades.add("9A");
        return grades;
    }

    // French letters 3a..9c (9c = hardest route yet climbed; 9c+ is not real).
    private static List<String> frenchLetterGrades() {
        List<String> grades = new ArrayList<>();
        for (int number = 3; number <= 8; number++) {
            for (String letter : List.of("a", "b", "c")) {
                grades.add(number + letter);
                grades.add(number + letter + "+");
            }
        }
        grades.addAll(List.of("9a", "9a+", "9b", "9b+", "9c"));
        return grades;
    }

    // 5.0..5.15d with letters, slash boundaries, and coarse -/plain/+ forms.
    // Sub-5.10 plus/minus is limited to the MP-attested set the seed prices
    // (5.7+ .. 5.9+); the parser would accept "5.3+" but no export writes it.
    private static List<String> ydsGrades() {
        List<String> grades = new ArrayList<>();
        for (int minor = 0; minor <= 9; minor++) {
            grades.add("5." + minor);
        }
        grades.addAll(List.of("5.7+", "5.8-", "5.8+", "5.9-", "5.9+"));
        for (int number = 10; number <= 15; number++) {
            for (String letter : List.of("a", "b", "c", "d")) {
                grades.add("5." + number + letter);
            }
            grades.addAll(List.of(
                "5." + number + "a/b", "5." + number + "b/c", "5." + number + "c/d",
                "5." + number + "-", "5." + number, "5." + number + "+"));
        }
        return grades;
    }

    // WI1..WI8 with the seeded plus/minus forms, then the Helmcken spray-ice
    // extension (WI10..WI13, WI10+ only). WI9 is deliberately absent from the
    // seed — the extension jumps WI8 -> WI10 — so it is not enumerated.
    // AI is plain-number only: AI plus/minus forms are parser-accepted but
    // unattested, and range grades (WI3-4, AI2-3) don't parse yet (known gap).
    private static List<String> iceGrades() {
        List<String> grades = new ArrayList<>();
        for (int number = 1; number <= 8; number++) {
            grades.add("WI" + number);
            if (number > 1) {
                grades.add("WI" + number + "-");
            }
            if (number < 8) {
                grades.add("WI" + number + "+");
            }
        }
        grades.addAll(List.of("WI10", "WI10+", "WI11", "WI12", "WI13"));
        for (int number = 1; number <= 6; number++) {
            grades.add("AI" + number);
        }
        return grades;
    }

    // M1..M16 with the seeded plus/minus forms (ranges like M5-6 don't parse yet).
    private static List<String> mixedGrades() {
        List<String> grades = new ArrayList<>();
        for (int number = 1; number <= 16; number++) {
            grades.add("M" + number);
            if (number > 1) {
                grades.add("M" + number + "-");
            }
            if (number < 16) {
                grades.add("M" + number + "+");
            }
        }
        return grades;
    }

    // A0..A5 / C0..C5, new-wave plus rungs from 2 up, and the A6 synonym.
    // These bind mappings with NO difficultyScore by design (risk/time axis).
    private static List<String> aidGrades() {
        List<String> grades = new ArrayList<>();
        for (String prefix : List.of("A", "C")) {
            for (int number = 0; number <= 5; number++) {
                grades.add(prefix + number);
                if (number >= 2) {
                    grades.add(prefix + number + "+");
                }
            }
        }
        grades.add("A6");
        return grades;
    }

    // Canonical logged "E<n> <tech>" pairings; mirrors BRITISH_TECH_GRADES_BY_E_GRADE
    // in docs/gen_grade_mappings.py. Bare "E2", slash "E2/3", and the adjectival
    // ladder ("VS", "HS 4b") are seeded but not yet parser-classifiable, so they
    // are excluded here until the parser learns them.
    private static final List<String> E_GRADE_FORMS = List.of(
        "E1 5a", "E1 5b", "E1 5c", "E2 5b", "E2 5c", "E3 5c", "E3 6a",
        "E4 6a", "E4 6b", "E5 6a", "E5 6b", "E6 6b", "E6 6c", "E7 6b", "E7 6c",
        "E8 6c", "E8 7a", "E9 6c", "E9 7a", "E9 7b", "E10 7a", "E10 7c",
        "E11 7a", "E12 7a");

    // Odd-but-real casing from exports and hand entry; normalization must land
    // every one of these on a seeded row.
    private static final List<GradeCase> CASE_VARIANT_SPOT_CHECKS = List.of(
        new GradeCase("v4", Discipline.BOULDER, GradeSystem.V_SCALE),
        new GradeCase("v4/5", Discipline.BOULDER, GradeSystem.V_SCALE),
        new GradeCase("vb", Discipline.BOULDER, GradeSystem.V_SCALE),
        new GradeCase("5.11D", Discipline.SPORT, GradeSystem.YDS),
        new GradeCase("wi4+", Discipline.ICE, GradeSystem.ICE_WI),
        new GradeCase("m8-", Discipline.MIXED, GradeSystem.MIXED_M),
        new GradeCase("a3+", Discipline.AID, GradeSystem.AID),
        new GradeCase("e3 6A", Discipline.TRAD, GradeSystem.E_Grade));
}
