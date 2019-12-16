package ch.bfh.ti.these.msp.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MavlinkData {
    private int seq;
    private String time;

    private List<SensorValue> sensors;

    private float x;
    private float y;
    private float z;

    public int getSeq() {
        return seq;
    }

    public String getTime() {
        return time;
    }

    public List<SensorValue> getSensors() {
        return sensors;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public static MavlinkData fromJson(JSONObject jsonObject) {
        MavlinkData mavlinkData = new MavlinkData();
        try {
            mavlinkData.time = jsonObject.getString("date");
            mavlinkData.seq = jsonObject.getInt("seq");
            mavlinkData.x = jsonObject.getLong("x");
            mavlinkData.y = jsonObject.getLong("y");
            mavlinkData.z = jsonObject.getLong("z");

            mavlinkData.sensors = SensorValue.fromJson(jsonObject.getJSONArray("sensors"));

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return mavlinkData;
    }

    public static class SensorValue {
        private int sensorId;
        private int commandId;
        private String value;

        public int getSensorId() {
            return sensorId;
        }

        public int getCommandId() {
            return commandId;
        }

        public String getValue() {
            return value;
        }

        private static SensorValue fromJson(JSONObject jsonObject) {
            SensorValue sensorValue = new SensorValue();

            try {
                sensorValue.sensorId = jsonObject.getInt("id");
                sensorValue.commandId = jsonObject.getInt("command_id");
                sensorValue.value = jsonObject.getString("value");
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }

            return sensorValue;
        }

        private static List<SensorValue> fromJson(JSONArray jsonArray) {
            JSONObject sensorDataJson;
            List<SensorValue> sensorValueList = new ArrayList<>(jsonArray.length());

            for (int i=0; i < jsonArray.length(); i++) {
                try {
                    sensorDataJson = jsonArray.getJSONObject(i);
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }

                SensorValue sensorValue = SensorValue.fromJson(sensorDataJson);
                if (sensorValue != null) {
                    sensorValueList.add(sensorValue);
                }
            }

            return sensorValueList;
        }
    }
}
