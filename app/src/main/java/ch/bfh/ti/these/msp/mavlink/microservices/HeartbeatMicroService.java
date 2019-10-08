package ch.bfh.ti.these.msp.mavlink.microservices;

import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;
import io.dronefleet.mavlink.common.*;

import java.io.IOException;


public class HeartbeatMicroService extends MicroService {

    public HeartbeatMicroService(MavlinkConnection connection, int systemId, int componentId) {
        super(connection, systemId, componentId);
    }

    @Override
    public void execute() throws IOException, InterruptedException, MicroServiceException {

        switch (step) {

            // initialize
            case 0:
                System.out.println("Hearbeat master "+ step +": init" );
                state = EnumMicroServiceState.EXECUTE;
                ++step;
                break;

            // send heartbeat
            case 1:
                System.out.println("Hearbeat master "+ step +": Heartbeat" );
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
                System.out.println("Hearbeat master "+ step +": Heartbeat response" );
                MavlinkMessage message = takeMessage();
                if (message.getPayload() instanceof Heartbeat) {
                    Heartbeat payload = (Heartbeat)message.getPayload();
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
