package com.riley.ticklist;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;

public final class DateParser {
    private static final List<DateTimeFormatter> LOCAL_DATE_FORMATTERS = List.of(
        DateTimeFormatter.ISO_LOCAL_DATE,
        formatter("M/d/uuuu"),
        formatter("M-d-uuuu"),
        formatter("MMM d, uuuu"),
        formatter("MMMM d, uuuu")
    );

    private static final DateTimeFormatter JAVA_SCRIPT_DATE_FORMATTER =
        new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("EEE MMM d uuuu HH:mm:ss 'GMT'Z")
            .toFormatter(Locale.US)
            .withResolverStyle(ResolverStyle.STRICT);

    private DateParser() {
    }

    public static LocalDate parse(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return null;
        }

        String date = rawDate.trim();

        for (DateTimeFormatter formatter : LOCAL_DATE_FORMATTERS) {
            try {
                return LocalDate.parse(date, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        try {
            String dateWithoutTimeZoneLabel = date.replaceFirst("\\s*\\([^)]*\\)$", "");
            return OffsetDateTime.parse(dateWithoutTimeZoneLabel, JAVA_SCRIPT_DATE_FORMATTER).toLocalDate();
        } catch (DateTimeParseException ignored) {
        }

        throw new IllegalArgumentException("Unsupported date format: " + rawDate);
    }

    private static DateTimeFormatter formatter(String pattern) {
        return new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern(pattern)
            .toFormatter(Locale.US)
            .withResolverStyle(ResolverStyle.STRICT);
    }
}
