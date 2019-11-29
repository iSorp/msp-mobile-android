package ch.bfh.ti.these.msp.adpater;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import ch.bfh.ti.these.msp.R;
import ch.bfh.ti.these.msp.models.Mission;

import java.util.ArrayList;
import java.util.List;

public class MissionAdapter extends RecyclerView.Adapter<MissionAdapter.MissionViewHolder> {

    private final List<Mission> data;
    private final LayoutInflater layoutInflater;
    private final RecyclerViewClickListener<Mission> itemListener;

    public MissionAdapter(Context context, RecyclerViewClickListener<Mission> itemListener) {
        data = new ArrayList<>();
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.itemListener = itemListener;
    }

    @Override
    public void onBindViewHolder(@NonNull MissionViewHolder holder, int position) {
        holder.bind(data.get(position));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void setData(List<Mission> newData) {
        if (data.size() > 0) {
            data.clear();
        }
        data.addAll(newData);
    }

    @NonNull
    @Override
    public MissionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = layoutInflater.inflate(R.layout.fragment_mission_list_item, parent, false);
        return new MissionViewHolder(itemView);
    }


    class MissionViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView itemNumber, content;

        MissionViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            itemNumber = itemView.findViewById(R.id.item_number);
            content = itemView.findViewById(R.id.content);
        }

        void bind(final Mission mission) {
            if (mission != null) {
                itemNumber.setText("1");
                content.setText(mission.getName());
            }
        }

        @Override
        public void onClick(View v) {
            int pos = this.getAdapterPosition();
            itemListener.recyclerViewListClicked(MissionAdapter.this.data.get(pos), pos);
        }
    }
}
