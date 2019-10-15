package ch.bfh.ti.these.msp.dji;

import ch.bfh.ti.these.msp.mavlink.MavlinkBridge;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static ch.bfh.ti.these.msp.MspApplication.getProductInstance;

public class MavlinkAirlinkBridge implements MavlinkBridge, FlightController.OnboardSDKDeviceDataCallback, CommonCallbacks.CompletionCallback {

    private Aircraft aircraft;
    private static int BYTES = 4;
    private byte[] receivedData;


    public InputStream getInputStream() {
        return this.is;
    }

    public OutputStream getOutputStream() {
        return this.os;
    }

    private InputStream is = new InputStream() {
        @Override
        public int read() throws IOException {
            synchronized (this) {
                int data = ByteBuffer.wrap(receivedData)
                        .order(ByteOrder.BIG_ENDIAN)
                        .asIntBuffer().get();
                receivedData = new byte[0];
                return data;
            }
        }
    };

    private OutputStream os = new OutputStream() {
        @Override
        public void write(int data) throws IOException {
            byte[] b = ByteBuffer.allocate(BYTES).putInt(data).array();
            MavlinkAirlinkBridge.this.send(b);
        }
    };

    public MavlinkAirlinkBridge() {
        aircraft = (Aircraft)getProductInstance();
        aircraft.getFlightController().setOnboardSDKDeviceDataCallback(this);
    }

    private void send(byte[] data) {
        aircraft.getFlightController().sendDataToOnboardSDKDevice(data, this);
    }

    @Override
    public void onReceive(byte[] bytes) {
        synchronized (this){
            receivedData = bytes;
        }
    }

    @Override
    public void onResult(DJIError djiError) {

    }
}