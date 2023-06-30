package io.gejsi.pufferfish.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public abstract class LocationUtils {
  private static final long MIN_TIME_INTERVAL = 1000; // Minimum time interval in milliseconds
  private static final float MIN_DISTANCE_INTERVAL = 1; // Minimum distance interval in meters

  private final LocationManager locationManager;
  private final LocationListener locationListener;

  // The geographical location where the device is currently located.
  private Location lastKnownLocation;

  public LocationUtils(Context context) {
    locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

    try {
      lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    } catch (SecurityException e) {
      Log.e("Exception: %s", e.getMessage(), e);
    }

    locationListener = new LocationListener() {
      @Override
      public void onLocationChanged(Location location) {
        Log.d("Location", "Location changed" + location);
        lastKnownLocation = location;
        onChangedLocation(location);
      }

      @Override
      public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Location", "Status changed");
        onChangedStatus(provider, status, extras);
      }

      @Override
      public void onProviderDisabled(String provider) {
        Log.d("Location", "Provider disabled");
        onDisabledProvider(provider);
      }
    };
  }

  public abstract void onChangedLocation(Location location);
  public abstract void onChangedStatus(String provider, int status, Bundle extras);
  public abstract void onDisabledProvider(String provider);

  @SuppressLint("MissingPermission")
  public void startLocationUpdates() {
    if (locationManager != null && locationListener != null) {
      locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_INTERVAL, MIN_DISTANCE_INTERVAL, locationListener);
    }
  }

  public void stopLocationUpdates() {
    if (locationManager != null && locationListener != null) {
      locationManager.removeUpdates(locationListener);
    }
  }

  public Location getLastKnownLocation() {
    return lastKnownLocation;
  }

  public void setLastKnownLocation(Location lastKnownLocation) {
    this.lastKnownLocation = lastKnownLocation;
  }
}
