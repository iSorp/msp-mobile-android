package ch.bfh.ti.these.msp.dji;

import dji.common.battery.BatteryState;
import dji.common.flightcontroller.FlightControllerState;

public interface DjiMessageListener {

    interface DjiBatteryStateListener extends DjiMessageListener {
        void batteryStateChanged(BatteryState state);
    }

    interface DjiFlightStateListener extends DjiMessageListener {
        void flightStateChanged(FlightControllerState state);
    }
}

