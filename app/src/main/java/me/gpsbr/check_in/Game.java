package me.gpsbr.check_in;

public class Game {
    private String home;
    private String away;
    private String venue;
    private String date;
    private String tournament ;

    public Game (String home, String away, String venue, String date, String tournament) {
        super();
        this.home = home;
        this.away = away;
        this.venue = venue;
        this.date = date;
        this.tournament = tournament;
    }

    public String getHome() { return home; }
    public String getAway() { return away; }
    public String getVenue() { return venue; }
    public String getDate() { return date; }
    public String getTournament() { return tournament; }
}