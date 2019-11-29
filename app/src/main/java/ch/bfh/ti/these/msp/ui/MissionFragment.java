package ch.bfh.ti.these.msp.ui;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.fragment.app.Fragment;
import ch.bfh.ti.these.msp.R;
import ch.bfh.ti.these.msp.http.MissionClient;
import ch.bfh.ti.these.msp.mavlink.MavlinkConnectionInfo;
import ch.bfh.ti.these.msp.mavlink.MavlinkMessageListener;
import ch.bfh.ti.these.msp.mavlink.model.Converter;
import ch.bfh.ti.these.msp.mavlink.model.MavlinkMission;
import ch.bfh.ti.these.msp.models.Mission;
import io.dronefleet.mavlink.MavlinkMessage;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static ch.bfh.ti.these.msp.MspApplication.getMavlinkMaster;
import static ch.bfh.ti.these.msp.util.Definitions.BACKEND_HOST;
import static ch.bfh.ti.these.msp.util.Definitions.BACKEND_PORT;

public class MissionFragment extends Fragment implements AdapterView.OnItemSelectedListener, MavlinkMessageListener {


    private Button btnUpload;
    private Button btnDownload;
    private Spinner missionSpinner;
    private ProgressBar progressBar;

    private boolean hasFiles;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    // The onCreateView method is called when Fragment should create its View object hierarchy,
    // either dynamically or via XML layout inflation.
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mission, parent, false);
    }

    // This event is triggered soon after onCreateView().
    // onViewCreated() is only called if the view returned from onCreateView() is non-null.
    // Any view setup should occur here.  E.g., view lookups and attaching view listeners.
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupView();

        getMavlinkMaster().addMessageListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        new LoadMission().execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getMavlinkMaster().removeMessageListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        btnUpload.setEnabled(true && getMavlinkMaster().isConnected());
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        btnUpload.setEnabled(false);
    }

    @Override
    public void messageReceived(MavlinkMessage message) {

        btnDownload.post(() -> {
            btnDownload.setEnabled(getMavlinkMaster().isConnected());
        });
    }

    @Override
    public void connectionStatusChanged(MavlinkConnectionInfo info) {

    }

    private void setupView() {

        btnUpload = getActivity().findViewById(R.id.btnUpload);
        btnUpload.setEnabled(false);
        btnUpload.setOnClickListener(view -> {
            new LoadWaypointsMission().execute();
        });

        btnDownload = getActivity().findViewById(R.id.btnDownload);
        btnDownload.setEnabled(true);
        btnDownload.setOnClickListener(view -> {
            new DownloadMissionResult().execute();
        });

        missionSpinner = getActivity().findViewById(R.id.spinner);
        missionSpinner.setOnItemSelectedListener(this);
        missionSpinner.setEnabled(false);

        progressBar = getActivity().findViewById(R.id.progressBar);
        progressBar.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void setStatusBusy(boolean status) {
        progressBar.post(()-> {
            btnDownload.setEnabled(!status);
            progressBar.setEnabled(!status);
            if (status)
                progressBar.setVisibility(View.VISIBLE);
            else
                progressBar.setVisibility(View.INVISIBLE);
        });
    }

    private void showToast(String message) {
        this.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private class LoadMission extends AsyncTask<Void, Void, List<Mission>> {
        @Override
        protected List<Mission> doInBackground(Void... voids) {
            return MissionClient.getInstance(BACKEND_HOST, BACKEND_PORT).getMissionList();
        }

        @Override
        protected void onPostExecute(List<Mission> missions) {
            super.onPostExecute(missions);

            MissionFragment.this.missionSpinner.setAdapter(new ArrayAdapter<> (
                    getActivity(),
                    R.layout.support_simple_spinner_dropdown_item,
                    missions));
            missionSpinner.setEnabled(true);

            progressBar.setEnabled(false);
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    private class LoadWaypointsMission extends AsyncTask<Void, Void, Mission> {
        @Override
        protected Mission doInBackground(Void... voids) {

            Mission selected = (Mission)missionSpinner.getSelectedItem();
            return MissionClient.getInstance(BACKEND_HOST, BACKEND_PORT).getMission(selected.getId());
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


