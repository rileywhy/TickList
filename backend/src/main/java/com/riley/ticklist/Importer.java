package com.riley.ticklist;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class Importer {
    // function to import the csv file and parse the data
    private static final Path DEFAULT_CSV_PATH = Path.of("inputs", "ticks.csv");
    private static final Path BACKEND_DEFAULT_CSV_PATH = Path.of("..", "inputs", "ticks.csv");
    private final ImportBatchRepository importBatchRepository;
    private final TickRepository tickRepository;
    private final GradeMappingService gradeMappingService;
    private final SkippedRowRepository skippedRowRepository;
    private static final Logger log = LoggerFactory.getLogger(Importer.class);

    public Importer(TickRepository tickRepository, GradeMappingService gradeMappingService, ImportBatchRepository importBatchRepository, SkippedRowRepository skippedRowRepository) {
        this.tickRepository = tickRepository;
        this.gradeMappingService = gradeMappingService;
        this.importBatchRepository = importBatchRepository;
        this.skippedRowRepository = skippedRowRepository;
    }

    public ImportResult importCSV(User user) throws Exception {
        return importCSV(defaultCsvPath(), user);
    }

    ImportResult importCSV(Path csvPath, User user) throws Exception {
        try (Reader reader = Files.newBufferedReader(csvPath)) {
            return importCSV(reader, user, csvPath.getFileName().toString());
        }
    }

    public ImportResult importCSV(Reader reader, User user, String filename) throws IOException {
        if (user == null) {
            throw new IllegalArgumentException("Import requires an authenticated user.");
        }

        Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get()
                .parse(reader);

        // Date Route Rating Notes URL Pitches Location Avg Stars Your Stars Style Lead
        // Style Route Type Your Rating Length Rating Code
        // Saved before the loop so ticks and skipped rows have an id to point at,
        // and so a batch exists even when every row fails.
        ImportBatch importBatch = new ImportBatch(user, SourceApp.MOUNTAIN_PROJECT, filename);
        importBatchRepository.save(importBatch);

        int importedRows = 0;
        List<SkippedRowResponse> skippedRows = new ArrayList<>();

        for (CSVRecord record : records) {
            Tick tick;
            try {
                tick = processMTNProjectRow(record);
            } catch (RuntimeException e) {
                long recordNumber = record.getRecordNumber();
                String reason =  e.getMessage();
                String rawRow = record.toMap().toString();
                SkippedRow skippedRow = new SkippedRow(recordNumber, reason, rawRow, importBatch);

                skippedRowRepository.save(skippedRow);
                // The entity stays server-side; the response twin is what leaves
                // (serializing the entity would drag batch -> user -> password along).
                skippedRows.add(SkippedRowResponse.fromEntity(skippedRow));
                log.warn("Skipping row {}: {}. Raw row: {}", recordNumber, reason, rawRow);
                continue;
            }
            tick.setUser(user);
            tick.setImportBatch(importBatch);
            gradeMappingService.applyGradeMapping(tick);
            tickRepository.save(tick);
            importedRows++;
        }

        importBatch.setSuccessfulRows(importedRows);
        importBatch.setFailedRows(skippedRows.size());
        importBatchRepository.save(importBatch);

        return new ImportResult(importedRows, skippedRows);
    }

    public record ImportResult(int importedRows, List<SkippedRowResponse> skippedRows) {
    }

    private static Tick processMTNProjectRow(CSVRecord record) {
        String date = record.get("Date");
        String route = record.get("Route");
        String grade = record.get("Rating");
        String notes = record.get("Notes");
        String url = record.get("URL");
        String pitches = record.get("Pitches");
        String Location = record.get("Location");
        String avgStars = record.get("Avg Stars");
        String yourStars = record.get("Your Stars");
        String style = record.get("Style");
        String leadStyle = record.get("Lead Style");
        String routeType = record.get("Route Type");
        String yourRating = record.get("Your Rating");
        String length = record.get("Length");
        // String ratingCode = record.get("Rating Code");

        Tick tick = new Tick();
        tick.setTickType(classifyTickType(style, leadStyle));
        tick.setTickDate(DateParser.parse(date));
        tick.setClimbName(route);
        // Resolve the discipline from the CSV's Route Type first so the grade
        // parse can use it to split Font from French sport ("7a" on a Boulder
        // row is Font), instead of trusting letter case.
        Discipline discipline = DisciplineParser.parsePrimaryDiscipline(routeType, grade);
        tick.setDiscipline(discipline);
        GradeParser.ParsedGrade parsedGrade = GradeParser.parse(grade, discipline);
        tick.setRawGrade(parsedGrade.rawGrade());
        tick.setGrade(parsedGrade.rawGrade());
        tick.setGradeSystem(parsedGrade.gradeSystem());
        tick.setGradeValue(parsedGrade.gradeValue());
        tick.setNotes(notes);
        tick.setSourceUrl(url);
        tick.setPitches(parseOptionalInteger(pitches));
        tick.setLocation(Location);
        tick.setStars(parseOptionalDouble(avgStars));
        tick.setUserStars(parseOptionalDouble(yourStars));
        tick.setStyle(style);
        tick.setRopeStyle(parseRopeStyle(leadStyle));

        tick.setPersonalGrade(yourRating);
        tick.setClimbHeight(parseOptionalDouble(length));
        tick.setSourceApp(SourceApp.MOUNTAIN_PROJECT);
        return tick;

    }

    private Path defaultCsvPath() {
        if (Files.exists(DEFAULT_CSV_PATH)) {
            return DEFAULT_CSV_PATH;
        }

        return BACKEND_DEFAULT_CSV_PATH;
    }

    private static Double parseOptionalDouble(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return null;
        }

        return Double.valueOf(rawValue.trim());
    }

    private static Integer parseOptionalInteger(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return null;
        }

        return Integer.valueOf(rawValue.trim());
    }

    private static TickType classifyTickType(String style, String leadStyle) {
        if (rawValueEquals(leadStyle, "Fell/Hung")) {
            return TickType.ATTEMPT;
        }

        if (rawValueEquals(style, "TR")) {
            return TickType.CLEAN_TR;
        }

        if ((style == null || style.trim().isEmpty()) && (leadStyle == null || leadStyle.trim().isEmpty())) {
            return TickType.UNKNOWN;
        }

        return TickType.SEND;
    }

    private static boolean rawValueEquals(String rawValue, String expected) {
        return rawValue != null && rawValue.trim().equalsIgnoreCase(expected);
    }

    private static RopeStyle parseRopeStyle(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return RopeStyle.UNKNOWN;
        }

        try {
            return RopeStyle.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            return RopeStyle.UNKNOWN;
        }
    }
}
