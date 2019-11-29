package ch.bfh.ti.these.msp.mavlink;

import io.dronefleet.mavlink.MavlinkMessage;

public interface MavlinkMessageListener {
    void messageReceived(MavlinkMessage message);
    void connectionStatusChanged(MavlinkConnectionInfo info);
}
