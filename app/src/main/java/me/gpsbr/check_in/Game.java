package me.gpsbr.check_in;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Game {
    private String id;
    private String home;
    private String away;
    private String venue;
    private String date;
    private String tournament;
    private List<Sector> sectors = new ArrayList<Sector>();

    public Game (String id, String home, String away, String venue, String date,
                 String tournament, List<Sector> sectorList) {

        super();
        this.id = id;
        this.home = home;
        this.away = away;
        this.venue = venue;
        this.date = date;
        this.tournament = tournament;
        this.sectors = sectorList;
    }

    public static class Sector {
        public String id;
        public String name;
        public String gates;
        public Sector(String id, String name, String gates) {
            this.id = id;
            this.name = App.Utils.capitalizeWords(name);
            this.gates = App.Utils.capitalizeWords(gates);
        }
    }

    public String getId() { return id; }
    public String getHome() { return home; }
    public String getAway() { return away; }
    public String getVenue() { return venue; }
    public String getDate() { return date; }
    public String getTournament() { return tournament; }
    public List<Sector> getSectors() { return sectors; }
    public Sector findSector(String sectorId) {
        for (Sector sector : sectors) {
            if (sector.id.equals(sectorId))
                return sector;
        }

        return null;
    }

    public boolean userCanCheckIn() {
        return false;
    }
}