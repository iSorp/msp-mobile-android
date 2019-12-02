package ch.bfh.ti.these.msp.ui;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import ch.bfh.ti.these.msp.DJIApplication;
import ch.bfh.ti.these.msp.R;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import dji.common.flightcontroller.FlightControllerState;

public class VehicleMapFragment extends Fragment implements OnMapReadyCallback {

    int ZOOM_LEVEL=15;

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(20 * 1000);

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

        mapView.getMapAsync(this);
        mapView.onCreate(savedInstanceState);
        mapView.onResume();

        if (DJIApplication.isAircraftConnected()) {
            DJIApplication.getAircraftInstance().getFlightController().setStateCallback((FlightControllerState state)-> {
                try {
                    if (state == null) return;

                    vLatitude = state.getAircraftLocation().getLatitude();
                    vLongitude = state.getAircraftLocation().getLongitude();

                    if (vehicleMarker == null) {
                        vehicleMarker = googleMap.addMarker(new MarkerOptions()
                                .position(new LatLng(vLatitude, vLongitude))
                                .title("Matrice 210"));
                    }

                    if (vehicleMarker != null) {
                        handler.post(()-> {
                            vehicleMarker.setPosition(new LatLng(vLatitude, vLongitude));
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
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
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();

        fusedLocationClient.removeLocationUpdates(locationCallback);
        if (DJIApplication.isAircraftConnected()) {
            DJIApplication.getAircraftInstance().getFlightController().setStateCallback(null);
        }
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
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLatitude, mLongitude), ZOOM_LEVEL);
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
}