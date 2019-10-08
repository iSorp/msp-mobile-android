package ch.bfh.ti.these.msp.mavlink.microservices;

public class MicroServiceException extends Exception {
    public MicroService microService;

    public MicroServiceException(MicroService microService, String message) {
        super(message);
        this.microService = microService;
    }

}
