package io.gejsi.pufferfish.controllers;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import io.gejsi.pufferfish.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

  private ActivityMainBinding binding;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    Toolbar toolbar = binding.toolbar;
    setSupportActionBar(toolbar);

    FloatingActionButton fab = binding.fab;
    fab.setOnClickListener(view -> {
      AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
      builder.setTitle("Select the type of measurement you want to perform");

      // Define the list of measurement types
      String[] measurementTypes = {"Noise", "WiFi", "LTE"};

      // Set the radio buttons for the measurement types
      builder.setSingleChoiceItems(measurementTypes, -1, (dialog, which) -> {
        String selectedMeasurementType = measurementTypes[which];

        Intent intent = new Intent(MainActivity.this, MapsActivity.class);
        intent.putExtra("measurementType", selectedMeasurementType);

        startActivity(intent);

        dialog.dismiss();
      });

      AlertDialog dialog = builder.create();
      dialog.show();
    });

    ImageButton settingsButton = binding.settingsButton;
    settingsButton.setOnClickListener((View.OnClickListener) view -> {
      Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
      startActivity(intent);
    });
  }

  @Override
  protected void onResume() {
    super.onResume();
    String[] files = this.getApplicationContext().fileList();
    for (String file : files) {
      Log.d("MainActivity", "file " + file);
    }
  }
}