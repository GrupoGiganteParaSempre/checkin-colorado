package me.gpsbr.check_in;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Model do jogo
 * Esta classe modela um jogo. Ele registra o ID e os dados do jogo  (como mandante, visitante,
 * local, data, hora, etc..), bem como os setores disponíveis e a possibilidade ou não de se
 * efetuar checkin.
 *
 * @author   Gustavo Seganfredo <gustavosf@gmail.com>
 * @since    1.0
 */
public class Game {
    private String id;
    private String home;
    private String away;
    private String venue;
    private String date;
    private String tournament;
    private List<Sector> sectors = new ArrayList<Sector>();
    private Boolean checkinOpen = false;

    public Game (String id, String home, String away, String venue, String date,
                 String tournament) {

        super();
        this.id = id;
        this.home = home;
        this.away = away;
        this.venue = venue;
        this.date = date;
        this.tournament = tournament;
    }

    /**
     * Model de um setor
     * Esta classe modela um setor. Ela registra o ID, nome e portões de acesso deste setor.
     *
     * @author   Gustavo Seganfredo <gustavosf@gmail.com>
     * @since    1.0
     */
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

    /**
     * Getters
     */
    public String getId() { return id; }
    public String getHome() { return home; }
    public String getAway() { return away; }
    public String getVenue() { return venue; }
    public String getDate() { return date; }
    public String getTournament() { return tournament; }
    public List<Sector> getSectors() { return sectors; }

    /**
     * Busca um setor do jogo com base no seu ID
     *
     * @param sectorId ID do setor
     * @return         Setor
     */
    public Sector findSector(String sectorId) {
        for (Sector sector : sectors) {
            if (sector.id.equals(sectorId))
                return sector;
        }

        return null;
    }

    /**
     * Verifica se o checkin está aberto para este jogo
     * @return true caso o checkin esteja aberto, false do contrário
     */
    public Boolean isCheckinOpen() {
        return checkinOpen;
    }

    /**
     * Libera o checkin para este jogo
     */
    public void enableCheckin() { enableCheckin(true); }

    /**
     * Altera o status do checkin para este jogo
     *
     * @param status true para liberar, false para bloquear
     */
    public void enableCheckin(Boolean status) { checkinOpen = status; }


    /**
     * Adiciona um setor para este jogo
     *
     * @param sector Setor a ser adicionado
     */
    public void addSector(Sector sector) {
        if (sector != null) sectors.add(sector);
    }

}