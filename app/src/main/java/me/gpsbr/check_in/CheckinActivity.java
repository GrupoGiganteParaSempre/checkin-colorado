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

import java.util.List;

public class CheckinActivity extends Activity {

    public final static String EXTRA_GAME_ID = "me.gpsbr.checkin.GAME_ID";

    private List<Game> games;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkin);

        games = App.getGameList();
        ArrayAdapter<Game> adapter = new GameListAdapter();
        ListView list = (ListView)findViewById(R.id.game_list);
        list.setAdapter(adapter);

        registerClickCallback();
    }

    private void registerClickCallback() {
        ListView list = (ListView)findViewById(R.id.game_list);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View viewClicked,
                                    int position, long id) {
                Intent intent = new Intent(CheckinActivity.this, CheckinGameActivity.class);
                intent.putExtra(EXTRA_GAME_ID, position);
                startActivity(intent);
                overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_left);
            }
        });
    }

    private class GameListAdapter extends ArrayAdapter<Game> {
        public GameListAdapter() {
            super(CheckinActivity.this, R.layout.game_list_view, games);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.game_list_view, parent, false);
            }

            Game currentGame = games.get(position);

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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.checkin, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            App.logout();
            finish();
        } else if (id == R.id.action_about) {
            // Go to AboutActivity
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}
