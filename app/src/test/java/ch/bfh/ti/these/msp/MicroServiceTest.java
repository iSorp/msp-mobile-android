package ch.bfh.ti.these.msp;

import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;

import ch.bfh.ti.these.msp.models.Mission;
import ch.bfh.ti.these.msp.models.WayPoint;
import io.dronefleet.mavlink.common.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;

import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class MicroServiceTest {

    private MavlinkTcpServer testServer;
    private Mission mission;

    private final int MISSION_COUNT = 2;

    public MicroServiceTest(){

        Mission mission = new Mission();
        for (int i = 0; i < MISSION_COUNT; i++) {
            mission.addWayPoint(new WayPoint());
        }
    }

    @Before
    public void tearUp() {
        testServer = new MavlinkTcpServer(new MavlinkMissionSlave());

        mission = new Mission();
        for (int i = 0; i < MISSION_COUNT; i++) {
            mission.addWayPoint(new WayPoint());
        }
    }

    @After
    public void tearDown() {

    }

    @Test
    public void mavLinkMasterTest() {

        try (Socket socket = new Socket("localhost", 5001)) {
            MavlinkMaster master = new MavlinkMaster();
            master.connect(1,1, socket.getInputStream(), socket.getOutputStream());

            master.sendMissionAsync(mission)
                    .thenAccept((a)-> {
                        if (a != null)
                            System.out.println(a.toString());
                    })
                    .exceptionally(throwable -> {
                        System.out.println(throwable.toString());
                        return null;
                    }).get();
        }
        catch (IOException eio) {
            System.out.println(eio.getMessage());
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    class MavlinkMissionSlave implements MavlinkSlave {

        int systemId = 1;
        int componentId = 1;
        int itemCount = 1;

        public boolean execute(MavlinkConnection connection) {

            try {

                MavlinkMessage message;
                while ((message = connection.next()) != null) {

                    if (message.getPayload() instanceof MissionCount) {
                        connection.send1(systemId, componentId, MissionRequestInt.builder()
                                .missionType(MavMissionType.MAV_MISSION_TYPE_MISSION)
                                .seq(itemCount)
                                .build());

                        System.out.println("Mission slave: MissionRequestInt");

                    }
                    if (message.getPayload() instanceof MissionItem) {
                        ++itemCount;
                        if (itemCount > MISSION_COUNT) {
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
            } catch (EOFException eof) {
                System.out.println(eof.getMessage());
            }
            catch (IOException eio) {
                System.out.println(eio.getMessage());
            }
            return true;
        }
    }



}