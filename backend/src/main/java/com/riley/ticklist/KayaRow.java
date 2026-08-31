package com.riley.ticklist;

import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class KayaRow {
    private KayaRow() {
        // Prevent instantiation
    }

    private static final Logger log = LoggerFactory.getLogger(KayaRow.class);

    static Tick processKayaRow(CSVRecord record) {
        String date = record.get("date").trim();
        String stiffness = record.get("stiffness").trim(); // unused still ig
        String yourStars = record.get("rating").trim();
        String style = record.get("ascent_type").trim();
        String attempts = record.get("attempts").trim();
        String rawGrade = record.get("grade").trim();
        String color = record.get("color").trim();
        String name = record.get("climb_name").trim();
        String gymName = record.get("gym").trim();
        String locationName = record.get("location").trim();
        String countryName = record.get("country").trim();

        Tick tick = new Tick();
        tick.setTickDate(DateParser.parse(date));
        tick.setClimbName(name);
        if (name.trim().isEmpty()) {
            tick.setClimbName(color + " " + rawGrade + " " + gymName);
        }

        Discipline discipline = DisciplineParser.parsePrimaryDiscipline(null, rawGrade);
        tick.setDiscipline(discipline);
        GradeParser.ParsedGrade parsedGrade = GradeParser.parse(rawGrade, discipline);
        tick.setRawGrade(parsedGrade.rawGrade());
        tick.setGrade(parsedGrade.rawGrade());
        tick.setGradeSystem(parsedGrade.gradeSystem());
        tick.setGradeValue(parsedGrade.gradeValue());
        // tick.setStiffness(stiffness);
        tick.setAttempts(ImportHelpers.parseOptionalInteger(attempts));

        String location;
        if (!locationName.isEmpty()) {
            location = countryName.isEmpty() ? locationName : locationName + ", " + countryName;
        } else {
            location = gymName.isEmpty() ? null : gymName;
        }
        tick.setLocation(location);

        tick.setUserStars(ImportHelpers.parseOptionalDouble(yourStars));
        tick.setStyle(style);
        RopeStyle ropeStyle = ImportHelpers.parseRopeStyle(style);
        tick.setRopeStyle(ropeStyle);
        if (ropeStyle == RopeStyle.UNKNOWN) {
            log.warn("Unrecognized Kaya ascent_type '{}' on row {} — importing as UNKNOWN", style,
                    record.getRecordNumber());
            tick.setTickType(TickType.UNKNOWN);
        } else {
            tick.setTickType(TickType.SEND);
        }
        tick.setSourceApp(SourceApp.KAYA);
        return tick;

    }

    static boolean isSupportedFile(List<String> headers) {
        return headers.contains("ascent_type") && headers.contains("gym");
    }
}
