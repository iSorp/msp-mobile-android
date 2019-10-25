package ch.bfh.ti.these.msp.mavlink;

import java.io.EOFException;
import java.io.IOException;

import java.util.concurrent.*;

import ch.bfh.ti.these.msp.mavlink.microservices.BaseMicroService;
import ch.bfh.ti.these.msp.mavlink.microservices.FtpService;
import ch.bfh.ti.these.msp.mavlink.microservices.HeartbeatService;
import ch.bfh.ti.these.msp.mavlink.microservices.MissionService;
import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;


public class MavlinkMaster {

    private boolean connected;
    private MavlinkConnection connection;
    private MavlinkConfig config;
    private MavlinkListener listener = new MavlinkListener();

    private MissionService missionService;
    private FtpService ftpService;
    private HeartbeatService heartbeatService;

    public MavlinkMaster(MavlinkConfig config) {
        this.config = config;
        connection = MavlinkConnection.create(
                config.getCommWrapper().getInputStream(),
                config.getCommWrapper().getOutputStream());

        this.sheredExecutor.submit(listener);

        missionService = new MissionService(connection, config.getSystemId(), config.getComponentId(), listener);
        heartbeatService = new HeartbeatService(connection, config.getSystemId(), config.getComponentId(), listener);
        ftpService = new FtpService(connection, config.getSystemId(), config.getComponentId(), listener);

    }

    public boolean connect() throws InterruptedException, ExecutionException, TimeoutException, IOException {

        CompletableFuture<Boolean> f = getHeartbeatService().ping()
                .exceptionally(throwable -> {
                    System.out.println("");
                    return null;
                });

        return f.get(config.getTimeout(), TimeUnit.MILLISECONDS);
    }

    public void dispose() {
        sheredExecutor.shutdown();
        connection = null;
        connected = false;
    }


    // region services

    public HeartbeatService getHeartbeatService() {
        return heartbeatService;
    }

    public MissionService getMissionService() {
        return missionService;
    }

    public FtpService getFtpService() {
        return ftpService;
    }


    private ExecutorService sheredExecutor = Executors.newSingleThreadExecutor(
        new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
                return t;
            }
        });

    /**
     *  Mavlink message listener, it follows the producer consumer principles.
     *  All micro services are consumer synchronized with this thread.
     */
    public class MavlinkListener implements Runnable {

        private BlockingQueue<BaseMicroService> serviceQueue = new LinkedBlockingDeque<>();

        public void addService(BaseMicroService service) {
            serviceQueue.add(service);
        }

        public void removeService(BaseMicroService service) {
            serviceQueue.remove(service);
        }

        @Override
        public void run() {

            while (connection == null){
                try{ Thread.sleep(100);}catch (InterruptedException e){}
            }

            while (true) {
                try {
                    MavlinkMessage message;
                    while ((message = connection.next()) != null) {
                        for (BaseMicroService service : serviceQueue) {
                            service.addMessage(message);
                        }
                        // -> notify registered listeners if necessary<-
                    }
                }
                catch (EOFException eof) { }
                catch (IOException eio) {
                    //System.out.println(eio.getMessage());
                }
            }
        }
    };
}
