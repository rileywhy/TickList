package com.riley.ticklist;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
/* Class to import the csv files and parse the data
 */

import java.io.FileReader;
import java.io.Reader;

public class Importer {
    //function to import the csv file and parse the data
    public void importCSV() throws Exception {
    Reader reader = new FileReader("ticks.csv");
        Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get()
                .parse(reader);

//Date	Route	Rating	Notes	URL	Pitches	Location	Avg Stars	Your Stars	Style	Lead Style	Route Type	Your Rating	Length	Rating Code

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
            String ratingCode = record.get("Rating Code");

            if (leadStyle.equals("Fell/Hung"))
            {
                // not actually a send
            }
            if (style.equals("TR"))
            {
                // also not a send
            }

            // these should all be actual sends.



        }
        reader.close();
    }
}
