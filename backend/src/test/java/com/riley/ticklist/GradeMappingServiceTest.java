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
