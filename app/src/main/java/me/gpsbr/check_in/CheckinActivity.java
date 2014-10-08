package me.gpsbr.check_in;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONObject;

/**
 * Controller da atividade "Checkin"
 * Esta atividade é mostrada logo após o login, tem como objetivo mostrar uma lista de partidas
 * cujo checkin está aberto. Normalmente, o clube abre apenas uma partida por vês mas o controller
 * prevê a possibilidade da abertura de checkin para mais de uma partida.
 *
 * @author   Gustavo Seganfredo <gustavosf@gmail.com>
 * @since    1.0
 */
public class CheckinActivity extends Activity {

    public final static String EXTRA_GAME_ID = "me.gpsbr.checkin.GAME_ID";

    // UI Refs
    protected ListView mGameList;
    protected TextView mCheckinClosedMessage;

    // ------------------------------------------------------------------------------------- //
    // - Métodos da Atividade -------------------------------------------------------------- //
    // ------------------------------------------------------------------------------------- //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkin);

        // Inicialização das referências de UI
        mGameList = (ListView) findViewById(R.id.game_list);
        mCheckinClosedMessage = (TextView) findViewById(R.id.checkin_closed_message);
        ((TextView) findViewById(R.id.subtitle)).setText("Jogos");
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Trata aqueles casos onde o usuário ficou um tempo sem acessar o app, e a sessão no site
        // foi pras cucuias. Demos um tempo máximo de 5 minutos ser necessário relogar
        if (App.client.timeout()) {
            (findViewById(R.id.progress)).setVisibility(View.VISIBLE);
            App.relogin(new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject json) {
                    if (json.optInt("status", 0) == 0 && !json.optString("msg").contains("nenhum jogo aberto")) {
                        App.toaster(App.context.getString(R.string.error_network));
                        finish();
                    }
                    else {
                        App.buildCheckinFrom(json);
                        buildInterface();
                    }
                }
                @Override
                public void onFailure(int statusCode, org.apache.http.Header[] headers,
                                      Throwable throwable, JSONObject errorResponse) {
                    App.toaster(App.context.getString(R.string.error_network));
                    finish();
                }
            });
        }
        else buildInterface();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        App.saveState(savedInstanceState);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        App.restoreState(savedInstanceState);
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
        (findViewById(R.id.progress)).setVisibility(View.GONE);
        if (App.games.isEmpty()) {
            // Checkin fechado, esconde a lista de jogos e mostra mensagem
            mCheckinClosedMessage.setVisibility(View.VISIBLE);
            mGameList.setVisibility(View.GONE);
        } else {
            // Monta lista de jogos
            ArrayAdapter<Game> adapter = new GameListAdapter();
            mGameList.setVisibility(View.VISIBLE);
            mGameList.setAdapter(adapter);
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
                Intent intent = new Intent(CheckinActivity.this, CheckinCardActivity.class);
                intent.putExtra(EXTRA_GAME_ID, position);
                startActivity(intent);
                overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_left);
            }
        });
    }

    /**
     * Adapter para mostrar os jogos no formato de lista
     */
    private class GameListAdapter extends ArrayAdapter<Game> {
        public GameListAdapter() {
            super(CheckinActivity.this, R.layout.game_list_view, App.games);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.game_list_view, parent, false);
            }

            Game currentGame = App.games.get(position);

            TextView tv;
            tv = (TextView)itemView.findViewById(R.id.game_tournament);
            tv.setText(currentGame.getTournament());
            tv = (TextView)itemView.findViewById(R.id.game_players);
            tv.setText(currentGame.getHome()+" x "+currentGame.getAway());
            tv = (TextView)itemView.findViewById(R.id.game_date);
            tv.setText(currentGame.getDate());
            tv = (TextView)itemView.findViewById(R.id.game_venue);
            tv.setText(currentGame.getVenue());

            return itemView;
        }
    }
}
