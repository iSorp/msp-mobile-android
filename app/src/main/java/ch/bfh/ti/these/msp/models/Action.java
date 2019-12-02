package ch.bfh.ti.these.msp.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "actions",foreignKeys =
@ForeignKey(entity=WayPoint.class,parentColumns = "id",childColumns = "way_point_id"))
public class Action {

    @NonNull
    @PrimaryKey
    private String id;

    @ColumnInfo(name = "way_point_id")
    private String wayPointId;

    private int command;
    private int sensor;
    private float param1;
    private float param2;
    private float param3;
    private float param4;


    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getWayPointId() {
        return wayPointId;
    }

    public void setWayPointId(String wayPointId) {
        this.wayPointId = wayPointId;
    }

    public int getCommand() {
        return command;
    }

    public void setCommand(int command) {
        this.command = command;
    }

    public int getSensor() {
        return sensor;
    }

    public void setSensor(int sensor) {
        this.sensor = sensor;
    }

    public float getParam1() {
        return param1;
    }

    public void setParam1(float param1) {
        this.param1 = param1;
    }

    public float getParam2() {
        return param2;
    }

    public void setParam2(float param2) {
        this.param2 = param2;
    }

    public float getParam3() {
        return param3;
    }

    public void setParam3(float param3) {
        this.param3 = param3;
    }

    public float getParam4() {
        return param4;
    }

    public void setParam4(float param4) {
        this.param4 = param4;
    }

    static List<Action> fromJson(JSONArray jsonArray) {
        JSONObject actionJson;
        List<Action> actions = new ArrayList<>(jsonArray.length());

        for (int i=0; i < jsonArray.length(); i++) {
            try {
                actionJson = jsonArray.getJSONObject(i);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            Action action = Action.fromJson(actionJson);
            if (action != null) {
                actions.add(action);
            }
        }

        return actions;
    }

    private static Action fromJson(JSONObject jsonObject) {
        Action a = new Action();
        // Deserialize json into object fields
        try {
            a.id = jsonObject.getString("id");
            a.wayPointId = jsonObject.getString("waypoint_id");
            a.sensor = jsonObject.getInt("sensor_id");
            a.command = jsonObject.getInt("command_id");
            a.param1 = jsonObject.optLong("param1");
            a.param2 = jsonObject.optLong("param2");
            a.param3 = jsonObject.optLong("param3");
            a.param4 = jsonObject.optLong("param4");
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return a;
    }
}
