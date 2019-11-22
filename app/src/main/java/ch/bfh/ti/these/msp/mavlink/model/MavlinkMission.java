package ch.bfh.ti.these.msp.mavlink.model;

import java.util.ArrayList;
import java.util.List;

public class MavlinkMission {

    private List<MavlinkMissionUploadItem> items;

    public MavlinkMission() {
        items = new ArrayList<>();
    }

    public void addUploadItem(MavlinkMissionUploadItem item) {
        items.add(item);
    }

    public MavlinkMissionUploadItem get(int i) {
        return items.get(i);
    }

    public int size() {
        return items.size();
    }
}
