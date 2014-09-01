package me.gpsbr.check_in;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.parse.ParseAnalytics;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller da atividade "LoginActivity"
 * Esta é a atividade principal da aplicação, chamada sempre ao iniciar, e trata do login do usuário
 * junto ao sistema do clube. Caso já exista uma combinação matrícula-senha registrada no app, ele
 * tenta logar o usuário automaticamente, caso contrário, exibe o formulário de login
 *
 * Uma vez logado, esta atividade chama a atividade CheckinActivity, repassando a execução do app
 * para lá
 *
 * @author   Gustavo Seganfredo <gustavosf@gmail.com>
 * @since    1.0
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

        // Parse analytics
        ParseAnalytics.trackAppOpened(getIntent());

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
        if (App.isUserLoggedIn()) {
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
            Intent intent = new Intent(LoginActivity.this, AboutActivity.class);
            startActivity(intent);
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

            Map<String, String> postValues = new HashMap<String, String>();
            postValues.put("matricula", mRegistrationNumber);
            postValues.put("senha", mPassword);

            String url = "http://internacional.com.br/checkincolorado/logar.php";
//            String url = "http://192.168.1.7/checkin-fig.html";
            String html = App.doRequest(url, postValues);

            if (html.equals("")) {
                // Empty means some connection error, treat better later
                return false;
            } else if (html.contains("Tente novamente")) {
                // Typital registration number error message
                errorId = 1;
                return false;
            } else if (html.contains("Tente outra vez")) {
                // Typital password error message
                errorId = 2;
                return false;
            } else {
                // No error message, login ok
                // Persists login information
                App.login(mRegistrationNumber, mPassword);

                // Building checkin information
                App.buildCheckinFrom(html);

                return true;
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



