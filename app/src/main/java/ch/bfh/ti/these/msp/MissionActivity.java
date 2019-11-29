package ch.bfh.ti.these.msp;

import android.content.Intent;
import android.os.AsyncTask;
import android.view.Menu;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import ch.bfh.ti.these.msp.http.MissionClient;
import ch.bfh.ti.these.msp.mavlink.MavlinkMessageListener;
import ch.bfh.ti.these.msp.mavlink.model.Converter;
import ch.bfh.ti.these.msp.mavlink.model.MavlinkMission;
import ch.bfh.ti.these.msp.models.Mission;
import io.dronefleet.mavlink.MavlinkMessage;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static ch.bfh.ti.these.msp.MspApplication.getMavlinkMaster;
import static ch.bfh.ti.these.msp.util.Definitions.*;



public class MissionActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, MavlinkMessageListener {

    final static String MISSION_ID = "missionId";

    private Button btnUpload;
    private Button btnDownload;
    private Spinner missionSpinner;
    private ProgressBar progressBar;

    private boolean hasFiles;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_mission);

        setupView();
    }

    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        getMavlinkMaster().addMessageListener(this);
        new LoadMission().execute();
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


    private void setupView() {

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });


        btnUpload = findViewById(R.id.btnUpload);
        btnUpload.setEnabled(false);
        btnUpload.setOnClickListener(view -> {
            new LoadWaypointsMission().execute();
        });

        btnDownload = findViewById(R.id.btnDownload);
        btnDownload.setEnabled(true);
        btnDownload.setOnClickListener(view -> {
            new DownloadMissionResult().execute();
        });

        missionSpinner = findViewById(R.id.spinner);
        missionSpinner.setOnItemSelectedListener(this);
        missionSpinner.setEnabled(false);

        progressBar = findViewById(R.id.progressBar);
        progressBar.setEnabled(true);
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        btnUpload.setEnabled(true);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        btnUpload.setEnabled(false);
    }

    private void setStatusBusy(boolean status) {
        progressBar.post(()-> {
            btnDownload.setEnabled(!status);
            btnUpload.setEnabled(!status);
            progressBar.setEnabled(!status);
            if (status)
                progressBar.setVisibility(View.VISIBLE);
            else
                progressBar.setVisibility(View.INVISIBLE);
        });
    }

    private void showToast(String message) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MissionActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void messageReceived(MavlinkMessage message) {

    }

    private class LoadMission extends AsyncTask<Void, Void, List<Mission>> {
        @Override
        protected List<Mission> doInBackground(Void... voids) {
            return MissionClient.getInstance(BACKEND_HOST, BACKEND_PORT).getMissionList();
        }

        @Override
        protected void onPostExecute(List<Mission> missions) {
            super.onPostExecute(missions);

            MissionActivity.this.missionSpinner.setAdapter(new ArrayAdapter<>(
                    MissionActivity.this,
                    R.layout.support_simple_spinner_dropdown_item, missions));
            missionSpinner.setEnabled(true);

            progressBar.setEnabled(false);
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    private class LoadWaypointsMission extends AsyncTask<Void, Void, Mission> {
        @Override
        protected Mission doInBackground(Void... voids) {
            Intent intent = getIntent();
            String missionId = intent.getStringExtra(MissionActivity.MISSION_ID);
            return MissionClient.getInstance(BACKEND_HOST, BACKEND_PORT).getMission(missionId);
        }

        @Override
        protected void onPostExecute(Mission m) {
            super.onPostExecute(m);

            try {
                setStatusBusy(true);
                missionSpinner.getSelectedItem();
                MavlinkMission mission = Converter.convertToUploadItems((Mission)missionSpinner.getSelectedItem());

                getMavlinkMaster().getMissionService().uploadMission(mission).thenAccept((a) -> {
                    setStatusBusy(false);
                            showToast("Upload erfolgreich");
                        })
                        .exceptionally(throwable -> {
                            setStatusBusy(false);
                            if (throwable != null)
                            {
                                showToast(throwable.getMessage());
                            }
                            return null;
                        });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class DownloadMissionResult extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {


            try {
                setStatusBusy(true);
                hasFiles = true;
                int index = 1;
                while (hasFiles) {
                    CompletableFuture cf = getMavlinkMaster().getFtpService().downloadFile("wp" + index++ + ".json").thenAccept(a -> {

                        System.out.println(a);
                        // TODO store to backend


                    }).exceptionally(throwable -> {
                        setStatusBusy(false);
                        if (throwable != null) {
                            hasFiles = false;
                            showToast(throwable.getMessage());
                        }
                        return null;
                    });

                    cf.get();
                }
                showToast("download beendet");
            } catch(Exception e){
                e.printStackTrace();
            }
            return true;
        }
    }
}
