package me.gpsbr.check_in;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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

    // ------------------------------------------------------------------------------------- //
    // - Properties ------------------------------------------------------------------------ //
    // ------------------------------------------------------------------------------------- //

    // Keep track of the login task to ensure we can cancel it if requested.
    private UserLoginTask mAuthTask = null;

    // UI references
    private EditText mRegistrationNumber;
    private EditText mPassword;
    private View mProgressView;
    private View mLoginFormView;

    // Caching
    private int mShortAnimationDuration;

    // ------------------------------------------------------------------------------------- //
    // - Basic methods --------------------------------------------------------------------- //
    // ------------------------------------------------------------------------------------- //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Android inicialization
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Caching the system's default "short" animation time.
        mShortAnimationDuration = getResources().getInteger(
                android.R.integer.config_shortAnimTime);


        // Set up the login form references
        mRegistrationNumber = (EditText) findViewById(R.id.registration_number);
        mPassword = (EditText) findViewById(R.id.password);
        mProgressView = findViewById(R.id.login_progress);
        mLoginFormView = findViewById(R.id.login_form);
        Button mLoginButton = (Button) findViewById(R.id.login_button);

        // Set up the login form actions
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

        // Autofill and submit the form in case we have already registered number and password
        if (App.data("registration_number") != "") {
            mRegistrationNumber.setText(App.data("registration_number"));
            mPassword.setText(App.data("password"));
            mLoginButton.callOnClick();
        } else {
            showForm();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_about) {
            // Go to AboutActivity
            // Intent intent = new Intent(LoginActivity.this, AboutActivity.class);
            // startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    // ------------------------------------------------------------------------------------- //
    // - Other methods --------------------------------------------------------------------- //
    // ------------------------------------------------------------------------------------- //

        private void showForm() {
            toggleForm(mProgressView, mLoginFormView);
        }
        private void hideForm() {
            toggleForm(mLoginFormView, mProgressView);
        }
        private void toggleForm(final View viewToHide, final View viewToShow) {

        // Set the content view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.

        // Simply return case the form is already (in)visible
        if (mLoginFormView == viewToHide && mLoginFormView.getVisibility() == View.GONE) {
            return;
        }

        viewToShow.setAlpha(0f);
        viewToShow.setVisibility(View.VISIBLE);

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        viewToShow.animate()
            .alpha(1f)
            .setDuration(mShortAnimationDuration)
            .setListener(null);

        // Animate the loading view to 0% opacity. After the animation ends,
        // set its visibility to GONE as an optimization step (it won't
        // participate in layout passes, etc.)
        viewToHide.animate()
            .alpha(0f)
            .setDuration(mShortAnimationDuration)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    viewToHide.setVisibility(View.GONE);
                }
            });
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin(View view) {
        attemptLogin();
    }
    public void attemptLogin() {

        // Hide keyboard
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

        // Validates password
        if (!TextUtils.isEmpty(password) && !validatePassword(password)) {
            mPassword.setError(getString(R.string.error_invalid_password));
            focusView = mPassword;
            cancel = true;
        }

        // Validates registration number
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
            hideForm();
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

                if (html.contains("Tente novamente")) {
                    errorId = 1;
                    return false;
                } else if (html.contains("Tente outra vez")) {
                    errorId = 2;
                    return false;
                } else {
                    // Persiste a informação de login e senha
                    App.login(mRegistrationNumber, mPassword);

                    // Pega os dados da partida
                    Document doc = Jsoup.parse(html);
                    String jogo = doc.select("td.SOCIO_destaque_titulo > strong").first().text();
                    String info = doc.select("span.SOCIO_texto_destaque_titulo2").first().text();

                    App.data("jogo", jogo);
                    App.data("info", info);
                    if (html.contains("Sua modalidade de cartão não faz Check-In")) {
                        App.data("checkin_disabled", "1");
                    }

                    return true;
                }
            } catch (IOException e) {
                return false;
            }

        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;

            if (success) {
                App.toaster(getString(R.string.login_sucessfull));
                Intent intent = new Intent(LoginActivity.this, CheckinActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);
                startActivity(intent);
                finish();
            } else {
                showForm();
                if (errorId == 1) {
                    LoginActivity.this.mRegistrationNumber.setError(getString(R.string.error_invalid_registration_number));
                    LoginActivity.this.mRegistrationNumber.requestFocus();
                } else if (errorId == 2) {
                    LoginActivity.this.mPassword.setError(getString(R.string.error_incorrect_password));
                    LoginActivity.this.mPassword.requestFocus();
                } else {
                    App.toaster(getString(R.string.unidentifyed_error));
                }
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showForm();
        }
    }
}



