package ch.bfh.ti.these.msp.dji;

import ch.bfh.ti.these.msp.mavlink.MavlinkBridge;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Semaphore;

import static ch.bfh.ti.these.msp.MspApplication.getProductInstance;

public class MavlinkAirlinkBridge implements MavlinkBridge, FlightController.OnboardSDKDeviceDataCallback, CommonCallbacks.CompletionCallback {

    private Aircraft aircraft;

    Semaphore readBuffer = new Semaphore(0);
    Semaphore writeBuffer = new Semaphore(1);
    Semaphore transfereBuffer = new Semaphore(1);

    private byte[] buffer = new byte[2048];
    private boolean empty = true;
    private int readPos = -1;

    public MavlinkAirlinkBridge() {
        aircraft = (Aircraft)getProductInstance();
    }

    public void connect() {
        aircraft.getFlightController().setOnboardSDKDeviceDataCallback(this);
    }

    public void disconnect() {
        aircraft.getFlightController().setOnboardSDKDeviceDataCallback(null);
    }

    public InputStream getInputStream() {
        return this.is;
    }

    public OutputStream getOutputStream() {
        return this.os;
    }

    private InputStream is = new InputStream() {
        @Override
        public int read() throws IOException {

            int ret = -1;

            // Wait for data
            if (empty) {
                try {
                    readBuffer.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return -1;
                }

                if (buffer.length > 0) {
                    empty = false;
                    readPos   = 0;
                }
            }

            // read buffer
            if (!empty) {
                ret = 0xff;
                ret = ret & (buffer[readPos++]);

                if (readPos >= buffer.length) {
                    empty = true;
                    readPos   = -1;
                    writeBuffer.release();
                }
            }

            return ret;
        }

    };

    private OutputStream os = new OutputStream() {
        @Override
        public void write(int data) throws IOException { }

        @Override
        public void write(byte b[]) throws IOException {
            MavlinkAirlinkBridge.this.send(b);
        }
    };

    private void send(byte[] data) {
        int writePos = 0;

        // DJI supports only a data size of 100 bytes per transfere
        if (data.length > 100) {
            try {
                while (writePos < data.length) {

                    // wait for callback response (if waiting for response is not necessary the sync can be removed)
                    transfereBuffer.acquire();

                    int length = Math.min(data.length - writePos, 100);
                    byte[] buf = new byte[length];
                    System.arraycopy(data, writePos, buf, 0, length);
                    writePos += length;
                    aircraft.getFlightController().sendDataToOnboardSDKDevice(buf, this);
                }
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        else {
            aircraft.getFlightController().sendDataToOnboardSDKDevice(data, this);
        }
    }

    @Override
    public void onReceive(byte[] bytes) {

        try {
            writeBuffer.acquire();
            buffer = bytes;
            readBuffer.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResult(DJIError djiError) {
        System.out.println(djiError.getDescription());
        if (transfereBuffer.availablePermits() == 0)
            transfereBuffer.release();
    }
}