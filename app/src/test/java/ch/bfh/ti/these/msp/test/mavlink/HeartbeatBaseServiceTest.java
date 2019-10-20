package ch.bfh.ti.these.msp.test.mavlink;

import ch.bfh.ti.these.msp.mavlink.MavlinkBridge;
import ch.bfh.ti.these.msp.mavlink.MavlinkConfig;
import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;
import ch.bfh.ti.these.msp.mavlink.MavlinkUdpBridge;
import ch.bfh.ti.these.msp.test.mavlink.slaves.HeartbeatMicroServiceSlave;
import ch.bfh.ti.these.msp.test.mavlink.slaves.MavlinkTcpServer;
import com.google.gson.internal.Streams;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

import static ch.bfh.ti.these.msp.util.Definitions.MAVLINK_TEST_HOST;
import static ch.bfh.ti.these.msp.util.Definitions.MAVLINK_TEST_PORT;

public class HeartbeatBaseServiceTest {

    private static int BYTES = 4;
    private MavlinkTcpServer testServer;


    @Before
    public void tearUp() {
        /*try {
            testServer = new MavlinkTcpServer(new HeartbeatMicroServiceSlave());
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    @After
    public void tearDown() {
       /*try {
            testServer.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
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
    }
}
