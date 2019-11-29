package ch.bfh.ti.these.msp.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import ch.bfh.ti.these.msp.R;
import ch.bfh.ti.these.msp.mavlink.MavlinkConnectionInfo;
import ch.bfh.ti.these.msp.mavlink.MavlinkMessageListener;
import io.dronefleet.mavlink.MavlinkMessage;

import static ch.bfh.ti.these.msp.MspApplication.getMavlinkMaster;


public class MissionActivity extends AppCompatActivity implements MavlinkMessageListener {

    // This event fires 2nd, before views are created for the fragment
    // The onCreate method is called when the Fragment instance is being created, or re-created.
    // Use onCreate for any standard setup that does not require the activity to be fully created
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission);

        setupView();
        getMavlinkMaster().addMessageListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getMavlinkMaster().removeMessageListener(this);
    }

    @Override
    public void messageReceived(MavlinkMessage message) {

    }

    @Override
    public void connectionStatusChanged(MavlinkConnectionInfo info) {

    }

    private void setupView() {


    }

}
