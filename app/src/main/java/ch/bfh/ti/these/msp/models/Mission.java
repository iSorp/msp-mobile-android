package ch.bfh.ti.these.msp.models;

import androidx.annotation.NonNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Mission {

    private String id;
    private String name;
    private List<WayPoint> wayPoints = new ArrayList<>();


    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<WayPoint> getWayPoints() {
        return wayPoints;
    }

    public void setWayPoints(List<WayPoint> wayPoints) {
        this.wayPoints = wayPoints;
    }

    public static ArrayList<Mission> fromJson(JSONArray jsonArray) {
        JSONObject missionJson;
        ArrayList<Mission> missions = new ArrayList<Mission>(jsonArray.length());

        for (int i=0; i < jsonArray.length(); i++) {
            try {
                missionJson = jsonArray.getJSONObject(i);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            Mission business = Mission.fromJson(missionJson);
            if (business != null) {
                missions.add(business);
            }
        }

        return missions;
    }

    private static Mission fromJson(JSONObject jsonObject) {
        Mission m = new Mission();
        // Deserialize json into object fields
        try {
            m.id = jsonObject.getString("id");
            m.name = jsonObject.getString("description");
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return m;
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
