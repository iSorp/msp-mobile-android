package ch.bfh.ti.these.msp.mavlink.microservices;

import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;
import io.dronefleet.mavlink.MavlinkConnection;

import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class BaseService {
    protected MavlinkMaster.MavlinkListener listener;
    protected int systemId, componentId;
    protected MavlinkConnection connection;

    private final Lock runLock;

    /**
     *
     * @param connection
     * @param systemId
     * @param componentId
     */
    public BaseService(MavlinkConnection connection, int systemId, int componentId, MavlinkMaster.MavlinkListener listener) {
        this.systemId = systemId;
        this.componentId = componentId;
        this.connection = connection;
        this.listener = listener;
        this.runLock = new ReentrantLock();
    }

    protected <T> CompletableFuture<T> runAsync(BaseMicroService service) {
        runLock.lock();

        CompletableFuture compf = new CompletableFuture();
        Executors.newSingleThreadExecutor().submit(()-> {
            try {
                service.addListener(listener);
                compf.complete(service.call());
            }
            catch (Exception e){
                compf.completeExceptionally(e);
            }
            finally{
                service.removeListener(listener);
                runLock.unlock();
            }
        });
        return compf;
    }
}
