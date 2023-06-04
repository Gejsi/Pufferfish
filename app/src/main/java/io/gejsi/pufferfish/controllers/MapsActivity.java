package io.gejsi.pufferfish.controllers;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

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
import java.util.List;

import io.gejsi.pufferfish.R;
import io.gejsi.pufferfish.databinding.ActivityMapsBinding;
import io.gejsi.pufferfish.models.MeasurementIntensity;
import io.gejsi.pufferfish.models.MeasurementType;
import io.gejsi.pufferfish.utils.AudioHandler;
import io.gejsi.pufferfish.utils.GridUtils;
import io.gejsi.pufferfish.utils.LocationHandler;
import io.gejsi.pufferfish.utils.LteHandler;
import io.gejsi.pufferfish.utils.WifiHandler;
import mil.nga.color.Color;
import mil.nga.mgrs.MGRS;
import mil.nga.mgrs.grid.GridType;
import mil.nga.mgrs.grid.style.Grids;
import mil.nga.mgrs.tile.MGRSTileProvider;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {
  public static final String TAG = MapsActivity.class.getSimpleName();
  private GoogleMap map;
  private ActivityMapsBinding binding;
  private LocationHandler locationHandler;
  private AudioHandler audioHandler;
  private WifiHandler wifiHandler;
  private LteHandler lteHandler;

  private MeasurementType measurementType = MeasurementType.Noise;

  public static final int PERMISSIONS_REQUEST_CODE = 1;
  public static final String[] PERMISSIONS_REQUIRED = {
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.ACCESS_WIFI_STATE
  };

  // Keys for storing activity state.
  public static final String KEY_CAMERA_POSITION = "camera_position";
  public static final String KEY_LOCATION = "location";

  /**
   * MGRS tile provider
   */
  private MGRSTileProvider tileProvider;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Retrieve location and camera position from saved instance state.
    if (savedInstanceState != null) {
      locationHandler.setLastKnownLocation(savedInstanceState.getParcelable(KEY_LOCATION));
      locationHandler.setCameraPosition(savedInstanceState.getParcelable(KEY_CAMERA_POSITION));
    }

    binding = ActivityMapsBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    Toolbar toolbar = binding.toolbar;
    setSupportActionBar(toolbar);

    // Retrieve the selected measurement type from intent extras
    if (getIntent().hasExtra("measurementType")) {
      String selectedMeasurementType = getIntent().getStringExtra("measurementType");

      // Convert the selected measurement type string to the MeasurementType enum
      measurementType = MeasurementType.valueOf(selectedMeasurementType);
    }

    FloatingActionButton loc = binding.loc;
    TooltipCompat.setTooltipText(loc, "My location");
    loc.setOnClickListener(view -> {
      locationHandler.getDeviceLocation();
    });

    FloatingActionButton recordBtn = binding.record;
    TooltipCompat.setTooltipText(recordBtn, "Save measurement");
    GridUtils gridUtils = new GridUtils();
    recordBtn.setOnClickListener(view -> {
      double lat = locationHandler.getLastKnownLocation().getLatitude();
      double lng = locationHandler.getLastKnownLocation().getLongitude();
      LatLng latLng = new LatLng(lat, lng);
      MGRS mgrs = tileProvider.getMGRS(latLng);

      try {
        String coordinate = mgrs.coordinate(GridType.TEN_METER);

        if (measurementType == MeasurementType.Noise) {
          double data = audioHandler.getAverageData();

          if (data < 10) gridUtils.fillTile(map, coordinate, MeasurementIntensity.Good);
          else if (data >= 10 && data <= 30)
            gridUtils.fillTile(map, coordinate, MeasurementIntensity.Average);
          else gridUtils.fillTile(map, coordinate, MeasurementIntensity.Poor);
        } else if (measurementType == MeasurementType.WiFi) {
          double data = wifiHandler.getAverageData();

          if (data >= 3) gridUtils.fillTile(map, coordinate, MeasurementIntensity.Good);
          else if (data == 2) gridUtils.fillTile(map, coordinate, MeasurementIntensity.Average);
          else gridUtils.fillTile(map, coordinate, MeasurementIntensity.Poor);
        } else if (measurementType == MeasurementType.LTE) {
          double data = lteHandler.getAverageData();

          if (data >= 3) gridUtils.fillTile(map, coordinate, MeasurementIntensity.Good);
          else if (data == 2) gridUtils.fillTile(map, coordinate, MeasurementIntensity.Average);
          else gridUtils.fillTile(map, coordinate, MeasurementIntensity.Poor);
        }
      } catch (ParseException e) {
        throw new RuntimeException(e);
      }
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
  protected void onDestroy() {
    super.onDestroy();
    locationHandler.stop();

    if (audioHandler != null) audioHandler.stop();

    if (wifiHandler != null) wifiHandler.stop();

    if (lteHandler != null) lteHandler.stop();
  }

  /**
   * Saves the state of the map when the activity is paused.
   */
  @Override
  protected void onSaveInstanceState(@NonNull Bundle outState) {
    if (map != null) {
      outState.putParcelable(KEY_CAMERA_POSITION, map.getCameraPosition());
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
    audioHandler = new AudioHandler(this, map);
    wifiHandler = new WifiHandler(this, map);
    lteHandler = new LteHandler(this, map);

    map.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));
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
          new AlertDialog.Builder(this).setTitle("Permission Required").setMessage("This app requires location, audio and Wi-Fi permissions to work properly.").setPositiveButton("OK", (dialog, which) -> requestPermissions()).setNegativeButton("Cancel", (dialog, which) -> finish()).setCancelable(false).show();
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

    if (measurementType == MeasurementType.Noise) {
      audioHandler.start();
    } else if (measurementType == MeasurementType.WiFi) {
      wifiHandler.start();
    } else if (measurementType == MeasurementType.LTE) {
      lteHandler.start();
    }
  }
}