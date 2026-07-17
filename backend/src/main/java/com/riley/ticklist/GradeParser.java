package com.riley.ticklist;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GradeParser {
    private static final Pattern V_SCALE = Pattern.compile("^V(?:-?EASY|B|\\d{1,2})(?:[-+]|[-/]\\d{1,2})?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FONT = Pattern.compile("^[3-9][ABC]\\+?$");
    private static final Pattern YDS = Pattern.compile("^5\\.(?:[0-9]|1[0-5])(?:[abcd])?(?:[+-])?(?:/[abcd])?(?:\\s.*)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FRENCH_SPORT = Pattern.compile("^[3-9][abc]\\+?$");
    private static final Pattern ICE_WI = Pattern.compile("^(?:WI|AI)\\d{1,2}(?:[+-])?(?:\\s.*)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MIXED_M = Pattern.compile("^M\\d{1,2}(?:[+-])?(?:\\s.*)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern AID = Pattern.compile("^[AC]\\d{1,2}(?:[+-])?(?:\\s.*)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern E_GRADE = Pattern.compile("^E\\d+\\s+\\d[abc]?$", Pattern.CASE_INSENSITIVE);

    private static final Pattern V_SCALE_VALUE = Pattern.compile("^V(?:(-?EASY|B)|(\\d{1,2}))(?:([+-])|[-/](\\d{1,2}))?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern LETTER_SCALE_VALUE = Pattern.compile("^([3-9])([A-C])(\\+)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern YDS_VALUE = Pattern.compile("^5\\.(\\d{1,2})([A-D])?([+-])?(?:/([A-D]))?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMBER_SUFFIX_VALUE = Pattern.compile("^(?:WI|AI|M|A|C)(\\d{1,2})([+-])?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern E_GRADE_VALUE = Pattern.compile("^E(\\d+)$", Pattern.CASE_INSENSITIVE);

    private GradeParser() {
    }

    public static ParsedGrade parse(String rawGrade) {
        return parse(rawGrade, null);
    }

    public static ParsedGrade parse(String rawGrade, Discipline disciplineHint) {
        String cleanedGrade = clean(rawGrade);
        GradeSystem gradeSystem = parseGradeSystem(cleanedGrade, disciplineHint);
        return new ParsedGrade(cleanedGrade, gradeSystem, parseGradeValue(cleanedGrade, gradeSystem));
    }

    public static GradeSystem parseGradeSystem(String rawGrade, Discipline disciplineHint) {
        return disambiguateFontFromFrench(parseGradeSystem(rawGrade), disciplineHint);
    }

    // Font and French sport share the same written shape ("7A" vs "7a") and the
    // regexes tell them apart only by letter case, which real data doesn't respect
    // (8a.nu exports boulder grades lowercase). When the discipline is known it
    // decides between the two ladders; case remains the fallback signal.
    private static GradeSystem disambiguateFontFromFrench(GradeSystem parsed, Discipline disciplineHint) {
        if (parsed != GradeSystem.FONT && parsed != GradeSystem.FRENCH_SPORT) {
            return parsed;
        }

        if (disciplineHint == Discipline.BOULDER) {
            return GradeSystem.FONT;
        }

        if (disciplineHint == Discipline.SPORT || disciplineHint == Discipline.TRAD) {
            return GradeSystem.FRENCH_SPORT;
        }

        return parsed;
    }

    public static GradeSystem parseGradeSystem(String rawGrade) {
        String cleanedGrade = clean(rawGrade);

        if (cleanedGrade == null) {
            return GradeSystem.UNKNOWN;
        }

        if (V_SCALE.matcher(cleanedGrade).matches()) {
            return GradeSystem.V_SCALE;
        }

        if (FONT.matcher(cleanedGrade).matches()) {
            return GradeSystem.FONT;
        }

        if (YDS.matcher(cleanedGrade).matches()) {
            return GradeSystem.YDS;
        }

        if (FRENCH_SPORT.matcher(cleanedGrade).matches()) {
            return GradeSystem.FRENCH_SPORT;
        }

        if (ICE_WI.matcher(cleanedGrade).matches()) {
            return GradeSystem.ICE_WI;
        }

        if (MIXED_M.matcher(cleanedGrade).matches()) {
            return GradeSystem.MIXED_M;
        }

        if (AID.matcher(cleanedGrade).matches()) {
            return GradeSystem.AID;
        }

        if (E_GRADE.matcher(cleanedGrade).matches()) {
            return GradeSystem.E_Grade;
        }

        return GradeSystem.UNKNOWN;
    }

    public static Double parseGradeValue(String rawGrade) {
        String cleanedGrade = clean(rawGrade);
        return parseGradeValue(cleanedGrade, parseGradeSystem(cleanedGrade));
    }

    // Numeric value for sorting and comparing grades within one system.
    // Values are only comparable between grades of the same GradeSystem.
    private static Double parseGradeValue(String cleanedGrade, GradeSystem gradeSystem) {
        if (cleanedGrade == null) {
            return null;
        }

        String grade = cleanedGrade.split("\\s+")[0];

        return switch (gradeSystem) {
            case V_SCALE -> parseVScaleValue(grade);
            case FONT, FRENCH_SPORT -> parseLetterScaleValue(grade);
            case YDS -> parseYdsValue(grade);
            case ICE_WI, MIXED_M, AID -> parseNumberSuffixValue(grade);
            case E_Grade -> parseEGradeValue(grade);
            case UNKNOWN -> null;
        };
    }

    private static Double parseVScaleValue(String grade) {
        Matcher matcher = V_SCALE_VALUE.matcher(grade);
        if (!matcher.matches()) {
            return null;
        }

        // VB and V-easy sit just below V0.
        if (matcher.group(1) != null) {
            return -1.0;
        }

        double value = Double.parseDouble(matcher.group(2));

        // Range grades like V4-5 or V4/5 land on the midpoint.
        if (matcher.group(4) != null) {
            return (value + Double.parseDouble(matcher.group(4))) / 2.0;
        }

        return value + signOffset(matcher.group(3), 0.25);
    }

    // Font and French sport: letters are thirds within the number, "+" is a half step further.
    private static Double parseLetterScaleValue(String grade) {
        Matcher matcher = LETTER_SCALE_VALUE.matcher(grade);
        if (!matcher.matches()) {
            return null;
        }

        double value = Double.parseDouble(matcher.group(1)) + letterIndex(matcher.group(2)) / 3.0;

        if (matcher.group(3) != null) {
            value += 1.0 / 6.0;
        }

        return value;
    }

    // YDS: letters are quarters within the number (a=0, b=.25, c=.5, d=.75),
    // slash grades land on the midpoint, "5.10+" reads as roughly 5.10d,
    // and "5.10" or "5.10-" sit at the bottom of the number.
    private static Double parseYdsValue(String grade) {
        Matcher matcher = YDS_VALUE.matcher(grade);
        if (!matcher.matches()) {
            return null;
        }

        double value = Double.parseDouble(matcher.group(1));
        Double letterOffset = matcher.group(2) == null ? null : letterIndex(matcher.group(2)) * 0.25;

        if (matcher.group(4) != null) {
            double slashOffset = letterIndex(matcher.group(4)) * 0.25;
            letterOffset = letterOffset == null ? slashOffset : (letterOffset + slashOffset) / 2.0;
        }

        if (letterOffset == null) {
            return "+".equals(matcher.group(3)) ? value + 0.75 : value;
        }

        return value + letterOffset + signOffset(matcher.group(3), 0.1);
    }

    private static Double parseNumberSuffixValue(String grade) {
        Matcher matcher = NUMBER_SUFFIX_VALUE.matcher(grade);
        if (!matcher.matches()) {
            return null;
        }

        return Double.parseDouble(matcher.group(1)) + signOffset(matcher.group(2), 0.25);
    }

    // British trad grades like "E2 5c" order by the adjectival E number.
    private static Double parseEGradeValue(String grade) {
        Matcher matcher = E_GRADE_VALUE.matcher(grade);
        if (!matcher.matches()) {
            return null;
        }

        return Double.parseDouble(matcher.group(1));
    }

    private static double signOffset(String sign, double step) {
        if ("+".equals(sign)) {
            return step;
        }

        if ("-".equals(sign)) {
            return -step;
        }

        return 0.0;
    }

    private static double letterIndex(String letter) {
        return Character.toUpperCase(letter.charAt(0)) - 'A';
    }

    private static String clean(String rawGrade) {
        if (rawGrade == null || rawGrade.trim().isEmpty()) {
            return null;
        }

        return rawGrade.trim();
    }

    public record ParsedGrade(String rawGrade, GradeSystem gradeSystem, Double gradeValue) {
    }
}
