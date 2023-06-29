package io.gejsi.pufferfish.controllers;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.gejsi.pufferfish.R;
import io.gejsi.pufferfish.databinding.ActivityMainBinding;
import io.gejsi.pufferfish.models.Heatmap;
import io.gejsi.pufferfish.models.IntentKey;
import io.gejsi.pufferfish.models.Measurement;
import io.gejsi.pufferfish.utils.HeatmapUtils;

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
        intent.putExtra(IntentKey.MeasurementType.toString(), selectedMeasurementType);
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

        if (position == 1) {
          fillOnlineList();
          fab.setVisibility(View.GONE);
        } else if (position == 2) {
          fillStats();
          fab.setVisibility(View.GONE);
        } else {
          fab.setVisibility(View.VISIBLE);
        }
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

    fillLocalList();
    fillOnlineList();
    fillStats();
  }

  private void fillLocalList() {
    List<String> files = HeatmapUtils.getLocalFiles(this.getApplicationContext().fileList());
    ListView localHeatmaps = findViewById(R.id.localHeatmapsList);
    LocalHeatmapListAdapter localListAdapter = new LocalHeatmapListAdapter(this, files);
    localHeatmaps.setAdapter(localListAdapter);
    localHeatmaps.setOnItemClickListener((parent, v, position, id) -> {
      String fileName = files.get(position);

      AlertDialog.Builder builder = new AlertDialog.Builder(parent.getContext());
      builder.setTitle("Actions")
              .setMessage("What do you will you do with this heatmap?")
              .setPositiveButton("Open", (dialog, which) -> {
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                intent.putExtra(IntentKey.MeasurementType.toString(), fileName.split("_")[1]);
                intent.putExtra(IntentKey.FileName.toString(), fileName);
                startActivity(intent);
              })
              .setNegativeButton("Delete", (dialog, which) -> {
                MainActivity.this.deleteFile(fileName);
                // update the ListView
                files.remove(position);
                localListAdapter.notifyDataSetChanged();
              })
              .setNeutralButton("Sync online", (dialog, which) -> {
                HeatmapUtils.syncHeatmap(this, fileName);
              })
              .show();
    });
  }

  private void fillOnlineList() {
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

    if (currentUser == null) {
      findViewById(R.id.not_logged).setVisibility(View.VISIBLE);
      findViewById(R.id.online_desc).setVisibility(View.GONE);
      return;
    }

    findViewById(R.id.not_logged).setVisibility(View.GONE);
    findViewById(R.id.online_desc).setVisibility(View.VISIBLE);

    ListView onlineHeatmaps = findViewById(R.id.onlineHeatmapsList);
    FirebaseDatabase database = FirebaseDatabase.getInstance(getString(R.string.db));
    DatabaseReference heatmapsRef = database.getReference("heatmaps").child(currentUser.getUid());
    List<Heatmap> heatmapList = new ArrayList<>();

    OnlineHeatmapListAdapter onlineListAdapter = new OnlineHeatmapListAdapter(MainActivity.this, heatmapList);
    onlineHeatmaps.setAdapter(onlineListAdapter);

    CompletableFuture<DataSnapshot> dataSnapshotFuture = new CompletableFuture<>();
    heatmapsRef.addValueEventListener(new ValueEventListener() {
      @Override
      public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        dataSnapshotFuture.complete(dataSnapshot);
      }

      @Override
      public void onCancelled(@NonNull DatabaseError databaseError) {
        heatmapList.clear();
        onlineListAdapter.notifyDataSetChanged();
      }
    });


    dataSnapshotFuture.thenAccept(dataSnapshot -> {
      for (DataSnapshot heatmapSnapshot : dataSnapshot.getChildren()) {
        Heatmap heatmap = heatmapSnapshot.getValue(Heatmap.class);
        heatmapList.add(heatmap);
      }

      // update data after fetching
      onlineListAdapter.notifyDataSetChanged();
    });


    onlineHeatmaps.setOnItemClickListener((parent, v, position, id) -> {
      Heatmap heatmap = heatmapList.get(position);

      AlertDialog.Builder builder = new AlertDialog.Builder(parent.getContext());
      builder.setTitle("Actions")
              .setMessage("What do you will you do with this heatmap?")
              .setPositiveButton("Open", (dialog, which) -> {
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                intent.putExtra(IntentKey.MeasurementType.toString(), heatmap.getMeasurementType().toString());
                intent.putExtra(IntentKey.OnlineTimestamp.toString(), heatmap.getTimestamp());
                startActivity(intent);
              })
              .setNegativeButton("Delete", (dialog, which) -> {
                heatmapsRef.child(heatmap.getTimestamp()).removeValue().addOnSuccessListener(MainActivity.this, __ -> {
                  Toast.makeText(MainActivity.this, "Heatmap successfully deleted.", Toast.LENGTH_SHORT).show();
                  heatmapList.remove(position);
                  onlineListAdapter.notifyDataSetChanged();
                }).addOnFailureListener(MainActivity.this, exception -> {
                  Toast.makeText(MainActivity.this, "Something went wrong while deleting the heatmap.", Toast.LENGTH_SHORT).show();
                });
              })
              .show();
    });
  }

  private void fillStats() {
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    BarChart intensityChart = findViewById(R.id.intensityDistributionChart);

    if (currentUser == null) {
      findViewById(R.id.not_logged_stats).setVisibility(View.VISIBLE);
      findViewById(R.id.stats_desc).setVisibility(View.GONE);
      intensityChart.setVisibility(View.GONE);
      return;
    }

    intensityChart.setVisibility(View.VISIBLE);
    intensityChart.setDrawBarShadow(false);
    intensityChart.setDrawValueAboveBar(true);
    intensityChart.setHighlightPerTapEnabled(false);
    intensityChart.setHighlightPerDragEnabled(false);
    intensityChart.setDoubleTapToZoomEnabled(false);
    intensityChart.getLegend().setEnabled(false);
    intensityChart.getDescription().setEnabled(false);
    intensityChart.setExtraOffsets(5, 5, 5, 15);
    XAxis xAxis = intensityChart.getXAxis();
    xAxis.setLabelCount(3, false);
    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
    xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"Good", "Average", "Bad"}));
    xAxis.setTextColor(Color.LTGRAY);
    intensityChart.getAxisLeft().setTextColor(Color.LTGRAY);
    intensityChart.getAxisRight().setTextColor(Color.LTGRAY);

    findViewById(R.id.not_logged_stats).setVisibility(View.GONE);
    findViewById(R.id.stats_desc).setVisibility(View.VISIBLE);

    Spinner spinner = findViewById(R.id.sampler_menu);
    ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.sampled_stat,
            android.R.layout.simple_spinner_item
    );
    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(spinnerAdapter);

    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String selectedItem = parent.getItemAtPosition(position).toString();
        HeatmapUtils.drawChart(MainActivity.this, intensityChart, Measurement.Type.valueOf(selectedItem));
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
  }
}