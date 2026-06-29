package com.riley.ticklist;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
/* Class to import the csv files and parse the data
 */
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.Reader;

@Service
public class Importer {
    //function to import the csv file and parse the data
     private final SendRepository sendRepository;

    public Importer(SendRepository sendRepository) {
        this.sendRepository = sendRepository;
    }
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
            //String ratingCode = record.get("Rating Code");

            if (leadStyle.equals("Fell/Hung"))
            {
                // not actually a send
            }
            if (style.equals("TR"))
            {
                // not a true send,
            }

            // these "should" all be actual sends.
            Send send = new Send();
            
            send.setSendDate(DateParser.parse(date));
            send.setClimbName(route);
            send.setGrade(rating);
            send.setNotes(notes);
            send.setSourceUrl(url);
            send.setPitches(Integer.parseInt(pitches));
            send.setLocation(Location);
            send.setStars(Integer.parseInt(avgStars));
            send.setUserStars(Integer.parseInt(yourStars));
            send.setRopeSendStyle(RopeSendStyle.valueOf(style.toUpperCase()));
            send.setRopeSendStyle(RopeSendStyle.valueOf(leadStyle.toUpperCase()));
            
            Discipline discipline = DisciplineParser.parsePrimaryDiscipline(routeType, rating);
            send.setDiscipline(discipline);
            
            send.setPersonalGrade(yourRating);
            send.setClimbHeight(Integer.parseInt(length));

            sendRepository.save(send);

        }
        reader.close();
    }
}
