package io.gejsi.pufferfish.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;

import io.gejsi.pufferfish.BuildConfig;
import io.gejsi.pufferfish.controllers.MapsActivity;

public class LocationHandler implements ActivityCompat.OnRequestPermissionsResultCallback {
  private CameraPosition cameraPosition;

  public void setCameraPosition(CameraPosition cameraPosition) {
    this.cameraPosition = cameraPosition;
  }

  // The entry point to the Places API.
  private PlacesClient placesClient;

  // The entry point to the Fused Location Provider.
  private FusedLocationProviderClient fusedLocationProviderClient;

  // A default location (Sydney, Australia) and default zoom to use when location permission is
  // not granted.
  private final LatLng defaultLocation = new LatLng(44, -11);
  private static final int DEFAULT_ZOOM = 20;
  private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
  private boolean locationPermissionGranted;

  // The geographical location where the device is currently located. That is, the last-known
  // location retrieved by the Fused Location Provider.
  private Location lastKnownLocation;

  public Location getLastKnownLocation() {
    return lastKnownLocation;
  }

  public void setLastKnownLocation(Location lastKnownLocation) {
    this.lastKnownLocation = lastKnownLocation;
  }

  // Keys for storing activity state.
  public static final String KEY_CAMERA_POSITION = "camera_position";
  public static final String KEY_LOCATION = "location";

  private LocationManager locationManager;
  private LocationListener locationListener;
  private MapsActivity activity;
  private GoogleMap map;

  public LocationHandler(MapsActivity mapsActivity, GoogleMap googleMap) {
    this.activity = mapsActivity;
    this.map = googleMap;

    Places.initialize(activity.getApplicationContext(), BuildConfig.MAPS_API_KEY);
    placesClient = Places.createClient(activity);
    fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity);

    locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
    locationListener = new LocationListener() {
      @Override
      public void onLocationChanged(Location location) {
        Log.d(activity.TAG, "Location changed");
      }

      @Override
      public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d(activity.TAG, "Status changed");
        if (status == LocationProvider.OUT_OF_SERVICE) {
          Toast.makeText(activity, "GPS is disabled", Toast.LENGTH_SHORT).show();
          activity.finish();
        }
      }

      @Override
      public void onProviderEnabled(String provider) {
        Log.d(activity.TAG, "Provider enabled");
        getDeviceLocation();
      }

      @Override
      public void onProviderDisabled(String provider) {
        Log.d(activity.TAG, "Provider disabled");
        locationPermissionGranted = false;
        Toast.makeText(activity, "GPS is disabled", Toast.LENGTH_SHORT).show();
        activity.finish();
      }
    };
  }

  /**
   * Gets the current location of the device, and positions the map's camera.
   */
  public void getDeviceLocation() {
    updateLocationUI();
    /*
     * Get the best and most recent location of the device, which may be null in rare
     * cases when a location is not available.
     */
    try {
      if (locationPermissionGranted) {
        Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
        locationResult.addOnCompleteListener(activity, task -> {
          if (task.isSuccessful()) {
            // Set the map's camera position to the current location of the device.
            lastKnownLocation = task.getResult();
            if (lastKnownLocation != null) {
              map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
            }
          } else {
            Log.d(activity.TAG, "Current location is null. Using defaults.");
            Log.e(activity.TAG, "Exception: %s", task.getException());
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
            map.getUiSettings().setMyLocationButtonEnabled(false);
          }
        });
      }
    } catch (SecurityException e) {
      Log.e("Exception: %s", e.getMessage(), e);
    }
  }

  /**
   * Prompts the user for permission to use the device location.
   */
  public void getLocationPermission() {
    /*
     * Request location permission, so that we can get the location of the
     * device. The result of the permission request is handled by a callback,
     * onRequestPermissionsResult.
     */
    if (ContextCompat.checkSelfPermission(activity.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
      if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        locationPermissionGranted = true;
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 1, locationListener);
      } else {
        Toast.makeText(activity, "GPS is disabled. Enable it to access this page.", Toast.LENGTH_SHORT).show();
        activity.finish();
      }
    } else {
      ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
    }
  }

  public void start() {
    // Prompt the user for permission.
    getLocationPermission();

    // Get the current location of the device and set the position of the map.
    getDeviceLocation();
  }

  public void stop() {
    locationManager.removeUpdates(locationListener);
  }

  /**
   * Handles the result of the request for location permissions.
   */
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    locationPermissionGranted = false;
    // If request is cancelled, the result arrays are empty.
    if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        locationPermissionGranted = true;
      }
    } else {
      activity.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    updateLocationUI();
    getDeviceLocation();
  }

  /**
   * Updates the map's UI settings based on whether the user has granted location permission.
   */
  private void updateLocationUI() {
    if (map == null) {
      return;
    }
    try {
      if (locationPermissionGranted) {
        map.setMyLocationEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);
      } else {
        map.setMyLocationEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        lastKnownLocation = null;
      }
    } catch (SecurityException e) {
      Log.e("Exception: %s", e.getMessage());
    }
  }
}
