package com.riley.ticklist;


import java.time.ZoneId;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVRecord;



public class MountainProjectRow {

    private MountainProjectRow() {
        // Prevent instantiation
    }
    
    static Tick processMTNProjectRow(CSVRecord record, ZoneId zone) {
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
        tick.setTickType(ImportHelpers.classifyTickType(style, leadStyle));
        DateParser.ParsedDate parsedDate = DateParser.parse(date, zone);
        tick.setTickDate(parsedDate.date());
        tick.setTickTimestamp(parsedDate.instant());
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
        tick.setPitches(ImportHelpers.parseOptionalInteger(pitches));
        tick.setLocation(Location);
        tick.setStars(ImportHelpers.parseOptionalDouble(avgStars));
        tick.setUserStars(ImportHelpers.parseOptionalDouble(yourStars));
        tick.setStyle(style);

        RopeStyle ropeStyle = ImportHelpers.parseRopeStyle(leadStyle);
        if (ropeStyle == RopeStyle.UNKNOWN) {
            // MP boulders carry ascent style in the Style column; "Send" is MP's
            // word for a worked clean ascent 
            ropeStyle = ImportHelpers.rawValueEquals(style, "Send") ? RopeStyle.REDPOINT : ImportHelpers.parseRopeStyle(style);
        }
        tick.setRopeStyle(ropeStyle);

        tick.setPersonalGrade(yourRating);
        tick.setClimbHeight(ImportHelpers.parseOptionalDouble(length));
        tick.setSourceApp(SourceApp.MOUNTAIN_PROJECT);
        // Identity = which route, which day, which style. Raw Style (not the parsed
        // enum) so a parser fix can't change every fingerprint. Order is frozen.
        tick.setExternalId(ImportHelpers.fingerprint(routeId(url, route), parsedDate.date().toString(), style));
        return tick;

    }

    private static final Pattern ROUTE_ID = Pattern.compile("/route/(\\d+)");

    // MP's numeric route id survives renames (and is OpenBeta's mp_id); the slug
    // after it doesn't. Match loosely so a path change upstream still finds it.
    static String routeId(String url, String routeName) {
        if (url != null) {
            Matcher matcher = ROUTE_ID.matcher(url);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return routeName;
    }

    static boolean isSupportedFile(List<String> headers) {
        return headers.contains("Pitches") && headers.contains("Rating Code") 
                && headers.contains("Route Type") && headers.contains("Lead Style");
    }
 
}
