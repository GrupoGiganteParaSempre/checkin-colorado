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
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.Connection;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;

/**
 * A login screen that offers login via matricula/senha.
 */
public class LoginActivity extends Activity {

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world", "12345:67890"
    };

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private EditText mCampoMatricula;
    private EditText mCampoSenha;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Set up the login form.
        mCampoMatricula = (EditText) findViewById(R.id.matricula);
        mCampoSenha = (EditText) findViewById(R.id.senha);
        mCampoSenha.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mBotaoLogin = (Button) findViewById(R.id.botao_login);
        mBotaoLogin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mCampoMatricula.setError(null);
        mCampoSenha.setError(null);

        // Store values at the time of the login attempt.
        String matricula = mCampoMatricula.getText().toString();
        String senha = mCampoSenha.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Valida a senha
        if (!TextUtils.isEmpty(senha) && !validarSenha(senha)) {
            mCampoSenha.setError(getString(R.string.erro_senha_invalida));
            focusView = mCampoSenha;
            cancel = true;
        }

        // Valida o número de matrícula
        if (TextUtils.isEmpty(matricula)) {
            mCampoMatricula.setError(getString(R.string.erro_campo_obrigatorio));
            focusView = mCampoMatricula;
            cancel = true;
        } else if (!validarMatricula(matricula)) {
            mCampoMatricula.setError(getString(R.string.erro_matricula_invalida));
            focusView = mCampoMatricula;
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
            mAuthTask = new UserLoginTask(matricula, senha);
            mAuthTask.execute((Void) null);
        }
    }
    private boolean validarMatricula(String matricula) {
        return matricula.length() > 4;
    }

    private boolean validarSenha(String senha) {
        return senha.length() == 6;
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

        private final String mMatricula;
        private final String mSenha;

        private int idErro;

        UserLoginTask(String matricula, String senha) {
            mMatricula = matricula;
            mSenha = senha;
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
                    .add("matricula", mMatricula)
                    .add("senha", mSenha)
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
                    idErro = 1;
                    return false;
                } else if (html.contains("Tente outra vez")) {
                    idErro = 2;
                    return false;
                } else {
                    // Persiste a informação de login e senha
                    SharedPreferences settings = getSharedPreferences("credentials", 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("matricula", mMatricula);
                    editor.putString("senha", mSenha);
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
                if (idErro == 1) {
                    mCampoMatricula.setError(getString(R.string.erro_matricula_invalida));
                    mCampoMatricula.requestFocus();
                } else if (idErro == 2) {
                    mCampoSenha.setError(getString(R.string.erro_senha_incorreta));
                    mCampoSenha.requestFocus();
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



