package io.gejsi.pufferfish.controllers;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import io.gejsi.pufferfish.handlers.AudioHandler;
import io.gejsi.pufferfish.handlers.LocationUtils;
import io.gejsi.pufferfish.handlers.LteHandler;
import io.gejsi.pufferfish.handlers.WifiHandler;
import io.gejsi.pufferfish.models.Measurement;
import io.gejsi.pufferfish.models.MeasurementSampler;

public class BackgroundIntentService extends IntentService {
  public static volatile boolean shouldContinue = true;

  public BackgroundIntentService() {
    super("BackgroundIntentService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    if (!shouldContinue) {
      stopSelf();
      return;
    }

    Measurement.Type measurementType = null;

    if (intent.hasExtra("measurementType")) {
      String selectedMeasurementType = intent.getStringExtra("measurementType");
      measurementType = Measurement.Type.valueOf(selectedMeasurementType);
    }

    assert measurementType != null;

    MeasurementSampler sampler = null;
    if (measurementType == Measurement.Type.Noise) {
      sampler = new AudioHandler(this);
    } else if (measurementType == Measurement.Type.WiFi) {
      sampler = new WifiHandler(this);
    } else if (measurementType == Measurement.Type.LTE) {
      sampler = new LteHandler(this);
    }

    assert sampler != null;

    LocationUtils locationUtils = new LocationUtils(getApplicationContext()) {
      @Override
      public void onChangedStatus(String provider, int status, Bundle extras) {
      }

      @Override
      public void onDisabledProvider(String provider) {
      }
    };

    // start sampling data
    sampler.start();

    double data = sampler.getAverageData();

    while (shouldContinue) {
      Log.d("Location", "onHandleIntent " + data + " " + locationUtils.getLastKnownLocation());
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    // stop sampling after the service has been stopped
    sampler.stop();

    /*
    Handler handler = new Handler(Looper.getMainLooper());
    handler.post(new Runnable() {
      @Override
      public void run() {
        // Code to update UI or perform other operations in the main thread
      }
    });
     */
  }
}