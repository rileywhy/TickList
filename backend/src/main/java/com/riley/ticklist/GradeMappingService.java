package com.riley.ticklist;

import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class GradeMappingService {
    private final GradeMappingRepository gradeMappingRepository;

    public GradeMappingService(GradeMappingRepository gradeMappingRepository) {
        this.gradeMappingRepository = gradeMappingRepository;
    }

    public void applyGradeMapping(Tick tick) {
        if (tick == null) {
            return;
        }

        String cleanedGrade = clean(firstPresent(tick.getGrade(), tick.getRawGrade()));
        // The discipline disambiguates Font from French sport ("7a" on a boulder
        // is Font), so a lowercase boulder grade doesn't land on the sport ladder.
        GradeParser.ParsedGrade parsedGrade = GradeParser.parse(cleanedGrade, tick.getDiscipline());
        GradeSystem gradeSystem = resolveGradeSystem(tick.getGradeSystem(), parsedGrade.gradeSystem());

        if (cleanedGrade != null) {
            tick.setGrade(cleanedGrade);
            if (isBlank(tick.getRawGrade())) {
                tick.setRawGrade(cleanedGrade);
            }
        }
        tick.setGradeSystem(gradeSystem);

        // Look the mapping up from the same string we parsed the system from
        // (grade first, rawGrade only as a fallback). Preferring rawGrade here
        // would let an edited grade keep the old grade's difficultyScore.
        Optional<GradeMapping> gradeMapping = findMapping(
            gradeSystem,
            tick.getDiscipline(),
            cleanedGrade
        );

        tick.setGradeMapping(gradeMapping.orElse(null));
        tick.setGradeValue(gradeMapping
            .map(GradeMapping::getSystemOrder)
            .orElse(parsedGrade.gradeValue()));
    }

    public Optional<GradeMapping> findMapping(GradeSystem gradeSystem, Discipline tickDiscipline, String rawGrade) {
        if (gradeSystem == null || gradeSystem == GradeSystem.UNKNOWN || isBlank(rawGrade)) {
            return Optional.empty();
        }

        Discipline mappingDiscipline = mappingDisciplineFor(gradeSystem, tickDiscipline);
        String normalizedRawGrade = normalizeRawGrade(rawGrade, gradeSystem);
        if (mappingDiscipline == null || normalizedRawGrade == null) {
            return Optional.empty();
        }

        return gradeMappingRepository.findByGradeSystemAndDisciplineAndRawGradeAndActiveTrue(
            gradeSystem,
            mappingDiscipline,
            normalizedRawGrade
        );
    }

    private static GradeSystem resolveGradeSystem(GradeSystem requested, GradeSystem parsed) {
        if (requested != null && requested != GradeSystem.UNKNOWN) {
            return requested;
        }

        return parsed == null ? GradeSystem.UNKNOWN : parsed;
    }

    private static Discipline mappingDisciplineFor(GradeSystem gradeSystem, Discipline tickDiscipline) {
        if (tickDiscipline == Discipline.GYM) {
            return Discipline.GYM;
        }

        return switch (gradeSystem) {
            case V_SCALE, FONT -> Discipline.BOULDER;
            case YDS, FRENCH_SPORT -> Discipline.SPORT;
            case ICE_WI -> Discipline.ICE;
            case MIXED_M -> Discipline.MIXED;
            case AID -> Discipline.AID;
            case E_Grade -> Discipline.TRAD;
            case UNKNOWN -> tickDiscipline;
        };
    }

    private static String normalizeRawGrade(String rawGrade, GradeSystem gradeSystem) {
        String compactGrade = clean(rawGrade);
        if (compactGrade == null) {
            return null;
        }

        return switch (gradeSystem) {
            case V_SCALE -> normalizeVScaleGrade(firstToken(compactGrade));
            case FONT -> firstToken(compactGrade).toUpperCase(Locale.ROOT);
            case YDS, FRENCH_SPORT -> firstToken(compactGrade).toLowerCase(Locale.ROOT);
            case ICE_WI, MIXED_M, AID -> firstToken(compactGrade).toUpperCase(Locale.ROOT);
            case E_Grade -> normalizeEGrade(compactGrade);
            case UNKNOWN -> compactGrade;
        };
    }

    private static String normalizeVScaleGrade(String rawGrade) {
        String normalized = rawGrade.toUpperCase(Locale.ROOT);
        if (normalized.equals("VEASY") || normalized.equals("V-EASY")) {
            return "V-easy";
        }
        if (normalized.startsWith("VB")) {
            return "VB";
        }
        // The seed spells range grades with a dash (V4-5); accept the slash form too.
        return normalized.replaceAll("(\\d)/(\\d)", "$1-$2");
    }

    private static String normalizeEGrade(String rawGrade) {
        String[] parts = rawGrade.split("\\s+");
        if (parts.length == 0) {
            return rawGrade;
        }
        if (parts.length == 1) {
            return parts[0].toUpperCase(Locale.ROOT);
        }

        return parts[0].toUpperCase(Locale.ROOT) + " " + parts[1].toLowerCase(Locale.ROOT);
    }

    private static String firstToken(String rawGrade) {
        return rawGrade.split("\\s+")[0];
    }

    private static String clean(String rawValue) {
        if (isBlank(rawValue)) {
            return null;
        }

        return rawValue.trim().replaceAll("\\s+", " ");
    }

    private static String firstPresent(String first, String second) {
        if (!isBlank(first)) {
            return first;
        }

        return second;
    }

    private static boolean isBlank(String rawValue) {
        return rawValue == null || rawValue.trim().isEmpty();
    }
}
