package io.gejsi.pufferfish.controllers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import io.gejsi.pufferfish.R;
import io.gejsi.pufferfish.databinding.ActivityMapsBinding;
import io.gejsi.pufferfish.handlers.AudioHandler;
import io.gejsi.pufferfish.handlers.LocationHandler;
import io.gejsi.pufferfish.handlers.LteHandler;
import io.gejsi.pufferfish.handlers.WifiHandler;
import io.gejsi.pufferfish.models.Heatmap;
import io.gejsi.pufferfish.models.IntentKey;
import io.gejsi.pufferfish.models.Measurement;
import io.gejsi.pufferfish.models.MeasurementSampler;
import io.gejsi.pufferfish.utils.GridUtils;
import io.gejsi.pufferfish.utils.HeatmapUtils;
import io.gejsi.pufferfish.utils.NotificationUtils;
import io.gejsi.pufferfish.utils.SettingsUtils;
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

  /** Used if a **local** heatmap is being edited rather than being created. */
  private volatile String existingFileName = null;

  /** Used if an **online** heatmap is being edited rather than being created. */
  private volatile String onlineTimestamp = null;

  private Measurement.Type measurementType = Measurement.Type.Noise;

  private GridType gridType = GridType.TEN_METER;

  public static final int PERMISSIONS_REQUEST_CODE = 1;
  @SuppressLint("InlinedApi")
  public static final String[] PERMISSIONS_REQUIRED = {
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION,
          Manifest.permission.RECORD_AUDIO,
          Manifest.permission.ACCESS_WIFI_STATE,
          Manifest.permission.POST_NOTIFICATIONS
  };

  // Keys for storing activity state.
  public static final String KEY_LOCATION = "location";

  /**
   * MGRS tile provider
   */
  private MGRSTileProvider tileProvider;

  private GridUtils gridUtils;
  private Timer backgroundRecordingTimer;
  private NotificationUtils notificationUtils;

  private AtomicBoolean isBackgroundModeEnabled = new AtomicBoolean(false);

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Retrieve location from saved instance state.
    if (savedInstanceState != null) {
      locationHandler.setLastKnownLocation(savedInstanceState.getParcelable(KEY_LOCATION));
    }

    if (getIntent().hasExtra(IntentKey.FileName.toString())) {
      // the measurements come from an existing local file
      existingFileName = getIntent().getStringExtra(IntentKey.FileName.toString());
      Heatmap heatmap = HeatmapUtils.loadHeatmap(this, existingFileName);
      measurements = heatmap.getMeasurements();
      gridType = heatmap.getGridType();
    } else if (getIntent().hasExtra(IntentKey.OnlineTimestamp.toString())) {
      // the measurements come from a heatmap saved online
      onlineTimestamp = getIntent().getStringExtra(IntentKey.OnlineTimestamp.toString());
      CompletableFuture<Heatmap> heatmapFuture = HeatmapUtils.fetchHeatmap(this, onlineTimestamp);

      heatmapFuture.thenAccept(heatmap -> {
        if (heatmap != null) {
          measurements = heatmap.getMeasurements();
          gridType = heatmap.getGridType();
        }
      });
    } else {
      // the measurements are brand new, they will be later saved locally
      measurements = new HashMap<>();
      gridType = SettingsUtils.getGridPreference(this);
    }

    String tileSize = gridType == GridType.METER
            ? "1 meter"
            : gridType == GridType.HUNDRED_METER
              ? "100 meters"
              : "10 meters";

    Toast.makeText(this, "Recording tiles of " + tileSize, Toast.LENGTH_SHORT).show();

    // Retrieve the selected measurement type from intent extras
    if (getIntent().hasExtra(IntentKey.MeasurementType.toString())) {
      String selectedMeasurementType = getIntent().getStringExtra(IntentKey.MeasurementType.toString());
      measurementType = Measurement.Type.valueOf(selectedMeasurementType);
    }

    binding = ActivityMapsBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    Toolbar toolbar = binding.toolbar;
    setSupportActionBar(toolbar);

    Button saveBtn = binding.btnSave;
    saveBtn.setOnClickListener(v -> {
      boolean isSaved;

      if (onlineTimestamp != null) {
        isSaved = HeatmapUtils.updateHeatmap(this, onlineTimestamp, measurements);
      } else {
        isSaved = HeatmapUtils.saveHeatmap(this, measurementType, measurements.values(), gridType, existingFileName);
      }

      if (isSaved) {
        this.finish();
      }
    });

    FloatingActionButton locationBtn = binding.myLoc;
    locationBtn.setOnClickListener(__ -> {
      if (locationHandler.getLastKnownLocation() == null) {
        Toast.makeText(this, "Unable to retrieve current position. Please, move around.", Toast.LENGTH_SHORT).show();
      }

      locationHandler.getDeviceLocation(gridType);
    });

    gridUtils = new GridUtils();
    notificationUtils = new NotificationUtils(this);

    FloatingActionButton backgroundBtn = binding.background;
    TooltipCompat.setTooltipText(backgroundBtn, "Save measurements in background");
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
    recordBtn.setOnClickListener(view -> recordMeasurement());

    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
    if (mapFragment != null) {
      mapFragment.getMapAsync(this);
    }

    // Create the grid
    Grids grids = Grids.create();
    grids.setColor(GridType.TEN_METER, Color.blue());
    tileProvider = MGRSTileProvider.create(this, grids);
    grids.enableAllLabelers();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    locationHandler.stop();
    sampler.stop();
    stopBackgroundRecording();
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
    map.getUiSettings().setMyLocationButtonEnabled(false);

    boolean notificationsEnabled = SettingsUtils.getNotificationsPreference(this);
    locationHandler = new LocationHandler(this, map) {
      @Override
      public void onLocationChanged(Location location) {
        String coordinate = getCurrentCoordinate();
        if (
                coordinate != null
                && !measurements.containsKey(coordinate)
                && notificationsEnabled
                && isBackgroundModeEnabled.get()
        ) {
          notificationUtils.sendNotification();
        }
      }
    };

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
    notificationUtils.createNotificationChannel();
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
        // At least one required permission is not granted, show an explanation dialog,
        // then request the permissions again
        boolean showRationale = false;
        for (String permission : permissions) {
          if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            showRationale = true;
            break;
          }
        }

        if (showRationale) {
          new AlertDialog.Builder(this)
                  .setTitle("Permission Required")
                  .setMessage("This app requires location, audio and notification permissions to work properly.")
                  .setPositiveButton("OK", (dialog, which) -> requestPermissions())
                  .setNegativeButton("Cancel", (dialog, which) -> finish())
                  .setCancelable(false)
                  .show();
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
    locationHandler.start(gridType);
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

  private String getCurrentCoordinate() {
    if (locationHandler.getLastKnownLocation() == null) {
      Toast.makeText(this, "Unable to retrieve current position. Please, move around.", Toast.LENGTH_SHORT).show();
      return null;
    }

    double lat = locationHandler.getLastKnownLocation().getLatitude();
    double lng = locationHandler.getLastKnownLocation().getLongitude();
    MGRS mgrs = tileProvider.getMGRS(new LatLng(lat, lng));
    return mgrs.coordinate(gridType);
  }

  private void recordMeasurement() {
    String coordinate = getCurrentCoordinate();
    if (coordinate == null) return;

    Measurement measurement = new Measurement(coordinate);
    measurement.setType(measurementType);
    measurement.setIntensity(sampler.getAverageData());

    try {
      gridUtils.fillTile(this, map, measurement, isBackgroundModeEnabled.get());
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }

    if (measurements.containsKey(measurement.getCoordinate())) {
      measurements.replace(measurement.getCoordinate(), measurement);
    } else {
      measurements.put(measurement.getCoordinate(), measurement);
    }
  }

  public void startBackgroundRecording() {
    TimerTask backgroundRecordingTask = new TimerTask() {
      @Override
      public void run() {
        runOnUiThread(() -> recordMeasurement());
      }
    };

    int timePreference = SettingsUtils.getTimePreference(this);
    backgroundRecordingTimer = new Timer();
    backgroundRecordingTimer.scheduleAtFixedRate(backgroundRecordingTask, 0, timePreference == 0 ? 2000 : timePreference);
  }

  public void stopBackgroundRecording() {
    if (backgroundRecordingTimer != null)
      backgroundRecordingTimer.cancel();
  }
}