package com.riley.ticklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class GradeParserTest {
    @Test
    void parsesYdsGrades() {
        GradeParser.ParsedGrade parsedGrade = GradeParser.parse(" 5.10a ");

        assertThat(parsedGrade.rawGrade()).isEqualTo("5.10a");
        assertThat(parsedGrade.gradeSystem()).isEqualTo(GradeSystem.YDS);
        assertThat(parsedGrade.gradeValue()).isEqualTo(10.0);
    }

    @Test
    void parsesBoulderingGradeSystems() {
        assertThat(GradeParser.parseGradeSystem("V6")).isEqualTo(GradeSystem.V_SCALE);
        assertThat(GradeParser.parseGradeSystem("6A+")).isEqualTo(GradeSystem.FONT);
    }

    @Test
    void parsesSportIceMixedAidAndEGradeSystems() {
        assertThat(GradeParser.parseGradeSystem("6a+")).isEqualTo(GradeSystem.FRENCH_SPORT);
        assertThat(GradeParser.parseGradeSystem("WI4+")).isEqualTo(GradeSystem.ICE_WI);
        assertThat(GradeParser.parseGradeSystem("M7")).isEqualTo(GradeSystem.MIXED_M);
        assertThat(GradeParser.parseGradeSystem("C2")).isEqualTo(GradeSystem.AID);
        assertThat(GradeParser.parseGradeSystem("E2 5c")).isEqualTo(GradeSystem.E_Grade);
    }

    @Test
    void returnsUnknownForBlankOrUnmatchedGrades() {
        assertThat(GradeParser.parse(null).rawGrade()).isNull();
        assertThat(GradeParser.parseGradeSystem("")).isEqualTo(GradeSystem.UNKNOWN);
        assertThat(GradeParser.parseGradeSystem("rainbow")).isEqualTo(GradeSystem.UNKNOWN);
    }

    @Test
    void parsesYdsGradeValues() {
        assertThat(GradeParser.parseGradeValue("5.9")).isEqualTo(9.0);
        assertThat(GradeParser.parseGradeValue("5.10a")).isEqualTo(10.0);
        assertThat(GradeParser.parseGradeValue("5.10b")).isEqualTo(10.25);
        assertThat(GradeParser.parseGradeValue("5.10d")).isEqualTo(10.75);
        assertThat(GradeParser.parseGradeValue("5.11a/b")).isEqualTo(11.125);
        assertThat(GradeParser.parseGradeValue("5.10+")).isEqualTo(10.75);
        assertThat(GradeParser.parseGradeValue("5.10-")).isEqualTo(10.0);
        assertThat(GradeParser.parseGradeValue("5.10a+")).isCloseTo(10.1, within(1e-9));
        assertThat(GradeParser.parseGradeValue("5.10b-")).isCloseTo(10.15, within(1e-9));
        assertThat(GradeParser.parseGradeValue("5.10a PG13")).isEqualTo(10.0);
    }

    @Test
    void parsesBoulderingGradeValues() {
        assertThat(GradeParser.parseGradeValue("V6")).isEqualTo(6.0);
        assertThat(GradeParser.parseGradeValue("V4+")).isEqualTo(4.25);
        assertThat(GradeParser.parseGradeValue("V4-")).isEqualTo(3.75);
        assertThat(GradeParser.parseGradeValue("V4-5")).isEqualTo(4.5);
        assertThat(GradeParser.parseGradeValue("V4/5")).isEqualTo(4.5);
        assertThat(GradeParser.parseGradeValue("VB")).isEqualTo(-1.0);
        assertThat(GradeParser.parseGradeValue("V-easy")).isEqualTo(-1.0);
        assertThat(GradeParser.parseGradeValue("6A+")).isCloseTo(6.1667, within(0.001));
    }

    @Test
    void parsesSportIceMixedAidAndEGradeValues() {
        assertThat(GradeParser.parseGradeValue("6a+")).isCloseTo(6.1667, within(0.001));
        assertThat(GradeParser.parseGradeValue("6b")).isCloseTo(6.3333, within(0.001));
        assertThat(GradeParser.parseGradeValue("WI4+")).isEqualTo(4.25);
        assertThat(GradeParser.parseGradeValue("WI3")).isEqualTo(3.0);
        assertThat(GradeParser.parseGradeValue("AI3")).isEqualTo(3.0);
        assertThat(GradeParser.parseGradeValue("M7")).isEqualTo(7.0);
        assertThat(GradeParser.parseGradeValue("A2+")).isEqualTo(2.25);
        assertThat(GradeParser.parseGradeValue("C2")).isEqualTo(2.0);
        assertThat(GradeParser.parseGradeValue("E2 5c")).isEqualTo(2.0);
        assertThat(GradeParser.parseGradeValue("E2 \n5c")).isEqualTo(2.0);
    }

    @Test
    void returnsNullGradeValueForBlankOrUnmatchedGrades() {
        assertThat(GradeParser.parse(null).gradeValue()).isNull();
        assertThat(GradeParser.parseGradeValue(null)).isNull();
        assertThat(GradeParser.parseGradeValue("")).isNull();
        assertThat(GradeParser.parseGradeValue("rainbow")).isNull();
    }

    @Test
    void parsesYdsBoundariesAndVariants() {
        assertThat(GradeParser.parseGradeValue("5.0")).isEqualTo(0.0);
        assertThat(GradeParser.parseGradeValue("5.15d")).isEqualTo(15.75);
        assertThat(GradeParser.parseGradeValue("5.9+")).isEqualTo(9.75);
        assertThat(GradeParser.parseGradeValue("5.10D")).isEqualTo(10.75);
        assertThat(GradeParser.parseGradeValue("5.10/b")).isEqualTo(10.25);
        assertThat(GradeParser.parseGradeValue("5.10b/c R")).isEqualTo(10.375);
        assertThat(GradeParser.parseGradeSystem("5.16")).isEqualTo(GradeSystem.UNKNOWN);
        assertThat(GradeParser.parseGradeSystem("5.9X")).isEqualTo(GradeSystem.UNKNOWN);
    }

    @Test
    void parsesVScaleBoundariesAndVariants() {
        assertThat(GradeParser.parseGradeSystem("v6")).isEqualTo(GradeSystem.V_SCALE);
        assertThat(GradeParser.parseGradeValue("v6")).isEqualTo(6.0);
        assertThat(GradeParser.parseGradeValue("V0")).isEqualTo(0.0);
        assertThat(GradeParser.parseGradeValue("V0-")).isEqualTo(-0.25);
        assertThat(GradeParser.parseGradeValue("Veasy")).isEqualTo(-1.0);
        assertThat(GradeParser.parseGradeValue("VB+")).isEqualTo(-1.0);
        assertThat(GradeParser.parseGradeValue("V10/11")).isEqualTo(10.5);
        assertThat(GradeParser.parseGradeValue("V4-10")).isEqualTo(7.0);
        assertThat(GradeParser.parseGradeSystem("V4-5+")).isEqualTo(GradeSystem.UNKNOWN);
        assertThat(GradeParser.parseGradeSystem("V")).isEqualTo(GradeSystem.UNKNOWN);
    }

    @Test
    void distinguishesFontFromFrenchSportByLetterCase() {
        assertThat(GradeParser.parseGradeSystem("7B")).isEqualTo(GradeSystem.FONT);
        assertThat(GradeParser.parseGradeSystem("7b")).isEqualTo(GradeSystem.FRENCH_SPORT);
        assertThat(GradeParser.parseGradeValue("7B")).isCloseTo(7.3333, within(0.001));
        assertThat(GradeParser.parseGradeValue("7b")).isCloseTo(7.3333, within(0.001));
        assertThat(GradeParser.parseGradeValue("3a")).isEqualTo(3.0);
        assertThat(GradeParser.parseGradeValue("9C")).isCloseTo(9.6667, within(0.001));
        assertThat(GradeParser.parseGradeValue("9c+")).isCloseTo(9.8333, within(0.001));
        assertThat(GradeParser.parseGradeSystem("5a")).isEqualTo(GradeSystem.FRENCH_SPORT);
        assertThat(GradeParser.parseGradeValue("5a")).isEqualTo(5.0);
        assertThat(GradeParser.parseGradeSystem("2A")).isEqualTo(GradeSystem.UNKNOWN);
        assertThat(GradeParser.parseGradeSystem("6D")).isEqualTo(GradeSystem.UNKNOWN);
    }

    @Test
    void disciplineHintSplitsFontFromFrenchSport() {
        // Case-ambiguous letter grades follow the discipline when it's known.
        assertThat(GradeParser.parse("7a", Discipline.BOULDER).gradeSystem()).isEqualTo(GradeSystem.FONT);
        assertThat(GradeParser.parse("7A", Discipline.SPORT).gradeSystem()).isEqualTo(GradeSystem.FRENCH_SPORT);
        assertThat(GradeParser.parse("6b+", Discipline.TRAD).gradeSystem()).isEqualTo(GradeSystem.FRENCH_SPORT);
        assertThat(GradeParser.parse("7A", Discipline.BOULDER).gradeSystem()).isEqualTo(GradeSystem.FONT);

        // Without a usable hint, letter case still decides.
        assertThat(GradeParser.parse("7a", null).gradeSystem()).isEqualTo(GradeSystem.FRENCH_SPORT);
        assertThat(GradeParser.parse("7a", Discipline.UNKNOWN).gradeSystem()).isEqualTo(GradeSystem.FRENCH_SPORT);
        assertThat(GradeParser.parse("7A", Discipline.GYM).gradeSystem()).isEqualTo(GradeSystem.FONT);

        // The hint never rewrites systems that aren't case-ambiguous.
        assertThat(GradeParser.parse("V6", Discipline.SPORT).gradeSystem()).isEqualTo(GradeSystem.V_SCALE);
        assertThat(GradeParser.parse("5.11a", Discipline.BOULDER).gradeSystem()).isEqualTo(GradeSystem.YDS);
        assertThat(GradeParser.parse("rainbow", Discipline.BOULDER).gradeSystem()).isEqualTo(GradeSystem.UNKNOWN);
    }

    @Test
    void parsesIceMixedAidAndEGradeVariants() {
        assertThat(GradeParser.parseGradeSystem("ai3")).isEqualTo(GradeSystem.ICE_WI);
        assertThat(GradeParser.parseGradeValue("ai3")).isEqualTo(3.0);
        assertThat(GradeParser.parseGradeValue("WI5-")).isEqualTo(4.75);
        assertThat(GradeParser.parseGradeValue("WI4+ M5 300m")).isEqualTo(4.25);
        assertThat(GradeParser.parseGradeValue("M13")).isEqualTo(13.0);
        assertThat(GradeParser.parseGradeSystem("a2")).isEqualTo(GradeSystem.AID);
        assertThat(GradeParser.parseGradeValue("A0")).isEqualTo(0.0);
        assertThat(GradeParser.parseGradeValue("E11 7a")).isEqualTo(11.0);
        assertThat(GradeParser.parseGradeValue("e4 6b")).isEqualTo(4.0);
        assertThat(GradeParser.parseGradeValue("E2 5")).isEqualTo(2.0);
        assertThat(GradeParser.parseGradeSystem("E2 9d")).isEqualTo(GradeSystem.UNKNOWN);
    }

    @Test
    void rejectsMalformedGradesWithoutThrowing() {
        assertThat(GradeParser.parseGradeSystem("   \t\n ")).isEqualTo(GradeSystem.UNKNOWN);
        assertThat(GradeParser.parseGradeValue("   \t\n ")).isNull();
        assertThat(GradeParser.parseGradeSystem("5.")).isEqualTo(GradeSystem.UNKNOWN);
        assertThat(GradeParser.parseGradeSystem("5.10e")).isEqualTo(GradeSystem.UNKNOWN);
        assertThat(GradeParser.parseGradeSystem("W4")).isEqualTo(GradeSystem.UNKNOWN);
        assertThat(GradeParser.parseGradeSystem("10a")).isEqualTo(GradeSystem.UNKNOWN);
        assertThat(GradeParser.parseGradeSystem("VB-easy")).isEqualTo(GradeSystem.UNKNOWN);
        // The trailing letter is a Cyrillic 'а' lookalike, not ASCII.
        assertThat(GradeParser.parseGradeSystem("5.10а")).isEqualTo(GradeSystem.UNKNOWN);
        assertThat(GradeParser.parseGradeValue("5.10a" + "x".repeat(10000))).isNull();
    }

    @Test
    void ordersGradesWithinEachSystem() {
        assertLadderSorted("5.6", "5.9", "5.9+", "5.10-", "5.10a", "5.10b", "5.10+", "5.11a", "5.12a", "5.15d");
        assertLadderSorted("VB", "V0-", "V0", "V1", "V4-5", "V6", "V9+", "V10");
        assertLadderSorted("5A", "6A", "6A+", "6B", "6C", "6C+", "7A");
        assertLadderSorted("WI2", "WI4-", "WI4", "WI4+", "WI5-", "WI5");
    }

    private static void assertLadderSorted(String... grades) {
        List<Double> values = Stream.of(grades).map(GradeParser::parseGradeValue).toList();
        assertThat(values).doesNotContainNull().isSorted();
    }
}
