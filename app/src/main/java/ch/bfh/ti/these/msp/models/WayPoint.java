package ch.bfh.ti.these.msp.models;

import androidx.annotation.NonNull;
import androidx.room.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "way_points",foreignKeys =
@ForeignKey(entity=Mission.class,parentColumns = "id",childColumns = "mission_id"))
public class WayPoint {

    @NonNull
    @PrimaryKey
    private String id;
    @ColumnInfo(name = "mission_id")
    private String missionId;
    private float longitude;
    private float latitude;
    private float altitude;

    @Ignore
    private List<Action> actions = new ArrayList<>();

    public WayPoint() { }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getMissionId() {
        return missionId;
    }

    public void setMissionId(String missionId) {
        this.missionId = missionId;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getAltitude() {
        return altitude;
    }

    public void setAltitude(float altitude) {
        this.altitude = altitude;
    }

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

    public static List<WayPoint> fromJson(JSONArray jsonArray) {
        JSONObject waypointJson;
        List<WayPoint> wayPoints = new ArrayList<>(jsonArray.length());

        for (int i=0; i < jsonArray.length(); i++) {
            try {
                waypointJson = jsonArray.getJSONObject(i);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            WayPoint wayPoint = WayPoint.fromJson(waypointJson);
            if (wayPoint != null) {
                wayPoints.add(wayPoint);
            }
        }

        return wayPoints;
    }

    private static WayPoint fromJson(JSONObject jsonObject) {
        WayPoint m = new WayPoint();
        // Deserialize json into object fields
        try {
            m.id = jsonObject.getString("id");
            m.missionId = jsonObject.getString("mission_id");
            m.longitude = (float) jsonObject.getDouble("long");
            m.latitude = (float) jsonObject.getDouble("lat");
            m.altitude = (float) jsonObject.getDouble("alt");
            JSONArray actions = jsonObject.optJSONArray("actions");
            if (actions != null) {
                m.actions = Action.fromJson(actions);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return m;
    }
}
