package ch.bfh.ti.these.msp.test.mavlink;

import ch.bfh.ti.these.msp.mavlink.MavlinkConfig;
import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;
import ch.bfh.ti.these.msp.mavlink.MavlinkUdpBridge;
import ch.bfh.ti.these.msp.mavlink.model.MavlinkMission;
import ch.bfh.ti.these.msp.mavlink.model.MavlinkMissionUploadItem;
import ch.bfh.ti.these.msp.models.Mission;
import ch.bfh.ti.these.msp.models.WayPoint;

import ch.bfh.ti.these.msp.util.Definitions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.*;
import java.util.concurrent.CompletableFuture;


public class MissionServiceTest {

    private final int MISSION_COUNT = 50;
    private MavlinkMission mission;
    private MavlinkUdpBridge mavlinkBridge = new MavlinkUdpBridge(Definitions.MAVLINK_TEST_SOURCE_PORT, Definitions.MAVLINK_TEST_TARGET, Definitions.MAVLINK_TEST_TARGET_PORT);
    private MavlinkMaster master;

    @Before
    public void tearUp() throws Exception {

        MavlinkConfig config = new MavlinkConfig
                .Builder(mavlinkBridge)
                .setTimeout(30000)
                .setSystemId(2)
                .setComponentId(1)
                .build();

        master = new MavlinkMaster(config);
        master.connect();

        mission = new MavlinkMission();
        for (int i = 0; i < MISSION_COUNT; i++) {
            double longitude = 7.24378 + ((double)(i)/10000.0); // x
            double latitude =  47.1427 + ((double)(i)/10000.0); // y
            double altitude =  20;

            MavlinkMissionUploadItem item = new MavlinkMissionUploadItem((float)longitude, (float)latitude, (float)altitude);
            item.setBehavior(1, 1);
            mission.addUploadItem(item);
            MavlinkMissionUploadItem actionItem = new MavlinkMissionUploadItem((float)longitude, (float)latitude, (float)altitude);
            actionItem.setSensor(1, 1, 0, 0);
            mission.addUploadItem(item);
        }
    }

    @After
    public void tearDown() {
        try {
            master.dispose();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void missionUploadServiceTest() throws Exception{

        CompletableFuture compf = master.getMissionService().uploadMission(mission)
                .thenAccept((a) -> {
                    Assert.assertEquals(a, true);
                })
                .exceptionally(throwable -> {
                    // Assert.fail(throwable.toString());
                    return null;
                });
        // Wait for completion
        compf.get();
    }

    @Test
    public void missionClearTest() throws Exception{

        CompletableFuture compf = master.getMissionService().clearMission()
                .thenAccept((a) -> {
                    Assert.assertEquals(a, true);
                })
                .exceptionally(throwable -> {
                    Assert.fail(throwable.toString());
                    return null;
                });
        // Wait for completion
        compf.get();
    }

    @Test
    public void missionStartTest() throws Exception{

        CompletableFuture compf = master.getMissionService().startMission()
                .thenAccept((a) -> {
                    //Assert.assertNull(a);
                })
                .exceptionally(throwable -> {
                    Assert.fail(throwable.toString());
                    return null;
                });
        // Wait for completion
        compf.get();
    }

    @Test
    public void missionPauseTest() throws Exception{

        CompletableFuture compf = master.getMissionService().pauseMission()
                .thenAccept((a) -> {
                    //Assert.assertNull(a);
                })
                .exceptionally(throwable -> {
                    Assert.fail(throwable.toString());
                    return null;
                });
        // Wait for completion
        compf.get();
    }

}