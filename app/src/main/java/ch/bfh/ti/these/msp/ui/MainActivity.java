package ch.bfh.ti.these.msp.ui;

import android.content.Intent;
import android.hardware.usb.UsbManager;

import android.view.MenuItem;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import ch.bfh.ti.these.msp.MspApplication;
import ch.bfh.ti.these.msp.R;
import ch.bfh.ti.these.msp.mavlink.MavlinkConnectionInfo;
import ch.bfh.ti.these.msp.mavlink.MavlinkMessageListener;
import com.google.android.material.navigation.NavigationView;

import dji.sdk.airlink.AirLink;
import dji.sdk.sdkmanager.DJISDKManager;
import io.dronefleet.mavlink.MavlinkMessage;


import static ch.bfh.ti.these.msp.MspApplication.*;


public class MainActivity extends AppCompatActivity implements
        MavlinkMessageListener,
        NavigationView.OnNavigationItemSelectedListener,
        RegisterFragment.OnRegisterCompleteListener,
        AirLink.BaseStationSignalQualityUpdatedCallback {

    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private TextView statusText;

    private String djiStatus = "-";
    private String mavlinkStatus = "-";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupView();
        updateStatusText();

        if (DJISDKManager.getInstance().hasSDKRegistered()) {
            navController.getGraph().setStartDestination(R.id.nav_frag_fpv);
            navController.popBackStack();
            navController.navigate(R.id.nav_frag_fpv);
        }
        else {
            getSupportActionBar().hide();
        }


        if (MspApplication.getProductInstance() != null)
            MspApplication.getProductInstance().getAirLink().addBaseStationSignalQualityUpdatedCallback(this);

        getMavlinkMaster().addMessageListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getMavlinkMaster().removeMessageListener(this);
        if (MspApplication.getProductInstance() != null)
            MspApplication.getProductInstance().getAirLink().removeBaseStationSignalQualityUpdatedCallback(this);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        navController.popBackStack();
        int id = item.getItemId();
        switch (id) {
            case R.id.nav_fpv:
                navController.navigate(R.id.nav_frag_fpv);
                break;
            case R.id.nav_telemetrie:
                navController.navigate(R.id.nav_frag_telemetrie);
                break;
            case R.id.nav_map:
                navController.navigate(R.id.nav_frag_map);
                break;
            case R.id.nav_mission:
                startActivity(new Intent(getApplicationContext(), MissionActivity.class));
                break;
            case R.id.nav_settings:
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void messageReceived(MavlinkMessage message) {

    }

    @Override
    public void connectionStatusChanged(MavlinkConnectionInfo info) {
        if (info.isConnected())
            mavlinkStatus = "ok";
        else
            mavlinkStatus = "-";

        updateStatusText();
    }

    @Override
    public void onRegisterComplete() {

        createMavlinkMasterConfig();
        connectAsyncMavlinkMaster();
        getSupportActionBar().show();
        navController.popBackStack();
        navController.navigate(R.id.nav_frag_telemetrie);
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        String action = intent.getAction();
        if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
            Intent attachedIntent = new Intent();
            attachedIntent.setAction(DJISDKManager.USB_ACCESSORY_ATTACHED);
            sendBroadcast(attachedIntent);
        }
        super.onNewIntent(intent);
    }

    @Override
    public void onBaseStationSignalQualityUpdated(int value) {
        djiStatus = String.valueOf(value);
        updateStatusText();
    }

    private void updateStatusText() {
        statusText.post(()-> {
            statusText.setText("Status: " + "DJI - "+ djiStatus + "MSP - "+ mavlinkStatus);
        });
    }

    private void setupView() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_frag_register, R.id.nav_frag_fpv, R.id.nav_frag_telemetrie, R.id.nav_frag_map)
                .setDrawerLayout(drawer)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        statusText = findViewById(R.id.toolbar_status);

    }
}

