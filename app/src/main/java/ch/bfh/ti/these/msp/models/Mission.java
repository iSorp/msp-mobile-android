package ch.bfh.ti.these.msp.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity(tableName = "missions")
public class Mission {

    @NonNull
    @PrimaryKey
    private String id;
    private String name;
    private String description;

    private Date createdAt;
    private Date updatedAt;

    @ColumnInfo(name = "waypoint_count")
    private int waypointCount;
    @ColumnInfo(name = "waypoint_action_count")
    private int waypointActionCount;
    private int distance;

    @Ignore
    private List<Waypoint> waypoints = new ArrayList<>();


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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getWaypointCount() {
        return waypointCount;
    }

    public void setWaypointCount(int waypointCount) {
        this.waypointCount = waypointCount;
    }

    public int getWaypointActionCount() {
        return waypointActionCount;
    }

    public void setWaypointActionCount(int waypointActionCount) {
        this.waypointActionCount = waypointActionCount;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(List<Waypoint> waypoints) {
        this.waypoints = waypoints;
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

            Mission mission = Mission.fromJson(missionJson);
            if (mission != null) {
                missions.add(mission);
            }
        }

        return missions;
    }

    private static Mission fromJson(JSONObject jsonObject) {
        Mission m = new Mission();
        // Deserialize json into object fields
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");

            m.id = jsonObject.getString("id");
            m.createdAt = format.parse(jsonObject.getString("created_at"));
            m.updatedAt = format.parse(jsonObject.getString("updated_at"));
            m.name = jsonObject.getString("name");
            m.description = jsonObject.optString("description", "");
            m.waypointCount = jsonObject.optInt("waypoint_count", 0);
            m.waypointActionCount = jsonObject.optInt("waypoint_action_count", 0);
            m.distance = jsonObject.optInt("distance", 0);
        } catch (JSONException | ParseException e) {
            e.printStackTrace();
            return null;
        }
        return m;
    }
}
