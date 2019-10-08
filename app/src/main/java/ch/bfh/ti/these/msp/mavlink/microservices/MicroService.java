package ch.bfh.ti.these.msp.mavlink.microservices;

import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;

import java.io.IOException;
import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;

import static ch.bfh.ti.these.msp.util.Definitions.*;


/**
 * The class MicroService defines an abstract Microservice for an higher level Mavlink API.
 *
 * @author  Samuel Ackermann, Simon Wälti
 * @version 1.0
 * @since   07-10-2019
 */
public abstract class MicroService<T> implements Callable<T> {

    protected enum EnumMicroServiceState {
        IDLE,
        EXECUTE,
        DONE
    }

    protected int systemId, componentId;
    protected MavlinkConnection connection;
    protected int step = 0;
    protected EnumMicroServiceState state;

    // If service does not supply a result, return null
    protected T result = null;

    private long timeout = 10000;
    private Timer timer = new Timer();
    private TimeoutTask timeoutTask = new TimeoutTask();
    private BlockingQueue<MavlinkMessage> messageQueue = new ArrayBlockingQueue<MavlinkMessage>(MAVLINK_MESSAGE_BUFFER);

    private volatile boolean timeoutReached;

    /**
     *
     * @param connection
     * @param systemId
     * @param componentId
     */
    public MicroService(MavlinkConnection connection, int systemId, int componentId) {
        this.systemId = systemId;
        this.componentId = componentId;
        this.connection = connection;
    }

    // region setter

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public void addMessage(MavlinkMessage message) {
        messageQueue.add(message);
    }

    /**
     * Runs the service until the service work is finished,
     *
     * @see EnumMicroServiceState
     * @return
     * @throws Exception
     */
    @Override
    public T call() throws Exception {
        state = EnumMicroServiceState.IDLE;

        try {
            while (state != EnumMicroServiceState.DONE) {
                restartTimer();
                execute();
                if (timeoutReached)
                    throw new TimeoutException();
            }
        }
        finally {
            stopTimer();
        }
        return result;
    }
    /**
     * Abstract execution function for the service.
     * @throws IOException
     */
    protected abstract void execute() throws IOException, InterruptedException, MavlinkMicroServiceException;

    /**
     * Takse a Mavlink message when ready out of the message queue
     *
     * @param <T>
     * @return
     * @throws InterruptedException
     */
    protected <T> MavlinkMessage<T> takeMessage() throws InterruptedException {
        return this.messageQueue.take();
    }

    /**
     * Timer task to manage timeouts
     */
    private class TimeoutTask extends TimerTask {
        @Override
        public void run() {
            timeoutReached = true;
        }
    };

    private void restartTimer() {
        timeoutTask.cancel();
        timeoutTask = new TimeoutTask();
        timer.schedule(timeoutTask, timeout);
        timer.purge();
    }

    private void stopTimer() {
        timeoutTask.cancel();
        timer.cancel();
        timer.purge();
    }

}