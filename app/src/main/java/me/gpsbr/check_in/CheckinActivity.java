package me.gpsbr.check_in;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class CheckinActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkin);
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
            // Clear login/password references
            SharedPreferences.Editor editor = credentials.edit();
            editor.putString("registration_number", "");
            editor.putString("password", "");
            editor.commit();

            // Go back to the main activity (LoginActivity)
            Intent intent = new Intent(LoginActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else if (id == R.id.action_about) {
            // Go to AboutActivity
            // Intent intent = new Intent(LoginActivity.this, AboutActivity.class);
            // startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}
