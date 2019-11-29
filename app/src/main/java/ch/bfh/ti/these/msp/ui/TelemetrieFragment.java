package ch.bfh.ti.these.msp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import ch.bfh.ti.these.msp.MspApplication;
import ch.bfh.ti.these.msp.R;
import ch.bfh.ti.these.msp.mavlink.MavlinkConnectionInfo;
import ch.bfh.ti.these.msp.mavlink.MavlinkMessageListener;
import dji.common.battery.BatteryState;
import dji.sdk.airlink.AirLink;
import dji.sdk.sdkmanager.DJISDKManager;
import io.dronefleet.mavlink.MavlinkMessage;

import static ch.bfh.ti.these.msp.MspApplication.getMavlinkMaster;


public class TelemetrieFragment extends Fragment implements MavlinkMessageListener {

    TextView textViewBatteryVoltage;
    TextView textViewBatteryCurrent;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_telemetrie, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupView();
        getMavlinkMaster().addMessageListener(this);

        try {

            MspApplication.getProductInstance().getBattery().setStateCallback(new BatteryState.Callback() {
                @Override
                public void onUpdate(BatteryState djiBatteryState) {

                    getActivity().runOnUiThread(()->{
                        textViewBatteryVoltage.setText(djiBatteryState.getVoltage());
                        textViewBatteryCurrent.setText(djiBatteryState.getCurrent());
                    });
                }
            });
        } catch (Exception ignored) {

        }
    }


    public void onDestroy() {
        super.onDestroy();
        getMavlinkMaster().removeMessageListener(this);

        try {
            MspApplication.getProductInstance().getBattery().setStateCallback(null);
        } catch (Exception ignored) {

        }
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
    }
}
