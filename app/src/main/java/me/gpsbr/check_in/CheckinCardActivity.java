package me.gpsbr.check_in;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONObject;

/**
 * Controller da atividade "CheckinCard"
 * Esta atividade visa listar os cartões disponíveis para serem usados para fazer checkin em uma
 * determinada partida. Ao selecionar o cartão, o usuário é direcionado para a seleção de setores
 * disponíveis.
 *
 * @author   Gustavo Seganfredo <gustavosf@gmail.com>
 * @since    1.1
 */
public class CheckinCardActivity extends Activity {

    public final static String EXTRA_GAME_ID = "me.gpsbr.checkin.GAME_ID";
    public final static String EXTRA_CARD_ID = "me.gpsbr.checkin.CARD_ID";

    protected int gameId;
    protected Game game;

    // UI Refs
    protected ListView mCardList;
    protected View mProgress;
    protected TextView mCheckinClosedMessage;

    // ------------------------------------------------------------------------------------- //
    // - Métodos da Atividade -------------------------------------------------------------- //
    // ------------------------------------------------------------------------------------- //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.activity_checkin_card);
        setContentView(R.layout.activity_checkin);

        // Inicialização das referências de UI
        mCardList = (ListView) findViewById(R.id.game_list);
        mCheckinClosedMessage = (TextView) findViewById(R.id.checkin_closed_message);
        mProgress = findViewById(R.id.progress);
        ((TextView) findViewById(R.id.subtitle)).setText("Cartões");

        Intent intent = getIntent();
        gameId = intent.getIntExtra(CheckinActivity.EXTRA_GAME_ID, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        buildInterface();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt("gameId", gameId);
        App.saveState(savedInstanceState);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        gameId = savedInstanceState.getInt("gameId");
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
        int id = item.getItemId();

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
     * Gera a interface, populando a lista de jogos com os jogos
     */
    private void buildInterface() {
        game = App.getGame(gameId);

        // Esconde algumas coisas que podem ficar visíveis num resume
        mCheckinClosedMessage.setVisibility(View.GONE);
        mCardList.setVisibility(View.GONE);

        if (App.cards.isEmpty()) {
            // Busca no servidor a lista de cartões do vivente
            mProgress.setVisibility(View.VISIBLE);
            App.client.get("index/jogo?id=" + game.getId(), null, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject json) {
                    if (json.optInt("status") == 0 && json.optString("erro").contains("Error")) {
                        App.toaster(getString(R.string.error_network));
                        finish();
                    }
                    mProgress.setVisibility(View.GONE);
                    if (json.optString("erro").equals("")) {
                        // Caso nao retorne nenhuma mensagem de erro, e porque possui cartoes
                        // elegiveis para check-in. Prossegue exibindo a interface
                        App.cards = (new App.Scrapper(json)).getCards();
                        buildInterface();
                    } else {
                        // Trata o caso de a pessoa não possuir cartões elegíveis para check-in
                        mCheckinClosedMessage.setText(json.optString("erro"));
                        mCheckinClosedMessage.setVisibility(View.VISIBLE);
                    }
                }
                @Override
                public void onFailure(int statusCode, org.apache.http.Header[] headers,
                                      Throwable throwable, JSONObject errorResponse) {
                    App.toaster(getString(R.string.error_network));
                    finish();
                }
            });
        } else {
            // Monta lista de cartões na interface
            ArrayAdapter<Card> adapter = new CardListAdapter();
            mCardList.setVisibility(View.VISIBLE);
            mCardList.setAdapter(adapter);
            registerClickCallback();
        }
    }

    /**
     * Callback para quando se clica em um jogo da lista
     */
    private void registerClickCallback() {
        ListView list = (ListView)findViewById(R.id.game_list);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View viewClicked,
                                    int position, long id) {
                Intent intent = new Intent(CheckinCardActivity.this, CheckinGameActivity.class);
                intent.putExtra(EXTRA_GAME_ID, gameId);
                intent.putExtra(EXTRA_CARD_ID, position);
                startActivity(intent);
                overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_left);
            }
        });
    }

    /**
     * Adapter para mostrar os jogos no formato de lista
     */
    private class CardListAdapter extends ArrayAdapter<Card> {
        public CardListAdapter() {
            super(CheckinCardActivity.this, R.layout.game_list_view, App.cards);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.game_list_view, parent, false);
            }

            Card currentCard = App.cards.get(position);

            TextView tv;
            tv = (TextView)itemView.findViewById(R.id.game_tournament);
            tv.setText(currentCard.getAssociationType());
            tv = (TextView)itemView.findViewById(R.id.game_players);
            tv.setText(currentCard.getId());
            tv = (TextView)itemView.findViewById(R.id.game_date);
            tv.setText(currentCard.getName());
            ((LinearLayout)itemView.findViewById(R.id.list_container))
                    .removeView(itemView.findViewById(R.id.game_venue));

            return itemView;
        }
    }

}