package me.gpsbr.check_in;

import java.util.HashMap;
import java.util.Map;

public class Card {
    protected String id;
    protected Map<String, Game.Sector> checkin;

    public Card(String cardId) {
        super();

        this.id = cardId;
        this.checkin = new HashMap<String, Game.Sector>();
    }

    public Boolean isCheckedIn(Game game) {
        return checkin.containsKey(game.getId());
    }

    public Game.Sector checkin(Game game, Game.Sector sector) {
        return checkin.put(game.getId(), sector);
    }

    public Game.Sector getCheckinSector(Game game) {
        if (isCheckedIn(game)) return checkin.get(game.getId());
        else return null;
    }

    public String getId() {
        return id;
    }

}
