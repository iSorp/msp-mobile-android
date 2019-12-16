package ch.bfh.ti.these.msp.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;

public class SensorData {

    private String waypointActionId;
    private String time;
    private float longitude;
    private float latitude;
    private float altitude;
    private String data;

    public void setWaypointActionId(String waypointActionId) {
        this.waypointActionId = waypointActionId;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public void setAltitude(float altitude) {
        this.altitude = altitude;
    }

    public void setData(String data) {
        this.data = data;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("waypoint_action_id", waypointActionId);
        obj.put("time", time);
        obj.put("long", longitude);
        obj.put("lat", latitude);
        obj.put("alt", altitude);
        obj.put("data", data);
        return obj;
    }
}
