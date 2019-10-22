package ch.bfh.ti.these.msp.mavlink.microservices;

import java.io.IOException;

public abstract class ServiceState<T extends BaseMicroService> {

    private T context;

    public ServiceState() { }

    public ServiceState(T context) {
        this.context = context;
    }

    public void setContext(T context) {
        this.context = context;
    }

    public T getContext() {
        return this.context;
    }

    /**
     * Abstract execution function for the service.
     * @throws IOException
     */
    protected abstract boolean execute() throws IOException, InterruptedException, StateException;

    public void enter() throws IOException {}
    public void exit() throws IOException {}
    public void timeout() throws IOException {
        ++context.retries;
    }
}
