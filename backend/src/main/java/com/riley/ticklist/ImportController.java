package com.riley.ticklist;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
public class ImportController {
    private final Importer importer;

    public ImportController(Importer importer) {
        this.importer = importer;
    }

    @PostMapping(path = "/imports/mountain-project", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResponse importMountainProject(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal User user) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a CSV file to import.");
        }

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            Importer.ImportResult result = importer.importCSV(reader, user);
            return new ImportResponse(
                file.getOriginalFilename(),
                result.importedRows(),
                result.skippedRows()
            );
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage(), error);
        } catch (IOException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read CSV file.", error);
        }
    }

    public record ImportResponse(String filename, int importedRows, int skippedRows) {
    }
}
