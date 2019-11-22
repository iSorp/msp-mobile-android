package ch.bfh.ti.these.msp;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import ch.bfh.ti.these.msp.http.MissionClient;
import ch.bfh.ti.these.msp.mavlink.*;
import ch.bfh.ti.these.msp.mavlink.model.Converter;
import ch.bfh.ti.these.msp.mavlink.model.MavlinkMission;
import ch.bfh.ti.these.msp.mavlink.model.MavlinkMissionUploadItem;
import ch.bfh.ti.these.msp.models.Mission;
import io.dronefleet.mavlink.MavlinkMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class MissionActivity extends AppCompatActivity implements MavlinkMessageListener {

    private TextView txtStatus;

    private Button btnTakeOff;
    private Button btnReturnToOrigin;
    private Button btnLanding;

    private MavlinkMaster mavlinkMaster;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission);
        setupView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        new LoadWaypointsMission().execute();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mavlinkMaster != null) {
            mavlinkMaster.dispose();
        }
    }

    @Override
    public void messageReceived(MavlinkMessage message) {
        txtStatus.setText(message.toString());
    }

    private void setupView() {
        txtStatus = findViewById(R.id.txtStatus);
        btnTakeOff = findViewById(R.id.btnTakeOff);
        btnTakeOff.setEnabled(false);
        btnReturnToOrigin = findViewById(R.id.btnRetrun);
        btnReturnToOrigin.setEnabled(false);
        btnLanding = findViewById(R.id.btnLanding);
        btnLanding.setEnabled(false);
    }

    private class LoadWaypointsMission extends AsyncTask<Void, Void, Mission> {
        @Override
        protected Mission doInBackground(Void... voids) {
            Intent intent = getIntent();
            String missionId = intent.getStringExtra(SelectMissionActivity.MISSION_ID);
            return MissionClient.getInstance("192.168.1.120", 8081).getMission(missionId);
        }

        @Override
        protected void onPostExecute(Mission m) {
            super.onPostExecute(m);

            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        MavlinkUdpBridge bridge = new MavlinkUdpBridge();
                        bridge.connect();
                        MavlinkConfig config = new MavlinkConfig
                                .Builder(1, bridge)
                                .setTimeout(30000)
                                .setSystemId(1)
                                .setComponentId(1)
                                .build();
                        mavlinkMaster = new MavlinkMaster(config);
                        mavlinkMaster.addMessageListener(MissionActivity.this);
                        mavlinkMaster.connect();

                        MavlinkMission mission = new MavlinkMission();
                        for (int i = 0; i < 20; i++) {
                            MavlinkMissionUploadItem item = new MavlinkMissionUploadItem(12 + i, 32 + i, 3);
                            item.setBehavior(5, 1);
                            mission.addUploadItem(item);
                            MavlinkMissionUploadItem actionItem = new MavlinkMissionUploadItem(12 + i, 32 + i, 3);
                            actionItem.setSensor(1, 1, 0, 0);
                            mission.addUploadItem(item);
                        }
                        CompletableFuture compf = MissionActivity.this.mavlinkMaster.getMissionService().uploadMission(mission);
                        // Wait for completion
                        compf.get();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            /*try {
                if (MissionActivity.this.mavlinkMaster != null && m != null) {

                }
            } catch (IOException e) {
                txtStatus.setText(e.getMessage());
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }*/
        }
    }
}
