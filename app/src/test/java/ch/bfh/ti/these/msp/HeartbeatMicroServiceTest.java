package ch.bfh.ti.these.msp;

import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;
import ch.bfh.ti.these.msp.models.Mission;
import ch.bfh.ti.these.msp.models.WayPoint;
import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;
import io.dronefleet.mavlink.common.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;

public class HeartbeatMicroServiceTest {

    private MavlinkTcpServer testServer;

    @Before
    public void tearUp() {
        testServer = new MavlinkTcpServer(new MavlinkHearbeatSlave());
    }

    @After
    public void tearDown() {

    }

    @Test
    public void heartbeatMicroServiceTest() {

        try (Socket socket = new Socket("localhost", 5001)) {
            MavlinkMaster master = new MavlinkMaster();
            master.connect(1, 1, socket.getInputStream(), socket.getOutputStream());

            CompletableFuture compf = master.sendHeartbeat()
                    .thenAccept((a) -> {
                        if (a != null)
                            System.out.println(a.toString());
                    })
                    .exceptionally(throwable -> {
                        System.out.println(throwable.toString());
                        return null;
                    });

            // Wait for completion
            compf.get();

        } catch (IOException eio) {
            System.out.println(eio.getMessage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    class MavlinkHearbeatSlave implements MavlinkSlave {

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
            } catch (EOFException eof) {
                System.out.println(eof.getMessage());
            } catch (IOException eio) {
                System.out.println(eio.getMessage());
            }
            return true;
        }
    }
}
