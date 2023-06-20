package io.gejsi.pufferfish.handlers;

import android.app.Activity;
import android.location.Location;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import io.gejsi.pufferfish.utils.LocationUtils;

public class LocationHandler {
  private static final int DEFAULT_ZOOM = 20;

  private boolean locationPermissionGranted;

  public void setLocationPermissionGranted(boolean locationPermissionGranted) {
    this.locationPermissionGranted = locationPermissionGranted;
  }

  private final LocationUtils locationUtils;

  private final Activity activity;
  private final GoogleMap map;

  public LocationHandler(Activity mapsActivity, GoogleMap googleMap) {
    this.activity = mapsActivity;
    this.map = googleMap;

    locationUtils = new LocationUtils(mapsActivity) {
      @Override
      public void onChangedStatus(String provider, int status, Bundle extras) {
        if (status == LocationProvider.OUT_OF_SERVICE) {
          handleDisabledProvider();
        }
      }

      @Override
      public void onDisabledProvider(String provider) {
        handleDisabledProvider();
      }
    };
  }

  public Location getLastKnownLocation() {
    return locationUtils.getLastKnownLocation();
  }

  public void setLastKnownLocation(Location lastKnownLocation) {
    locationUtils.setLastKnownLocation(lastKnownLocation);
  }

  /**
   * Gets the current location of the device, and positions the map's camera.
   */
  public void getDeviceLocation() {
    updateLocationUI();

    Location lastKnownLocation = locationUtils.getLastKnownLocation();

    if (locationPermissionGranted && lastKnownLocation != null) {
      map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
    }
  }

  public void start() {
    getDeviceLocation();
    locationUtils.startLocationUpdates();
  }

  public void stop() {
    locationUtils.stopLocationUpdates();
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
        locationUtils.setLastKnownLocation(null);
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
