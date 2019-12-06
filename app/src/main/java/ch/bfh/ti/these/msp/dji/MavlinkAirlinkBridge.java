package ch.bfh.ti.these.msp.dji;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import ch.bfh.ti.these.msp.DJIApplication;
import ch.bfh.ti.these.msp.MspApplication;
import ch.bfh.ti.these.msp.mavlink.MavlinkBridge;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Semaphore;


public class MavlinkAirlinkBridge implements MavlinkBridge {

    private Aircraft aircraft;

    Semaphore readBuffer = new Semaphore(0);
    Semaphore writeBuffer = new Semaphore(1);
    Semaphore transfereBuffer = new Semaphore(1);

    private byte[] buffer = new byte[2048];
    private boolean empty = true;
    private int readPos = -1;

    public MavlinkAirlinkBridge() {

        // Register the broadcast receiver for receiving the device connection's changes.
        IntentFilter filter = new IntentFilter();
        filter.addAction(DJIApplication.FLAG_CONNECTION_CHANGE);
        MspApplication.getInstance().registerReceiver(djiReceiver, filter);

        aircraft = DJIApplication.getAircraftInstance();
    }

    public void connect() {
        if (DJIApplication.isAircraftConnected()) {
            aircraft.getFlightController().setOnboardSDKDeviceDataCallback(sdkDeviceDataCallback);
        }
    }

    public void disconnect() {
        if (DJIApplication.isAircraftConnected()) {
            aircraft.getFlightController().setOnboardSDKDeviceDataCallback(null);
        }
    }

    private BroadcastReceiver djiReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(DJIApplication.FLAG_CONNECTION_CHANGE)) {
                Aircraft aircraft = DJIApplication.getAircraftInstance();
                if (aircraft != null) {
                    if(aircraft.isConnected()) {
                        aircraft.getFlightController().setOnboardSDKDeviceDataCallback(sdkDeviceDataCallback);
                    }
                    else {
                        aircraft.getFlightController().setOnboardSDKDeviceDataCallback(null);
                    }
                }
            }
        }
    };

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
                    readPos = 0;
                }
            }

            // read buffer
            if (!empty) {
                ret = 0xff;
                ret = ret & (buffer[readPos++]);

                if (readPos >= buffer.length) {
                    empty = true;
                    readPos = -1;
                    writeBuffer.release();
                }
            }

            return ret;
        }

    };

    private OutputStream os = new OutputStream() {
        @Override
        public void write(int data) throws IOException {
        }

        @Override
        public void write(byte b[]) throws IOException {
            MavlinkAirlinkBridge.this.send(b);
        }
    };

    private void send(byte[] data) {
        if (aircraft == null) return;

        int writePos = 0;

        // DJI supports only a data size of 100 bytes per transfere
        if (data.length > 300) {
            try {
                while (writePos < data.length) {

                    // wait for callback response (if waiting for response is not necessary the sync can be removed)
                    transfereBuffer.acquire();

                    int length = Math.min(data.length - writePos, 300);
                    byte[] buf = new byte[length];
                    System.arraycopy(data, writePos, buf, 0, length);
                    writePos += length;
                    aircraft.getFlightController().sendDataToOnboardSDKDevice(buf, completionCallback);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            aircraft.getFlightController().sendDataToOnboardSDKDevice(data, completionCallback);
        }
    }


    private FlightController.OnboardSDKDeviceDataCallback sdkDeviceDataCallback = (byte[] data) -> {
        try {
            writeBuffer.acquire();
            buffer = data;
            readBuffer.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    };

    private CommonCallbacks.CompletionCallback completionCallback = (DJIError djiError) -> {

        if (djiError != null)
            System.out.println(djiError.getDescription());
        if (transfereBuffer.availablePermits() == 0)
            transfereBuffer.release();
    };
}