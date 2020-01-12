package ch.bfh.ti.these.msp.test.mavlink;


import ch.bfh.ti.these.msp.dji.MavlinkAirlinkBridge;
import ch.bfh.ti.these.msp.mavlink.MavlinkConfig;
import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;
import ch.bfh.ti.these.msp.mavlink.MavlinkUdpBridge;

import ch.bfh.ti.these.msp.util.Definitions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class HeartbeatServiceTest {

    private MavlinkUdpBridge mavlinkBridge = new MavlinkUdpBridge(Definitions.MAVLINK_TEST_SOURCE_PORT, Definitions.MAVLINK_TEST_TARGET, Definitions.MAVLINK_TEST_TARGET_PORT);
    private MavlinkMaster master;

    //private MavlinkAirlinkBridge mavlinkBridge = new MavlinkAirlinkBridge();

    @Before
    public void tearUp() throws Exception {
        mavlinkBridge.connect();

        MavlinkConfig config = new MavlinkConfig
                .Builder(mavlinkBridge)
                .setTimeout(30000)
                .setSystemId(2)
                .setComponentId(1)
                .build();

        master = new MavlinkMaster(config);
        master.connect();
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
    public void heartbeatServiceTest() throws Exception{

        CompletableFuture compf = master.getHeartbeatService().ping()
                .thenAccept((a)-> {
                    Assert.assertTrue(a);
                })
                .exceptionally(throwable -> {
                    Assert.fail(throwable.toString());
                    return null;
                });

            // Wait for completion
            compf.get();
    }
}
