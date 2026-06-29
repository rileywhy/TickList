package com.riley.ticklist;
import java.util.EnumSet;
import java.util.Locale;

public class DisciplineParser {

    public static Discipline parsePrimaryDiscipline(String routeType, String rating) {
        EnumSet<Discipline> matches = parseDisciplines(routeType, rating);

        if (matches.contains(Discipline.BOULDER)) return Discipline.BOULDER;
        if (matches.contains(Discipline.SPORT)) return Discipline.SPORT;
        if (matches.contains(Discipline.TRAD)) return Discipline.TRAD;
        if (matches.contains(Discipline.ICE)) return Discipline.ICE;
        if (matches.contains(Discipline.MIXED)) return Discipline.MIXED;
        if (matches.contains(Discipline.AID)) return Discipline.AID;
        if (matches.contains(Discipline.GYM)) return Discipline.GYM;

        return Discipline.UNKNOWN;
    }

    public static EnumSet<Discipline> parseDisciplines(String routeType, String rating) {
        EnumSet<Discipline> disciplines = EnumSet.noneOf(Discipline.class);

        String text = ((routeType == null ? "" : routeType) + " " + (rating == null ? "" : rating))
            .toLowerCase(Locale.ROOT);

        if (text.contains("boulder") || text.matches(".*\\bv\\d.*") || text.contains("v-easy")) {
            disciplines.add(Discipline.BOULDER);
        }

        if (text.contains("sport")) {
            disciplines.add(Discipline.SPORT);
        }

        if (text.contains("trad")) {
            disciplines.add(Discipline.TRAD);
        }

        if (text.contains("ice") || text.matches(".*\\bwi\\d.*")) {
            disciplines.add(Discipline.ICE);
        }

        if (text.contains("mixed") || text.matches(".*\\bm\\d.*")) {
            disciplines.add(Discipline.MIXED);
        }

        if (text.contains("aid") || text.matches(".*\\b[ac]\\d.*")) {
            disciplines.add(Discipline.AID);
        }

        if (text.contains("gym") || text.contains("indoor")) {
            disciplines.add(Discipline.GYM);
        }

        return disciplines;
    }
}