package ch.bfh.ti.these.msp.mavlink.microservices;

import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;
import io.dronefleet.mavlink.MavlinkConnection;

import java.util.concurrent.*;

public abstract class BaseService {

    protected MavlinkMaster.ServiceTask listener;
    protected int systemId, componentId; // Target
    protected MavlinkConnection connection;


    /**
     *
     * @param connection
     * @param systemId
     * @param componentId
     */
    public BaseService(MavlinkConnection connection, int systemId, int componentId, MavlinkMaster.ServiceTask listener) {
        this.systemId = systemId;
        this.componentId = componentId;
        this.connection = connection;
        this.listener = listener;
    }

    protected <T> CompletableFuture<T> runAsync(BaseMicroService service) {

        CompletableFuture compf = new CompletableFuture();
        Executors.newSingleThreadExecutor().submit(()-> {
            try {
                service.addListener(listener);
                service.state.enter();
                compf.complete(service.call());
            }
            catch (Exception e){
                compf.completeExceptionally(e);
            }
            finally{
                service.removeListener(listener);
            }
        });
        return compf;
    }
}
