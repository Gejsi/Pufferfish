package io.gejsi.pufferfish.controllers;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

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
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import io.gejsi.pufferfish.R;
import io.gejsi.pufferfish.databinding.ActivityMapsBinding;
import io.gejsi.pufferfish.models.MeasurementType;
import io.gejsi.pufferfish.utils.AudioHandler;
import io.gejsi.pufferfish.utils.LocationHandler;
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

  private MeasurementType measurementType = MeasurementType.Noise;

  public static final int PERMISSIONS_REQUEST_CODE = 1;
  public static final String[] PERMISSIONS_REQUIRED = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.RECORD_AUDIO};

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

    Spinner measurementSpinner = findViewById(R.id.measurement_spinner);
    String[] measurementTypes = new String[]{"Acoustic Noise", "WiFi strength", "LTE strength"};
    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, measurementTypes);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    measurementSpinner.setAdapter(adapter);

    measurementSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position == 0) {
          measurementType = MeasurementType.Noise;
          audioHandler.start();
        } else if (position == 1) {
          measurementType = MeasurementType.WiFi;
          audioHandler.stop();
        } else if (position == 2) {
          measurementType = MeasurementType.LTE;
          audioHandler.stop();
        }
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
        Log.d(TAG, "Nothing selected");
      }
    });

    FloatingActionButton loc = binding.loc;
    TooltipCompat.setTooltipText(loc, "My location");
    loc.setOnClickListener(view -> {
      locationHandler.getDeviceLocation();
    });

    FloatingActionButton recordBtn = binding.record;
    TooltipCompat.setTooltipText(recordBtn, "Save measurement");
    recordBtn.setOnClickListener(view -> {
      double lat = locationHandler.getLastKnownLocation().getLatitude();
      double lng = locationHandler.getLastKnownLocation().getLongitude();
      LatLng latLng = new LatLng(lat, lng);
      MGRS mgrs = tileProvider.getMGRS(latLng);

      try {
        LatLng topLeft = new LatLng(MGRS.parse("32T PQ 8607 3036").toPoint().getLatitude(), MGRS.parse("32T PQ 8607 3036").toPoint().getLongitude());
        LatLng topRight = new LatLng(MGRS.parse("32T PQ 8608 3036").toPoint().getLatitude(), MGRS.parse("32T PQ 8608 3036").toPoint().getLongitude());
        LatLng bottomLeft = new LatLng(MGRS.parse("32T PQ 8607 3035").toPoint().getLatitude(), MGRS.parse("32T PQ 8607 3035").toPoint().getLongitude());
        LatLng bottomRight = new LatLng(MGRS.parse("32T PQ 8608 3035").toPoint().getLatitude(), MGRS.parse("32T PQ 8608 3035").toPoint().getLongitude());
        List<LatLng> vertices = getTileVertices(mgrs.coordinate(GridType.TEN_METER));
        Polygon polygon1 = map.addPolygon(new PolygonOptions().addAll(vertices));
        polygon1.setStrokeColor(0x00000000);
        polygon1.setFillColor(0x8081C784);
      } catch (ParseException e) {
        throw new RuntimeException(e);
      }
    });

    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
    if (mapFragment != null) {
      mapFragment.getMapAsync(this);
    }

    Grids grids = Grids.create(GridType.TEN_METER);
    grids.setColor(GridType.TEN_METER, Color.blue());
    tileProvider = MGRSTileProvider.create(this, grids);
    grids.enableAllLabelers();
  }

  private List<LatLng> getTileVertices(String coordinate) throws ParseException {
    MGRS topLeftMGRS = MGRS.parse(modifyCoordinateByType(coordinate, "tl"));
    LatLng topLeft = new LatLng(topLeftMGRS.toPoint().getLatitude(), topLeftMGRS.toPoint().getLongitude());

    MGRS topRightMGRS = MGRS.parse(modifyCoordinateByType(coordinate, "tr"));
    LatLng topRight = new LatLng(topRightMGRS.toPoint().getLatitude(), topRightMGRS.toPoint().getLongitude());

    MGRS bottomLeftMGRS = MGRS.parse(modifyCoordinateByType(coordinate, "bl"));
    LatLng bottomLeft = new LatLng(bottomLeftMGRS.toPoint().getLatitude(), bottomLeftMGRS.toPoint().getLongitude());

    MGRS bottomRightMGRS = MGRS.parse(modifyCoordinateByType(coordinate, "br"));
    LatLng bottomRight = new LatLng(bottomRightMGRS.toPoint().getLatitude(), bottomRightMGRS.toPoint().getLongitude());

    List<LatLng> vertices = new ArrayList<>();
    vertices.add(topRight);
    vertices.add(bottomRight);
    vertices.add(bottomLeft);
    vertices.add(topLeft);

    return vertices;
  }

  private String modifyCoordinateByType(String str, String type) {
    String text = str.substring(5);
    String first = text.substring(0, text.length() / 2);
    String second = text.substring(text.length() / 2);

    int firstNumber = Integer.parseInt(first);
    int secondNumber = Integer.parseInt(second);

    if (type == "tl") {
      secondNumber++;
    } else if (type == "tr") {
      firstNumber++;
      secondNumber++;
    } else if (type == "br") {
      firstNumber++;
    }

    return str.substring(0, 5) + String.valueOf(firstNumber) + String.valueOf(secondNumber);
  }


  @Override
  protected void onDestroy() {
    super.onDestroy();
    locationHandler.stop();
    audioHandler.stop();
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
   * Handles the result of the request for permissions.
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
          new AlertDialog.Builder(this).setTitle("Permission Required").setMessage("This app requires location and audio recording permissions to work properly.").setPositiveButton("OK", (dialog, which) -> requestPermissions()).setNegativeButton("Cancel", (dialog, which) -> finish()).setCancelable(false).show();
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
    audioHandler.setAudioPermissionGranted(true);
  }
}