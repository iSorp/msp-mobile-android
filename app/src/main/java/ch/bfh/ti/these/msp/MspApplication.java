package ch.bfh.ti.these.msp;

import android.app.Application;
import android.content.Context;

import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import ch.bfh.ti.these.msp.dji.MavlinkAirlinkBridge;
import ch.bfh.ti.these.msp.mavlink.MavlinkBridge;
import ch.bfh.ti.these.msp.mavlink.MavlinkConfig;
import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;
import ch.bfh.ti.these.msp.mavlink.MavlinkUdpBridge;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;
import dji.sdk.sdkmanager.BluetoothProductConnector;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * Main application
 */
public class MspApplication extends Application {

    public static final String TAG = MspApplication.class.getName();

    private static BaseProduct product;
    private static Application app = null;
    private static MavlinkMaster mavlinkMaster;
    private static BluetoothProductConnector bluetoothConnector = null;

    public static Application getInstance() {
        return MspApplication.app;
    }

    /**
     * Gets instance of the specific product connected after the
     * API KEY is successfully validated. Please make sure the
     * API_KEY has been added in the Manifest
     */
    public static synchronized BaseProduct getProductInstance() {
        product = DJISDKManager.getInstance().getProduct();
        return product;
    }

    public static synchronized BluetoothProductConnector getBluetoothProductConnector() {
        bluetoothConnector = DJISDKManager.getInstance().getBluetoothProductConnector();
        return bluetoothConnector;
    }

    public static boolean isAircraftConnected() {
        return getProductInstance() != null && getProductInstance() instanceof Aircraft;
    }

    public static boolean isHandHeldConnected() {
        return getProductInstance() != null && getProductInstance() instanceof HandHeld;
    }

    public static synchronized Aircraft getAircraftInstance() {
        if (!isAircraftConnected()) {
            return null;
        }
        return (Aircraft) getProductInstance();
    }

    public static synchronized HandHeld getHandHeldInstance() {
        if (!isHandHeldConnected()) {
            return null;
        }
        return (HandHeld) getProductInstance();
    }

    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        //MultiDex.install(this);
        com.secneo.sdk.Helper.install(this);
        app = this;
    }


    public static void createMavlinkMaster() {
        int type = 0;
        if (getAircraftInstance() != null) {
            type = 1;
        }

        MavlinkBridge mavlinkBridge = null;

        int sport = 0;
        String ip = "";
        int tport = 0;
        int sysId = 0;
        int compId = 0;
        int timeout = 0;

        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getInstance().getBaseContext());
            sport = Integer.parseInt(prefs.getString("sourcePort", "0"));
            ip = prefs.getString("vehicleAddress", "");
            tport = Integer.parseInt(prefs.getString("vehiclePort", "0"));
            sysId = Integer.parseInt(prefs.getString("vehicleSystemId", "0"));
            compId = Integer.parseInt(prefs.getString("vehicleComponentId", "0"));
            timeout = Integer.parseInt(prefs.getString("mavTimeout", "0"));
        }catch (Exception e){
            e.printStackTrace();
        }

        if (mavlinkMaster != null) {
            try {
                mavlinkMaster.dispose();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (type == 0) {
            mavlinkBridge = new MavlinkUdpBridge(sport, ip, tport);

        }
        else {
            mavlinkBridge = new MavlinkAirlinkBridge();
        }

        mavlinkMaster = new MavlinkMaster(new MavlinkConfig
                .Builder(mavlinkBridge)
                .setTimeout(timeout)
                .setSystemId(sysId)
                .setComponentId(compId)
                .build());

        try { mavlinkMaster.connect(); }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static MavlinkMaster getMavlinkMaster() {
        return mavlinkMaster;
    }
}