package ch.bfh.ti.these.msp.mavlink.microservices;

public class MicroServiceException extends Exception {
    public BaseMicroService baseMicroService;

    public MicroServiceException(BaseMicroService baseMicroService, String message) {
        super(message);
        this.baseMicroService = baseMicroService;
    }

}
