package me.gpsbr.check_in;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Created by gust on 26/08/14.
 */
public class App extends Application {

    protected static Application app;
    protected static Context context;
    protected static SharedPreferences data;
    protected static OkHttpClient client;

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

    /**
     * HTML Scrapper
     */
    static String scrape_html_cache;
    static Document scrape_dom;
    public static String scrape(String html, String option) {
        // Simple cache to not repeat jsoup parse for the same page
        if (html != scrape_html_cache) {
            scrape_dom = Jsoup.parse(html);
            scrape_html_cache = html;
        }

        if (option.equals("game")) {
            return scrape_dom.select("td.SOCIO_destaque_titulo > strong").first().text();
        } else if (option.equals("venue")) {
            String[] info = scrape_dom.select("span.SOCIO_texto_destaque_titulo2").first().text().split(" - ");
            return info[2];
        } else if (option.equals("tournment")) {
            String[] info = scrape_dom.select("span.SOCIO_texto_destaque_titulo2").first().text().split(" - ");
            return info[0];
        } else if (option.equals("date")) {
            String[] info = scrape_dom.select("span.SOCIO_texto_destaque_titulo2").first().text().split(" - ");
            return info[1];
        } else if (option.equals("checkin")) {
            return String.valueOf(html.contains("Sua modalidade de car"));
        } else {
            return "";
        }
    }
}
