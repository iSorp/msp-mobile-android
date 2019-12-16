package ch.bfh.ti.these.msp.mavlink;

import java.io.EOFException;
import java.io.IOException;

import java.util.ArrayList;
import java.util.concurrent.*;
import static java.util.concurrent.TimeUnit.*;

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
    private MavlinkTask mavlinkTask = new MavlinkTask();
    private ArrayList<MavlinkMessageListener> messageListeners = new ArrayList<>();
    private MissionService missionService;
    private FtpService ftpService;
    private HeartbeatService heartbeatService;


    public MavlinkMaster(MavlinkConfig config) {
        this.config = config;
    }

    /**
     * Sets a new Mavlinkmaster configuration. After the call a reconnection is necessary
     * @param config
     */
    public void setConfig(MavlinkConfig config) {
        this.config = config;
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

        // Create a configuration specific mavlink connection
        connection = MavlinkConnection.create(
                config.getCommWrapper().getInputStream(),
                config.getCommWrapper().getOutputStream());

        // Initialize micro services
        missionService = new MissionService(connection, config.getSystemId(), config.getComponentId(), mavlinkTask);
        heartbeatService = new HeartbeatService(connection, config.getSystemId(), config.getComponentId(), mavlinkTask);
        ftpService = new FtpService(connection, config.getSystemId(), config.getComponentId(), mavlinkTask);


        // Start the communication wrapper
        config.getCommWrapper().connect();

        // Start mavlink message listener
        mavlinkTask.Start();

        // Try to connect. On fail, the vehicle will be connected (bind) on the first received package
        CompletableFuture<Boolean> f = getHeartbeatService().ping()
                .exceptionally(throwable -> {
                    return false;
                });
        return f.get(config.getTimeout(), TimeUnit.MILLISECONDS);
    }

    public void dispose() throws Exception {
        if (config != null)
            config.getCommWrapper().disconnect();
        if (mavlinkTask != null)
            mavlinkTask.Stop();

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


    public interface ServiceTask {
        void addService(BaseMicroService service);
        void removeService(BaseMicroService service);
    }
    /**
     *  Mavlink message listener, it follows the producer consumer principles.
     *  All micro services are consumer synchronized with this thread.
     */
    public final class MavlinkTask implements ServiceTask {

        private final static int CONNECTION_TIMEOUT = 3;

        private ScheduledExecutorService scheduler;
        private BlockingQueue<BaseMicroService> serviceQueue = new LinkedBlockingDeque<>();
        private volatile MavlinkConnectionInfo info = new MavlinkConnectionInfo();
        private boolean lastConnState, connectionValid, connectionChanged;
        private boolean exit = false;
        private final Object syncObject = new Object();

        public void addService(BaseMicroService service) {
            serviceQueue.add(service);
        }
        public void removeService(BaseMicroService service) {
            serviceQueue.remove(service);
        }


        public void Start() {
            exit = false;
            scheduler = Executors.newScheduledThreadPool(2);
            scheduler.execute(messageHandler);
            scheduler.scheduleAtFixedRate(connectionHandler, 3, CONNECTION_TIMEOUT, SECONDS);
        }

        public void Stop() {
            exit = true;
            if (scheduler != null)
                scheduler.shutdown();
        }

        final Runnable messageHandler = new Runnable() {

            @Override
            public void run() {

                while (!exit) {

                    try {
                        MavlinkMessage message;
                        while ((message = connection.next()) != null) {

                            lastConnState = message.getOriginSystemId() == config.getSystemId();

                            if (lastConnState){
                                // notify registered services
                                for (BaseMicroService service : serviceQueue) {
                                    service.addMessage(message);
                                }

                                // notify registered listeners
                                for (MavlinkMessageListener listener : messageListeners) {
                                    listener.messageReceived(message);
                                }

                                connectionValid = true;
                            }

                            // Connection state handling, synchronized with connectionHandler
                            synchronized (syncObject) {

                                if (connected != lastConnState) {
                                    connected = lastConnState;
                                    connectionChanged = true;   // checked by connectionHandler

                                    info = new MavlinkConnectionInfo(
                                            message.getOriginSystemId(),
                                            message.getOriginComponentId(),
                                            connected);
                                }
                            }

                        }
                    } catch (EOFException eof) {
                    } catch (IOException eio) {
                        //System.out.println(eio.getMessage());
                    }
                }
            }
        };

        final Runnable connectionHandler = new Runnable() {

            @Override
            public void run() {

                boolean notify = false;

                synchronized (syncObject) {

                    // Notify listener when:
                    // - connection status has changed
                    // - connection status was true but no package received in the last n seconds
                    if (connectionChanged || (!connectionValid && connected)) {
                        connectionChanged = false;

                        if (!connectionValid) {
                            connected = false;
                            info = new MavlinkConnectionInfo();
                        }

                        notify = true;
                    }
                    // need to be set to true on next received package
                    connectionValid = false;
                }

                if (notify) {
                    for (MavlinkMessageListener listener : messageListeners) {
                        listener.connectionStatusChanged(info);
                    }
                }

                if (!connected) {
                    // Send a heartbeat every n seconds
                    getHeartbeatService().sendHeartbeat();
                }
            }
        };
    };
}
