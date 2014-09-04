package me.gpsbr.check_in;

import java.util.HashMap;
import java.util.Map;

/**
 * Model do cartão
 * Esta classe modela o cartão (ou carteirinha) de um usuário. Ela registra o número do cartão,
 * a modalidade ao qual ela pertence, e os jogos/checkins associados a ela.
 *
 * @author   Gustavo Seganfredo <gustavosf@gmail.com>
 * @since    1.0
 */
public class Card {

    protected String id;
    protected String associationType;
    protected Map<String, Boolean> checkinAvailable;
    protected Map<String, Game.Sector> checkin;

    public Card(String cardId, String associationType) {
        super();

        this.id = cardId;
        this.associationType = associationType;
        this.checkin = new HashMap<String, Game.Sector>();
    }

    /**
     * Retorna se um cartão fez checkin para um determinado jogo
     *
     * @param game Objeto do jogo alvo
     * @return     true se o cartão fez checkin para o jogo, do contrário falso
     */
    public Boolean isCheckedIn(Game game) {
        return checkin.containsKey(game.getId());
    }

    // Getters
    public String getId() {
        return id;
    }
    public String getAssociationType() { return associationType; }

    /**
     * Retorna o setor para o qual foi feito checkin
     *
     * @param game Jogo alvo
     * @return     Setor para o qual foi feito checkin, do contrário null
     */
    public Game.Sector getCheckinSector(Game game) {
        if (isCheckedIn(game)) return checkin.get(game.getId());
        else return null;
    }

    /**
     * Seta o checkin para um determinado jogo
     *
     * @param game   Jogo alvo
     * @param sector Setor do checkin
     * @return       Setor do checkin
     */
    public Game.Sector checkin(Game game, Game.Sector sector) {
        return checkin.put(game.getId(), sector);
    }

    /**
     * Libera checkin para um determinado jogo para este cartão
     *
     * @param game Jogo alvo
     */
    public void enableCheckin(Game game) {
        checkinAvailable.put(game.getId(), true);
    }

    /**
     * Desfaz um checkin em um determinado jogo, vulgo check-out
     *
     * @param game Jogo alvo
     */
    public void checkout(Game game) {
        checkin.remove(game.getId());
    }
}
