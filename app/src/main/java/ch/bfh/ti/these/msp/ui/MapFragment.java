package ch.bfh.ti.these.msp.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import ch.bfh.ti.these.msp.DJIApplication;
import ch.bfh.ti.these.msp.R;
import ch.bfh.ti.these.msp.dji.DjiMessageListener;
import ch.bfh.ti.these.msp.mavlink.MavlinkConnectionInfo;
import ch.bfh.ti.these.msp.mavlink.MavlinkMessageListener;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import dji.common.flightcontroller.FlightControllerState;
import io.dronefleet.mavlink.MavlinkMessage;
import io.dronefleet.mavlink.common.MissionItemReached;

import java.io.IOException;

import static ch.bfh.ti.these.msp.MspApplication.getMavlinkMaster;

public class MapFragment extends Fragment implements OnMapReadyCallback, DjiMessageListener.DjiFlightStateListener, MavlinkMessageListener {

    int ZOOM_LEVEL=15;

    private volatile boolean flightStatusUpdated = true;

    private Handler handler;
    private  MapView mapView;
    private GoogleMap googleMap;
    private Marker vehicleMarker;

    private double vLatitude = 0;
    private double vLongitude = 0;

    private double mLatitude = 0;
    private double mLongitude = 0;


    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, parent, false);
        mapView = (MapView) view.findViewById(R.id.map);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);
        handler = new Handler(Looper.getMainLooper());
        mapView.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }


    @Override
    public void onStart() {
        super.onStart();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(20 * 1000);

        mapView.getMapAsync(this);
        mapView.onStart();

        DJIApplication.addMessageListener(this);
        getMavlinkMaster().addMessageListener(this);

        try {
            getMavlinkMaster().getMissionService().getCurrent()
                    .thenAccept((result)-> {
                        handler.post(() -> {
                            short seq = result;
                            if (googleMap != null && seq < 0)
                                googleMap.clear();
                        });
                    }).exceptionally(throwable -> {
                        return null;
                    });
        }
        catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();

        fusedLocationClient.removeLocationUpdates(locationCallback);
        DJIApplication.removeMessageListener(this);
        getMavlinkMaster().removeMessageListener(this);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation().addOnSuccessListener(getActivity(), location -> {
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(mLatitude, mLongitude), ZOOM_LEVEL);
            googleMap.animateCamera(cameraUpdate);
        });
    }

    private void updateLocation(Location location) {
        if (location != null) {
            mLatitude = location.getLatitude();
            mLongitude = location.getLongitude();
        }
    }


    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            updateLocation(locationResult.getLastLocation());
        }
    };

    @Override
    public void flightStateChanged(FlightControllerState state) {
        try {
            vLatitude = state.getAircraftLocation().getLatitude();
            vLongitude = state.getAircraftLocation().getLongitude();

            if (flightStatusUpdated) {
                flightStatusUpdated = false;

                handler.post(()->{
                    flightStatusUpdated = true;

                    LatLng pos = new LatLng(vLatitude, vLongitude);

                    // Create MarkerOptions object
                    final MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.title("Matrice 210");
                    BitmapDescriptor bitmapDescriptor = vectorToBitmap(R.drawable.ic_drone, Color.RED);
                    if (bitmapDescriptor != null)
                        markerOptions.icon(bitmapDescriptor);
                    markerOptions.position(pos);

                    if (vehicleMarker != null) {
                        vehicleMarker.remove();
                    }
                    vehicleMarker = googleMap.addMarker(markerOptions);
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void messageReceived(MavlinkMessage message) {
        if (message.getPayload() instanceof MissionItemReached) {
            MavlinkMessage<MissionItemReached> msg = (MavlinkMessage<MissionItemReached>)message;
            short seq =  (short)msg.getPayload().seq();

            handler.post(()->{
                if (seq == 0) {
                    googleMap.clear();
                }

                if (seq >= 0) {
                    LatLng pos = new LatLng(vLatitude, vLongitude);
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.title("wp " + (seq+1));
                    markerOptions.position(pos);
                    googleMap.addMarker(markerOptions);
                }
            });
        }
    }

    @Override
    public void connectionStatusChanged(MavlinkConnectionInfo info) {
    }


    private BitmapDescriptor vectorToBitmap(@DrawableRes int id, @ColorInt int color) {
        BitmapDescriptor bitmapDescriptor = null;
        try {
            Drawable vectorDrawable = ResourcesCompat.getDrawable(getResources(), id, null);
            Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                    vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            DrawableCompat.setTint(vectorDrawable, color);
            vectorDrawable.draw(canvas);
            bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }

        return bitmapDescriptor;
    }
}
