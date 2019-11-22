package ch.bfh.ti.these.msp.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class WayPoint {

    private float longitude, latitude, altitude;
    private List<Action> actions = new ArrayList<>();

    public WayPoint() { }

    public float getLongitude() {
        return longitude;
    }

    public float getLatitude() {
        return latitude;
    }

    public float getAltitude() {
        return altitude;
    }

    public List<Action> getActions() {
        return actions;
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
