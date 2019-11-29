package ch.bfh.ti.these.msp;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import ch.bfh.ti.these.msp.mavlink.MavlinkMessageListener;
import io.dronefleet.mavlink.MavlinkMessage;

import static ch.bfh.ti.these.msp.MspApplication.getMavlinkMaster;


public class MainActivity extends AppCompatActivity implements MavlinkMessageListener {

    private CheckBox checkBoxMavState;
    private Button btnSettings;
    private Button btnMission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);

        setupView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getMavlinkMaster().addMessageListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getMavlinkMaster().removeMessageListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        getMavlinkMaster().removeMessageListener(this);
    }

    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public void messageReceived(MavlinkMessage message) {
        checkBoxMavState.post(()->{
            checkBoxMavState.setChecked(getMavlinkMaster().isConnected());
        });
    }


    private void setupView() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        checkBoxMavState = findViewById(R.id.checkBoxMavState);
        btnSettings      = findViewById(R.id.btnSettings);
        btnMission       = findViewById(R.id.btnMission);

        checkBoxMavState.setChecked(getMavlinkMaster().isConnected());
        btnSettings.setOnClickListener((e)-> {

            Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
            MainActivity.this.startActivity(settingsIntent);
        });

        btnMission.setOnClickListener((e)-> {

            Intent missionIntent = new Intent(MainActivity.this, MissionActivity.class);
            MainActivity.this.startActivity(missionIntent);
        });
    }
}
