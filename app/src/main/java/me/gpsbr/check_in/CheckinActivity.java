package me.gpsbr.check_in;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


public class CheckinActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkin);

        TextView tv = (TextView)findViewById(R.id.game_players);
        tv.setText(App.data("info-game"));

        tv = (TextView)findViewById(R.id.game_date);
        tv.setText(App.data("info-date"));

        tv = (TextView)findViewById(R.id.game_venue);
        tv.setText(App.data("info-venue"));

        tv = (TextView)findViewById(R.id.game_tournment);
        tv.setText(App.data("info-tournment"));
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
            // Intent intent = new Intent(LoginActivity.this, AboutActivity.class);
            // startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}
