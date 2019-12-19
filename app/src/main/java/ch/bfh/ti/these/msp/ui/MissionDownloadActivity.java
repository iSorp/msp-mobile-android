package ch.bfh.ti.these.msp.ui;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import ch.bfh.ti.these.msp.MspApplication;
import ch.bfh.ti.these.msp.R;
import ch.bfh.ti.these.msp.db.ActionDao;
import ch.bfh.ti.these.msp.db.MissionDatabase;
import ch.bfh.ti.these.msp.http.MissionClient;
import ch.bfh.ti.these.msp.mavlink.MavlinkDirParser;
import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;
import ch.bfh.ti.these.msp.mavlink.microservices.FtpService;
import ch.bfh.ti.these.msp.mavlink.model.Converter;
import ch.bfh.ti.these.msp.models.MavlinkData;
import ch.bfh.ti.these.msp.models.SensorData;
import ch.bfh.ti.these.msp.util.Definitions;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static ch.bfh.ti.these.msp.util.Definitions.BACKEND_HOST;
import static ch.bfh.ti.these.msp.util.Definitions.BACKEND_PORT;

public class MissionDownloadActivity extends AppCompatActivity {

    private final static String MAVLINK_DATA_DIR = "/";

    private Button downloadButton;
    private Button clearButton;
    private TextView statusText;

    private int count;
    private List<String[]> fileList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_download);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle("Sensor Data Transfer");
        }

        downloadButton = findViewById(R.id.btn_download_data);
        downloadButton.setEnabled(false);
        downloadButton.setOnClickListener(v -> new DownloadMissionResult().execute());
        clearButton = findViewById(R.id.btn_clear_data);
        clearButton.setEnabled(false);
        clearButton.setOnClickListener(v -> new ClearMissionResult().execute());
        statusText = findViewById(R.id.txt_status);
        Button backButton = findViewById(R.id.btn_back);
        backButton.setOnClickListener(v -> this.finish());
    }

    @Override
    protected void onResume() {
        super.onResume();

        count = 0;
        fileList = Collections.emptyList();
        mavlinkExecCommand(
                MavlinkFtpCommand.ListDir,
                MAVLINK_DATA_DIR,
               (String response) -> {
                    count = MavlinkDirParser.countFiles(response);
                    fileList = MavlinkDirParser.getFiles(response);
                    this.runOnUiThread(() -> {
                        if (count > 0) {
                            downloadButton.setEnabled(true);
                            clearButton.setEnabled(true);
                            statusText.setText(String.format(Locale.ENGLISH, "%d files found", count));
                        } else {
                            statusText.setText("No files found");
                        }
                    });
                }
        );
    }

    private <T> void mavlinkExecCommand(MavlinkFtpCommand command, String arg, Consumer<? super T> consumer) {
        mavlinkExecCommand(command, arg, consumer, true);
    }

    private <T> CompletableFuture<T> mavlinkExecCommand(MavlinkFtpCommand command, String arg, Consumer<? super T> consumer, boolean handleException) {
        Consumer<String> onError = (msg) -> {
            downloadButton.setEnabled(false);
            clearButton.setEnabled(false);
            statusText.setText(msg);
        };
        final MavlinkMaster mavlinkMaster = MspApplication.getMavlinkMaster();
        if (mavlinkMaster.isConnected()) {
            try {
                CompletableFuture<T> future;
                final FtpService ftpService = mavlinkMaster.getFtpService();
                switch (command) {
                    case ListDir:
                        future = (CompletableFuture<T>)ftpService.listDirectory(arg);
                        break;
                    case Download:
                        future = (CompletableFuture<T>)ftpService.downloadFile(arg);
                        break;
                    case Clear:
                        future = (CompletableFuture<T>)ftpService.deletedFile(arg);
                        break;
                    default:
                        return null;
                }
                future.thenAccept(consumer);
                if (handleException) {
                    future.exceptionally(throwable -> {
                        throwable.printStackTrace();
                        MissionDownloadActivity.this.runOnUiThread(() -> onError.accept(throwable.getMessage()));
                        return null;
                    });
                }
                return future;
            } catch (IOException e) {
                onError.accept(e.getMessage());
            }
        } else {
            this.runOnUiThread(() -> statusText.setText("Device is not connected"));
        }
        return null;
    }

    private class DownloadMissionResult extends AsyncTask<Void, Void, MavlinkFtpResult<List<MavlinkData>>> {

        @Override
        protected MavlinkFtpResult<List<MavlinkData>> doInBackground(Void... voids) {
            MavlinkFtpResult<List<MavlinkData>> result = new MavlinkFtpResult<>();
            result.payload = new ArrayList<>();
            for (String[] file : fileList) {
                CompletableFuture cf = mavlinkExecCommand(
                        MavlinkFtpCommand.Download,
                        MAVLINK_DATA_DIR + file[0],
                        (byte[] response) -> {
                            try {
                                result.payload.add(MavlinkData.fromJson(new JSONObject(new String(response))));
                            } catch (JSONException e) {
                                result.status = false;
                                result.msgs.add("Can not parse JSON " + file[0]);
                            }
                        },
                        true
                    );
                if (cf != null) {
                    cf.join();
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(MavlinkFtpResult<List<MavlinkData>> result) {
            super.onPostExecute(result);
            if (result.status) {
                statusText.setText("All files downloaded");
                new UploadWaypointData(
                        MissionDownloadActivity.this.getApplication(),
                        MissionDownloadActivity.this.statusText
                ).execute(result.payload);
            } else {
                statusText.setText(result.msgs.stream().collect(Collectors.joining()));
            }
        }
    }

    private class ClearMissionResult extends AsyncTask<Void, Void, MavlinkFtpResult<byte[]>> {

        @Override
        protected MavlinkFtpResult<byte[]> doInBackground(Void... voids) {
            MavlinkFtpResult<byte[]> result = new MavlinkFtpResult<>();
            for (String[] file : fileList) {
                CompletableFuture cf = mavlinkExecCommand(
                        MavlinkFtpCommand.Clear,
                        MAVLINK_DATA_DIR + file[0],
                        (byte[] response) -> {
                            fileList.remove(file);
                        },
                        true
                );
                if (cf != null) {
                    cf.join();
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(MavlinkFtpResult<byte[]> result) {
            super.onPostExecute(result);
            if (result.status) {
                statusText.setText("All files cleared");
            }
        }
    }

    private static class UploadWaypointData extends AsyncTask<List<MavlinkData>, Void, List<Integer>> {

        private final MissionClient client;
        private final ActionDao actionDao;
        private final TextView statusText;

        private UploadWaypointData(Application app, TextView statusText) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(app);
            String host = prefs.getString("backendAddress", BACKEND_HOST);
            int port = Integer.parseInt(prefs.getString("backendPort", ""+BACKEND_PORT));
            client = MissionClient.getInstance(host, port);
            actionDao = MissionDatabase.getInstance(app).actionDao();
            this.statusText = statusText;
        }

        @Override
        protected List<Integer> doInBackground(List<MavlinkData>... waypointDataList) {
            List<SensorData> allSensorData = new ArrayList<>(waypointDataList[0].size());
            for (MavlinkData da : waypointDataList[0]) {
                List<SensorData> sensorData = Converter.convertToSensorData(da, actionDao);
                allSensorData.addAll(sensorData);
            }
            return client.uploadSensorData(allSensorData);
        }

        @Override
        protected void onPostExecute(List<Integer> integers) {
            super.onPostExecute(integers);
            long count = integers.stream().filter(status -> status == 200).count();
            this.statusText.setText(String.format("%1$d/%2$d uploaded", count, integers.size()));
        }
    }

    private class MavlinkFtpResult<T> {
        private MavlinkFtpResult() {
            status = true;
            msgs = new ArrayList<>();
        }

        boolean status;
        List<String> msgs;

        T payload;
    }

    private enum MavlinkFtpCommand {
        ListDir,
        Download,
        Clear
    }
}
