package ch.bfh.ti.these.msp.mavlink.microservices;

import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;
import ch.bfh.ti.these.msp.models.Mission;
import ch.bfh.ti.these.msp.models.WayPoint;
import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;
import io.dronefleet.mavlink.common.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;


public class MissionBaseService extends BaseService {

    public MissionBaseService(MavlinkConnection connection, int systemId, int componentId, MavlinkMaster.MavlinkListener listener) {
        super(connection, systemId, componentId, listener);
    }

    public CompletableFuture uploadMission(Mission mission) {

        return runAsync(new BaseMicroService(connection, listener) {

            int intemIndex = 0;

            @Override
            protected void execute() throws IOException, InterruptedException, MicroServiceException {
                switch (step) {

                    // initialize
                    case 0:
                        System.out.println("Mission master " + step + ": init");
                        state = EnumMicroServiceState.EXECUTE;
                        ++step;
                        break;

                    // send mission count
                    case 1:
                        System.out.println("Mission master " + step + ": MissionCount");
                        connection.send1(systemId, componentId, MissionCount.builder()
                                .missionType(MavMissionType.MAV_MISSION_TYPE_MISSION)
                                .count(mission.getWayPoints().size())
                                .build());
                        ++step;
                        break;
                    // receive request int
                    case 2:
                        System.out.println("Mission master " + step + ": MissionRequestInt");
                        MavlinkMessage message = takeMessage();
                        if (message.getPayload() instanceof MissionRequestInt) {
                            MissionRequestInt payload = (MissionRequestInt) message.getPayload();

                            if (payload.seq() == intemIndex + 1) {
                                ++step;
                            } else {
                                throw new MicroServiceException(this, "Wrong sequence number");
                            }
                        }
                        break;
                    // send mission item
                    case 3:
                        System.out.println("Mission master " + step + ": MissionItem");
                        WayPoint p = mission.getWayPoints().get(intemIndex);
                        connection.send1(systemId, componentId, MissionItem.builder()
                                .missionType(MavMissionType.MAV_MISSION_TYPE_MISSION)
                                .x((float) p.getLatitude())
                                .y((float) p.getLongitude())
                                .z((float) p.getAltitude())
                                .frame(MavFrame.MAV_FRAME_GLOBAL)
                                /* TODO action of the waypoint  .command(MavCmd.MAV_CMD_ACCELCAL_VEHICLE_POS)*/
                                .build());
                        ++intemIndex;
                        ++step;
                        break;
                    // wait for mission ACK
                    case 4:

                        if (intemIndex >= mission.getWayPoints().size()) {
                            System.out.println("Mission master " + step + ": MissionAck");
                            if (takeMessage().getPayload() instanceof MissionAck) {
                                // upload successful
                                step = 0;
                                state = EnumMicroServiceState.DONE;
                            }
                        } else {
                            System.out.println("Mission master " + step + ": next item");
                            // upload next item?
                            step = 2;
                        }
                        break;
                }
            }
        });
    }

    /*public CompletableFuture downloadMission() {
        return runAsync(new Download());
    }*/

}