package com.riley.ticklist;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TickTest {
    @Test
    void savesGradeAsRawGradeWhenNoRawGradeIsProvided() {
        Tick tick = new Tick();

        tick.setGrade("V4");

        assertThat(tick.getGrade()).isEqualTo("V4");
        assertThat(tick.getRawGrade()).isEqualTo("V4");
    }

    @Test
    void keepsExplicitRawGradeWhenGradeIsSet() {
        Tick tick = new Tick();

        tick.setRawGrade("6A+");
        tick.setGrade("6A+");

        assertThat(tick.getRawGrade()).isEqualTo("6A+");
    }
}
