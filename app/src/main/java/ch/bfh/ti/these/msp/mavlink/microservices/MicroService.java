package ch.bfh.ti.these.msp.mavlink.microservices;

import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;


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
    protected Timer timer = new Timer();
    protected T result;

    private long timeout = 30000;
    private TimeoutTask timeoutTimer;
    private BlockingQueue<MavlinkMessage> messageQueue = new ArrayBlockingQueue<MavlinkMessage>(1);

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

    public void setMessage(MavlinkMessage message) {
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
        while (state != EnumMicroServiceState.DONE) {

            // TODO better solution for Timeout, timeout after each step or after the whole service?
            timer = new Timer();
            timer.schedule(new TimeoutTask(), timeout);
            execute();
            timer.cancel();

            if (timeoutReached)
                throw new TimeoutException();
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

}