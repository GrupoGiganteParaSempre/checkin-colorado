package me.gpsbr.check_in;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.parse.ParseAnalytics;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller da atividade "CheckinGame"
 * Esta atividade trata do checkin para um determinado jogo, exibindo os dados da partida, as
 * opções de checkin/checkout, bem como trata do envio das opções para o servidor do clube.
 * Ela possui suporte para tratar com mais de um cartão, mas está programada para exibir e
 * tratar de checkin apenas para o primeiro da listagem
 *
 * @author   Gustavo Seganfredo <gustavosf@gmail.com>
 * @since    1.0
 *
 * TODO:     Permitir gerenciamento de mais de um cartão para o checkin. O sistema do inter foi
 *           programado para poder exibir mais de um cartão, mas não tivemos ainda experiência
 *           com situações que caiam nesse caso.
 */
public class CheckinGameActivity extends Activity {

    protected int gameId;
    protected Game game;
    protected int cardId;
    protected Card card;
    protected String operation;

    // UI references
    protected Button mButtonSectorSelection;
    protected Button mButtonConfirm;
    protected Switch mSwitchCheckin;
    protected View mViewCheckin;
    protected View mCheckinUnavailableMessage;
    protected View mCheckinEndedContainer;
    protected View mProgress;
    protected TextView mCheckinEndedMessage;
    protected TextView mMessageCheckout;
    protected TextView mWarningCheckout;
    protected TextView mCheckinQuestion;

    protected Game.Sector checkedSector;

    // ------------------------------------------------------------------------------------- //
    // - Métodos da Atividade -------------------------------------------------------------- //
    // ------------------------------------------------------------------------------------- //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkin_game);

        // Libera a execução de atividade de rede (checkin/out) sem criação de nova thread
        // TODO: Mandar isso para uma thread separada da thread da interface
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        // Referencias de UI
        mButtonSectorSelection = (Button)findViewById(R.id.button_sector_choice);
        mViewCheckin = findViewById(R.id.checkin_available_form);
        mSwitchCheckin = (Switch)findViewById(R.id.checkin_switch);
        mCheckinUnavailableMessage = findViewById(R.id.checkin_unavailable_message);
        mCheckinEndedContainer = findViewById(R.id.checkin_ended_container);
        mButtonConfirm = (Button)findViewById(R.id.button_confirmation);
        mCheckinEndedMessage = (TextView)findViewById(R.id.checkin_ended_message);
        mMessageCheckout = (TextView)findViewById(R.id.checkedout_message);
        mWarningCheckout = (TextView)findViewById(R.id.checkout_warning);
        mCheckinQuestion= (TextView)findViewById(R.id.checkin_question);
        mProgress = findViewById(R.id.progress);

        // Inicializaçao da UI
        Intent intent = getIntent();
        gameId = intent.getIntExtra(CheckinCardActivity.EXTRA_GAME_ID, 0);
        cardId = intent.getIntExtra(CheckinCardActivity.EXTRA_CARD_ID, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Caso o delay aqui seja maior que 5 minutos, recarrega o aplicativo
        long delay = (System.currentTimeMillis() / 1000L) - App.client.lastRequest;
        if (delay > 60*5) finish();
        else buildInterface();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt("gameId", gameId);
        savedInstanceState.putInt("cardId", cardId);
        App.saveState(savedInstanceState);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        gameId = savedInstanceState.getInt("gameId");
        cardId = savedInstanceState.getInt("cardId");
        App.restoreState(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.activity_slide_in_left, R.anim.activity_slide_out_right);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.checkin, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                overridePendingTransition(R.anim.activity_slide_in_left, R.anim.activity_slide_out_right);
                break;
            case R.id.action_logout:
                App.logout();
                break;
            case R.id.action_about:
                App.showAbout(this);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    // ------------------------------------------------------------------------------------- //
    // - Outros Métodos -------------------------------------------------------------------- //
    // ------------------------------------------------------------------------------------- //

    /**
     * Monta a interface
     */
    private void buildInterface() {
        game = App.getGame(gameId);
        card = App.getCard(cardId);

        // Esconde alguamas coisas que podem voltar visíveis em um resume
        mViewCheckin.setVisibility(View.GONE);
        mCheckinUnavailableMessage.setVisibility(View.GONE);
        mCheckinEndedContainer.setVisibility(View.GONE);
        mCheckinEndedMessage.setVisibility(View.GONE);
        mMessageCheckout.setVisibility(View.GONE);

        // Tacale pau na interface
        mProgress.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.game_home)).setText(game.getHome());
        ((TextView) findViewById(R.id.game_away)).setText(game.getAway());
        ((TextView) findViewById(R.id.game_venue)).setText(game.getVenue());
        ((TextView) findViewById(R.id.game_date)).setText(game.getDate());
        ((TextView) findViewById(R.id.game_tournament)).setText(game.getTournament());

        App.client.get("checkin/opcoes?cartao=" + card.getId(), null, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject json) {
                if (json.optInt("status") == 0) {
                    App.toaster(getString(R.string.error_network));
                    finish();
                    return;
                }

                // Registra as operações permitidas para este cartão
                JSONArray options = json.optJSONArray("opcoesCheckin");
                for (int i = 0; i < options.length(); i++) {
                    card.addOperation(options.optJSONObject(i).optString("codigo"));
                }
                operation = card.hasOperation("checkout") ? "checkout" : "checkin";
                if (operation.equals("checkin")) {
                    // Caso um check-in já tenha sido feito, registra o checkin aqui também
                    JSONObject checkinStatus = json.optJSONObject("checkinStatus");
                    if (checkinStatus != null) {
                        Game.Sector sector = game.findSector(checkinStatus.optString("codigosetor"));
                        card.checkin(game, sector, checkinStatus.optString("sid"));
                    }
                }
                if (operation.equals("checkout")) {
                    // Inscreve o cara no canal das locadas
                    App.parseSubscribe("locada");
                    // TODO : Tentar encontrar um jeito de verificar se CHECKOUT já foi feito
                }

                mProgress.setVisibility(View.GONE);
                buildInterface2();
            }
            @Override
            public void onFailure(int statusCode, org.apache.http.Header[] headers,
                                  Throwable throwable, JSONObject errorResponse) {
                App.toaster(getString(R.string.error_network));
                finish();
            }
        });
    }

    private void buildInterface2() {
        if (game.isCheckinOpen()) {
            mViewCheckin.setVisibility(View.VISIBLE);

            if (operation.equals("checkin")) {
                mCheckinQuestion.setText(getString(R.string.checkin_question_in));
                if (card.isCheckedIn(game)) {
                    mSwitchCheckin.setChecked(true);
                    mButtonSectorSelection.setVisibility(View.VISIBLE);
                    checkedSector = card.getCheckinSector(game);
                    if (checkedSector != null) {
                        mButtonSectorSelection.setText(
                                checkedSector.name + "\n" + checkedSector.gates);
                    }
                } else {
                    mSwitchCheckin.setChecked(false);
                    mButtonConfirm.setEnabled(false);
                    mButtonSectorSelection.setVisibility(View.GONE);
                }
            }
            else if (operation.equals("checkout")) {
                mCheckinQuestion.setText(getString(R.string.checkin_question_out));
                if (card.isCheckedOut(game)) {
                    // Esconde o formulário de checkout
                    mViewCheckin.setVisibility(View.GONE);
                    // Mostra a confirmação de que o cara já fez checkout
                    mMessageCheckout.setVisibility(View.VISIBLE);
                } else {
                    mSwitchCheckin.setChecked(false);
                    mButtonConfirm.setEnabled(false);
                }
            }

            // Seta listener para mudanças no botão "vai ao jogo?"
            mSwitchCheckin.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // Mostra o resto do form em caso de sim, senao esconde
                    boolean on = buttonView.isChecked();
                    if (operation.equals("checkin")) {
                        // Em caso de check-in, mostra o form
                        mButtonSectorSelection.setVisibility(on ? View.VISIBLE : View.GONE);
                        mButtonConfirm.setEnabled(on || card.isCheckedIn(game));
                    } else if (operation.equals("checkout")) {
                        // Em caso de check-out, mostra o warning
                        mWarningCheckout.setVisibility(on ? View.VISIBLE : View.GONE);
                        mButtonConfirm.setEnabled(on);
                    }
                }
            });
        } else {
            mCheckinEndedContainer.setVisibility(View.VISIBLE);
            String text;
            if (card.isCheckedIn(game)) {
                Game.Sector sector = card.getCheckinSector(game);
                text = getString(R.string.check_in_made, sector.name, sector.gates);
            } else {
                text = getString(R.string.didnt_checked_in);
            }
            mCheckinEndedMessage.setText(text);
        }
    }

    /**
     * Trata da submissão do checkin/out no sistema do clube
     */
    public void submitCheckin(View view) {
        final boolean checked = mSwitchCheckin.isChecked();

        // Verifica se o usuário tá fazendo checkin e não selecionou um setor
        if (operation.equals("checkin") && checked && checkedSector == null) {
            App.Dialog.showAlert(this, getString(R.string.error_no_sector_message),
                    getString(R.string.error_no_sector_title));
            return;
        }

        // Faz o checkin rodando em background
        String url;

        if (operation.equals("checkout")) {
            url = "checkin/padrao?operacao=200";
            App.Dialog.showProgress(this, "Efetuando check-out...");
        }
        else {
            // checkin
            if (checked) url = "checkin/padrao?operacao=100&setor=" + checkedSector.id;
            else url = "checkin/cancelar?sid=" + card.getCheckinId(game);
            App.Dialog.showProgress(this, "Efetuando " + (checked ? "check-in" : "cancelamento do check-in") + "...");
        }

        App.client.get(url, null, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject json) {
                App.Dialog.dismissProgress();

                // Trata problemas de rede e no servidor do clube
                if (json.optInt("status") == 0) {
                    App.toaster(getString(R.string.error_network));
                    return;
                }

                Boolean isCheckin = operation.equals("checkin");
                if (checked) {
                    // Registra o checkin / out
                    JSONObject checkinData = json.optJSONObject(operation);
                    if (isCheckin) card.checkin(game, checkedSector, checkinData.optString("sid"));
                    else card.checkout(game, checkinData.optString("sid"));
                    App.printReceipt(game, card, checkinData);
                }
                else {
                    // Remove o check-in
                    card.checkout(game);
                    mButtonConfirm.setEnabled(false);
                }

                // Registra o checkin no push (removendo o user do channel "NOT_CHECKIN")
                App.parseUnsubscribe("NOT_CHECKIN");

                // Mostra mensagem de sucesso :)
                if (isCheckin) {
                    // Para check-in ou cancelamento de check-in
                    String message = getString(checked ? R.string.checkin_sucessfull : R.string.checkin_cancel_sucessfull);
                    App.Dialog.showAlert(CheckinGameActivity.this,
                            message, (checked ? "Check-in" : "Cancelamento") + " efetuado");
                } else {
                    // Para check-out
                    App.Dialog.showAlert(CheckinGameActivity.this,
                            getString(R.string.checkout_sucessfull),
                            getString(R.string.checkout_sucessfull_title));
                    // Esconde o formulário de checkout
                    mViewCheckin.setVisibility(View.GONE);
                    // Mostra a confirmação de que o cara já fez checkout
                    mMessageCheckout.setVisibility(View.VISIBLE);
                }

                // Parse Analytics
                Map<String, String> checkinAnalytics = new HashMap<String, String>();
                checkinAnalytics.put("mode", isCheckin ? "checkin" : "checkout");
                if (isCheckin && checked) checkinAnalytics.put("sector", checkedSector.name);
                ParseAnalytics.trackEvent("checkin", checkinAnalytics);

                // Google Analytics
                Tracker t = ((App) CheckinGameActivity.this.getApplication()).getTracker(
                        App.TrackerName.APP_TRACKER);
                t.send(new HitBuilders.EventBuilder().setCategory("mode")
                        .setAction(isCheckin ? "checkin" : "checkout").build());
                if (isCheckin)
                    t.send(new HitBuilders.EventBuilder().setCategory("sector")
                            .setAction(checkedSector.name).build());
            }
            @Override
            public void onFailure(int statusCode, org.apache.http.Header[] headers,
                                  Throwable throwable, JSONObject errorResponse) {
                App.Dialog.dismissProgress();
                App.toaster(getString(R.string.error_network));
            }
        });
    }

    /**
     * Trata a mudança de setor, ao ser selecionada na listagem
     * @param view Listagem
     */
    public void sectorSelectionClicked(View view) {

        int i = 0;
        final List<Game.Sector> sectors = game.getSectors();
        final CharSequence[] items = new CharSequence[sectors.size()];
        for (Game.Sector sector : sectors) {
            items[i++] = sector.name;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Selecione o setor");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                mButtonSectorSelection.setText(sectors.get(item).name + "\n" + sectors.get(item).gates);
                checkedSector = sectors.get(item);
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

}