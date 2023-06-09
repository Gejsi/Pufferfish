package io.gejsi.pufferfish.handlers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

public class LocationHandler {
  private static final int DEFAULT_ZOOM = 20;

  private boolean locationPermissionGranted;

  public void setLocationPermissionGranted(boolean locationPermissionGranted) {
    this.locationPermissionGranted = locationPermissionGranted;
  }

  // The geographical location where the device is currently located.
  private Location lastKnownLocation;

  private LocationManager locationManager;
  private LocationListener locationListener;
  private Activity activity;
  private GoogleMap map;

  public LocationHandler(Activity mapsActivity, GoogleMap googleMap) {
    this.activity = mapsActivity;
    this.map = googleMap;

    locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

    locationListener = new LocationListener() {
      @Override
      public void onLocationChanged(Location location) {
        Log.d("MapsActivity", "Location changed" + location);
        lastKnownLocation = location;
      }

      @Override
      public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("MapsActivity", "Status changed");
        if (status == LocationProvider.OUT_OF_SERVICE) {
          handleDisabledProvider();
        }
      }

      @Override
      public void onProviderEnabled(String provider) {
        Log.d("MapsActivity", "Provider enabled");
        getDeviceLocation();
      }

      @Override
      public void onProviderDisabled(String provider) {
        Log.d("MapsActivity", "Provider disabled");
        handleDisabledProvider();
      }
    };
  }

  public Location getLastKnownLocation() {
    return lastKnownLocation;
  }

  public void setLastKnownLocation(Location lastKnownLocation) {
    this.lastKnownLocation = lastKnownLocation;
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
        lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastKnownLocation != null) {
          map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
        }
      }
    } catch (SecurityException e) {
      Log.e("Exception: %s", e.getMessage(), e);
    }
  }

  @SuppressLint("MissingPermission")
  public void start() {
    getDeviceLocation();
    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
  }

  public void stop() {
    locationManager.removeUpdates(locationListener);
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
      } else {
        map.setMyLocationEnabled(false);
        lastKnownLocation = null;
      }
    } catch (SecurityException e) {
      Log.e("Exception: %s", e.getMessage());
    }
  }

  private void handleDisabledProvider() {
    Toast.makeText(activity, "GPS is disabled. Enable it to access the map.", Toast.LENGTH_SHORT).show();
    activity.finish();
  }
}
