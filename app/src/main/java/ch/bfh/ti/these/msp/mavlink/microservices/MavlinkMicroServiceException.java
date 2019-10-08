package ch.bfh.ti.these.msp.mavlink.microservices;

public class MavlinkMicroServiceException extends Exception {
    public MicroService microService;

    public MavlinkMicroServiceException(MicroService microService, String message) {
        super(message);
        this.microService = microService;
    }

}
