package com.riley.ticklist;

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

    private GradeParser() {
    }

    public static ParsedGrade parse(String rawGrade) {
        String cleanedGrade = clean(rawGrade);
        return new ParsedGrade(cleanedGrade, parseGradeSystem(cleanedGrade));
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

    private static String clean(String rawGrade) {
        if (rawGrade == null || rawGrade.trim().isEmpty()) {
            return null;
        }

        return rawGrade.trim();
    }

    public record ParsedGrade(String rawGrade, GradeSystem gradeSystem) {
    }
}
