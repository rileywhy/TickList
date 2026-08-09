package com.riley.ticklist;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;

/**
 * The only fields a client may supply when creating or updating a tick.
 * Server-owned fields (id, user, gradeValue, gradeMapping, rawGrade,
 * timestamps) have no representation here, so a request carrying them has
 * nowhere to land -- the boundary, not a guard, prevents mass assignment
 * against the Tick entity.
 */
public record TickRequest(
    @NotBlank String climbName,
    String climbId,
    String location,
    Discipline discipline,
    String grade,
    GradeSystem gradeSystem,
    SourceApp sourceApp,
    TickType tickType,
    String externalId,
    String sourceUrl,
    LocalDate tickDate,
    String style,
    RopeStyle ropeStyle,
    Integer attempts,
    String notes
) {}
