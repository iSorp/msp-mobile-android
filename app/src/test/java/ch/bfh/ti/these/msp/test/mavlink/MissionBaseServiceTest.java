package ch.bfh.ti.these.msp.test.mavlink;

import ch.bfh.ti.these.msp.mavlink.MavlinkBridge;
import ch.bfh.ti.these.msp.mavlink.MavlinkConfig;
import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;
import ch.bfh.ti.these.msp.models.Mission;
import ch.bfh.ti.these.msp.models.WayPoint;
import ch.bfh.ti.these.msp.test.mavlink.slaves.MavlinkTcpServer;
import ch.bfh.ti.these.msp.test.mavlink.slaves.MissionMicroServiceSlave;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;

import static ch.bfh.ti.these.msp.util.Definitions.MAVLINK_TEST_HOST;
import static ch.bfh.ti.these.msp.util.Definitions.MAVLINK_TEST_PORT;


public class MissionBaseServiceTest {

    private final int MISSION_COUNT = 2;

    private MavlinkTcpServer testServer;
    private Mission mission;


    @Before
    public void tearUp() {
        try {
            testServer = new MavlinkTcpServer(new MissionMicroServiceSlave());
        } catch (IOException e) {
            e.printStackTrace();
        }

        mission = new Mission();
        for (int i = 0; i < MISSION_COUNT; i++) {
            mission.addWayPoint(new WayPoint());
        }
    }

    @After
    public void tearDown() {
        try {
            testServer.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void missionMicroServiceTest() {

        try (Socket socket = new Socket(MAVLINK_TEST_HOST, MAVLINK_TEST_PORT)) {
            MavlinkConfig config = new MavlinkConfig
                    .Builder(1, new MavlinkBridge() {
                @Override
                public InputStream getInputStream() {
                    InputStream is = null;
                    try {
                        is = socket.getInputStream();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return is;
                }

                @Override
                public OutputStream getOutputStream() {
                    OutputStream os = null;
                    try {
                        os = socket.getOutputStream();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return os;
                }
            })
                    .setTimeout(5000)
                    .build();
            MavlinkMaster master = new MavlinkMaster(config);
            master.connect();

            CompletableFuture compf = master.getMissionService().uploadMission(mission)
                    .thenAccept((a) -> {
                        Assert.assertNull(a);
                    })
                    .exceptionally(throwable -> {
                        Assert.fail(throwable.toString());
                        return null;
                    });

            // Wait for completion
            compf.get();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}