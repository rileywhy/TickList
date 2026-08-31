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
import org.apache.commons.csv.CSVParser;

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

    public Importer(TickRepository tickRepository, GradeMappingService gradeMappingService,
            ImportBatchRepository importBatchRepository, SkippedRowRepository skippedRowRepository) {
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

        CSVParser records = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get()
                .parse(reader);

        SourceApp source;
        List<String> headers = records.getHeaderNames();

        if (MountainProjectRow.isSupportedFile(headers)) {
            log.info("Importing Mountain Project CSV file: {}", filename);
            source = SourceApp.MOUNTAIN_PROJECT;
        } else if (KayaRow.isSupportedFile(headers)) {
            log.info("Importing Kaya CSV file: {}", filename);
            source = SourceApp.KAYA;
        } else {
            throw new IllegalArgumentException(
                    "CSV file does not match Mountain Project or Kaya format. Headers found: " + headers);
        }

        // Saved before the loop so ticks and skipped rows have an id to point at,
        // and so a batch exists even when every row fails.
        ImportBatch importBatch = new ImportBatch(user, source, filename);
        importBatchRepository.save(importBatch);

        int importedRows = 0;
        List<SkippedRowResponse> skippedRows = new ArrayList<>();

        for (CSVRecord record : records) {
            Tick tick;
            try {
                if (source == SourceApp.KAYA) {
                    tick = KayaRow.processKayaRow(record);
                } else {
                    tick = MountainProjectRow.processMTNProjectRow(record);
                }
            } catch (RuntimeException e) {
                long recordNumber = record.getRecordNumber();
                String reason = e.getMessage();
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

    private Path defaultCsvPath() {
        if (Files.exists(DEFAULT_CSV_PATH)) {
            return DEFAULT_CSV_PATH;
        }

        return BACKEND_DEFAULT_CSV_PATH;
    }

}
