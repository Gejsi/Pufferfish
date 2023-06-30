package io.gejsi.pufferfish.controllers;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.gejsi.pufferfish.R;
import io.gejsi.pufferfish.databinding.ActivityMainBinding;
import io.gejsi.pufferfish.models.Heatmap;
import io.gejsi.pufferfish.models.IntentKey;
import io.gejsi.pufferfish.models.Measurement;
import io.gejsi.pufferfish.utils.ChartUtils;
import io.gejsi.pufferfish.utils.HeatmapUtils;

public class MainActivity extends AppCompatActivity {
  ActivityResultLauncher<String> importLauncher;
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

    ImageButton exportButton = binding.exportButton;
    exportButton.setOnClickListener(view -> {
      List<String> jsonFiles = Arrays.stream(MainActivity.this.fileList())
              .filter(fileName -> fileName.startsWith("Heatmap_") && fileName.endsWith(".json"))
              .collect(Collectors.toList());

      try {
        File zipFile = HeatmapUtils.createZipFile(MainActivity.this, jsonFiles);
        Uri fileUri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".fileprovider", zipFile);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_STREAM, fileUri);
        intent.putExtra(Intent.EXTRA_SUBJECT, "Database Dump");
        intent.putExtra(Intent.EXTRA_TEXT, "Pufferfish: attached heatmaps database dump.");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent chooser = Intent.createChooser(intent, "Share heatmaps dump.");
        List<ResolveInfo> resolveInfoList = getPackageManager().queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resolveInfoList) {
          String packageName = resolveInfo.activityInfo.packageName;
          grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        startActivity(chooser);
      } catch (IOException e) {
        e.printStackTrace();
        Toast.makeText(this, "Error while exporting the database dump.", Toast.LENGTH_SHORT).show();
      }
    });

    importLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
      if (uri != null) {
        importDatabaseDump(uri);
      }
    });

    ImageButton importButton = binding.importButton;
    importButton.setOnClickListener(view -> {
      importLauncher.launch("application/zip");
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

  private void importDatabaseDump(Uri zipUri) {
    try {
      // Open an InputStream for the zip file
      InputStream inputStream = getContentResolver().openInputStream(zipUri);
      if (inputStream != null) {
        // Create a temporary directory to extract the files
        File tempDir = new File(getCacheDir(), "temp");
        if (!tempDir.exists()) {
          tempDir.mkdirs();
        }

        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        ZipEntry zipEntry;

        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
          File jsonFile = new File(tempDir, zipEntry.getName());

          FileOutputStream fileOutputStream = new FileOutputStream(jsonFile);
          byte[] buffer = new byte[1024];
          int length;
          while ((length = zipInputStream.read(buffer)) > 0) {
            fileOutputStream.write(buffer, 0, length);
          }
          fileOutputStream.close();

          processImportedJsonFile(jsonFile);
        }

        zipInputStream.close();
        deleteRecursive(tempDir);

        // Show a toast indicating successful import
        Toast.makeText(this, "Database dump imported successfully", Toast.LENGTH_SHORT).show();
      }
    } catch (IOException e) {
      e.printStackTrace();
      Toast.makeText(this, "Error importing database dump", Toast.LENGTH_SHORT).show();
    }
  }

  // Helper method to recursively delete a directory and its contents
  private void deleteRecursive(File fileOrDirectory) {
    if (fileOrDirectory.isDirectory()) {
      for (File child : Objects.requireNonNull(fileOrDirectory.listFiles())) {
        deleteRecursive(child);
      }
    }

    fileOrDirectory.delete();
  }

  private void processImportedJsonFile(File jsonFile) {
    try {
      // Read the JSON data from the file
      StringBuilder jsonBuilder = new StringBuilder();
      BufferedReader reader = new BufferedReader(new FileReader(jsonFile));
      String line;
      while ((line = reader.readLine()) != null) {
        jsonBuilder.append(line);
      }
      reader.close();

      String fileName = jsonFile.getName();
      String fileContent = jsonBuilder.toString();
      FileOutputStream fileOutputStream = openFileOutput(fileName, Context.MODE_PRIVATE);
      fileOutputStream.write(fileContent.getBytes());
      fileOutputStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();

    fillLocalList();
    fillOnlineList();
    fillStats();
  }

  private void fillLocalList() {
    List<String> files = HeatmapUtils.getLocalFiles(this.fileList());
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
      findViewById(R.id.logged_stats).setVisibility(View.GONE);
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
    findViewById(R.id.logged_stats).setVisibility(View.VISIBLE);

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
        ChartUtils.drawChart(MainActivity.this, intensityChart, Measurement.Type.valueOf(selectedItem));
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
  }
}