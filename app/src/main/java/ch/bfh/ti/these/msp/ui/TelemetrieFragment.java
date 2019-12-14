package ch.bfh.ti.these.msp.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import ch.bfh.ti.these.msp.DJIApplication;
import ch.bfh.ti.these.msp.R;
import ch.bfh.ti.these.msp.dji.DjiMessageListener;
import ch.bfh.ti.these.msp.mavlink.MavlinkConnectionInfo;
import ch.bfh.ti.these.msp.mavlink.MavlinkMessageListener;
import dji.common.battery.BatteryState;
import dji.common.flightcontroller.FlightControllerState;
import io.dronefleet.mavlink.MavlinkMessage;

import java.text.DecimalFormat;

import static ch.bfh.ti.these.msp.MspApplication.getMavlinkMaster;


public class TelemetrieFragment extends Fragment implements MavlinkMessageListener,
        DjiMessageListener.DjiFlightStateListener,
        DjiMessageListener.DjiBatteryStateListener {

    private Handler handler;

    TextView textViewBatteryVoltage;
    TextView textViewBatteryCurrent;
    TextView textViewHeightValue;
    TextView textViewSpeedValue;
    TextView textViewPosNValue;
    TextView textViewPosOValue;


    int voltage = 0;
    int current = 0;
    float altitude = 0;
    double velocity = 0;
    double latitude = 0;
    double longitude = 0;

    // Flags to avoid high frequency ui update
    private volatile boolean flightStatusUpdated = true;
    private volatile boolean batteryStatusUpdated = true;

    private static DecimalFormat df4 = new DecimalFormat("#.#####");


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_telemetrie, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        handler = new Handler(Looper.getMainLooper());
        setupView();
    }

    @Override
    public void onStart() {
        super.onStart();
        getMavlinkMaster().addMessageListener(this);
        DJIApplication.addMessageListener(this);
    }


    public void onDestroy() {
        super.onDestroy();
        getMavlinkMaster().removeMessageListener(this);
        DJIApplication.removeMessageListener(this);
    }

    @Override
    public void messageReceived(MavlinkMessage message) {

    }

    @Override
    public void connectionStatusChanged(MavlinkConnectionInfo info) {

    }

    private void setupView() {
        textViewBatteryVoltage = getActivity().findViewById(R.id.textViewVoltageValue);
        textViewBatteryCurrent = getActivity().findViewById(R.id.textViewCurrentValue);

        textViewHeightValue = getActivity().findViewById(R.id.textViewHeightValue);
        textViewSpeedValue  = getActivity().findViewById(R.id.textViewSpeedValue);
        textViewPosNValue   = getActivity().findViewById(R.id.textViewPosNValue);
        textViewPosOValue   = getActivity().findViewById(R.id.textViewPosOValue);
    }

    @Override
    public void flightStateChanged(FlightControllerState state) {
        try {
            velocity = Math.sqrt(Math.pow(state.getVelocityX(), 2)+Math.pow(state.getVelocityY(), 2)+Math.pow(state.getVelocityZ(), 2));
            altitude = state.getAircraftLocation().getAltitude();
            latitude = state.getAircraftLocation().getLatitude();
            longitude = state.getAircraftLocation().getLongitude();

            if (flightStatusUpdated) {
                flightStatusUpdated = false;
                handler.post(()-> {
                    flightStatusUpdated = true;
                    textViewHeightValue.setText(String.valueOf(altitude));
                    textViewSpeedValue.setText(df4.format(velocity));
                    textViewPosNValue.setText("N " + df4.format(latitude));
                    textViewPosOValue.setText("O " + df4.format(longitude));
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void batteryStateChanged(BatteryState state) {
        try {
            voltage = state.getVoltage();
            current = state.getCurrent() * -1;

            if (batteryStatusUpdated) {
                batteryStatusUpdated = false;
                handler.post(()-> {
                    batteryStatusUpdated = true;
                    textViewBatteryVoltage.setText(String.valueOf(voltage));
                    textViewBatteryCurrent.setText(String.valueOf(current));
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
