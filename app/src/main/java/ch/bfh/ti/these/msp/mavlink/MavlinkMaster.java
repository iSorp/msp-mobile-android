package ch.bfh.ti.these.msp.mavlink;

import java.io.EOFException;
import java.io.IOException;

import java.util.ArrayList;
import java.util.TimerTask;
import java.util.Timer;
import java.util.concurrent.*;

import ch.bfh.ti.these.msp.mavlink.microservices.BaseMicroService;
import ch.bfh.ti.these.msp.mavlink.microservices.FtpService;
import ch.bfh.ti.these.msp.mavlink.microservices.HeartbeatService;
import ch.bfh.ti.these.msp.mavlink.microservices.MissionService;
import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;
import io.dronefleet.mavlink.common.Heartbeat;


public class MavlinkMaster {

    private boolean connected, listenerExit;
    private MavlinkConnection connection;
    private MavlinkConfig config;
    private MavlinkListener listener = new MavlinkListener();
    private ArrayList<MavlinkMessageListener> messageListeners = new ArrayList<>();
    private Thread listenerThread;
    private MissionService missionService;
    private FtpService ftpService;
    private HeartbeatService heartbeatService;


    public MavlinkMaster(MavlinkConfig config) {
        this.config = config;

        // Create a configuration specific mavlink connection
        connection = MavlinkConnection.create(
                config.getCommWrapper().getInputStream(),
                config.getCommWrapper().getOutputStream());

        // Add mavlink connection handler
        this.addMessageListener(connectionHandler);

        // initialize micro services
        missionService      = new MissionService(connection, config.getSystemId(), config.getComponentId(), listener);
        heartbeatService    = new HeartbeatService(connection, config.getSystemId(), config.getComponentId(), listener);
        ftpService          = new FtpService(connection, config.getSystemId(), config.getComponentId(), listener);
    }

    /**
     * Connects to a mavlink vehicle
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     * @throws IOException
     */
    public boolean connect() throws Exception {

        config.getCommWrapper().connect();

        // Start mavlink message listener
        listenerExit = false;
        listenerThread = new Thread(listener);
        listenerThread.setDaemon(true);
        listenerThread.start();

        CompletableFuture<Boolean> f = getHeartbeatService().ping()
                .exceptionally(throwable -> {
                    return false;
                });
        return f.get(config.getTimeout(), TimeUnit.MILLISECONDS);
    }

    public void dispose() throws Exception {
        this.config.getCommWrapper().disconnect();

        if (listenerThread != null) {
            listenerExit = true;
            listenerThread.interrupt();
            listenerThread.join();
        }

        connection = null;
        connected = false;
    }

    public void addMessageListener(MavlinkMessageListener listener) {
        messageListeners.add(listener);
    }

    public void removeMessageListener(MavlinkMessageListener listener) {
        messageListeners.remove(listener);
    }

    public boolean isConnected() {
        return connected;
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
                try{ Thread.sleep(100);}catch (InterruptedException e){
                    e.printStackTrace();
                }
            }

            while (!listenerExit) {
                try {
                    MavlinkMessage message;
                    while ((message = connection.next()) != null) {

                        // notify registered services
                        for (BaseMicroService service : serviceQueue) {
                            service.addMessage(message);
                        }

                        // TODO inform listeners asynchronous
                        // notify registered listeners
                        for (MavlinkMessageListener listener : messageListeners) {
                            listener.messageReceived(message);
                        }
                    }
                }
                catch (EOFException eof) { }
                catch (IOException eio) {
                    //System.out.println(eio.getMessage());
                }
            }
        }
    };

    /**
     * Handles the connection status of the configured system component
     */
    private MavlinkMessageListener connectionHandler = new MavlinkMessageListener() {

        TimeoutTask timeoutTask = new TimeoutTask();
        Timer timer = new Timer();

        @Override
        public void messageReceived(MavlinkMessage message) {
            if (message.getPayload() instanceof Heartbeat) {
                MavlinkMessage<Heartbeat> msg = (MavlinkMessage<Heartbeat>)message;

                connected = msg.getOriginSystemId() == config.getSystemId();
                if (connected) {
                    restartTimer();
                }
            }
        }

        class TimeoutTask extends TimerTask {
            @Override
            public void run() {
                connected = false;
            }
        }

        private void restartTimer() {
            timeoutTask.cancel();
            timeoutTask = new TimeoutTask();
            timer.schedule(timeoutTask, 2000);
            timer.purge();
        }

    };

}
