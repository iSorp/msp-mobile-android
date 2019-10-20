package ch.bfh.ti.these.msp.mavlink.microservices;

import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;
import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;
import io.dronefleet.mavlink.common.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static ch.bfh.ti.these.msp.util.Definitions.MAVLINK_GCS_COMP_ID;
import static ch.bfh.ti.these.msp.util.Definitions.MAVLINK_GCS_SYS_ID;


public class HeartbeatBaseService extends BaseService {

    public HeartbeatBaseService(MavlinkConnection connection, int systemId, int componentId, MavlinkMaster.MavlinkListener listener) {
        super(connection, systemId, componentId, listener);
    }

    public CompletableFuture<Boolean> ping() {
        return runAsync(new BaseMicroService<Boolean>(connection, listener) {
            @Override
            protected void execute() throws IOException, InterruptedException, MicroServiceException {
                switch (step) {

                    // initialize
                    case 0:
                        System.out.println("Hearbeat master " + step + ": init");
                        state = EnumMicroServiceState.EXECUTE;
                        ++step;
                        break;

                    // send heartbeat
                    case 1:
                        connection.send1(MAVLINK_GCS_SYS_ID, MAVLINK_GCS_COMP_ID, Heartbeat.builder()
                                .type(MavType.MAV_TYPE_GCS)
                                .autopilot(MavAutopilot.MAV_AUTOPILOT_INVALID)
                                .systemStatus(MavState.MAV_STATE_UNINIT)
                                .mavlinkVersion(3)
                                .build());
                        System.out.println("Hearbeat master " + step + ": Heartbeat");
                        ++step;
                        break;
                    // receive heartbeat
                    case 2:
                        MavlinkMessage message = takeMessage();
                        if (message.getPayload() instanceof Heartbeat) {
                            System.out.println("Hearbeat master " + step + ": Heartbeat response");
                            Heartbeat payload = (Heartbeat) message.getPayload();
                            state = EnumMicroServiceState.DONE;

                            result = true; // heartbeat received
                        } else {
                            ++step;
                            Thread.sleep(100);
                        }
                        break;
                }
            }
        });
    }
}
