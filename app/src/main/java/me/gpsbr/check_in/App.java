package me.gpsbr.check_in;

import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.method.LinkMovementMethod;
import android.util.Log;
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

import org.json.JSONException;
import org.json.JSONObject;
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

    protected static ArrayList<Game> games = new ArrayList<Game>();
    protected static ArrayList<Card> cards = new ArrayList<Card>();

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
     * Salva o estado do app.
     * Resumidamente, salva os jogos e os cartões, porque o resto ele regenera no onCreate
     *
     * @param savedInstanceState Instância do estado
     */
    public static void saveState(Bundle savedInstanceState) {
        savedInstanceState.putParcelableArrayList("games", games);
        savedInstanceState.putParcelableArrayList("cards", cards);
    }

    /**
     * Recupera o estado do app.
     * Resumidamente, recupera os jogos e os cartões, porque o resto ele regenera no onCreate
     *
     * @param savedInstanceState Instância do estado
     */
    public static void restoreState(Bundle savedInstanceState) {
        games = savedInstanceState.getParcelableArrayList("games");
        cards = savedInstanceState.getParcelableArrayList("cards");
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

        // Reseta os jogos e cartões
        games = new ArrayList<Game>();
        cards = new ArrayList<Card>();

        // Informa o usuário
        App.toaster(context.getString(R.string.logout));

        // Volta pra tela de login
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
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
     * @param url    URL a ser acessada
     * @param params Dados a serem postados
     * @return       JSON resultante da requisição, "" em caso de problemas
     */
    public static String doRequest(String url, Map<String, String> params) {
        // building request
        Request.Builder builder = new Request.Builder().url(url);

        if (!params.isEmpty()) {
            FormEncodingBuilder formBody = new FormEncodingBuilder();

            // in case of a post request (params not empty), include the post fields
            Iterator<String> keySetIterator = params.keySet().iterator();
            while (keySetIterator.hasNext()) {
                String key = keySetIterator.next();
                formBody.add(key, params.get(key));
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
     * @param game      Jogo para o qual foi feito o checkin
     * @param checkinId Id do check-in conforme sistema do inter
     */
    public static void printReceipt(Game game,String checkinId) {
        // Busca o template do comprovante nos resources
        Resources res = App.context.getResources();
        Bitmap bitmap = (BitmapFactory.decodeResource(res, R.drawable.comprovante))
                .copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(18);

        // Pinta os dados sobre o comprovante
        canvas.drawText(checkinId, 170, 511, paint);
        canvas.drawText("Inter X "+game.getAway(), 131, 540, paint);
        canvas.drawText(game.getDate(), 129, 569, paint);
        canvas.drawText(game.getVenue(), 155, 598, paint);

        // Salva o bitmap :)
        File file = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Checkin/checkin "+game.getId()+".jpg");
        file.mkdirs();
        if (file.exists()) file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
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

    protected static class Scrapper {
        protected JSONObject json;

        public Scrapper(JSONObject json) {
            this.json = json;
        }

        public ArrayList<Game> getGames() {
            ArrayList<Game> games = new ArrayList<Game>();
            JSONObject gameList = json.optJSONObject("jogos");
            Iterator<String> keys = gameList.keys();
            while (keys.hasNext()) {
                String id = keys.next();
                JSONObject gameInfo = gameList.optJSONObject(id);
                String away = gameInfo.optString("timevisitante");
                String venue = gameInfo.optString("estadio");
                String date = gameInfo.optString("data");
                String hour = gameInfo.optString("hora");
                String tournament = gameInfo.optString("campeonato");
                Game game = new Game(id, "Internacional", away, venue, date+' '+hour, tournament);
                game.enableCheckin(); // se aparece na listagem é porque está habilitado
                // TODO : adicionar setores
                games.add(game);
            }
            return games;
        }

        public ArrayList<Card> getCards() {
            ArrayList<Card> cards = new ArrayList<Card>();
            JSONObject cardList = json.optJSONObject("cartoes");
            Iterator<String> keys = cardList.keys();
            while (keys.hasNext()) {
                String id = keys.next();
                JSONObject cardInfo = cardList.optJSONObject(id);
                cards.add(new Card(
                        id,
                        cardInfo.optString("chave"),
                        cardInfo.optString("nome"),
                        cardInfo.optString("descricao")
                ));
            }
            return cards;
        }
    }

    /**
     * Cria uma lista de jogos fazendo scrape da página de checkin
     *
     * @param json JSON retornado pelo login
     */
    public static void buildCheckinFrom(JSONObject json) {
        Scrapper scrapper = new Scrapper(json);
        games = scrapper.getGames();
    }

    /**
     * Mostra a dialog de "sobre"
     */
    public static void showAbout(Context context) {
        android.app.Dialog dialog = App.Dialog.show(context, R.layout.dialog_about, "Sobre");

        String version = "?.0.0";
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException ignored) {}

        ((TextView)dialog.findViewById(R.id.link_policy))
                .setMovementMethod(LinkMovementMethod.getInstance());
        ((TextView)dialog.findViewById(R.id.link_fanpage))
                .setMovementMethod(LinkMovementMethod.getInstance());
        ((TextView)dialog.findViewById(R.id.about_version))
                .setText(context.getString(R.string.about_version, version));
    }

    /**
     * Retorna a lista de jogos
     * @return Lista de jogos
     */
    public static ArrayList<Game> getGameList() {
        return games;
    }

    /**
     * Retorna a lista de cartões
     * @return Lista de cartões
     */
    public static ArrayList<Card> getCards() { return cards; }

    /**
     * Busca por um jogo
     */
    public static Game getGame(int gameId) {
        return games.get(gameId);
    }

    /**
     * Busca por um cartão
     */
    public static Card getCard(int cardId) {
        return cards.get(cardId);
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

interface JSONClientCallbackInterface {
    public void success(JSONObject json);
}

/**
 * Represents an asynchronous login/registration task used to authenticate
 * the user.
 */
class JSONClient extends AsyncTask<Void, Void, String> {

    protected String url;
    protected Map<String, String> params = new HashMap<String, String>();
    protected JSONClientCallbackInterface callback;

    JSONClient(String url) {
        this(url, null, null);
    }
    JSONClient(String url, JSONClientCallbackInterface callback) {
        this(url, null, callback);
    }
    JSONClient(String url, Map<String, String> params, JSONClientCallbackInterface callback) {
        this.url = url;
        this.params = params;
        this.callback = callback;
    }

    @Override
    protected String doInBackground(Void... par) {
        if (params != null && !params.isEmpty()) {
            String queryString = "?";

            // in case of a post request (params not empty), include the post fields
            Iterator<String> keySetIterator = params.keySet().iterator();
            while (keySetIterator.hasNext()) {
                String key = keySetIterator.next();
                queryString = queryString + key + "=" + params.get(key) + "&";
            }

            url = url + queryString;
        }

        // building request
        Request request = new Request.Builder().url(url).build();

        try {
            Response response = App.client.newCall(request).execute();
            if (response.isSuccessful()) {
                return response.body().string();
                // return new String(response.body().bytes(), "ISO-8859-1");
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
        if (callback != null)
        {
            JSONObject ret;
            try {
                ret = new JSONObject(html);
            } catch (JSONException e) {
                ret = null;
            }
            callback.success(ret);
        }
    }

    @Override
    protected void onCancelled() {
        if (callback != null) callback.success(null);
    }
}