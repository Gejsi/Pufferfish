package io.gejsi.pufferfish.controllers;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import io.gejsi.pufferfish.R;
import io.gejsi.pufferfish.databinding.ActivityMapsBinding;
import io.gejsi.pufferfish.handlers.AudioHandler;
import io.gejsi.pufferfish.handlers.GridUtils;
import io.gejsi.pufferfish.handlers.HeatmapUtils;
import io.gejsi.pufferfish.handlers.LocationHandler;
import io.gejsi.pufferfish.handlers.LteHandler;
import io.gejsi.pufferfish.handlers.SettingsUtils;
import io.gejsi.pufferfish.handlers.WifiHandler;
import io.gejsi.pufferfish.models.Measurement;
import io.gejsi.pufferfish.models.MeasurementSampler;
import mil.nga.color.Color;
import mil.nga.mgrs.MGRS;
import mil.nga.mgrs.grid.GridType;
import mil.nga.mgrs.grid.style.Grids;
import mil.nga.mgrs.tile.MGRSTileProvider;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {
  private GoogleMap map;
  private ActivityMapsBinding binding;
  private LocationHandler locationHandler;

  private MeasurementSampler sampler;

  private Map<String, Measurement> measurements;

  private Measurement.Type measurementType = Measurement.Type.Noise;

  public static final int PERMISSIONS_REQUEST_CODE = 1;
  public static final String[] PERMISSIONS_REQUIRED = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_WIFI_STATE};

  // Keys for storing activity state.
  public static final String KEY_LOCATION = "location";

  /**
   * MGRS tile provider
   */
  private MGRSTileProvider tileProvider;

  private GridUtils gridUtils;
  private Timer backgroundRecordingTimer;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Retrieve location from saved instance state.
    // TODO: maybe add this info in the files as well, so they are different for each heatmap
    if (savedInstanceState != null) {
      locationHandler.setLastKnownLocation(savedInstanceState.getParcelable(KEY_LOCATION));
    }

    // Retrieve the measurements from intent extras
    if (getIntent().hasExtra("fileName")) {
      String fileName = getIntent().getStringExtra("fileName");
      measurements = HeatmapUtils.loadHeatmap(this, fileName);
    } else {
      measurements = new HashMap<>();
    }

    // Retrieve the selected measurement type from intent extras
    if (getIntent().hasExtra("measurementType")) {
      String selectedMeasurementType = getIntent().getStringExtra("measurementType");
      measurementType = Measurement.Type.valueOf(selectedMeasurementType);
    }

    binding = ActivityMapsBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    Toolbar toolbar = binding.toolbar;
    setSupportActionBar(toolbar);

    Button saveBtn = binding.btnSave;
    saveBtn.setOnClickListener(v -> {
      HeatmapUtils.saveHeatmap(this, measurementType, measurements.values());
      this.finish();
    });

    FloatingActionButton loc = binding.loc;
    TooltipCompat.setTooltipText(loc, "My location");
    loc.setOnClickListener(view -> locationHandler.getDeviceLocation());

    gridUtils = new GridUtils();

    FloatingActionButton backgroundBtn = binding.background;
    TooltipCompat.setTooltipText(backgroundBtn, "Save measurements in background");
    AtomicBoolean isBackgroundModeEnabled = new AtomicBoolean(false);
    backgroundBtn.setOnClickListener(view -> {
      if (isBackgroundModeEnabled.get()) {
        enableUIInteraction();
        stopBackgroundRecording();
      } else {
        disableUIInteraction();
        startBackgroundRecording();
      }

      // toggle background mode
      isBackgroundModeEnabled.set(!isBackgroundModeEnabled.get());
    });

    FloatingActionButton recordBtn = binding.record;
    TooltipCompat.setTooltipText(recordBtn, "Save measurement");
    recordBtn.setOnClickListener(view -> {
      recordMeasurement();
    });

    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
    if (mapFragment != null) {
      mapFragment.getMapAsync(this);
    }

    Grids grids = Grids.create();
    grids.setColor(GridType.TEN_METER, Color.blue());
    tileProvider = MGRSTileProvider.create(this, grids);
    grids.enableAllLabelers();
  }


  @Override
  protected void onPause() {
    super.onPause();
    this.finish();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    locationHandler.stop();
    sampler.stop();
  }

  /**
   * Saves the state of the map when the activity is paused.
   */
  @Override
  protected void onSaveInstanceState(@NonNull Bundle outState) {
    if (map != null) {
      outState.putParcelable(KEY_LOCATION, locationHandler.getLastKnownLocation());
    }
    super.onSaveInstanceState(outState);
  }

  /**
   * Manipulates the map when it's available.
   * This callback is triggered when the map is ready to be used.
   */
  @Override
  public void onMapReady(@NonNull GoogleMap googleMap) {
    map = googleMap;
    locationHandler = new LocationHandler(this, map);

    if (measurementType == Measurement.Type.Noise) {
      sampler = new AudioHandler(this);
    } else if (measurementType == Measurement.Type.WiFi) {
      sampler = new WifiHandler(this);
    } else if (measurementType == Measurement.Type.LTE) {
      sampler = new LteHandler(this);
    }

    // add MGRS grid
    map.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));

    // draw an initial map if measurements are coming from an existing heatmap
    if (!measurements.isEmpty()) {
      try {
        gridUtils.drawHeatmap(this, map, measurements.values());
      } catch (ParseException e) {
        throw new RuntimeException(e);
      }
    }

    requestPermissions();
  }

  // Call this method to request permissions
  private void requestPermissions() {
    List<String> missingPermissions = new ArrayList<>();
    for (String permission : PERMISSIONS_REQUIRED) {
      if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
        missingPermissions.add(permission);
      }
    }
    if (!missingPermissions.isEmpty()) {
      String[] permissionsToRequest = missingPermissions.toArray(new String[0]);
      ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSIONS_REQUEST_CODE);
    } else {
      // All required permissions are granted, start the handlers
      startHandlers();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == PERMISSIONS_REQUEST_CODE) {
      boolean allPermissionsGranted = true;
      for (int result : grantResults) {
        if (result != PackageManager.PERMISSION_GRANTED) {
          allPermissionsGranted = false;
          break;
        }
      }
      if (allPermissionsGranted) {
        // All required permissions are granted, continue with the app
        startHandlers();
      } else {
        // At least one required permission is not granted, show an explanation dialog if
        // necessary, then request the permissions again
        boolean showRationale = false;
        for (String permission : permissions) {
          if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            showRationale = true;
            break;
          }
        }

        if (showRationale) {
          new AlertDialog.Builder(this).setTitle("Permission Required").setMessage("This app requires location, audio, Wi-Fi and LTE permissions to work properly.").setPositiveButton("OK", (dialog, which) -> requestPermissions()).setNegativeButton("Cancel", (dialog, which) -> finish()).setCancelable(false).show();
        } else {
          requestPermissions();
        }
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  private void startHandlers() {
    locationHandler.setLocationPermissionGranted(true);
    locationHandler.start();
    sampler.start();
  }

  private void disableUIInteraction() {
    FloatingActionButton recordBtn = binding.record;
    recordBtn.setEnabled(false);
    recordBtn.setAlpha(0.5f);
  }

  private void enableUIInteraction() {
    FloatingActionButton recordBtn = binding.record;
    recordBtn.setEnabled(true);
    recordBtn.setAlpha(1.0f);
  }

  private void recordMeasurement() {
    double lat = locationHandler.getLastKnownLocation().getLatitude();
    double lng = locationHandler.getLastKnownLocation().getLongitude();
    MGRS mgrs = tileProvider.getMGRS(new LatLng(lat, lng));
    String coordinate = mgrs.coordinate(GridType.TEN_METER);

    Measurement measurement = new Measurement(coordinate);
    measurement.setType(measurementType);
    measurement.setIntensity(sampler.getAverageData());

    try {
      gridUtils.fillTile(this, map, measurement);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }

    if (measurements.containsKey(measurement.getCoordinate())) {
      measurements.replace(measurement.getCoordinate(), measurement);
    } else {
      measurements.put(measurement.getCoordinate(), measurement);
    }
  }

  private void startBackgroundRecording() {
    TimerTask backgroundRecordingTask = new TimerTask() {
      @Override
      public void run() {
        runOnUiThread(() -> recordMeasurement());
      }
    };

    // Schedule the background recording task to run every n minutes
    int timePreference = SettingsUtils.getTimePreference(this);
    backgroundRecordingTimer = new Timer();
    backgroundRecordingTimer.scheduleAtFixedRate(backgroundRecordingTask, 0, timePreference == 0 ? 2000 : timePreference);
  }

  private void stopBackgroundRecording() {
    backgroundRecordingTimer.cancel();
  }
}