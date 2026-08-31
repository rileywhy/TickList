package com.riley.ticklist;

import java.util.Locale;

public class ImportHelpers {

    private ImportHelpers() {
        // Prevent instantiation
    }

    static Double parseOptionalDouble(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return null;
        }

        return Double.valueOf(rawValue.trim());
    }

    static Integer parseOptionalInteger(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return null;
        }

        return Integer.valueOf(rawValue.trim());
    }

    static TickType classifyTickType(String style, String leadStyle) {
        if (rawValueEquals(leadStyle, "Fell/Hung")) {
            return TickType.ATTEMPT;
        }

        if (rawValueEquals(style, "Attempt")) {
            return TickType.ATTEMPT;
        }

        if (rawValueEquals(style, "TR")) {
            return TickType.CLEAN_TR;
        }

        if ((style == null || style.trim().isEmpty()) && (leadStyle == null || leadStyle.trim().isEmpty())) {
            return TickType.UNKNOWN;
        }

        return TickType.SEND;
    }

    static boolean rawValueEquals(String rawValue, String expected) {
        return rawValue != null && rawValue.trim().equalsIgnoreCase(expected);
    }

    static RopeStyle parseRopeStyle(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return RopeStyle.UNKNOWN;
        }

        try {
            return RopeStyle.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            return RopeStyle.UNKNOWN;
        }
    }
    
}
