package ch.bfh.ti.these.msp.http;

import ch.bfh.ti.these.msp.models.Mission;
import ch.bfh.ti.these.msp.models.SensorData;
import ch.bfh.ti.these.msp.models.Waypoint;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MissionClient {

    private String host;
    private int port;

    private static MissionClient missionClient;

    public static MissionClient getInstance(String host, int port) {
        if (missionClient == null) {
            missionClient = new MissionClient(host, port);
        }
        return missionClient;
    }

    private MissionClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public List<Mission> getMissionList() {
        URL url;
        try {
            url = new URL("http", host, port, "mission");
        } catch (MalformedURLException e) {
            return new ArrayList<>();
        }

        JSONArray response = getRequest(url);
        List<Mission> missions = new ArrayList<>();
        if (response != null) {
            missions =  Mission.fromJson(response);
        }
        return missions;
    }

    public Mission getMission(String missionId) {
        URL url;
        Mission mission = new Mission();
        try {
            url = new URL("http", host, port, "mission/" + missionId + "/load");
        } catch (MalformedURLException e) {
            return mission;
        }

        JSONArray response = getRequest(url);

        if (response != null) {
            mission.setWaypoints(Waypoint.fromJson(response));
        }
        return mission;
    }

    public List<Integer> uploadSensorData(List<SensorData> sensorDataList) {
        URL url;
        try {
            url = new URL("http", host, port, "save");
        } catch (MalformedURLException e) {
            return new ArrayList<>();
        }
        List<Integer> statusCodes = new ArrayList<>(sensorDataList.size());

        for (SensorData sensorData : sensorDataList) {
            try {
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/json; utf-8");
                urlConnection.setReadTimeout(10000 );
                urlConnection.setConnectTimeout(15000);
                urlConnection.setDoOutput(true);
                urlConnection.connect();
                try(DataOutputStream os = new DataOutputStream(urlConnection.getOutputStream())) {
                    os.writeBytes(sensorData.toJson().toString());
                    os.flush();
                } catch (JSONException e) {
                    statusCodes.add(400);
                    continue;
                }
                statusCodes.add(urlConnection.getResponseCode());
                urlConnection.disconnect();
            } catch (IOException e) {
                statusCodes.add(500);
            }
        }
        return statusCodes;
    }

    private JSONArray getRequest(URL url) {
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setReadTimeout(10000 /* milliseconds */);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);
            // urlConnection.setDoOutput(true);
            urlConnection.connect();

            int status = urlConnection.getResponseCode();
            if (status == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
                StringBuilder sb = new StringBuilder();

                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                br.close();

                try {
                    return new JSONObject(sb.toString()).getJSONArray("data");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            urlConnection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
