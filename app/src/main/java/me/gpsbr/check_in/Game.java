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
                 String tournament, Map<String, String> sectorList) {

        super();
        this.id = id;
        this.home = home;
        this.away = away;
        this.venue = venue;
        this.date = date;
        this.tournament = tournament;

        // Parse and include the venue sectors
        for (Map.Entry<String, String> sector : sectorList.entrySet()) {
            this.sectors.add(new Sector(sector.getKey(), sector.getValue()));
        }
    }

    public class Sector {
        public String id;
        public String name;
        public Sector(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public String getId() { return id; }
    public String getHome() { return home; }
    public String getAway() { return away; }
    public String getVenue() { return venue; }
    public String getDate() { return date; }
    public String getTournament() { return tournament; }
    public List<Sector> getSectors() { return sectors; }

    public boolean userCanCheckIn() {
        return false;
    }
}