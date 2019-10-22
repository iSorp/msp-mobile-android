package ch.bfh.ti.these.msp.mavlink.microservices;


public class StateException extends Exception {
    public ServiceState state;

    public StateException(ServiceState state, String message) {
        super(message);
        this.state = state;
    }
}