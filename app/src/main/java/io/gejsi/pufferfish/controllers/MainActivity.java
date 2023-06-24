package io.gejsi.pufferfish.controllers;

import android.content.Intent;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.gejsi.pufferfish.R;
import io.gejsi.pufferfish.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    Toolbar toolbar = binding.toolbar;
    setSupportActionBar(toolbar);

    FloatingActionButton fab = binding.fab;
    fab.setOnClickListener(v -> {
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

    ImageButton profileButton = binding.me;
    profileButton.setOnClickListener(view -> {
      Intent intent = new Intent(MainActivity.this, UserActivity.class);
      startActivity(intent);
    });

    ImageButton settingsButton = binding.settingsButton;
    settingsButton.setOnClickListener(view -> {
      Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
      startActivity(intent);
    });

    TabLayout tabs = binding.tabLayout;
    ViewFlipper viewFlipper = binding.viewFlipper;
    tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
      @Override
      public void onTabSelected(TabLayout.Tab tab) {
        int position = tab.getPosition();
        viewFlipper.setDisplayedChild(position);
      }
      @Override
      public void onTabUnselected(TabLayout.Tab tab) {
      }
      @Override
      public void onTabReselected(TabLayout.Tab tab) {
      }
    });
  }

  @Override
  protected void onResume() {
    super.onResume();

    List<String> files = Arrays.stream(this.getApplicationContext().fileList())
            .filter(fileName -> fileName.startsWith("Heatmap_") && fileName.endsWith(".json"))
            .sorted((fileName1, fileName2) -> {
              // Split the file names into parts
              String[] fileParts1 = fileName1.split("_");
              String[] fileParts2 = fileName2.split("_");
              String date1 = fileParts1[2];
              String time1 = fileParts1[3].substring(0, fileParts1[3].length() - 5);
              String date2 = fileParts2[2];
              String time2 = fileParts2[3].substring(0, fileParts2[3].length() - 5);

              // compare the date and time values
              int dateComparison = date2.compareTo(date1);
              if (dateComparison != 0) {
                return dateComparison; // sort by date in descending order
              } else {
                return time2.compareTo(time1); // sort by time in descending order
              }
            })
            .collect(Collectors.toList());

    ListView heatmapListView = findViewById(R.id.heatmapListView);
    HeatmapListAdapter heatmapListAdapter = new HeatmapListAdapter(this, files);
    heatmapListView.setAdapter(heatmapListAdapter);

    AdapterView.OnItemClickListener dialogHandler = (parent, v, position, id) -> {
      String fileName = files.get(position);

      AlertDialog.Builder builder = new AlertDialog.Builder(parent.getContext());
      builder.setTitle("Actions")
              .setMessage("lorem")
              .setPositiveButton("Open", (dialog, which) -> {
                String[] fileParts = fileName.split("_");
                String selectedMeasurementType = fileParts[1];

                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                intent.putExtra("measurementType", selectedMeasurementType);
                intent.putExtra("fileName", fileName);

                startActivity(intent);
              })
              .setNegativeButton("Delete", (dialog, which) -> {
                deleteHeatmap(fileName);
                // update the ListView
                files.remove(position);
                heatmapListAdapter.notifyDataSetChanged();
              })
              .show();
    };

    heatmapListView.setOnItemClickListener(dialogHandler);
  }

  private void deleteHeatmap(String fileName) {
    this.deleteFile(fileName);
  }
}