package ch.bfh.ti.these.msp.test.mavlink;


import ch.bfh.ti.these.msp.mavlink.MavlinkConfig;
import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;
import ch.bfh.ti.these.msp.mavlink.MavlinkUdpBridge;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.*;
import java.util.concurrent.CompletableFuture;

public class HeartbeatServiceTest {

    private static int BYTES = 4;


    @Before
    public void tearUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void heartbeatMicroServiceTest() throws SocketException {

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
        catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        finally {
            mavlinkBridge.disconnect();
        }
    }
}
