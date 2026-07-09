package com.riley.ticklist;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class GradeMappingSeeder implements ApplicationRunner {
    private final GradeMappingRepository gradeMappingRepository;

    public GradeMappingSeeder(GradeMappingRepository gradeMappingRepository) {
        this.gradeMappingRepository = gradeMappingRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ClassPathResource resource = new ClassPathResource("grade-mappings.csv");

        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get()
                .parse(reader);

            List<GradeMapping> mappings = new ArrayList<>();
            for (CSVRecord record : records) {
                GradeSystem gradeSystem = GradeSystem.valueOf(record.get("gradeSystem"));
                Discipline discipline = Discipline.valueOf(record.get("discipline"));
                String rawGrade = record.get("rawGrade");

                GradeMapping mapping = gradeMappingRepository
                    .findByGradeSystemAndDisciplineAndRawGrade(gradeSystem, discipline, rawGrade)
                    .orElseGet(GradeMapping::new);

                mapping.setGradeSystem(gradeSystem);
                mapping.setDiscipline(discipline);
                mapping.setRawGrade(rawGrade);
                mapping.setSystemOrder(parseOptionalDouble(record.get("systemOrder")));
                mapping.setDifficultyScore(parseOptionalDouble(record.get("difficultyScore")));
                mapping.setConfidence(parseOptionalDouble(record.get("confidence")));
                mapping.setNote(record.get("note"));
                mapping.setActive(true);
                mappings.add(mapping);
            }

            gradeMappingRepository.saveAll(mappings);
        }
    }

    private static Double parseOptionalDouble(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return null;
        }

        return Double.valueOf(rawValue.trim());
    }
}
