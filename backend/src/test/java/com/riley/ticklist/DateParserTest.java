package com.riley.ticklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class DateParserTest {
    private static final ZoneId DENVER = ZoneId.of("America/Denver");

    @Test
    void dateOnlyStringGivesDayAndNoInstant() {
        DateParser.ParsedDate parsed = DateParser.parse("2026-06-15", DENVER);

        assertThat(parsed.date()).isEqualTo(LocalDate.of(2026, 6, 15));
        // A bare day never gets an invented midnight.
        assertThat(parsed.instant()).isNull();
    }

    @Test
    void javaScriptStringKeepsTheExactInstant() {
        DateParser.ParsedDate parsed = DateParser.parse("Mon Aug 24 2026 23:43:39 GMT+0000 (GMT+00:00)", DENVER);

        assertThat(parsed.instant()).isEqualTo(Instant.parse("2026-08-24T23:43:39Z"));
        // 23:43 UTC is 17:43 in Denver, still the 24th.
        assertThat(parsed.date()).isEqualTo(LocalDate.of(2026, 8, 24));
    }

    @Test
    void lateEveningSessionLandsOnTheClimbersDayNotUtc() {
        // 03:10 UTC on the 25th is 21:10 on the 24th in Denver — the bug this fixes.
        DateParser.ParsedDate parsed = DateParser.parse("Tue Aug 25 2026 03:10:00 GMT+0000 (GMT+00:00)", DENVER);

        assertThat(parsed.date()).isEqualTo(LocalDate.of(2026, 8, 24));
        assertThat(parsed.instant()).isEqualTo(Instant.parse("2026-08-25T03:10:00Z"));
    }

    @Test
    void midnightUtcIsADateOnlyRowNotAnEveningSession() {
        // Row from inputs/mtnprojecttokayatoexport.csv: Kaya's stamp for an MP tick
        // that never had a time. Must stay on Sep 8, not slide to Sep 7 in Denver.
        DateParser.ParsedDate parsed = DateParser.parse("Wed Sep 08 2021 00:00:00 GMT+0000 (GMT+00:00)", DENVER);

        assertThat(parsed.date()).isEqualTo(LocalDate.of(2021, 9, 8));
        assertThat(parsed.instant()).isNull();
    }

    @Test
    void blankInputIsEmptyNotNull() {
        assertThat(DateParser.parse("  ", DENVER)).isSameAs(DateParser.ParsedDate.EMPTY);
        assertThat(DateParser.parse(null, DENVER).date()).isNull();
    }

    @Test
    void unsupportedFormatIsRejected() {
        assertThatThrownBy(() -> DateParser.parse("yesterday", DENVER))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("yesterday");
    }
}
