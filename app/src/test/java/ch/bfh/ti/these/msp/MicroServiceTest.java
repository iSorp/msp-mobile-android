package ch.bfh.ti.these.msp;

import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;

import ch.bfh.ti.these.msp.models.Mission;
import ch.bfh.ti.these.msp.models.WayPoint;
import io.dronefleet.mavlink.common.*;
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

    final MavLinkTcpServer testServer = new MavLinkTcpServer();

    private final int MISSION_COUNT = 2;

    public MicroServiceTest(){

    }

    @Test
    public void mavLinkMasterTes() {
        Executors.newSingleThreadExecutor().submit(testServer);

        try (Socket socket = new Socket("localhost", 5001)) {
            MavlinkMaster master = new MavlinkMaster();
            master.connect(1,1, socket.getInputStream(), socket.getOutputStream());

            Mission mission = new Mission();
            mission.addWayPoint(new WayPoint());
            mission.addWayPoint(new WayPoint());

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


    class MavLinkTcpServer implements Runnable {

        @Override
        public void run() {
            ServerSocket server = null;
            Socket client = null;
            try {
                server = new ServerSocket(5001);

                client = server.accept();
                MavlinkConnection connection = MavlinkConnection.create(
                        client.getInputStream(),
                        client.getOutputStream());
                while (mavLinkMissionSlave(connection));
            }
            catch (IOException eio) {
                System.out.println(eio.getMessage());
            }
            finally {
                try {
                    client.close();
                }
                catch (IOException e) {

                }

            }
        }

        public boolean mavLinkMissionSlave(MavlinkConnection connection) {

            int systemId = 1;
            int componentId = 1;
            int itemCount = 1;

            try {

                MavlinkMessage message;
                while ((message = connection.next()) != null) {

                    if (message.getPayload() instanceof MissionCount) {
                        connection.send1(systemId, componentId, MissionRequestInt.builder()
                                .missionType(MavMissionType.MAV_MISSION_TYPE_MISSION)
                                .seq(itemCount)
                                .build());

                        System.out.println("Server: MissionRequestInt");

                    }
                    if (message.getPayload() instanceof MissionItem) {
                        ++itemCount;
                        if (itemCount > MISSION_COUNT) {
                            connection.send1(systemId, componentId, MissionAck.builder()
                                    .missionType(MavMissionType.MAV_MISSION_TYPE_MISSION)
                                    .build());

                            System.out.println("Server: MissionAck");
                            return false;
                        }
                        else {
                            connection.send1(systemId, componentId, MissionRequestInt.builder()
                                    .missionType(MavMissionType.MAV_MISSION_TYPE_MISSION)
                                    .seq(itemCount)
                                    .build());

                            System.out.println("Server: MissionRequestInt");
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