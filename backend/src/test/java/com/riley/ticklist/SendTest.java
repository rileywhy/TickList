package com.riley.ticklist;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SendTest {
    @Test
    void savesGradeAsRawGradeWhenNoRawGradeIsProvided() {
        Send send = new Send();

        send.setGrade("V4");

        assertThat(send.getGrade()).isEqualTo("V4");
        assertThat(send.getRawGrade()).isEqualTo("V4");
    }

    @Test
    void keepsExplicitRawGradeWhenGradeIsSet() {
        Send send = new Send();

        send.setRawGrade("6A+");
        send.setGrade("6A+");

        assertThat(send.getRawGrade()).isEqualTo("6A+");
    }
}
