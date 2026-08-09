package com.riley.ticklist;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The tick shape clients see. Chosen field by field (it must stay in step with
 * TickRecord in frontend/src/tickConfig.ts) instead of inherited from the
 * entity, so schema changes never leak into API responses by default.
 * Deliberately absent: user, gradeMapping, rawGrade, personalGrade, stars,
 * userStars, climbHeight, pitches.
 */
public record TickResponse(
    Long id,
    String climbName,
    String climbId,
    String location,
    Discipline discipline,
    TickType tickType,
    String grade,
    GradeSystem gradeSystem,
    Double gradeValue,
    Double difficultyScore,
    SourceApp sourceApp,
    String externalId,
    String sourceUrl,
    String style,
    RopeStyle ropeStyle,
    LocalDate tickDate,
    Integer attempts,
    String notes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static TickResponse from(Tick tick) {
        return new TickResponse(
            tick.getId(),
            tick.getClimbName(),
            tick.getClimbId(),
            tick.getLocation(),
            tick.getDiscipline(),
            tick.getTickType(),
            tick.getGrade(),
            tick.getGradeSystem(),
            tick.getGradeValue(),
            tick.getDifficultyScore(),
            tick.getSourceApp(),
            tick.getExternalId(),
            tick.getSourceUrl(),
            tick.getStyle(),
            tick.getRopeStyle(),
            tick.getTickDate(),
            tick.getAttempts(),
            tick.getNotes(),
            tick.getCreatedAt(),
            tick.getUpdatedAt()
        );
    }
}
