package me.gpsbr.check_in;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
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
 * Created by gust on 26/08/14.
 */
public class App extends Application {

    protected static Application app;
    protected static Context context;
    protected static SharedPreferences data;
    protected static OkHttpClient client;
    protected static List<Game> games;
    protected static List<Card> cards;

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
     * Simply show toasts
     */
    public static void toaster(CharSequence text) {
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    /**
     * Getter and setter for a simple data storage
     */
    public static String data(String key) {
        return data.getString(key, "");
    }
    public static Boolean data(String key, String value) {
        SharedPreferences.Editor editor = data.edit();
        editor.putString(key, value);
        return editor.commit();
    }

    /**
     * Finds if a user is logged in or not
     */
    public static Boolean isUserLoggedIn() {
        return !data("registration_number").equals("");
    }

    /**
     * Logs out a user
     */
    public static Boolean logout() {
        data("registration_number", "");
        data("password", "");
        data("checkin_disabled", "");

        // Inform the user
        App.toaster(context.getString(R.string.logout));

        // Go back to the main activity (LoginActivity)
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        return true;
    }

    /**
     * Logs in a user
     * Simply register their r-number and password in the storage
     * @TODO Think about extracting the login logic from LoginActivity to here?
     */
    public static Boolean login(String registration_number, String password) {
        data("registration_number", registration_number);
        data("password", password);
        return true;
    }

    /**
     * HTTP Request handler
     */
    public static String doRequest(String url) {
        Map<String, String> map = new HashMap<String, String>();
        return doRequest(url, map);
    }
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
            return response.body().string();
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

        public String[] getGame() {
            String game = dom.select("td.SOCIO_destaque_titulo > strong").first().text();
            return game.split(" X ");
        }

        public String getVenue() {
            String[] info = dom.select("span.SOCIO_texto_destaque_titulo2").first().text().split(" - ");
            return info[2];
        }
        public String getTournament() {
            String[] info = dom.select("span.SOCIO_texto_destaque_titulo2").first().text().split(" - ");
            return info[0];
        }
        public String getDate() {
            String[] info = dom.select("span.SOCIO_texto_destaque_titulo2").first().text().split(" - ");
            return info[1];
        }
        public Boolean checkinPossible() {
            return !html.contains("Sua modalidade");
        }
        public String getGameId() {
            if (!checkinPossible()) return "";
            else return dom.select("input[name=id_jogo]").first().val();
        }
        public List<Card> getCards() {
            Elements cardInputs = dom.select("input[name=cartao]");
            List<Card> cards = new ArrayList<Card>();
            for (Element cardElement : cardInputs) {
                Card card = new Card(cardElement.val());
                Elements sectorSelected = cardElement.parent()
                        .select("select[name=setor] option[selected]");
                if (!sectorSelected.isEmpty()) {
                    card.checkin(getGameId(), sectorSelected.first().val());
                }
                cards.add(card);
            }
            return cards;
        }
        public Map<String, String> getSectors() {
            Map<String, String> sectors = new HashMap<String, String>();
            if (!checkinPossible()) return sectors;

            Elements options = dom.select("select[name=setor]").first().select("option");
            for (Element option : options) {
                sectors.put(option.val(), option.text());
            }
            return sectors;
        }
    }

    /**
     * Creates a list of games scraping a page
     */
    public static void buildCheckinFrom(String html) {
        Scrapper scrapper = new Scrapper(html);

        // Initializing games list
        games = new ArrayList<Game>();

        // @TODO No need for game lists, refator this for a single game entitity
        String[] players = scrapper.getGame();
        games.add(new Game(
                scrapper.getGameId(),
                players[0],
                players[1],
                scrapper.getVenue(),
                scrapper.getDate(),
                scrapper.getTournament(),
                scrapper.getSectors()));

        cards = scrapper.getCards();
    }
    public static List<Game> getGameList() {
        return games;
    }
    public static List<Card> getCards() { return cards; }

    /**
     * Returns the game
     */
    public static Game getGame(int gameId) {
        return games.get(gameId);
    }
}
