package ch.bfh.ti.these.msp;

import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import ch.bfh.ti.these.msp.http.MissionClient;
import ch.bfh.ti.these.msp.models.Mission;

import java.util.List;

public class SelectMissionActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    final static String MISSION_ID = "missionId";

    private Button startBtn;
    private Spinner missionSpinner;
    private ProgressBar progressBar;

    private Mission selectMission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_mission);
        setupContent();
    }

    @Override
    protected void onResume() {
        super.onResume();
        new LoadMission().execute();
    }

    private void setupContent() {
        startBtn = findViewById(R.id.btnStart);
        startBtn.setEnabled(false);
        startBtn.setOnClickListener(view -> {
            Intent missionIntent = new Intent(SelectMissionActivity.this, MissionActivity.class);
            missionIntent.putExtra(MISSION_ID, selectMission.getId());
            SelectMissionActivity.this.startActivity(missionIntent);
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
        selectMission = (Mission) missionSpinner.getSelectedItem();
        startBtn.setEnabled(true);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        selectMission = null;
        startBtn.setEnabled(false);
    }

    private class LoadMission extends AsyncTask<Void, Void, List<Mission>> {
        @Override
        protected List<Mission> doInBackground(Void... voids) {
            return MissionClient.getInstance("192.168.1.120", 8081).getMissionList();
        }

        @Override
        protected void onPostExecute(List<Mission> missions) {
            super.onPostExecute(missions);

            SelectMissionActivity.this.missionSpinner.setAdapter(new ArrayAdapter<>(
                    SelectMissionActivity.this,
                    R.layout.support_simple_spinner_dropdown_item,
                    missions
            ));
            missionSpinner.setEnabled(true);

            progressBar.setEnabled(false);
            progressBar.setVisibility(View.INVISIBLE);
        }
    }
}
