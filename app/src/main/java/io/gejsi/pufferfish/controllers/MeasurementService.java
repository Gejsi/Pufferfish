package io.gejsi.pufferfish.controllers;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import io.gejsi.pufferfish.handlers.AudioHandler;
import io.gejsi.pufferfish.handlers.LocationUtils;
import io.gejsi.pufferfish.handlers.LteHandler;
import io.gejsi.pufferfish.handlers.WifiHandler;
import io.gejsi.pufferfish.models.Measurement;
import io.gejsi.pufferfish.models.MeasurementSampler;

public class MeasurementService extends Service {
  private LocationUtils locationUtils;

  private Measurement.Type measurementType = Measurement.Type.Noise;

  private MeasurementSampler sampler;
  private Handler handler;
  private Runnable measurementRunnable;

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

    if (measurementType == Measurement.Type.Noise) {
      sampler = new AudioHandler(this.getApplicationContext());
    } else if (measurementType == Measurement.Type.WiFi) {
      sampler = new WifiHandler(this.getApplicationContext());
    } else if (measurementType == Measurement.Type.LTE) {
      sampler = new LteHandler(this.getApplicationContext());
    }

    startMeasurementThread();

    return super.onStartCommand(intent, flags, startId);
  }

  private void startMeasurementThread() {
    Toast.makeText(this, "Started background monitoring.", Toast.LENGTH_SHORT).show();
    handler = new Handler();
    measurementRunnable = new Runnable() {
      @Override
      public void run() {
        Log.d("Test", "measurement taken (" + Thread.currentThread().getName() + "): ");

        handler.postDelayed(this, 2000);
      }
    };

    handler.post(measurementRunnable);
  }

  private void stopMeasurementThread() {
    if (handler != null && measurementRunnable != null) {
      handler.removeCallbacks(measurementRunnable);
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Toast.makeText(this, "Stopped background monitoring.", Toast.LENGTH_SHORT).show();
    stopMeasurementThread();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}