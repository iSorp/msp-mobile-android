package ch.bfh.ti.these.msp.ui;

import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
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

import static ch.bfh.ti.these.msp.MspApplication.getMavlinkMaster;

public class MissionDetailFragment extends Fragment {

    private Handler handler;
    private MissionViewModel viewModel;
    private TextView tvName;
    private Button btnSelectMission;

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
        tvName.setVisibility(View.INVISIBLE);
        btnSelectMission = view.findViewById(R.id.btn_select_mission);
        btnSelectMission.setVisibility(View.INVISIBLE);
        btnSelectMission.setOnClickListener(
                v -> viewModel.downloadWayPoints().observe(this, this::uploadMavlinkMission)
        );
    }

    private void updateDetails(Mission m) {
        tvName.setText(m.getName());
        showMission();
    }

    private void showMission() {
        tvName.setVisibility(View.VISIBLE);
        btnSelectMission.setVisibility(View.VISIBLE);
    }

    private void uploadMavlinkMission(MissionRepository.Result<Mission> result) {
        if (result.getStatus()) {
            MavlinkMission mission = Converter.convertToUploadItems(result.getPayload());
            try {
                getMavlinkMaster().getMissionService().uploadMission(mission).thenAccept((a) -> {
                    showToast("Mission upload successful");
                }).exceptionally(throwable -> {
                    showToast("Mission upload failed");
                    return null;
                });
            } catch (IOException e) {
                e.printStackTrace();
                // @TODO SetStatus
            }
        }
    }

    private void showToast(String text) {
        handler.post(()-> {
            Toast.makeText(this.getActivity(), text, Toast.LENGTH_LONG).show();
        });
    }
}
