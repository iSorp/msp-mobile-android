package ch.bfh.ti.these.msp.test.mavlink.slaves;

import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;
import io.dronefleet.mavlink.common.*;
import java.io.EOFException;
import java.io.IOException;

public class HeartbeatMicroServiceSlave implements MavlinkTcpServer.MavlinkSlave {

    int systemId = 1;
    int componentId = 1;

    public boolean execute(MavlinkConnection connection) {

        try {

            MavlinkMessage message;
            while ((message = connection.next()) != null) {

                if (message.getPayload() instanceof Heartbeat) {
                    connection.send1(systemId, componentId, Heartbeat.builder()
                            .type(MavType.MAV_TYPE_GENERIC)
                            .autopilot(MavAutopilot.MAV_AUTOPILOT_INVALID)
                            .systemStatus(MavState.MAV_STATE_UNINIT)
                            .mavlinkVersion(3)
                            .build());

                    System.out.println("Heartbeat slave: Heartbeat");
                }

            }
        }
        catch (EOFException eof) { }
        catch (IOException eio) {
            System.out.println(eio.getMessage());
        }
        return true;
    }
}
