package io.gejsi.pufferfish.controllers;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import io.gejsi.pufferfish.databinding.ActivityBackgroundBinding;
import io.gejsi.pufferfish.handlers.LocationUtils;
import io.gejsi.pufferfish.models.Measurement;

public class BackgroundActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ActivityBackgroundBinding binding = ActivityBackgroundBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    Measurement.Type measurementType = Measurement.Type.Noise;

    if (getIntent().hasExtra("measurementType")) {
      String selectedMeasurementType = getIntent().getStringExtra("measurementType");
      measurementType = Measurement.Type.valueOf(selectedMeasurementType);
    }

    Intent intent = new Intent(this, BackgroundIntentService.class);
    intent.putExtra("measurementType", measurementType.toString());

    LocationUtils locationUtils = new LocationUtils(this) {
      @Override
      public void onChangedStatus(String provider, int status, Bundle extras) {
      }

      @Override
      public void onDisabledProvider(String provider) {
      }
    };

    binding.toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
      if (isChecked) {
        locationUtils.startLocationUpdates();
        startService(intent);
      } else {
        locationUtils.stopLocationUpdates();
      }

      BackgroundIntentService.shouldContinue = isChecked;
    });
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    BackgroundIntentService.shouldContinue = false;
  }
}