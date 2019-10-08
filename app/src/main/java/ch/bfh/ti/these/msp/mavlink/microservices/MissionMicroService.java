package ch.bfh.ti.these.msp.mavlink.microservices;

import ch.bfh.ti.these.msp.models.Mission;
import ch.bfh.ti.these.msp.models.WayPoint;
import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;
import io.dronefleet.mavlink.common.*;

import java.io.IOException;


public class MissionMicroService extends MicroService {

    private Mission mission;
    private int intemIndex = 0;

    public MissionMicroService(MavlinkConnection connection, int systemId, int componentId, Mission mission) {
        super(connection, systemId, componentId);
        this.mission = mission;
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

            // send mission count
            case 1:
                connection.send1(systemId, componentId, MissionCount.builder()
                        .missionType(MavMissionType.MAV_MISSION_TYPE_MISSION)
                        .count(mission.getWayPoints().size())
                        .build());
                ++step;
                break;
            // receive request int
            case 2:

                MavlinkMessage message = takeMessage();
                if (message.getPayload() instanceof MissionRequestInt) {
                    MissionRequestInt payload = (MissionRequestInt)message.getPayload();

                    if (payload.seq() == intemIndex+1) {
                        ++step;
                    }
                    else {
                        throw new MavlinkMicroServiceException(this, "Wrong sequence number");
                    }
                }
                break;
            // send mission item
            case 3:
                WayPoint p = mission.getWayPoints().get(intemIndex);
                connection.send1(systemId, componentId, MissionItem.builder()
                        .missionType(MavMissionType.MAV_MISSION_TYPE_MISSION)
                        .x((float)p.getLatitude())
                        .y((float)p.getLongitude())
                        .z((float)p.getAltitude())
                        .frame(MavFrame.MAV_FRAME_GLOBAL)
                        /* TODO action of the waypoint  .command(MavCmd.MAV_CMD_ACCELCAL_VEHICLE_POS)*/
                        .build());
                ++intemIndex;
                ++step;
                break;
            // wait for mission ACK
            case 4:
                if (intemIndex >= mission.getWayPoints().size()){
                    if (takeMessage().getPayload() instanceof MissionAck) {
                        // upload successful
                        step = 0;
                        state = EnumMicroServiceState.DONE;
                    }
                }
                else {
                    // upload next item?
                    step = 2;
                }
                break;
        }
    }
}
