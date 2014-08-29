package me.gpsbr.check_in;

import java.util.HashMap;
import java.util.Map;

public class Card {

    protected String id;
    protected String associationType;
    protected Map<String, Boolean> checkinAvailable;
    protected Map<String, Game.Sector> checkin;

    // Constructor
    public Card(String cardId, String associationType) {
        super();

        this.id = cardId;
        this.associationType = associationType;
        this.checkin = new HashMap<String, Game.Sector>();
    }

    // Boletters hehe
    public Boolean isCheckedIn(Game game) {
        return checkin.containsKey(game.getId());
    }

    // Getters
    public String getId() {
        return id;
    }
    public String getAssociationType() { return associationType; }
    public Game.Sector getCheckinSector(Game game) {
        if (isCheckedIn(game)) return checkin.get(game.getId());
        else return null;
    }

    // Setter
    public Game.Sector checkin(Game game, Game.Sector sector) {
        return checkin.put(game.getId(), sector);
    }

    public Boolean enableCheckin(Game game) {
        checkinAvailable.put(game.getId(), true);
        return true;
    }

}
