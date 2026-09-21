package com.riley.ticklist;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ImportHelpersTest {
    @Test
    void fingerprintIsDeterministic() {
        assertThat(ImportHelpers.fingerprint("106289394", "2026-06-22", "Lead"))
            .isEqualTo(ImportHelpers.fingerprint("106289394", "2026-06-22", "Lead"))
            .hasSize(64);
    }

    @Test
    void fingerprintChangesWhenAnyPartChanges() {
        String base = ImportHelpers.fingerprint("106289394", "2026-06-22", "Lead");

        assertThat(ImportHelpers.fingerprint("106289394", "2026-06-23", "Lead")).isNotEqualTo(base);
        assertThat(ImportHelpers.fingerprint("106289394", "2026-06-22", "TR")).isNotEqualTo(base);
    }

    @Test
    void fingerprintTreatsNullAndBlankAndPaddedAlike() {
        // Kaya's gym column carries a trailing space; a missing column arrives as null.
        assertThat(ImportHelpers.fingerprint("a", null, "c"))
            .isEqualTo(ImportHelpers.fingerprint("a", "", "c"))
            .isEqualTo(ImportHelpers.fingerprint(" a ", "  ", "c "));
    }

    @Test
    void fingerprintKeepsSlotBoundaries() {
        // Without a separator "ab"+"c" and "a"+"bc" would hash identical.
        assertThat(ImportHelpers.fingerprint("ab", "c")).isNotEqualTo(ImportHelpers.fingerprint("a", "bc"));
    }
}
