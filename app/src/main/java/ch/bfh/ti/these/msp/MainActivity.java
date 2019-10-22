package ch.bfh.ti.these.msp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Build;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import ch.bfh.ti.these.msp.mavlink.MavlinkBridge;
import ch.bfh.ti.these.msp.mavlink.MavlinkConfig;
import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;
import ch.bfh.ti.these.msp.models.Mission;
import ch.bfh.ti.these.msp.models.WayPoint;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.log.DJILog;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.atomic.AtomicBoolean;


public class MainActivity extends AppCompatActivity {

    private TextView textView;
    private MavlinkMaster mavlinkMaster= null;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String[] REQUIRED_PERMISSION_LIST = new String[] {
            Manifest.permission.VIBRATE, // Gimbal rotation
            Manifest.permission.INTERNET, // API requests
            Manifest.permission.ACCESS_WIFI_STATE, // WIFI connected products
            Manifest.permission.ACCESS_COARSE_LOCATION, // Maps
            Manifest.permission.ACCESS_NETWORK_STATE, // WIFI connected products
            Manifest.permission.ACCESS_FINE_LOCATION, // Maps
            Manifest.permission.CHANGE_WIFI_STATE, // Changing between WIFI and USB connection
            Manifest.permission.WRITE_EXTERNAL_STORAGE, // Log files
            Manifest.permission.BLUETOOTH, // Bluetooth connected products
            Manifest.permission.BLUETOOTH_ADMIN, // Bluetooth connected products
            Manifest.permission.READ_EXTERNAL_STORAGE, // Log files
            Manifest.permission.READ_PHONE_STATE, // Device UUID accessed upon registration
            Manifest.permission.RECORD_AUDIO // Speaker accessory
    };
    private static final int REQUEST_PERMISSION_CODE = 12345;
    private List<String> missingPermission = new ArrayList<>();
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private BaseComponent.ComponentListener mDJIComponentListener = new BaseComponent.ComponentListener() {

        @Override
        public void onConnectivityChange(boolean isConnected) {
            Log.d(TAG, "onComponentConnectivityChanged: " + isConnected);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView)findViewById(R.id.textView1);
        Button btnMavlink = (Button)findViewById(R.id.btnMavlink);

        checkAndRequestPermissions();
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        String action = intent.getAction();
        if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
            Intent attachedIntent = new Intent();
            attachedIntent.setAction(DJISDKManager.USB_ACCESSORY_ATTACHED);
            sendBroadcast(attachedIntent);
        }
    }

    /**
     * Result of runtime permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
            Toast.makeText(getApplicationContext(), "Missing permissions!!!", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }
    }

    private void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    //ToastUtils.setResultToToast(MainActivity.this.getString(R.string.sdk_registration_doing_message));
                    DJISDKManager.getInstance().registerApp(MainActivity.this.getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                        @Override
                        public void onRegister(DJIError djiError) {
                            if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                                DJILog.e("App registration", DJISDKError.REGISTRATION_SUCCESS.getDescription());
                                DJISDKManager.getInstance().startConnectionToProduct();
                                setTextAsync();
                                //ToastUtils.setResultToToast(MainActivity.this.getString(R.string.sdk_registration_success_message));
                                //showDBVersion();
                            } else {
                                //ToastUtils.setResultToToast(MainActivity.this.getString(R.string.sdk_registration_message) + djiError.getDescription());
                            }
                            Log.v(TAG, djiError.getDescription());
                        }
                        @Override
                        public void onProductDisconnect() {
                            Log.d(TAG, "onProductDisconnect");

                        }
                        @Override
                        public void onProductConnect(BaseProduct baseProduct) {
                            Log.d(TAG, String.format("onProductConnect newProduct:%s", baseProduct));

                        }
                        @Override
                        public void onComponentChange(BaseProduct.ComponentKey componentKey,
                                                      BaseComponent oldComponent,
                                                      BaseComponent newComponent) {
                            if (newComponent != null) {
                                newComponent.setComponentListener(mDJIComponentListener);
                            }
                            Log.d(TAG,
                                    String.format("onComponentChange key:%s, oldComponent:%s, newComponent:%s",
                                            componentKey,
                                            oldComponent,
                                            newComponent));

                        }

                        @Override
                        public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i) {

                        }

                        @Override
                        public void onDatabaseDownloadProgress(long current, long total) {

                        }
                    });
                }
            });
        }
    }

    private void setTextAsync() {
        textView.post(new Runnable() {
            public void run() {
                textView.setText("sdk successful registered");
            }
        });
    }

    public void mavlinkConnect(View view) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                mavlinkSendMission();
            }
        });

    }

    private void mavlinkSendMission() {

        try {
            if (this.mavlinkMaster == null)
                this.mavlinkMaster = createMavlinkMaster();

            this.mavlinkMaster.connect();
            Mission m = new Mission();
            m.addWayPoint(new WayPoint());
            mavlinkMaster.getMissionService().uploadMission(m)
                    .thenAccept((a)-> {
                        textView.setText("Mission sent");
                    })
                    .exceptionally(throwable -> {
                        System.out.println("");
                        return null;
                    });
        }
        catch (Exception e) {
            this.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this.getBaseContext(),e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });



        }
    }


    private MavlinkMaster createMavlinkMaster() throws Exception {
        /*MavlinkConfig config = new MavlinkConfig
                .Builder(1, new MavlinkAirlinkBridge())
                .setTimeout(5000)
                .build();*/

        MavlinkConfig config = createMavlinkTCPWrapper();
        return new MavlinkMaster(config);
    }

    private MavlinkConfig createMavlinkTCPWrapper() throws Exception{

        MavlinkConfig config;
        Socket client = new Socket("192.168.1.132", 5001);
        config = new MavlinkConfig
                .Builder(1, new MavlinkBridge() {
            @Override
            public InputStream getInputStream() {
                InputStream is = null;
                try {  is = client.getInputStream();
                } catch (IOException e) { e.printStackTrace(); }
                return is;
            }

            @Override
            public OutputStream getOutputStream() {
                OutputStream os = null;
                try {  os = client.getOutputStream();
                } catch (IOException e) { e.printStackTrace(); }
                return os;
            }
        })
                .setTimeout(2000)
                .build();

        return config;
    }












}

