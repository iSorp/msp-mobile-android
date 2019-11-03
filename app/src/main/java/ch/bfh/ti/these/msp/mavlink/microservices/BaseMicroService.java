package ch.bfh.ti.these.msp.mavlink.microservices;

import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;
import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;

import static ch.bfh.ti.these.msp.util.Definitions.*;

public class BaseMicroService<TResult> implements Callable<TResult> {

    protected ServiceState state;
    protected MavlinkConnection connection;
    protected MavlinkMessage message;

    private TResult result;
    private boolean exit = false; // true = ends the service loop
    private BlockingQueue<MavlinkMessage> messageQueue = new ArrayBlockingQueue<MavlinkMessage>(MAVLINK_MESSAGE_BUFFER);
    private long timeout = MAVLINK_RESPONSE_TIMEOUT;
    private Timer timer = new Timer();
    private TimeoutTask timeoutTask = new TimeoutTask();
    private volatile boolean timeoutReached;

    protected MavlinkMaster.MavlinkListener listener;
    protected int retries = 0;

    public BaseMicroService(MavlinkConnection connection) {
        this.connection = connection;
    }

    public BaseMicroService(MavlinkConnection connection, ServiceState initState) {
        this.connection = connection;
        initState.setContext(this);
        this.state = initState;
    }

    public void setState(ServiceState state) throws IOException {
        if (state!= null) {
            System.out.println("State exit : " + state.getClass());
            state.exit();
        }
        this.state = state;
        System.out.println("State enter : " + state.getClass());
        this.state.enter();
    }

    public void send(Object payload) throws IOException {
        connection.send1(MAVLINK_GCS_SYS_ID, MAVLINK_GCS_COMP_ID, payload);
    }

    public void exit(TResult result) {
        this.result = result;
        this.exit = true;
    }


    public void addListener(MavlinkMaster.MavlinkListener listener) {
        this.listener = listener;
        listener.addService(this);
    }

    public void removeListener(MavlinkMaster.MavlinkListener listener) {
        listener.removeService(this);
    }

    public void addMessage(MavlinkMessage message) {
        messageQueue.add(message);
    }

    /**
     * Takes a Mavlink message when ready out of the message queue
     *
     * @return
     * @throws InterruptedException
     */
    protected MavlinkMessage takeMessage() throws InterruptedException {
        // TODO change to interrupt
        MavlinkMessage mess = this.messageQueue.poll(10, TimeUnit.MILLISECONDS);
        //if (mess != null)
        //    System.out.println("State message received : " + state.getClass());
        return mess;
    }

    /**
     * Runs the service until the service work is finished
     *
     * @return
     * @throws Exception
     */
    @Override
    public TResult call() throws Exception {

        try {
            retries = 0;
            exit = false;
            restartTimer();
            while (!exit) {
                this.message = takeMessage();
                if (state.execute()) {
                    restartTimer();
                    retries = 0;
                }
                if (timeoutReached) {
                    if (retries < MAVLINK_MAX_RETRIES) {
                        state.timeout();
                        restartTimer();
                        System.out.println("State timeout : " + state.getClass());
                    }
                    else {
                        throw new TimeoutException();
                    }
                }
            }
        }
        finally {
            stopTimer();
        }

        return result;
    }

    /**
     * Timer task to manage timeouts
     */
    private class TimeoutTask extends TimerTask {
        @Override
        public void run() {
            timeoutReached = true;
        }
    }

    private void restartTimer() {
        timeoutReached = false;
        timeoutTask.cancel();
        timeoutTask = new TimeoutTask();
        timer.schedule(timeoutTask, timeout);
        timer.purge();
    }

    private void stopTimer() {
        timeoutTask.cancel();
        timer.purge();
    }

}
