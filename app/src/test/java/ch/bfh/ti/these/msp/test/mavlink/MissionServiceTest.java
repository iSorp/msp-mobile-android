package ch.bfh.ti.these.msp.test.mavlink;

import ch.bfh.ti.these.msp.mavlink.MavlinkConfig;
import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;
import ch.bfh.ti.these.msp.mavlink.MavlinkUdpBridge;
import ch.bfh.ti.these.msp.models.Mission;
import ch.bfh.ti.these.msp.models.WayPoint;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.*;
import java.util.concurrent.CompletableFuture;


public class MissionServiceTest {

    private final int MISSION_COUNT = 100;

    private Mission mission;


    @Before
    public void tearUp() {

        mission = new Mission();
        for (int i = 0; i < MISSION_COUNT; i++) {
            mission.addWayPoint(new WayPoint());
        }
    }

    @After
    public void tearDown() {
    }

    @Test
    public void missionMicroServiceTest() throws SocketException {
        MavlinkUdpBridge mavlinkBridge = new MavlinkUdpBridge();
        try {

            mavlinkBridge.connect();

            MavlinkConfig config = new MavlinkConfig
                    .Builder(1, mavlinkBridge)
                    .setTimeout(30000)
                    .setSystemId(1)
                    .setComponentId(1)
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

        }
        catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        finally {
            mavlinkBridge.disconnect();
        }
    }
}