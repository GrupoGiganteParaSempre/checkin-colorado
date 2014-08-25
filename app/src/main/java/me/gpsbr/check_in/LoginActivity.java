package me.gpsbr.check_in;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;

/**
 * A login screen that offers login via matricula/senha.
 */
public class LoginActivity extends Activity {

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references
    private EditText mRegistrationNumber;
    private EditText mPassword;
    private View mProgressView;
    private View mLoginFormView;

    // Credentials
    private SharedPreferences credentials;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        credentials = getSharedPreferences("credentials", 0);

        // Set up the login form.
        mRegistrationNumber = (EditText) findViewById(R.id.registration_number);
        mPassword = (EditText) findViewById(R.id.password);
        mPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mLoginButton = (Button) findViewById(R.id.login_button);
        mLoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        /* Submete o fomulário caso já se tenha uma matrícula e senha registradas */
        if (credentials.getString("registration_number", "") != "") {
            mRegistrationNumber.setText(credentials.getString("registration_number", ""));
            mPassword.setText(credentials.getString("password", ""));
            mLoginButton.callOnClick();
        }

    }


    public void animate(View view) {
//        FrameLayout imageView = (FrameLayout) findViewById(R.id.login_image_frame);
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.login_animation);
        anim.setFillAfter(true);

        view.startAnimation(anim);

//        ScaleAnimation scale = new ScaleAnimation((float)1.0, (float)1.0, (float)1.0, (float)0.3);
//        scale.setFillAfter(true);
//        scale.setDuration(800);
//        imageView.startAnimation(scale);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {

        InputMethodManager imm = (InputMethodManager)getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mPassword.getWindowToken(), 0);

        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mRegistrationNumber.setError(null);
        mPassword.setError(null);

        // Store values at the time of the login attempt.
        String registration_number = mRegistrationNumber.getText().toString();
        String password = mPassword.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Valida a senha
        if (!TextUtils.isEmpty(password) && !validatePassword(password)) {
            mPassword.setError(getString(R.string.error_invalid_password));
            focusView = mPassword;
            cancel = true;
        }

        // Valida o número de matrícula
        if (TextUtils.isEmpty(registration_number)) {
            mRegistrationNumber.setError(getString(R.string.error_mandatory_field));
            focusView = mRegistrationNumber;
            cancel = true;
        } else if (!validateRegistrationNumber(registration_number)) {
            mRegistrationNumber.setError(getString(R.string.error_invalid_registration_number));
            focusView = mRegistrationNumber;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(registration_number, password);
            mAuthTask.execute((Void) null);
        }
    }
    private boolean validateRegistrationNumber(String number) {
        return number.length() > 4;
    }

    private boolean validatePassword(String password) {
        return password.length() == 6;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mRegistrationNumber;
        private final String mPassword;

        private int errorId;

        UserLoginTask(String registration_number, String password) {
            mRegistrationNumber = registration_number;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            /* @TODO tratar problemas de rede */

            /* cookies! */
            CookieManager cookieManager = new CookieManager();
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            cookieManager.getCookieStore().removeAll();

            /* request */
            OkHttpClient client = new OkHttpClient();
            client.setCookieHandler(cookieManager);

            String url = "http://internacional.com.br/checkincolorado/logar.php";

            RequestBody formBody = new FormEncodingBuilder()
                    .add("matricula", mRegistrationNumber)
                    .add("senha", mPassword)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(formBody)
                    .build();

            Response response = null;
            try { /* problema na conexão de rede */
                response = client.newCall(request).execute();
                String html = response.body().string();

                Log.d("html", html);
                if (html.contains("Tente novamente")) {
                    errorId = 1;
                    return false;
                } else if (html.contains("Tente outra vez")) {
                    errorId = 2;
                    return false;
                } else {
                    // Persiste a informação de login e senha
                    SharedPreferences.Editor editor = credentials.edit();
                    editor.putString("registration_number", mRegistrationNumber);
                    editor.putString("password", mPassword);
                    editor.commit();

                    // Pega os dados da partida
                    Document doc = Jsoup.parse(html);
                    String jogo = doc.select("td.SOCIO_destaque_titulo > strong").first().text();
                    String info = doc.select("span.SOCIO_texto_destaque_titulo2").first().text();

                    editor.putString("jogo", jogo);
                    editor.putString("info", info);
                    editor.putBoolean("pode_fazer_checkin", html.matches("Sua modalidade de cartão não faz Check-In"));
                    editor.commit();

                    Log.d("jogo", jogo);
                    Log.d("info", info);
                    Log.d("pode fazer", String.valueOf(html.matches("Sua modalidade de cartão não faz Check-In")));

                    // Log.d("html", doc.toString());
                    // Elements nome = doc.select("span.SOCIO_texto_destaque_titulo");
                    return true;
                }
            } catch (IOException e) {
                return false;
            }

        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                finish();
            } else {
                if (errorId == 1) {
                    LoginActivity.this.mRegistrationNumber.setError(getString(R.string.error_invalid_registration_number));
                    LoginActivity.this.mRegistrationNumber.requestFocus();
                } else if (errorId == 2) {
                    LoginActivity.this.mPassword.setError(getString(R.string.error_incorrect_password));
                    LoginActivity.this.mPassword.requestFocus();
                } else {
                    Context context = getApplicationContext();
                    CharSequence text = "Um erro ocorreu, tente novamente mais tarde";
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                }
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}



