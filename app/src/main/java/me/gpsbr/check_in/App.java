package me.gpsbr.check_in;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.parse.Parse;
import com.parse.PushService;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Classe da aplicação.
 * Consiste em basicamente toda a lógica de login, checkin, checkuot e armazenamento de dados do
 * aplicativo.
 *
 * @author   Gustavo Seganfredo <gustavosf@gmail.com>
 * @since    1.0
 */
public class App extends Application {

    protected static Application app;
    protected static Context context;
    protected static SharedPreferences data;
    protected static OkHttpClient client;
    protected static List<Game> games = new ArrayList<Game>();
    protected static List<Card> cards = new ArrayList<Card>();

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        context = app.getApplicationContext();
        data = context.getSharedPreferences("data", MODE_PRIVATE);

        // Creating http client with cookie management
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        cookieManager.getCookieStore().removeAll();
        client = new OkHttpClient();
        client.setCookieHandler(cookieManager);

        // Initializing Parse
        Parse.initialize(this,
                "0V4fqB7pR03LwgQ1CMdXyyECAoHl5yLpPndQw64V",
                "vg6KxhzclZgLc3eFlR8c0MSSd6LZCeJDQxmxLsrU");
        PushService.setDefaultPushCallback(this, LoginActivity.class);
    }

    /**
     * Proxy para exibir toasts no app
     *
     * @param text Texto a ser exibido no toast
     */
    public static void toaster(CharSequence text) {
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    /**
     * Proxy para a recuperação de dados básico do app, usando key-value
     *
     * @param key Chave do dado a ser recuperado
     * @return    Dado
     */
    public static String data(String key) {
        return data.getString(key, "");
    }

    /**
     * Proxy para o armazenameto de dados básico do app, usando key-value
     *
     * @param key   Chave do dado a ser inserido / editado
     * @param value Valor do dado
     * @return      true se o dado foi inserido corretamente, false do contrário
     */
    public static Boolean data(String key, String value) {
        SharedPreferences.Editor editor = data.edit();
        editor.putString(key, value);
        return editor.commit();
    }

    /**
     * Verifica se usuário está logado ou não
     *
     * @return true se o usuário estiver logado, falso do contrário
     */
    public static Boolean isUserLoggedIn() {
        return !data("registration_number").equals("");
    }

    /**
     * Desloga um usuário
     */
    public static void logout() {
        data("registration_number", "");
        data("password", "");
        data("checkin_disabled", "");

        // Inform the user
        App.toaster(context.getString(R.string.logout));

        // Go back to the main activity (LoginActivity)
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Loga um usuário
     * Simplesmente registra o número de matrícula e senha
     *
     * @param registration_number Número de matrícula
     * @param password            Senha
     */
    public static void login(String registration_number, String password) {
        // @TODO Mover toda a lógia de login do controller LoginActivity pra cá?
        data("registration_number", registration_number);
        data("password", password);
    }

    /**
     * HTTP GET Request handler
     *
     * @param url URL a ser acessada
     * @return    HTMl resultante da requisição, "" em caso de problemas
     */
    public static String doRequest(String url) {
        Map<String, String> map = new HashMap<String, String>();
        return doRequest(url, map);
    }

    /**
     * HTTP POST Request handler
     *
     * @param url        URL a ser acessada
     * @param postValues Dados a serem postados
     * @return           HTMl resultante da requisição, "" em caso de problemas
     */
    public static String doRequest(String url, Map<String, String> postValues) {
        // building request
        Request.Builder builder = new Request.Builder().url(url);

        if (!postValues.isEmpty()) {
            FormEncodingBuilder formBody = new FormEncodingBuilder();

            // in case of a post request (postValues not empty), include the post fields
            Iterator<String> keySetIterator = postValues.keySet().iterator();
            while (keySetIterator.hasNext()) {
                String key = keySetIterator.next();
                formBody.add(key, postValues.get(key));
            }
            builder.post(formBody.build());
        }

        Request request = builder.build();

        try {
            Response response = client.newCall(request).execute();
            return new String(response.body().bytes(), "ISO-8859-1");
        } catch (IOException e) {
            return "";
        }
    }

    protected static class Scrapper {
        protected String html;
        protected Document dom;

        public Scrapper(String html) {
            this.html = html;
            this.dom = Jsoup.parse(html);
        }

        public List<Game> getGames() {
            List<Game> games = new ArrayList<Game>();

            Elements gameTitles = dom.select("td.SOCIO_destaque_titulo > strong");
            if (!gameTitles.isEmpty()) for (Element gameTitle : gameTitles) {
                games.add(getGame(gameTitle.parent()));
            }
            return games;
        }

        protected Game getGame(Element gameContainer) {
            // Get basic information about a game
            String[] match = gameContainer.select("strong").first().text().split(" X ");
            String home = match[0];
            String away = match[1];

            String[] info = gameContainer.select("span.SOCIO_texto_destaque_titulo2").first().text().split(" - ");
            String tournament = info[0];
            String date = info[1];
            String venue = info[2];

            // We have a gameid and sectors to fill
            Element checkinForm = gameContainer.select("form").first();
            String id = checkinForm.select("input[name=id_jogo]").first().val();

            Game game = new Game(id, home, away, venue, date, tournament);

            if (!gameContainer.html().contains("do site foi finalizado")) {
                // Enables checkin for this game
                game.enableCheckin();
            }

            // Get available sectors
            Elements options = checkinForm.select("select[name=setor] option[value!=1]");
            String[] sectorInfo;
            for (Element option : options) {
                sectorInfo = option.text().split(" - ");
                if (game.findSector(option.val()) == null) {
                    game.addSector(new Game.Sector(option.val(), sectorInfo[0], sectorInfo[1]));
                }
            }

            return game;
        }

        public List<Card> getCards() {
            Element container = dom.select("td.SOCIO_destaque_titulo > strong").first().parent();
            Elements cardSpans = container.select(".blocodecartao .SOCIO_texto_destaque_titulo2");

            List<Card> cards = new ArrayList<Card>();
            for (Element cardElement : cardSpans) {
                String[] cardInfo = cardElement.text().split("-");
                Card card = new Card(cardInfo[0].split(" ")[1], cardInfo[1]);
                cards.add(card);
            }
            return cards;
        }

        public Game.Sector getCheckin(Card card, Game game) {
            Elements checkin = dom
                    .select("input[name=id_jogo][value="+game.getId()+"]+input[name=cartao][value="+card.getId()+"]")
                    .parents()
                    .select("select[name=setor] option[selected][value!=1]");

            if (checkin.isEmpty()) return null;
            else return game.findSector(checkin.val());
        }

    }

    /**
     * Cria uma lista de jogos fazendo scrape da página de checkin
     *
     * @param html HTML da página de checkin
     */
    public static void buildCheckinFrom(String html) {
        Scrapper scrapper = new Scrapper(html);

        games = scrapper.getGames();

        for (Game game : games) {
            // Scrapping cards
            cards = scrapper.getCards();

            // Scrapping checkins
            for (Card c : cards) {
                for (Game g : games) {
                    Game.Sector sector = scrapper.getCheckin(c, g);
                    if (sector != null) c.checkin(g, sector);
                }
            }
        }
    }

    /**
     * Retorna a lista de jogos
     * @return Lista de jogos
     */
    public static List<Game> getGameList() {
        return games;
    }

    /**
     * Retorna a lista de cartões
     * @return Lista de cartões
     */
    public static List<Card> getCards() { return cards; }

    /**
     * Returns the game
     */
    public static Game getGame(int gameId) {
        return games.get(gameId);
    }


    /**
     * Classe de utilidades variadas não-diretamente relacionadas ao app
     *
     * @author  Gustavo Seganfredo
     * @since   1.0
     */
    public static class Utils {

        /**
         * Retorna uma frase com todas as palavras capitalizadas
         * @param string Frase a ser capitalizada
         * @return       Frase capitalizada
         */
        public static String capitalizeWords(String string) {
            char[] chars = string.toLowerCase().toCharArray();
            boolean found = false;
            for (int i = 0; i < chars.length; i++) {
                if (!found && Character.isLetter(chars[i])) {
                    chars[i] = Character.toUpperCase(chars[i]);
                    found = true;
                } else if (Character.isWhitespace(chars[i]) || chars[i]=='.' || chars[i]=='\'') { // You can add other chars here
                    found = false;
                }
            }
            return String.valueOf(chars);
        }
    }
}
