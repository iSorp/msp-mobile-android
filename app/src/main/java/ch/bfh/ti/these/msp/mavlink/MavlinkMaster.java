package ch.bfh.ti.these.msp.mavlink;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.*;

import ch.bfh.ti.these.msp.mavlink.microservices.HeartbeatMicroService;
import ch.bfh.ti.these.msp.mavlink.microservices.MicroService;
import ch.bfh.ti.these.msp.mavlink.microservices.MissionMicroService;
import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;

import ch.bfh.ti.these.msp.models.Mission;


public class MavlinkMaster {

    private int systemId, componentId;
    private MavlinkConnection connection;
    private Thread readerThread;
    private BlockingQueue<MicroService> serviceQueue = new ArrayBlockingQueue<MicroService>(1);

    public MavlinkMaster() {}

    public void connect(int systemId, int componentId, InputStream is, OutputStream os) {
        if (connection == null) {
            this.systemId = systemId;
            this.componentId = componentId;
            connection = MavlinkConnection.create(is, os);
            readerThread = new Thread(mavlinkListener);
            readerThread.start();
        }
    }

    public CompletableFuture sendHeartbeat() throws Exception {
        if (connection == null)
            throw new Exception("Master not connected");
        return runAsync(new HeartbeatMicroService(connection, systemId, componentId));
    }

    public CompletableFuture sendMissionAsync(Mission mission) throws Exception {
        if (connection == null)
            throw new Exception("Master not connected");
        return runAsync(new MissionMicroService(connection, systemId, componentId, mission));
    }

    private CompletableFuture runAsync(MicroService service) throws Exception {
        CompletableFuture compf = new CompletableFuture();
        Executors.newCachedThreadPool().submit(()-> {
            try {
                serviceQueue.add(service);
                compf.complete(service.call());
            }
            catch (Exception e){
                compf.completeExceptionally(e);
            }
            finally{
                serviceQueue.remove(service);
            }
            return null;
        });
        return compf;
    }

    /**
     *  Mavlink message listener, it follows the producer consumer principles.
     *  All micro services are consumer synchronized with this thread.
     */
    private Runnable mavlinkListener = new Runnable() {
        @Override
        public void run() {
            while (true) {

                try {
                    MavlinkMessage message;

                    while ((message = connection.next()) != null) {
                        for (MicroService service : serviceQueue) {
                            service.addMessage(message);
                        }

                        // -> notify registered listeners <-
                    }
                }
                catch (EOFException eof) { }
                catch (IOException eio) {
                    System.out.println(eio.getMessage());
                }
            }
        }
    };
}
