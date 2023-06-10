package io.gejsi.pufferfish.controllers;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;

import io.gejsi.pufferfish.handlers.AudioHandler;
import io.gejsi.pufferfish.handlers.LocationUtils;
import io.gejsi.pufferfish.handlers.LteHandler;
import io.gejsi.pufferfish.handlers.WifiHandler;
import io.gejsi.pufferfish.models.Measurement;
import io.gejsi.pufferfish.models.MeasurementSampler;

public class MeasurementService extends Service {
  public static boolean isServiceRunning = false;
  private LocationUtils locationUtils;

  private Measurement.Type measurementType = Measurement.Type.Noise;

  private MeasurementSampler sampler;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    // Retrieve the selected measurement type from intent extras
    if (intent.hasExtra("measurementType")) {
      String selectedMeasurementType = intent.getStringExtra("measurementType");
      measurementType = Measurement.Type.valueOf(selectedMeasurementType);
    }

    locationUtils = new LocationUtils(this) {
      @Override
      public void onChangedLocation(Location location) {

      }

      @Override
      public void onChangedStatus(String provider, int status, Bundle extras) {

      }

      @Override
      public void onDisabledProvider(String provider) {

      }
    };

    isServiceRunning = true;

    if (measurementType == Measurement.Type.Noise) {
      sampler = new AudioHandler(this.getApplicationContext());
    } else if (measurementType == Measurement.Type.WiFi) {
      sampler = new WifiHandler(this.getApplicationContext());
    } else if (measurementType == Measurement.Type.LTE) {
      sampler = new LteHandler(this.getApplicationContext());
    }

    if (isServiceRunning) {
      locationUtils.startLocationUpdates();
      sampler.start();
    }

    return super.onStartCommand(intent, flags, startId);
  }

  public static void stopService() {
    isServiceRunning = false;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    stopService();
    locationUtils.stopLocationUpdates();
    sampler.stop();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}