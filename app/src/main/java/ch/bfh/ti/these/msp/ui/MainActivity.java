package ch.bfh.ti.these.msp.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;

import android.os.*;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import ch.bfh.ti.these.msp.DJIApplication;
import ch.bfh.ti.these.msp.R;
import ch.bfh.ti.these.msp.mavlink.MavlinkConnectionInfo;
import ch.bfh.ti.these.msp.mavlink.MavlinkMessageListener;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import com.google.android.material.snackbar.Snackbar;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import io.dronefleet.mavlink.MavlinkMessage;

import java.io.IOException;

import static ch.bfh.ti.these.msp.MspApplication.*;


public class MainActivity extends AppCompatActivity implements
        MavlinkMessageListener,
        NavigationView.OnNavigationItemSelectedListener,
        RegisterFragment.OnRegisterCompleteListener {

    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private TextView statusText;
    private FloatingActionButton droneActionButton;

    private String djiStatus = "-";
    private String mavlinkStatus = "-";

    private static final String TAG = MainActivity.class.getName();
    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";


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
        } else {
            getSupportActionBar().hide();
        }

        // Register the broadcast receiver for receiving the device connection's changes.
        IntentFilter filter = new IntentFilter();
        filter.addAction(DJIApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(djiReceiver, filter);

        getMavlinkMaster().addMessageListener(this);
    }


    protected BroadcastReceiver djiReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean ret = false;
            BaseProduct product = DJIApplication.getProductInstance();
            if (product != null) {
                if (product.isConnected()) {
                    //The product is connected
                    djiStatus = "OK";//DJIApplication.getProductInstance().getModel() + " Connected";
                    ret = true;
                } else {
                    if (product instanceof Aircraft) {
                        Aircraft aircraft = (Aircraft) product;
                        if (aircraft.getRemoteController() != null && aircraft.getRemoteController().isConnected()) {
                            // The product is not connected, but the remote controller is connected
                            djiStatus = "only RC Connected";
                            ret = true;
                        }
                    }
                }
            }

            if (!ret) {
                // The product or the remote controller are not connected.
                djiStatus = "Disconnected";
            }
            updateStatusText();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(djiReceiver);
        getMavlinkMaster().removeMessageListener(this);
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
            case R.id.nav_mission_download:
                startActivity(new Intent(getApplicationContext(), MissionDownloadActivity.class));
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
            mavlinkStatus = "OK";
        else
            mavlinkStatus = "-";

        updateStatusText();
    }

    @Override
    public void onRegisterComplete() {

        createMavlinkMasterConfig();
        connectAsyncMavlinkMaster();
        getSupportActionBar().show();
        droneActionButton.show();
        navController.popBackStack();
        navController.navigate(R.id.nav_frag_telemetrie);
    }

    @Override
    public void onProductConnect() {
        djiStatus = "Product connected";
        updateStatusText();
    }

    @Override
    public void onProductDisconnect() {
        djiStatus = "Product disconnected";
        updateStatusText();
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

    private void updateStatusText() {
        statusText.post(() -> {
            statusText.setText("Status: " + "DJI-" + djiStatus + "  MSP-" + mavlinkStatus);
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

        FloatingActionButton fabTakeOff = findViewById(R.id.fab_take_off);
        fabTakeOff.setOnClickListener(v -> {
            try {
                getMavlinkMaster().getMissionService().startMission();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        FloatingActionButton fabPause = findViewById(R.id.fab_pause);
        fabPause.setOnClickListener(v -> {
            try {
                getMavlinkMaster().getMissionService().pauseMission();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        FabAnimation.init(fabTakeOff);
        FabAnimation.init(fabPause);
        droneActionButton = findViewById(R.id.fab_drone_action);
        droneActionButton.hide();
        droneActionButton.setOnClickListener(new View.OnClickListener() {
            boolean isRotate;
            @Override
            public void onClick(View v) {
                isRotate = FabAnimation.rotateFab(v, !isRotate);
                if (isRotate) {
                    FabAnimation.showIn(fabTakeOff);
                    FabAnimation.showIn(fabPause);
                } else {
                    FabAnimation.showOut(fabTakeOff);
                    FabAnimation.showOut(fabPause);
                }
            }
        });
    }

    private static class FabAnimation {

        static void init(final View v) {
            v.setVisibility(View.GONE);
            v.setTranslationY(v.getHeight());
            v.setAlpha(0f);
        }

        static void showIn(final View v) {
            v.setVisibility(View.VISIBLE);
            v.setAlpha(0f);
            v.setTranslationY(v.getHeight());
            v.animate()
                    .setDuration(200)
                    .translationY(0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                        }
                    })
                    .alpha(1f)
                    .start();
        }

        static void showOut(final View v) {
            v.setVisibility(View.VISIBLE);
            v.setAlpha(1f);
            v.setTranslationY(0);
            v.animate()
                    .setDuration(200)
                    .translationY(v.getHeight())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            v.setVisibility(View.GONE);
                            super.onAnimationEnd(animation);
                        }
                    }).alpha(0f)
                    .start();
        }

        static boolean rotateFab(final View v, boolean rotate) {
            v.animate().setDuration(200)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                        }
                    })
                    .rotation(rotate ? 135f : 0f);
            return rotate;
        }
    }
}
