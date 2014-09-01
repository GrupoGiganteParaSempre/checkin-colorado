package me.gpsbr.check_in;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;

/**
 * Controller da atividade "About"
 * Esta atividade é ativada ao clicar sobre o botão de "informação" no app. Ela opera a exibição
 * de ĩnformações do app, como seus criadores, seus objetivos, como funciona por baixo dos panos,
 * entre outros.
 *
 * @author   Gustavo Seganfredo <gustavosf@gmail.com>
 * @since    1.0
 */
public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
