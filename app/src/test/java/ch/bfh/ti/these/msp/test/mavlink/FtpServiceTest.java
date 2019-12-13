package ch.bfh.ti.these.msp.test.mavlink;

import ch.bfh.ti.these.msp.mavlink.MavlinkConfig;
import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;
import ch.bfh.ti.these.msp.mavlink.MavlinkMessageListener;
import ch.bfh.ti.these.msp.mavlink.MavlinkUdpBridge;

import ch.bfh.ti.these.msp.util.Definitions;
import io.dronefleet.mavlink.MavlinkMessage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileOutputStream;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;


public class FtpServiceTest {

    private MavlinkUdpBridge mavlinkBridge = new MavlinkUdpBridge(Definitions.MAVLINK_TEST_SOURCE_PORT, Definitions.MAVLINK_TEST_TARGET, Definitions.MAVLINK_TEST_TARGET_PORT);
    private MavlinkMaster master;

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
    public void ftpDownloadServiceTest() throws Exception{

        CompletableFuture compf = master.getFtpService().downloadFile("wp1.json")
                .thenAccept((a) -> {
                   try (FileOutputStream fos = new FileOutputStream("wp0.json")) {
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

    @Test
    public void ftpListDirServiceTest() throws Exception{

        CompletableFuture compf = master.getFtpService().listDirectory("/")
                .thenAccept((a) -> {
                    String dirstring = a;
                    String[] entries = dirstring.split("\\\\0");
                    for (String entry: entries) {

                        char type = entry.charAt(0);
                        switch (type) {
                            case 'F':
                                String[] file = entry.split("\\\\t");
                                file[0] = file[0].substring(1);
                                System.out.println("File: " + file[0] + " size: " + file[1]);
                                break;
                            case 'D':
                                entry = entry.substring(1);
                                System.out.println("Directory: " + entry);
                                break;
                            case 'S':
                                System.out.println("skip");
                                break;
                        }
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