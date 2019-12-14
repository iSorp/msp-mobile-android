package ch.bfh.ti.these.msp;

import android.app.Application;
import android.content.Context;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import androidx.preference.PreferenceManager;
import ch.bfh.ti.these.msp.dji.MavlinkAirlinkBridge;
import ch.bfh.ti.these.msp.mavlink.MavlinkBridge;
import ch.bfh.ti.these.msp.mavlink.MavlinkConfig;
import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;
import ch.bfh.ti.these.msp.mavlink.MavlinkUdpBridge;

import com.secneo.sdk.Helper;


/**
 * Main application
 */
public class MspApplication extends Application {

    private static MspApplication instance;
    private static MavlinkMaster mavlinkMaster;

    private DJIApplication djiApplication;


    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        instance = this;
        Helper.install(MspApplication.this);
        if (djiApplication == null) {
            djiApplication = new DJIApplication();
            djiApplication.setContext(this);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public static void startDjiRegistration() {
        instance.djiApplication.onCreate();
    }

    public static Application getInstance() {
        return MspApplication.instance;
    }

    public static void createMavlinkMasterConfig() {
        MavlinkBridge mavlinkBridge = null;

        boolean udp = false;
        int sport = 0;
        String ip = "";
        int tport = 0;
        int sysId = 0;
        int compId = 0;
        int timeout = 0;

        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getInstance().getBaseContext());
            udp = prefs.getBoolean("udpMavlinkEnable", false);
            sport = Integer.parseInt(prefs.getString("sourcePort", "0"));
            ip = prefs.getString("vehicleAddress", "");
            tport = Integer.parseInt(prefs.getString("vehiclePort", "0"));
            sysId = Integer.parseInt(prefs.getString("vehicleSystemId", "0"));
            compId = Integer.parseInt(prefs.getString("vehicleComponentId", "0"));
            timeout = Integer.parseInt(prefs.getString("mavTimeout", "0"));
        }catch (Exception e){
            e.printStackTrace();
        }

        // Airlink oder udp connection
        int type = 0;
        if (!udp && DJIApplication.getAircraftInstance() != null) {
            type = 1;
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

        getMavlinkMaster().setConfig(new MavlinkConfig
                .Builder(mavlinkBridge)
                .setTimeout(timeout)
                .setSystemId(sysId)
                .setComponentId(compId)
                .build());
    }
    public static void connectAsyncMavlinkMaster() {

        AsyncTask.execute(() -> {
            try { getMavlinkMaster().connect(); }
            catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    public static MavlinkMaster getMavlinkMaster() {
        if (mavlinkMaster == null)
            mavlinkMaster = new MavlinkMaster(null);
        return mavlinkMaster;
    }
}