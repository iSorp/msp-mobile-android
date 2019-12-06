package ch.bfh.ti.these.msp;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class DJIApplication extends Application {

    private static final String TAG = DJIApplication.class.getName();
    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";
    public static final String FLAG_REGISTER_CHANGE = "dji_sdk_register_change";
    public static final String FLAG_DB_DOWNLOAD_CHANGE = "dji_sdk_db_download_change";

    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private static BaseProduct mProduct;
    private Handler handler;
    private DJISDKManager.SDKManagerCallback mDJISDKManagerCallback;
    private int lastProcess = -1;
    private Application instance;


    public void setContext(Application application) {
        instance = application;
    }

    @Override
    public Context getApplicationContext() {
        return instance;
    }


    public DJIApplication() {

    }

    /**
     * Gets instance of the specific product connected after the
     * API KEY is successfully validated. Please make sure the
     * API_KEY has been added in the Manifest
     */
    public static synchronized BaseProduct getProductInstance() {
        if (null == mProduct) {
            mProduct = DJISDKManager.getInstance().getProduct();
        }
        return mProduct;
    }

    public static boolean isAircraftConnected() {
        return getProductInstance() != null && getProductInstance() instanceof Aircraft;
    }

    public static synchronized Aircraft getAircraftInstance() {
        if (!isAircraftConnected()) return null;
        return (Aircraft) getProductInstance();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler(Looper.getMainLooper());
        startSDKRegistration();
    }



    private void startSDKRegistration() {

        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    DJISDKManager.getInstance().registerApp(getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                        @Override
                        public void onRegister(DJIError djiError) {
                            if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                                DJISDKManager.getInstance().startConnectionToProduct();
                            }
                            notifyStatusChange(FLAG_REGISTER_CHANGE);
                            Log.v(TAG, djiError.getDescription());
                        }

                        @Override
                        public void onProductDisconnect() {
                            Log.d(TAG, "onProductDisconnect");
                            notifyStatusChange(FLAG_CONNECTION_CHANGE);

                        }
                        @Override
                        public void onProductConnect(BaseProduct baseProduct) {
                            Log.d(TAG, String.format("onProductConnect newProduct:%s", baseProduct));
                            notifyStatusChange(FLAG_CONNECTION_CHANGE);

                        }
                        @Override
                        public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent,
                                                      BaseComponent newComponent) {

                            if (newComponent != null) {
                                newComponent.setComponentListener(new BaseComponent.ComponentListener() {

                                    @Override
                                    public void onConnectivityChange(boolean isConnected) {
                                        Log.d(TAG, "onComponentConnectivityChanged: " + isConnected);
                                        notifyStatusChange(FLAG_CONNECTION_CHANGE);
                                    }
                                });
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

                            int process = (int) (100 * current / total);
                            if (process == lastProcess) {
                                return;
                            }
                            lastProcess = process;
                            notifyStatusChange(FLAG_DB_DOWNLOAD_CHANGE, process);
                            if (process % 25 == 0){
                                notifyStatusChange(FLAG_DB_DOWNLOAD_CHANGE, process);
                            }else if (process == 0){
                                notifyStatusChange(FLAG_DB_DOWNLOAD_CHANGE, process);
                            }
                        }
                    });
                }
            });
        }
    }

    private void notifyStatusChange(String flag) {
        notifyStatusChange(flag, 0);
    }

    private void notifyStatusChange(String flag, int value) {

        Runnable updateRunnable = () -> {
            Intent intent = new Intent(flag);
            intent.putExtra("value", value);
            getApplicationContext().sendBroadcast(intent);
        };

        handler.removeCallbacks(updateRunnable);
        handler.postDelayed(updateRunnable, 500);
    }
}