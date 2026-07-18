package com.riley.ticklist;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeMappingRepository extends JpaRepository<GradeMapping, Long> {
    Optional<GradeMapping> findByGradeSystemAndDisciplineAndRawGrade(
        GradeSystem gradeSystem,
        Discipline discipline,
        String rawGrade
    );

    Optional<GradeMapping> findByGradeSystemAndDisciplineAndRawGradeAndActiveTrue(
        GradeSystem gradeSystem,
        Discipline discipline,
        String rawGrade
    );
}
