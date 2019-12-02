package ch.bfh.ti.these.msp.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "missions")
public class Mission {

    @NonNull
    @PrimaryKey
    private String id;
    private String name;
    @Ignore
    private List<WayPoint> wayPoints = new ArrayList<>();


    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<WayPoint> getWayPoints() {
        return wayPoints;
    }

    public void setWayPoints(List<WayPoint> wayPoints) {
        this.wayPoints = wayPoints;
    }

    public static ArrayList<Mission> fromJson(JSONArray jsonArray) {
        JSONObject missionJson;
        ArrayList<Mission> missions = new ArrayList<>(jsonArray.length());

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
}
