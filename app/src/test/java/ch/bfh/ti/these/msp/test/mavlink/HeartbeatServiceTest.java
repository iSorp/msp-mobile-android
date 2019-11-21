package ch.bfh.ti.these.msp.test.mavlink;


import ch.bfh.ti.these.msp.dji.MavlinkAirlinkBridge;
import ch.bfh.ti.these.msp.mavlink.MavlinkConfig;
import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;
import ch.bfh.ti.these.msp.mavlink.MavlinkUdpBridge;

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

    private MavlinkUdpBridge mavlinkBridge = new MavlinkUdpBridge();
    private MavlinkMaster master;

    @Before
    public void tearUp() throws Exception {
        mavlinkBridge.connect();

        MavlinkConfig config = new MavlinkConfig
                .Builder(1, mavlinkBridge)
                .setTimeout(30000)
                .setSystemId(1)
                .setComponentId(1)
                .build();

        master = new MavlinkMaster(config);
        master.connect();
    }

    @After
    public void tearDown() {
        mavlinkBridge.disconnect();
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
