package ch.bfh.ti.these.msp.test.mavlink;

import ch.bfh.ti.these.msp.mavlink.MavlinkConfig;
import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;
import ch.bfh.ti.these.msp.mavlink.MavlinkMessageListener;
import ch.bfh.ti.these.msp.mavlink.MavlinkUdpBridge;

import io.dronefleet.mavlink.MavlinkMessage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileOutputStream;

import java.util.concurrent.CompletableFuture;


public class FtpServiceTest {

    private MavlinkUdpBridge mavlinkBridge = new MavlinkUdpBridge();
    private MavlinkMaster master;

    @Before
    public void tearUp() throws Exception {
       mavlinkBridge.connect();

        MavlinkConfig config = new MavlinkConfig.Builder(1, mavlinkBridge)
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
    public void ftpDownloadServiceTest() throws Exception{

        CompletableFuture compf = master.getFtpService().downloadFile("/home/simon/Develop/msp-onboard/wp0data.json")
                .thenAccept((a) -> {
                   try (FileOutputStream fos = new FileOutputStream("wp0data.json")) {
                        fos.write(a);
                    }
                    catch (Exception e){

                    }
                })
                .exceptionally(throwable -> {
                    Assert.fail(throwable.toString());
                    return null;
                });
        // Wait for completion
        compf.get();
    }
}