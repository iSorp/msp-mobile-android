package ch.bfh.ti.these.msp;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import ch.bfh.ti.these.msp.dji.DjiMessageListener;
import dji.common.battery.BatteryState;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.flightcontroller.FlightControllerState;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.battery.Inspire2Battery;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.flightcontroller.FlightControllerBase;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class DJIApplication extends Application {

    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";
    public static final String FLAG_REGISTER_CHANGE = "dji_sdk_register_change";
    public static final String FLAG_DB_DOWNLOAD_CHANGE = "dji_sdk_db_download_change";


    private static final String TAG = DJIApplication.class.getName();
    private static DJIApplication instance;
    private static BaseProduct mProduct;

    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);

    private Handler handler;
    private int lastProcess = -1;
    private Application context;
    private ArrayList<DjiMessageListener.DjiFlightStateListener> flightStateMessageListeners = new ArrayList<>();
    private ArrayList<DjiMessageListener.DjiBatteryStateListener> batteryStateMessageListeners = new ArrayList<>();


    public DJIApplication() {
        instance = this;
    }

    @Override
    public Context getApplicationContext() {
        return context;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler(Looper.getMainLooper());
        startSDKRegistration();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }


    public void setContext(Application application) {
        context = application;
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

    public static synchronized Aircraft getAircraftInstance() {
        if (!isAircraftConnected()) return null;
        return (Aircraft) getProductInstance();
    }

    public static boolean isAircraftConnected() {
        return getProductInstance() != null && getProductInstance() instanceof Aircraft;
    }

    public static void addMessageListener(DjiMessageListener listener) {

        if (listener instanceof DjiMessageListener.DjiFlightStateListener)
            instance.flightStateMessageListeners.add((DjiMessageListener.DjiFlightStateListener)listener);

        if (listener instanceof DjiMessageListener.DjiBatteryStateListener)
            instance.batteryStateMessageListeners.add((DjiMessageListener.DjiBatteryStateListener)listener);
    }

    public static void removeMessageListener(DjiMessageListener listener) {

        if (listener instanceof DjiMessageListener.DjiFlightStateListener)
            instance.flightStateMessageListeners.remove((DjiMessageListener.DjiFlightStateListener)listener);

        if (listener instanceof DjiMessageListener.DjiBatteryStateListener)
            instance.batteryStateMessageListeners.remove((DjiMessageListener.DjiBatteryStateListener)listener);
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
                                newComponent.setComponentListener((isConnected)-> {
                                    Log.d(TAG, "onComponentConnectivityChanged: " + isConnected);
                                    notifyStatusChange(FLAG_CONNECTION_CHANGE);
                                });

                                switch (componentKey) {
                                    case FLIGHT_CONTROLLER:
                                        registerFlightControllerCallback((FlightControllerBase)newComponent);
                                        break;
                                    case BATTERY:
                                        registerBatteryCallback((Inspire2Battery)newComponent);
                                        break;
                                }
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

    private void registerBatteryCallback(Inspire2Battery inspire2Battery) {

        inspire2Battery.setStateCallback((BatteryState state) -> {
            try {
                for (DjiMessageListener.DjiBatteryStateListener listener : batteryStateMessageListeners) {
                    listener.batteryStateChanged(state);
                }
            }
            catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    private void registerFlightControllerCallback(FlightControllerBase flightControllerBase) {
        flightControllerBase.setStateCallback((FlightControllerState state)-> {
            try {

                for (DjiMessageListener.DjiFlightStateListener listener : flightStateMessageListeners) {
                    listener.flightStateChanged(state);
                }
            }
            catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

}