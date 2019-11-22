package ch.bfh.ti.these.msp.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Action {

    private int command;
    private int sensor;
    private float param1;
    private float param2;
    private float param3;
    private float param4;


    public int getCommand() {
        return command;
    }

    public int getSensor() {
        return sensor;
    }

    public float getParam1() {
        return param1;
    }

    public float getParam2() {
        return param2;
    }

    public float getParam3() {
        return param3;
    }

    public float getParam4() {
        return param4;
    }

    public static List<Action> fromJson(JSONArray jsonArray) {
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
