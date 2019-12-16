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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static ch.bfh.ti.these.msp.util.Definitions.OSDK_DATA_MAX_SIZE;


public class MavlinkAirlinkBridge implements MavlinkBridge {

    private volatile Aircraft aircraft;

    private final Semaphore readBuffer = new Semaphore(0);        // read buffer from mvalink (read input stream)
    private final Semaphore writeBuffer = new Semaphore(1);       // fill buffer from OSDK (provide data for input stream)
    private final Semaphore transfereBuffer = new Semaphore(1);   // send data to OSDK and wait (transfereBuffer) for response
    private final Object aircraftLock = new Object();

    private byte[] buffer = new byte[0];
    private boolean empty = true;
    private int readPos = -1;

    public MavlinkAirlinkBridge() {

        // Register the broadcast receiver for receiving the device connection's changes.
        IntentFilter filter = new IntentFilter();
        filter.addAction(DJIApplication.FLAG_COMPONENT_CHANGE);
        MspApplication.getInstance().registerReceiver(djiReceiver, filter);
    }

    public void connect() {
        if (DJIApplication.isAircraftConnected()) {
            synchronized (aircraftLock) {
                aircraft = DJIApplication.getAircraftInstance();
                aircraft.getFlightController().setOnboardSDKDeviceDataCallback(sdkDeviceDataCallback);
            }
        }
    }

    public void disconnect() {
        synchronized (aircraftLock) {
            if (aircraft != null && DJIApplication.isAircraftConnected()) {
                aircraft.getFlightController().setOnboardSDKDeviceDataCallback(null);
            }
        }
    }

    private BroadcastReceiver djiReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(DJIApplication.FLAG_COMPONENT_CHANGE)) {
                BaseProduct.ComponentKey componentKey = (BaseProduct.ComponentKey)intent.getSerializableExtra("component");
                boolean isConnected = intent.getBooleanExtra("isConnected", false);
                if (componentKey == BaseProduct.ComponentKey.FLIGHT_CONTROLLER) {
                    synchronized (aircraftLock) {
                        aircraft = DJIApplication.getAircraftInstance();
                        if (aircraft != null) {
                            if (isConnected) {
                                aircraft.getFlightController().setOnboardSDKDeviceDataCallback(sdkDeviceDataCallback);
                            } else {
                                aircraft.getFlightController().setOnboardSDKDeviceDataCallback(null);
                            }
                        }
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

    private FlightController.OnboardSDKDeviceDataCallback sdkDeviceDataCallback = (byte[] data) -> {
        try {
            if (writeBuffer.tryAcquire(100, TimeUnit.MILLISECONDS)) {
                buffer = data;
                readBuffer.release();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
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
        synchronized (aircraftLock) {
            if (aircraft == null) return;

            int writePos = 0;

            if (data.length > OSDK_DATA_MAX_SIZE) {
                try {
                    while (writePos < data.length) {

                        // wait for callback response (if waiting for response is not necessary the sync can be removed)
                        transfereBuffer.acquire();

                        int length = Math.min(data.length - writePos, OSDK_DATA_MAX_SIZE);
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
    }

    private CommonCallbacks.CompletionCallback completionCallback = (DJIError djiError) -> {

        if (djiError != null)
            System.out.println(djiError.getDescription());
        if (transfereBuffer.availablePermits() == 0)
            transfereBuffer.release();
    };
}