package ch.bfh.ti.these.msp.mavlink.microservices;

import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;
import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;
import io.dronefleet.mavlink.common.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static ch.bfh.ti.these.msp.util.Definitions.MAVLINK_GCS_COMP_ID;
import static ch.bfh.ti.these.msp.util.Definitions.MAVLINK_GCS_SYS_ID;


public class HeartbeatService extends BaseService {

    public HeartbeatService(MavlinkConnection connection, int systemId, int componentId, MavlinkMaster.MavlinkListener listener) {
        super(connection, systemId, componentId, listener);
    }

    public CompletableFuture<Boolean> ping() {
        return runAsync(new BaseMicroService<Boolean>(this.connection, new ServiceState() {

            @Override
            public void timeout() throws IOException {
                super.timeout();
                sendHearbeat();
            }

            @Override
            public void enter() throws IOException {
                getContext().setResult(false);
                sendHearbeat();
            }

            @Override
            public boolean execute() {

                if (getContext().message == null) return false;

                if (getContext().message.getPayload() instanceof Heartbeat) {
                    this.getContext().exit(true);
                }
                return true;
            }

            private void sendHearbeat() throws IOException {
                getContext().send(Heartbeat.builder()
                        .type(MavType.MAV_TYPE_GCS)
                        .autopilot(MavAutopilot.MAV_AUTOPILOT_INVALID)
                        .systemStatus(MavState.MAV_STATE_UNINIT)
                        .mavlinkVersion(3)
                        .build());

            }

        }));
    }

}
