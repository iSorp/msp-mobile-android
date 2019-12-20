package ch.bfh.ti.these.msp.ui;

import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import ch.bfh.ti.these.msp.R;
import ch.bfh.ti.these.msp.mavlink.model.Converter;
import ch.bfh.ti.these.msp.mavlink.model.MavlinkMission;
import ch.bfh.ti.these.msp.models.Mission;
import ch.bfh.ti.these.msp.viewmodel.MissionRepository;
import ch.bfh.ti.these.msp.viewmodel.MissionViewModel;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import static ch.bfh.ti.these.msp.MspApplication.getMavlinkMaster;

public class MissionDetailFragment extends Fragment {

    private Handler handler;
    private MissionViewModel viewModel;
    private TextView tvName;
    private TextView tvDescription;
    private TextView tvCreateDate;
    private TextView tvUpdateDate;
    private TextView tvWaypointCount;
    private TextView tvWaypointActionCount;
    private TextView tvDistance;
    private Button btnSelectMission;
    private ProgressBar progressBar;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mission_detail, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        handler = new Handler(Looper.getMainLooper());
        FragmentActivity activity = getActivity();
        if (activity != null) {
            viewModel = ViewModelProviders.of(activity).get(MissionViewModel.class);
            viewModel.getSelectedMission().observe(this, this::updateDetails);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvName = view.findViewById(R.id.tv_mission_name);
        tvDescription = view.findViewById(R.id.tv_mission_description);
        tvCreateDate = view.findViewById(R.id.tv_createDate);
        tvUpdateDate = view.findViewById(R.id.tv_updateDate);
        tvWaypointCount = view.findViewById(R.id.tv_waypoint_count);
        tvWaypointActionCount = view.findViewById(R.id.tv_waypoint_action_count);
        tvDistance = view.findViewById(R.id.tv_distance);
        btnSelectMission = view.findViewById(R.id.btn_select_mission);
        btnSelectMission.setEnabled(false);
        btnSelectMission.setOnClickListener(
                v -> viewModel.downloadWayPoints().observe(this, this::uploadMavlinkMission)
        );
        Button btnBack = view.findViewById(R.id.btn_back);
        btnBack.setEnabled(true);
        btnBack.setOnClickListener(v -> getActivity().finish());

        progressBar = view.findViewById(R.id.progressBar);
    }

    private void updateDetails(Mission m) {
        DateFormat formatter = new SimpleDateFormat("dd:MM:yyyy HH:mm:ss");
        tvName.setText(m.getName());
        tvDescription.setText(m.getDescription());
        tvCreateDate.setText(formatter.format(m.getCreatedAt()));
        tvUpdateDate.setText(formatter.format(m.getUpdatedAt()));
        tvWaypointCount.setText(String.valueOf(m.getWaypointCount()));
        tvWaypointActionCount.setText(String.valueOf(m.getWaypointActionCount()));
        tvDistance.setText(m.getDistance() + " m");
        showMission();
    }

    private void showMission() {
        btnSelectMission.setEnabled(true);
    }

    private void uploadMavlinkMission(MissionRepository.Result<Mission> result) {
        if (result.getStatus()) {
            progressBar.post(()->progressBar.setVisibility(View.VISIBLE));
            MavlinkMission mission = Converter.convertToUploadItems(result.getPayload());
            try {
                getMavlinkMaster().getMissionService().uploadMission(mission).thenAccept((a) -> {
                    showToast("Mission upload successful");
                    getActivity().finish();
                }).exceptionally(throwable -> {
                    showToast("Mission upload failed");
                    return null;
                });
            } catch (IOException e) {
                e.printStackTrace();
                // @TODO SetStatus
            }
            finally {
                progressBar.post(()->progressBar.setVisibility(View.INVISIBLE));
            }
        }
    }

    private void showToast(String text) {
        handler.post(()-> {
            Toast.makeText(this.getActivity(), text, Toast.LENGTH_LONG).show();
        });
    }
}
