package ch.bfh.ti.these.msp.ui;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import ch.bfh.ti.these.msp.R;
import ch.bfh.ti.these.msp.adpater.MissionAdapter;
import ch.bfh.ti.these.msp.adpater.RecyclerViewClickListener;
import ch.bfh.ti.these.msp.models.Mission;
import ch.bfh.ti.these.msp.viewmodel.MissionViewModel;


/**
 * A list {@link Fragment}.
 */
public class MissionListFragment extends Fragment implements RecyclerViewClickListener<Mission> {

    private MissionViewModel viewModel;
    private MissionAdapter missionAdapter;

    public MissionListFragment() {}

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentActivity activity = getActivity();
        if (activity != null) {
            missionAdapter = new MissionAdapter(activity, this);

            viewModel = ViewModelProviders.of(activity).get(MissionViewModel.class);
            viewModel.getMissions().observe(this, missions -> missionAdapter.setData(missions));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mission_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.mission_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(missionAdapter);

        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.HORIZONTAL));
    }

    @Override
    public void recyclerViewListClicked(Mission m, int position) {
        viewModel.selectMission(m);
    }
}
