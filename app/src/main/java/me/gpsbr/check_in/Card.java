package me.gpsbr.check_in;

import java.util.HashMap;
import java.util.Map;

public class Card {
    protected String id;
    protected Map<String, String> checkin;

    public Card(String cardId) {
        super();

        this.id = cardId;
        this.checkin = new HashMap<String, String>();
    }

    public Boolean isCheckedIn(String gameId) {
        return checkin.containsKey(gameId);
    }

    public String checkin(String gameId, String sectorId) {
        return checkin.put(gameId, sectorId);
    }

    public String getCheckinSector(String gameId) {
        if (isCheckedIn(gameId)) return checkin.get(gameId);
        else return "";
    }

}
