package me.gpsbr.check_in;

import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.text.method.LinkMovementMethod;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Classe da aplicação.
 * Consiste em basicamente toda a lógica de login, checkin, checkuot e armazenamento de dados do
 * aplicativo.
 *
 * @author   Gustavo Seganfredo <gustavosf@gmail.com>
 * @since    1.0
 */
public class App extends Application {

    final public static String TAG = "Check-in";

    protected static Application app;
    protected static Context context;
    protected static SharedPreferences data;

    public static OkHttpClient client;

    protected static List<Game> games = new ArrayList<Game>();
    protected static List<Card> cards = new ArrayList<Card>();

    protected static Set<String> parseSubscriptions;

    // Google Analytics
    public enum TrackerName {
        APP_TRACKER, // Tracker used only in this app.
        GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
        ECOMMERCE_TRACKER, // Tracker used by all ecommerce transactions from a company.
    }
    HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();

    // ------------------------------------------------------------------------------------- //
    // - Métodos da Aplicação -------------------------------------------------------------- //
    // ------------------------------------------------------------------------------------- //

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        context = app.getApplicationContext();
        data = context.getSharedPreferences("data", MODE_PRIVATE);

        CookieSyncManager.createInstance(this);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().removeAllCookie();
        WebkitCookieManagerProxy coreCookieManager = new WebkitCookieManagerProxy(null, CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(coreCookieManager);

        // Cria um client http com cookies
        client = new OkHttpClient();
        client.setCookieHandler(coreCookieManager);

        // Initializing Parse
        Parse.initialize(this, getString(R.string.parse_app_id), getString(R.string.parse_app_key));
        PushService.setDefaultPushCallback(this, LoginActivity.class);

        // Garante a inscrição do user no canal "checkin"
        parseSubscriptions = PushService.getSubscriptions(this);
        parseSubscribe("checkin");
    }

    /**
     * Inscreve o usuário em um canal do parse
     *
     * @param channel ID do canal para inscrever a pessoa
     */
    public static void parseSubscribe(String channel) {
        manageParseSubscription(channel, true);
    }

    /**
     * Desinscreve o usuário em um canal do parse
     *
     * @param channel ID do canal para desinscrever a pessoa
     */
    public static void parseUnsubscribe(String channel) {
        manageParseSubscription(channel, false);
    }

    /**
     * Gerencia inscrições em canais no parse
     *
     * @param channel ID do canal para inscrever a pessoa
     * @param tuneIn  true pra ligar a inscrição no canal, false para desligar
     */
    protected static void manageParseSubscription(String channel, Boolean tuneIn) {
        if (tuneIn && !parseSubscriptions.contains(channel)) {
            PushService.subscribe(App.app, channel, LoginActivity.class);
            if (channel == "checkin") {
                // Checkin só é setado uma vez, então se estiver sendo setado é porque é a primeira
                // vez que o usuário abre o celular. Assim, setamos "NOT_CHECKIN" também porque
                // a princípio ele não fez checkin. Após se logar, caso ele já tenha feito login
                // o app identifica e remove ele do canal
                PushService.subscribe(App.app, "NOT_CHECKIN", LoginActivity.class);
            }
            parseSubscriptions = PushService.getSubscriptions(App.app);
        }
        if (!tuneIn && parseSubscriptions.contains(channel)) {
            PushService.unsubscribe(App.app, channel);
            parseSubscriptions = PushService.getSubscriptions(App.app);
        }
    }

    /**
     * Tretas do Google Analytics
     *
     * @param trackerId ID da propriedade no analytics
     * @return          Tracker
     */
    synchronized Tracker getTracker(TrackerName trackerId) {
        if (!mTrackers.containsKey(trackerId)) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            Tracker t = (trackerId == TrackerName.APP_TRACKER) ? analytics.newTracker("UA-42184575-3")
                    : analytics.newTracker(R.xml.global_tracker);
            mTrackers.put(trackerId, t);
        }
        return mTrackers.get(trackerId);
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

    /**
     * Salva o recibo de check-in na memória do aparelho
     *
     * @param card Cartão ao qual foi feito o checkin
     * @param game Jogo para o qual foi feito o checkin
     *
     * TODO: Tratar problemas com a impressão do checkin (rede, salvamento do arquivo, etc)
     */
    public static void printReceipt(final Card card, final Game game) {
        final WebView w = new WebView(App.app);
        final WebSettings settings = w.getSettings();

        // Seta escala e zoom para que o webview não ajuste o tamanho a belprazer, ferrando com
        // o layout da página
        w.setInitialScale(100);
        settings.setTextZoom(100);

        // Libera javascript, porque iremos injetar depois para fixar o tamanho da janela
        settings.setJavaScriptEnabled(true);

        String url = App.context.getString(R.string.url_receipt, card.getId(), game.getId());
        w.loadUrl(url);
        w.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(final WebView view, String url) {
                // Injeta javascript para fixar tamanho da div no layout da página
                view.loadUrl("javascript:(function(){document.body.style.width='465px'})()");

                // Delay de 200ms, suficiente para ser renderizada a página
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        // Salva um printscreen da página em um bitmap
                        Bitmap b = Bitmap.createBitmap(465, 465, Bitmap.Config.ARGB_8888);
                        Canvas c = new Canvas(b);
                        view.draw(c);

                        // Salva o bitmap :)
                        File file = new File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                                "Checkin/checkin_" + game.getId() + ".jpg");
                        file.mkdirs();
                        if (file.exists()) file.delete();
                        try {
                            FileOutputStream out = new FileOutputStream(file);
                            b.compress(Bitmap.CompressFormat.JPEG, 80, out);
                            out.flush();
                            out.close();

                            // Disparado media scanner, porque por algum motivo não aparece o
                            // comprovante na galeria
                            MediaScannerConnection.scanFile(App.app,
                                    new String[]{file.toString()}, null, null);
                        } catch (Exception e) {
                            // TODO: Tratar problema no salvamento do arquivo
                            // e.printStackTrace();
                        }
                    }
                }, 500);
            }
        });
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
            Elements checkinForms = gameContainer.select("form");
            if (checkinForms.isEmpty()) {
                return new Game(null, home, away, venue, date, tournament);
            }

            Element checkinForm = checkinForms.first();
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
                    if (sector != null) {
                        c.checkin(g, sector);

                        // O cara fez checkin, remove do canal NOT_CHECKIN (caso esteja ainda)
                        parseUnsubscribe("NOT_CHECKIN");
                    }
                }
            }
        }
    }

    /**
     * Mostra a dialog de "sobre"
     */
    public static void showAbout(Context context) {
        android.app.Dialog dialog = App.Dialog.show(context, R.layout.dialog_about, "Sobre");
        ((TextView)dialog.findViewById(R.id.link_policy))
                .setMovementMethod(LinkMovementMethod.getInstance());
        ((TextView)dialog.findViewById(R.id.link_fanpage))
                .setMovementMethod(LinkMovementMethod.getInstance());
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
     * Classe proxy para dialogs do android
     *
     * @author  Gustavo Seganfredo
     * @since   1.0
     */
    public static class Dialog {

        protected static ProgressDialog progressDialog;
        protected static AlertDialog alertDialog;

        /**
         * Mostra dialog de progresso com mensagem
         *
         * @param context Contexto da atividade pai
         * @param message Mensagem a ser exibida
         * @return        Objeto ProgressDialog
         */
        public static ProgressDialog showProgress(Context context, String message) {
            progressDialog = ProgressDialog.show(context, "", message, true);
            return progressDialog;
        }

        /**
         * Desaparece com a dialog de progresso
         */
        public static void dismissProgress() {
            progressDialog.dismiss();
        }

        /**
         * Exibe uma mensagem de alerta
         *
         * @param context  Contexto da atividade pai
         * @param message  Mensagem da janela
         * @param okText   Texto do botão OK
         * @param callback Callback a ser executada depois de clicar no botão ok
         * @return         Objeto AlertDialog
         */
        public static AlertDialog showAlert(Context context, String message, String title,
                                            String okText,
                                            DialogInterface.OnClickListener callback) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(message).setCancelable(false);
            if (title != null) builder.setTitle(title);
            builder.setPositiveButton(okText, callback);
            alertDialog = builder.create();
            alertDialog.show();
            return alertDialog;
        }
        public static AlertDialog showAlert(Context context, String message) {
            return showAlert(context, message, null, "OK");
        }
        public static AlertDialog showAlert(Context context, String message, String title) {
            return showAlert(context, message, title, "OK");
        }
        public static AlertDialog showAlert(Context context, String message, String title,
                                            String okText) {
            return showAlert(context, message, title, okText, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });
        }

        // Mesma coisa só que com content-view
        public static android.app.Dialog show(Context context, int contentView, String title) {
            android.app.Dialog dialog = new android.app.Dialog(context);
            dialog.setContentView(contentView);
            dialog.setCanceledOnTouchOutside(true);
            dialog.setTitle(title);
            dialog.show();
            return dialog;
        }
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

interface HTTPClientCallbackInterface {
    public void success(String html);
}

/**
 * Represents an asynchronous login/registration task used to authenticate
 * the user.
 */
class HTTPClient extends AsyncTask<Void, Void, String> {

    protected String url;
    protected Map<String, String> postValues = new HashMap<String, String>();
    protected HTTPClientCallbackInterface callback;

    HTTPClient(String url) {
        this(url, null, null);
    }
    HTTPClient(String url, HTTPClientCallbackInterface callback) {
        this(url, null, callback);
    }
    HTTPClient(String url, Map<String, String> postValues, HTTPClientCallbackInterface callback) {
        this.url = url;
        this.postValues = postValues;
        this.callback = callback;
    }

    @Override
    protected String doInBackground(Void... params) {
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
            Response response = App.client.newCall(request).execute();
            if (response.isSuccessful()) {
                return new String(response.body().bytes(), "ISO-8859-1");
            } else {
                // Evita problemas de 404 ou 50x
                return "";
            }
        } catch (IOException e) {
            return "";
        }
    }

    @Override
    protected void onPostExecute(final String html) {
        if (callback != null) callback.success(html);
    }

    @Override
    protected void onCancelled() {
        if (callback != null) callback.success(null);
    }
}