package ch.bfh.ti.these.msp.test.mavlink;

import ch.bfh.ti.these.msp.mavlink.*;
import ch.bfh.ti.these.msp.util.Definitions;
import io.dronefleet.mavlink.MavlinkMessage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;

public class ReceiverTest {
    private MavlinkUdpBridge mavlinkBridge = new MavlinkUdpBridge(Definitions.MAVLINK_TEST_SOURCE_PORT, Definitions.MAVLINK_TEST_TARGET, Definitions.MAVLINK_TEST_TARGET_PORT);
    private MavlinkMaster master;

    @Before
    public void tearUp() throws Exception {
        mavlinkBridge.connect();

        MavlinkConfig config = new MavlinkConfig
                .Builder(mavlinkBridge)
                .setTimeout(30000)
                .setSystemId(1)
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
    public void mavlinkReceiverTest() throws Exception {

        master.addMessageListener(new MavlinkMessageListener() {
            @Override
            public void messageReceived(MavlinkMessage message) {
                System.out.println(message.toString());
            }

            @Override
            public void connectionStatusChanged(MavlinkConnectionInfo info) {

            }
        });

        while (true) {
            Thread.sleep(100);
        }
    }
}