package com.riley.ticklist;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;

public final class DateParser {
    public record ParsedDate(LocalDate date, Instant instant) {
        static final ParsedDate EMPTY = new ParsedDate(null, null);
    }

    private static final List<DateTimeFormatter> LOCAL_DATE_FORMATTERS = List.of(
        DateTimeFormatter.ISO_LOCAL_DATE,
        formatter("M/d/uuuu"),
        formatter("M-d-uuuu"),
        formatter("MMM d, uuuu"),
        formatter("MMMM d, uuuu")
    );

    // JavaScript's Date.toString(): "Mon Aug 24 2026 23:43:39 GMT+0000". Kaya
    // exports every row in +0000 regardless of where the climb happened, which
    // is why the zone has to come from the caller rather than the string.
    private static final DateTimeFormatter JAVA_SCRIPT_DATE_FORMATTER =
        new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("EEE MMM d uuuu HH:mm:ss 'GMT'Z")
            .toFormatter(Locale.US)
            .withResolverStyle(ResolverStyle.STRICT);

    private DateParser() {
    }

    public static ParsedDate parse(String rawDate, ZoneId zone) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return ParsedDate.EMPTY;
        }

        String date = rawDate.trim();

        for (DateTimeFormatter formatter : LOCAL_DATE_FORMATTERS) {
            try {
                return new ParsedDate(LocalDate.parse(date, formatter), null);
            } catch (DateTimeParseException ignored) {
            }
        }

        try {
            // Drop the trailing "(GMT+00:00)" label; the numeric offset before it is what we parse.
            String dateWithoutTimeZoneLabel = date.replaceFirst("\\s*\\([^)]*\\)$", "");
            OffsetDateTime parsed = OffsetDateTime.parse(dateWithoutTimeZoneLabel, JAVA_SCRIPT_DATE_FORMATTER);
            // Kaya stamps date-only ticks (its MP/8a imports) as exactly midnight UTC.
            // That's a missing time, not a real one — shifting it into a zone would
            // file the tick a day early.
            if (parsed.toLocalTime().equals(LocalTime.MIDNIGHT)) {
                return new ParsedDate(parsed.toLocalDate(), null);
            }
            Instant instant = parsed.toInstant();
            return new ParsedDate(instant.atZone(zone).toLocalDate(), instant);
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
