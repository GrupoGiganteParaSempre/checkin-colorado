package me.gpsbr.check_in;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

    // UI references
    private EditText mRegistrationNumber;
    private EditText mPassword;
    private View mProgressView;
    private View mLoginFormView;

    // Caching
    private int mShortAnimationDuration;

    // ------------------------------------------------------------------------------------- //
    // - Métodos da Atividade -------------------------------------------------------------- //
    // ------------------------------------------------------------------------------------- //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Parse analytics
        ParseAnalytics.trackAppOpened(getIntent());

        // Cache do tempo curto de animação
        mShortAnimationDuration = getResources().getInteger(
                android.R.integer.config_shortAnimTime);

        // Set up das referências de UI
        mRegistrationNumber = (EditText) findViewById(R.id.registration_number);
        mPassword = (EditText) findViewById(R.id.password);
        mProgressView = findViewById(R.id.login_progress);
        mLoginFormView = findViewById(R.id.login_form);
        Button mLoginButton = (Button) findViewById(R.id.login_button);

        // Set up das ações do formulário de login
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

        // Preenche e submete o formulário no caso de já termos login/senha registrados
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
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_about) {
            Intent intent = new Intent(LoginActivity.this, AboutActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    // ------------------------------------------------------------------------------------- //
    // - Outros Métodos -------------------------------------------------------------------- //
    // ------------------------------------------------------------------------------------- //

    /**
     * Mostra o formulário de login
     *
     * @see LoginActivity#toggleForm
     */
    private void showForm() {
        toggleForm(mProgressView, mLoginFormView);
    }

    /**
     * Esconde o formulário de login
     *
     * @see LoginActivity#toggleForm
     */
    private void hideForm() {
        toggleForm(mLoginFormView, mProgressView);
    }

    /**
     * Mostra um elemento e esconde outro
     *
     * @param viewToHide Elemento a ser escondido
     * @param viewToShow Elemento a ser exibido
     * @see LoginActivity#toggleForm
     */
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
     * Tenta efetuar o login de um usuário, com base nos dados do formulário
     */
    public void attemptLogin(View view) {
        attemptLogin();
    }
    public void attemptLogin() {

        // Esconde o teclado
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mPassword.getWindowToken(), 0);

        // Reseta os erros
        mRegistrationNumber.setError(null);
        mPassword.setError(null);

        // Guarda os dados a serem submetidos no login
        final String registration_number = mRegistrationNumber.getText().toString();
        final String password = mPassword.getText().toString();

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
            // Algum erro aconteceu, não tenta fazer login e mostra mensagem
            focusView.requestFocus();
        } else {
            // Partiu login, mostra o loading e faz o login no background
            hideForm();

            // Monta o post
            Map<String, String> postValues = new HashMap<String, String>();
            postValues.put("matricula", registration_number);
            postValues.put("senha", password);
            String url = getString(R.string.url_login);

            HTTPClient httpClient = new HTTPClient(url, postValues, new HTTPClientCallbackInterface() {
                @Override
                public void success(String html) {
                    if (html.equals("")) {
                        // Vazio significa um erro de conexão
                        showForm();
                        App.toaster(getString(R.string.unidentifyed_error));
                    } else if (html.contains("Tente novamente")) {
                        // Mensagem típica de erro de matrícula inválida
                        showForm();
                        mRegistrationNumber.setError(getString(R.string.error_invalid_registration_number));
                        mRegistrationNumber.requestFocus();
                    } else if (html.contains("Tente outra vez")) {
                        // Mensagem típica de erro de senha
                        showForm();
                        mPassword.setError(getString(R.string.error_incorrect_password));
                        mPassword.requestFocus();
                    } else {
                        // Sem erro, login efetuado
                        App.toaster(getString(R.string.login_sucessfull));

                        // Persiste as credenciais no aplicativo
                        App.login(registration_number, password);

                        // Extrai os dados para a próxima atividade
                        App.buildCheckinFrom(html);

                        // Chama a próxima atividade e mata a atividade de login
                        Intent intent = new Intent(LoginActivity.this, CheckinActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);
                        startActivity(intent);
                        finish();
                    }
                }
            });
            httpClient.execute((Void) null);
        }
    }

    /**
     * Verifica se o número de matrícula é válido
     * @param number Número de matrícula
     * @return       true se o número de matrícula for válido, false do contrário
     */
    private boolean validateRegistrationNumber(String number) {
        return number.length() > 4;
    }

    /**
     * Verifica se a senha é válida
     * @param password Senha
     * @return         true se a senha for válida, false do contrário
     */
    private boolean validatePassword(String password) {
        return password.length() == 6;
    }
}