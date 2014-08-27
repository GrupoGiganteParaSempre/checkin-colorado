package me.gpsbr.check_in;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;


public class CheckinGameActivity extends Activity {

    protected Game game;
    protected Card card;

    // UI references
    protected Button mButtonSectorSelection;
    protected Switch mSwitchCheckin;
    protected View mViewCheckin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkin_game);

        // UI references
        mButtonSectorSelection = (Button)findViewById(R.id.button_sector_choice);
        mViewCheckin = findViewById(R.id.checkin_available_form);
        mSwitchCheckin = (Switch)findViewById(R.id.checkin_switch);

        // UI initialization
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
            findViewById(R.id.checkin_unavilable_message).setVisibility(View.VISIBLE);
        } else {
            // Only using the first card for now
            // @TODO Let user choose between cards, if he own more than one
            card = cards.get(0);

            mViewCheckin.setVisibility(View.VISIBLE);

            if (card.isCheckedIn(game)) {
                mSwitchCheckin.setChecked(true);
                mButtonSectorSelection.setVisibility(View.VISIBLE);
                Game.Sector checkinSector = card.getCheckinSector(game);
                if (checkinSector != null) {
                    mButtonSectorSelection.setText(
                            checkinSector.name + "\n" + checkinSector.gates);
                }
            } else {
                mSwitchCheckin.setChecked(false);
                mButtonSectorSelection.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Handles checkin / checkout changes in layout
     */
    public void onSwitchClicked(View view) {
        boolean on = ((Switch) view).isChecked();

        if (on) {
            mButtonSectorSelection.setVisibility(View.VISIBLE);
        } else {
            mButtonSectorSelection.setVisibility(View.GONE);
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
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

}