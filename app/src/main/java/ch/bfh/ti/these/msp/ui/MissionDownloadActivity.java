package ch.bfh.ti.these.msp.ui;

import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import ch.bfh.ti.these.msp.MspApplication;
import ch.bfh.ti.these.msp.R;
import ch.bfh.ti.these.msp.mavlink.MavlinkDirParser;
import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;

import java.io.IOException;
import java.util.Locale;
import java.util.function.Consumer;

public class MissionDownloadActivity extends AppCompatActivity {

    private final static String MAVLINK_DATA_DIR = "/";

    private Button downloadButton;
    private Button clearButton;
    private TextView statusText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_download);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle("Sensor data transfer");
        }

        downloadButton = findViewById(R.id.btn_download_data);
        downloadButton.setEnabled(false);
        downloadButton.setOnClickListener(v -> {

        });
        clearButton = findViewById(R.id.btn_clear_data);
        clearButton.setEnabled(false);
        clearButton.setOnClickListener(v -> {

        });
        statusText = findViewById(R.id.txt_status);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mavlinkListDir(((response) -> {
            int count = MavlinkDirParser.countFiles(response);
            this.runOnUiThread(() -> {
                if (count > 0) {
                    downloadButton.setEnabled(true);
                    clearButton.setEnabled(true);
                    statusText.setText(String.format(Locale.ENGLISH, "%d files found", count));
                } else {
                    statusText.setText("Can not list files via Mavlink");
                }
            });
        }));
    }


    private void mavlinkListDir(Consumer<String> consumer) {
        Consumer<String> onError = (msg) -> {
            downloadButton.setEnabled(false);
            clearButton.setEnabled(false);
        };
        final MavlinkMaster mavlinkMaster = MspApplication.getMavlinkMaster();
        if (mavlinkMaster.isConnected()) {
            try {
                mavlinkMaster.getFtpService().listDirectory(MAVLINK_DATA_DIR)
                        .thenAccept(consumer)
                        .exceptionally(throwable -> {
                            onError.accept(throwable.getMessage());
                            return null;
                        });
            } catch (IOException e) {
                onError.accept(e.getMessage());
            }
        } else {
            statusText.setText("Device is not connected");
        }
    }
}
