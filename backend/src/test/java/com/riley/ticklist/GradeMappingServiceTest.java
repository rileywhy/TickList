package com.riley.ticklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for the grade-string normalization / lookup-key logic in
 * {@link GradeMappingService}. The repository is mocked so these assert exactly
 * which (gradeSystem, discipline, rawGrade) key the service asks for.
 */
class GradeMappingServiceTest {

    private GradeMappingRepository repository;
    private GradeMappingService service;

    @BeforeEach
    void setUp() {
        repository = mock(GradeMappingRepository.class);
        when(repository.findByGradeSystemAndDisciplineAndRawGradeAndActiveTrue(any(), any(), any()))
            .thenReturn(Optional.empty());
        service = new GradeMappingService(repository);
    }

    @Test
    void mappingIsLookedUpFromGradeNotStaleRawGrade() {
        // The tick's grade was edited to 5.11d but the provenance rawGrade still
        // reads 5.11a. The mapping must follow the current grade, not rawGrade.
        Tick tick = new Tick();
        tick.setGrade("5.11d");
        tick.setRawGrade("5.11a");
        tick.setGradeSystem(GradeSystem.YDS);
        tick.setDiscipline(Discipline.SPORT);

        service.applyGradeMapping(tick);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(repository).findByGradeSystemAndDisciplineAndRawGradeAndActiveTrue(
            eq(GradeSystem.YDS), eq(Discipline.SPORT), key.capture());
        assertThat(key.getValue()).isEqualTo("5.11d");
    }

    @Test
    void lowercaseBoulderGradeResolvesToTheFontLadderNotFrenchSport() {
        // 8a.nu (and plenty of humans) write Font grades lowercase. The tick's
        // discipline, not letter case, must pick the ladder — otherwise this
        // boulder scores as a 7a sport route, ~5 letter grades too low.
        Tick tick = new Tick();
        tick.setGrade("7a");
        tick.setGradeSystem(GradeSystem.UNKNOWN); // the form default
        tick.setDiscipline(Discipline.BOULDER);

        service.applyGradeMapping(tick);

        assertThat(tick.getGradeSystem()).isEqualTo(GradeSystem.FONT);
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(repository).findByGradeSystemAndDisciplineAndRawGradeAndActiveTrue(
            eq(GradeSystem.FONT), eq(Discipline.BOULDER), key.capture());
        assertThat(key.getValue()).isEqualTo("7A");
    }

    @Test
    void uppercaseGradeOnASportTickResolvesToTheFrenchSportLadder() {
        Tick tick = new Tick();
        tick.setGrade("7A");
        tick.setGradeSystem(GradeSystem.UNKNOWN);
        tick.setDiscipline(Discipline.SPORT);

        service.applyGradeMapping(tick);

        assertThat(tick.getGradeSystem()).isEqualTo(GradeSystem.FRENCH_SPORT);
        verify(repository).findByGradeSystemAndDisciplineAndRawGradeAndActiveTrue(
            eq(GradeSystem.FRENCH_SPORT), eq(Discipline.SPORT), eq("7a"));
    }

    @Test
    void unmappedGradeLeavesGradeValueNull() {
        // H1: when no seed row matches, gradeValue must be null -- not the
        // GradeParser's arithmetic, which is a different scale than systemOrder
        // (67 seed rows disagree). A null sorts honestly; a foreign-scale number
        // silently corrupts every comparison it appears in.
        Tick tick = new Tick();
        tick.setGrade("M5");
        tick.setGradeSystem(GradeSystem.UNKNOWN);
        tick.setDiscipline(Discipline.GYM); // known seed gap: (MIXED_M, GYM) has no rows

        service.applyGradeMapping(tick);

        assertThat(tick.getGradeMapping()).isNull();
        assertThat(tick.getGradeValue()).isNull();
    }

    @Test
    void mappedGradeGetsTheSeedRowsSystemOrderExactly() {
        GradeMapping seedRow = new GradeMapping();
        seedRow.setSystemOrder(4.0); // the curated axis value for Font 4B
        when(repository.findByGradeSystemAndDisciplineAndRawGradeAndActiveTrue(
                eq(GradeSystem.FONT), eq(Discipline.BOULDER), eq("4B")))
            .thenReturn(Optional.of(seedRow));

        Tick tick = new Tick();
        tick.setGrade("4B");
        tick.setGradeSystem(GradeSystem.FONT);
        tick.setDiscipline(Discipline.BOULDER);

        service.applyGradeMapping(tick);

        // Exactly the seed's number -- the parser would say 4.333 for the same string.
        assertThat(tick.getGradeMapping()).isSameAs(seedRow);
        assertThat(tick.getGradeValue()).isEqualTo(4.0);
    }

    @Test
    void vScaleSlashRangeNormalizesToTheDashFormTheSeedUses() {
        Tick tick = new Tick();
        tick.setGrade("V4/5");
        tick.setGradeSystem(GradeSystem.V_SCALE);
        tick.setDiscipline(Discipline.BOULDER);

        service.applyGradeMapping(tick);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(repository).findByGradeSystemAndDisciplineAndRawGradeAndActiveTrue(
            eq(GradeSystem.V_SCALE), eq(Discipline.BOULDER), key.capture());
        assertThat(key.getValue()).isEqualTo("V4-5");
    }
}
