package ch.bfh.ti.these.msp.mavlink.microservices;

import ch.bfh.ti.these.msp.models.Mission;
import ch.bfh.ti.these.msp.models.WayPoint;
import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;
import io.dronefleet.mavlink.common.*;

import java.io.IOException;


public class HeartbeatMicroService extends MicroService {

    public HeartbeatMicroService(MavlinkConnection connection, int systemId, int componentId) {
        super(connection, systemId, componentId);
    }

    @Override
    public void execute() throws IOException, InterruptedException, MavlinkMicroServiceException {

        System.out.println("Client :" +step);
        switch (step) {

            // initialize
            case 0:
                state = EnumMicroServiceState.EXECUTE;
                ++step;
                break;

            // send heartbeat
            case 1:
                connection.send1(systemId, componentId, Heartbeat.builder()
                        .type(MavType.MAV_TYPE_GCS)
                        .autopilot(MavAutopilot.MAV_AUTOPILOT_INVALID)
                        .systemStatus(MavState.MAV_STATE_UNINIT)
                        .mavlinkVersion(3)
                        .build());
                ++step;
                break;
            // receive heartbeat
            case 2:

                MavlinkMessage message = takeMessage();
                if (message.getPayload() instanceof Heartbeat) {
                    MissionRequestInt payload = (MissionRequestInt)message.getPayload();
                    state = EnumMicroServiceState.DONE;
                }
                else {
                    ++step;
                    Thread.sleep(100);
                }
                break;

        }
    }
}
