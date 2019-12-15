package ch.bfh.ti.these.msp.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import ch.bfh.ti.these.msp.DJIApplication;
import ch.bfh.ti.these.msp.MspApplication;
import ch.bfh.ti.these.msp.R;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

import java.util.ArrayList;
import java.util.List;

public class RegisterFragment extends Fragment {


    public interface OnRegisterCompleteListener {
        void onRegisterComplete();
        void onProductConnect();
        void onProductDisconnect();
    }


    private TextView textView;
    private ProgressBar progressBar;

    private  Handler handler;

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

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_PERMISSION_CODE = 12345;
    private List<String> missingPermission = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler(Looper.getMainLooper());
        // When the compile and target version is higher than 22, please request the following permission at runtime to ensure the SDK works well.
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkAndRequestPermissions();
       // }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupView();

        // Register the broadcast receiver for receiving the device connection's changes.
        IntentFilter filter = new IntentFilter();
        filter.addAction(DJIApplication.FLAG_CONNECTION_CHANGE);
        filter.addAction(DJIApplication.FLAG_REGISTER_CHANGE);
        filter.addAction(DJIApplication.FLAG_DB_DOWNLOAD_CHANGE);
        getActivity().registerReceiver(djiReceiver, filter);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(djiReceiver);
    }

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(getActivity(), eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            MspApplication.startDjiRegistration();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(getActivity(),
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
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
            MspApplication.startDjiRegistration();
        } else {
            Toast.makeText(getActivity().getApplicationContext(), "Missing permissions!!!", Toast.LENGTH_LONG).show();
        }
    }

    private void setupView() {
        textView = (TextView)getActivity().findViewById(R.id.textView1);
        progressBar = getActivity().findViewById(R.id.progressBar);

        progressBar.setEnabled(true);
        progressBar.setVisibility(View.VISIBLE);
    }

    private BroadcastReceiver djiReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(DJIApplication.FLAG_REGISTER_CHANGE)) {
                if (DJISDKManager.getInstance().hasSDKRegistered()) {
                    setStatusTextAsync("Register Success");
                    getListener().onRegisterComplete();
                }
                else {
                    setStatusTextAsync("Register sdk fails, check network is available");
                }
            }
            else if (intent.getAction().equals(DJIApplication.FLAG_DB_DOWNLOAD_CHANGE)) {
                int process = intent.getIntExtra("value", 0);
                showProgress(process);
                setStatusTextAsync("DB load process : " + process);
            }
            else if (intent.getAction().equals(DJIApplication.FLAG_CONNECTION_CHANGE)) {
                BaseProduct product = DJIApplication.getProductInstance();
                if (product != null) {
                    setStatusTextAsync("Product connected");
                    handler.postDelayed(()-> {
                        progressBar.setVisibility(View.INVISIBLE);
                        progressBar.setProgress(0);

                    }, 1000);
                }
            }
        }
    };


    private void setStatusTextAsync(String text) {
        textView.post(() -> { textView.setText(text); });
    }

    private void showProgress(final int process){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
                if (process >= 0)
                    progressBar.setProgress(process);
            }
        });
    }

    private OnRegisterCompleteListener getListener() {
        if (getActivity() instanceof OnRegisterCompleteListener) {
            return (OnRegisterCompleteListener) getActivity();
        } else {
            throw new ClassCastException(getActivity().toString()
                    + " must implement RegisterFragment.OnRegisterCompleteListener");
        }
    }
}
