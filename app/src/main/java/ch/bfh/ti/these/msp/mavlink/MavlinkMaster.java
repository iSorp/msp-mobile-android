package ch.bfh.ti.these.msp.mavlink;

import java.io.EOFException;
import java.io.IOException;

import java.util.concurrent.*;

import ch.bfh.ti.these.msp.mavlink.microservices.BaseMicroService;
import ch.bfh.ti.these.msp.mavlink.microservices.HeartbeatBaseService;
import ch.bfh.ti.these.msp.mavlink.microservices.MissionBaseService;
import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;


public class MavlinkMaster {

    private boolean connected;
    private MavlinkConnection connection;
    private MavlinkConfig config;
    private MavlinkListener listener = new MavlinkListener();

    private MissionBaseService missionService;
    private HeartbeatBaseService heartbeatService;

    public MavlinkMaster(MavlinkConfig config) {
        this.config = config;
        connection = MavlinkConnection.create(
                config.getCommWrapper().getInputStream(),
                config.getCommWrapper().getOutputStream());

        this.sheredExecutor.submit(listener);

        missionService = new MissionBaseService(connection, config.getSystemId(), config.getComponentId(), listener);
        heartbeatService = new HeartbeatBaseService(connection, config.getSystemId(), config.getComponentId(), listener);
    }

    public boolean connect() throws InterruptedException, ExecutionException, TimeoutException {

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

    public HeartbeatBaseService getHeartbeatService() {
        return heartbeatService;
    }

    public MissionBaseService getMissionService() {
        return missionService;
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
                    System.out.println(eio.getMessage());
                }
            }
        }
    };
}
