package ch.bfh.ti.these.msp.test.mavlink.slaves;

import io.dronefleet.mavlink.common.*;

import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;

import java.io.EOFException;
import java.io.IOException;

public class MissionMicroServiceSlave  implements MavlinkTcpServer.MavlinkSlave {

    int systemId = 1;
    int componentId = 1;
    int itemCount = 1;

    public boolean execute(MavlinkConnection connection) {

        try {
            int numberOfItems = -1;
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
                else if (message.getPayload() instanceof MissionCount) {
                    if (numberOfItems < 0)
                    {
                        numberOfItems = ((MissionCount)message.getPayload()).count();
                    }

                    connection.send1(systemId, componentId, MissionRequestInt.builder()
                            .missionType(MavMissionType.MAV_MISSION_TYPE_MISSION)
                            .seq(itemCount)
                            .build());

                    System.out.println("Mission slave: MissionRequestInt");
                }
                else if (message.getPayload() instanceof MissionItem) {
                    ++itemCount;
                    if (itemCount > numberOfItems) {
                        connection.send1(systemId, componentId, MissionAck.builder()
                                .missionType(MavMissionType.MAV_MISSION_TYPE_MISSION)
                                .build());

                        System.out.println("Mission slave: MissionAck");
                        return false;
                    }
                    else {
                        connection.send1(systemId, componentId, MissionRequestInt.builder()
                                .missionType(MavMissionType.MAV_MISSION_TYPE_MISSION)
                                .seq(itemCount)
                                .build());

                        System.out.println("Mission slave: MissionRequestInt");
                    }
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