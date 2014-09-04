package me.gpsbr.check_in;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.parse.ParseAnalytics;

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

    // Ids do Checkin / Checkout conforme formulário
    private static final String FORM_CHECKIN_ID = "1";
    private static final String FORM_CHECKOUT_ID = "2";
    private static final String CHECKIN_URL = "http://internacional.com.br/checkincolorado/checkincolorado_res.php";

    protected Game game;
    protected Card card;

    // UI references
    protected Button mButtonSectorSelection;
    protected Button mButtonConfirm;
    protected Switch mSwitchCheckin;
    protected View mViewCheckin;
    protected View mCheckinUnavailableMessage;
    protected View mCheckinEndedContainer;
    protected TextView mCheckinEndedMessage;

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

        // Inicializaçao da UI
        Intent intent = getIntent();
        int gameId = intent.getIntExtra(CheckinActivity.EXTRA_GAME_ID, 0);

        game = App.getGame(gameId);
        ((TextView) findViewById(R.id.game_home)).setText(game.getHome());
        ((TextView) findViewById(R.id.game_away)).setText(game.getAway());
        ((TextView) findViewById(R.id.game_venue)).setText(game.getVenue());
        ((TextView) findViewById(R.id.game_date)).setText(game.getDate());
        ((TextView) findViewById(R.id.game_tournament)).setText(game.getTournament());

        List<Card> cards = App.getCards();

        if (cards.isEmpty()) {
            // Usuário é provavelmente CB ou algum outro tipo de associação que não faz check-in
            mCheckinUnavailableMessage.setVisibility(View.VISIBLE);
        } else {
            // Usando apenas o primeiro cartão da listagem por hora
            // TODO: Deixar o usuário escolher entre cartões, caso exista mais de um
            card = cards.get(0);

            if (game.isCheckinOpen()) {
                mViewCheckin.setVisibility(View.VISIBLE);

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
                    mButtonSectorSelection.setVisibility(View.GONE);
                }

                // Seta listener para mudanças no botão "vai ao jogo?"
                mSwitchCheckin.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        // Mostra o resto do form em caso de sim, senao esconde
                        boolean on = buttonView.isChecked();
                        mButtonSectorSelection.setVisibility(on ? View.VISIBLE : View.GONE);
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
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.activity_slide_in_left, R.anim.activity_slide_out_right);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                overridePendingTransition(R.anim.activity_slide_in_left, R.anim.activity_slide_out_right);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ------------------------------------------------------------------------------------- //
    // - Outros Métodos -------------------------------------------------------------------- //
    // ------------------------------------------------------------------------------------- //

    /**
     * Trata da submissão do checkin/out no sistema do clube
     */
    public void submitCheckin(View view) {
        final boolean in = mSwitchCheckin.isChecked();

        // Verifica se o usuário tá fazendo checkin e não selecionou um setor
        if (in && checkedSector == null) {
            App.Dialog.showAlert(this, getString(R.string.error_no_sector_message),
                    getString(R.string.error_no_sector_title));
            return;
        }

        // Submete o formulário
        Map<String, String> postValues = new HashMap<String, String>();
        postValues.put("id_jogo", game.getId());
        postValues.put("cartao", card.getId());
        postValues.put("opcao", in ? FORM_CHECKIN_ID : FORM_CHECKOUT_ID);
        postValues.put("setor", in ? checkedSector.id : "1");

        App.Dialog.showProgress(this, "Efetuando " + (in ? "Checkin" : "Checkout") + "...");

        // Faz o checkin rodando em background
        HTTPClient httpClient = new HTTPClient(CHECKIN_URL, postValues, new HTTPClientCallbackInterface() {
            @Override
            public void success(String html) {
                App.Dialog.dismissProgress();

                // Trata problemas de rede
                if (html == null || html.equals("")) {
                    App.Dialog.showAlert(CheckinGameActivity.this,
                            getString(R.string.error_network), "Erro");
                    return;
                }

                // Trata problema do check-in já ter sido finalizado
                if (html.contains("site foi finalizado") || html.contains("O prazo para o check-in referente")) {
                    App.Dialog.showAlert(CheckinGameActivity.this,
                            "Desculpe, mas o check-in já foi finalizado para este jogo", "Erro");
                    return;
                }

                // Registra o checkin
                if (in) card.checkin(game, checkedSector);
                else card.checkout(game);

                // Registra o checkin no push (removendo o user do channel "NOT_CHECKIN")
                App.parseUnsubscribe("NOT_CHECKIN");

                // Gera o recibo
                App.printReceipt(card, game);

                // Mostra mensagem de sucesso :)
                String message = getString(R.string.checkin_sucessfull, (in ? "VAI" : "NÃO VAI"));
                App.Dialog.showAlert(CheckinGameActivity.this,
                        message, (in ? "Checkin" : "Checkout") + " efetuado");

                // Parse Analytics
                Map<String, String> checkinAnalytics = new HashMap<String, String>();
                checkinAnalytics.put("mode", in ? "checkin" : "checkout");
                if (in) checkinAnalytics.put("sector", checkedSector.name);
                ParseAnalytics.trackEvent("checkin", checkinAnalytics);

                // Google Analytics
                Tracker t = ((App) CheckinGameActivity.this.getApplication()).getTracker(
                        App.TrackerName.APP_TRACKER);
                t.send(new HitBuilders.EventBuilder()
                        .setCategory("mode")
                        .setAction(in ? "checkin" : "checkout")
                        .build());
                t.send(new HitBuilders.EventBuilder()
                        .setCategory("sector")
                        .setAction(checkedSector.name)
                        .build());
            }
        });
        httpClient.execute((Void) null);
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