package ch.bfh.ti.these.msp.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import ch.bfh.ti.these.msp.R;

import static ch.bfh.ti.these.msp.MspApplication.*;


public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment())
                .commit();
    }

    @Override
    public void onBackPressed() {
        createMavlinkMasterConfig();
        connectAsyncMavlinkMaster();
        super.onBackPressed();
    }
}
