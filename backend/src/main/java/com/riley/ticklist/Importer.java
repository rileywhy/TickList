package com.riley.ticklist;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
/* Class to import the csv files and parse the data
 */
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Service
public class Importer {
    //function to import the csv file and parse the data
    private static final Path DEFAULT_CSV_PATH = Path.of("inputs", "ticks.csv");
    private static final Path BACKEND_DEFAULT_CSV_PATH = Path.of("..", "inputs", "ticks.csv");
     private final SendRepository sendRepository;

    public Importer(SendRepository sendRepository) {
        this.sendRepository = sendRepository;
    }
    public void importCSV() throws Exception {
        importCSV(defaultCsvPath());
    }

    ImportResult importCSV(Path csvPath) throws Exception {
        try (Reader reader = Files.newBufferedReader(csvPath)) {
            return importCSV(reader);
        }
    }

    public ImportResult importCSV(Reader reader) throws IOException {
        Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get()
                .parse(reader);

//Date	Route	Rating	Notes	URL	Pitches	Location	Avg Stars	Your Stars	Style	Lead Style	Route Type	Your Rating	Length	Rating Code

        int importedRows = 0;
        int skippedRows = 0;

        for (CSVRecord record : records) {
            
            
            String date = record.get("Date");
            String route = record.get("Route");
            String rating = record.get("Rating");
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
            //String ratingCode = record.get("Rating Code");

            if (leadStyle.equals("Fell/Hung"))
            {
               System.out.println("Skipping send for route " + route + " on date " + date + " because lead style is Fell/Hung");
               skippedRows++;
            }
            else if (style.equals("TR"))
            {
                // not a true send,
                skippedRows++;
            }
            else {
            // these "should" all be actual sends.
            Send send = new Send();
            
            send.setSendDate(DateParser.parse(date));
            send.setClimbName(route);
            GradeParser.ParsedGrade parsedGrade = GradeParser.parse(rating);
            send.setRawGrade(parsedGrade.rawGrade());
            send.setGrade(parsedGrade.rawGrade());
            send.setGradeSystem(parsedGrade.gradeSystem());
            send.setNotes(notes);
            send.setSourceUrl(url);
            send.setPitches(parseOptionalInteger(pitches));
            send.setLocation(Location);
            send.setStars(parseOptionalDouble(avgStars));
            send.setUserStars(parseOptionalDouble(yourStars));
            send.setStyle((style));
            send.setRopeSendStyle(parseRopeSendStyle(leadStyle));
            
            Discipline discipline = DisciplineParser.parsePrimaryDiscipline(routeType, rating);
            send.setDiscipline(discipline);
            
            send.setPersonalGrade(yourRating);
            send.setClimbHeight(parseOptionalDouble(length));
            send.setSourceApp(SourceApp.MOUNTAIN_PROJECT);

            sendRepository.save(send);
            importedRows++;

        }
    }

        return new ImportResult(importedRows, skippedRows);
    }

    public record ImportResult(int importedRows, int skippedRows) {
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

    private static RopeSendStyle parseRopeSendStyle(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return RopeSendStyle.UNKNOWN;
        }

        try {
            return RopeSendStyle.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            return RopeSendStyle.UNKNOWN;
        }
    }
}
