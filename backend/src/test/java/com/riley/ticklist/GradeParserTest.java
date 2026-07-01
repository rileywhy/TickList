package com.riley.ticklist;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GradeParserTest {
    @Test
    void parsesYdsGrades() {
        GradeParser.ParsedGrade parsedGrade = GradeParser.parse(" 5.10a ");

        assertThat(parsedGrade.rawGrade()).isEqualTo("5.10a");
        assertThat(parsedGrade.gradeSystem()).isEqualTo(GradeSystem.YDS);
    }

    @Test
    void parsesBoulderingGradeSystems() {
        assertThat(GradeParser.parseGradeSystem("V6")).isEqualTo(GradeSystem.V_SCALE);
        assertThat(GradeParser.parseGradeSystem("6A+")).isEqualTo(GradeSystem.FONT);
    }

    @Test
    void parsesSportIceMixedAidAndEGradeSystems() {
        assertThat(GradeParser.parseGradeSystem("6a+")).isEqualTo(GradeSystem.FRENCH_SPORT);
        assertThat(GradeParser.parseGradeSystem("WI4+")).isEqualTo(GradeSystem.ICE_WI);
        assertThat(GradeParser.parseGradeSystem("M7")).isEqualTo(GradeSystem.MIXED_M);
        assertThat(GradeParser.parseGradeSystem("C2")).isEqualTo(GradeSystem.AID);
        assertThat(GradeParser.parseGradeSystem("E2 5c")).isEqualTo(GradeSystem.E_Grade);
    }

    @Test
    void returnsUnknownForBlankOrUnmatchedGrades() {
        assertThat(GradeParser.parse(null).rawGrade()).isNull();
        assertThat(GradeParser.parseGradeSystem("")).isEqualTo(GradeSystem.UNKNOWN);
        assertThat(GradeParser.parseGradeSystem("rainbow")).isEqualTo(GradeSystem.UNKNOWN);
    }
}
