package com.riley.ticklist;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.security.MessageDigest;

public class ImportHelpers {

    private ImportHelpers() {
        // Prevent instantiation
    }

    // Varargs so MP can pass its 3 identity fields and Kaya its 6. A blank field
    // still takes its slot (as ""), so the same source always hashes the same shape.
    static String fingerprint(String... parts)
    {
        String[] cleaned = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            cleaned[i] = parts[i] == null ? "" : parts[i].trim();
        }
        String joined = String.join("|", cleaned);

        try {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] bytes = digest.digest(joined.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(bytes);
} catch (NoSuchAlgorithmException e) {
    throw new IllegalStateException("SHA-256 missing from this JVM", e);
}

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
