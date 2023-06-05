package io.gejsi.pufferfish.utils;

import android.content.SharedPreferences;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.gejsi.pufferfish.controllers.MapsActivity;
import io.gejsi.pufferfish.models.MeasurementIntensity;
import mil.nga.mgrs.MGRS;

public class GridUtils {
  private Map<String, Polygon> polygons = new HashMap<>();
  private Map<String, Long> timestamps = new HashMap<>();

  public void fillTile(MapsActivity activity, GoogleMap map, String coordinate, MeasurementIntensity intensity) throws ParseException {
    int timePreference = getTimePreference(activity);

    // check if enough time has passed since last fill
    long currentTime = System.currentTimeMillis();
    Long lastFillTime = timestamps.get(coordinate);
    if (lastFillTime != null && (currentTime - lastFillTime) < timePreference) {
      new AlertDialog.Builder(activity)
              .setTitle("Cannot fill tile")
              .setMessage("Not enough time has passed since last fill. Check your settings to tweak timings.")
              .setPositiveButton("OK", null)
              .create()
              .show();
      return;
    }

    // update the timestamp for the current coordinate
    timestamps.put(coordinate, currentTime);

    // if a tile is already filled, remove it so it will be replaced
    Polygon prevPolygon = polygons.get(coordinate);
    if (prevPolygon != null) prevPolygon.remove();

    List<LatLng> vertices = getTileVertices(coordinate);
    Polygon polygon = map.addPolygon(new PolygonOptions().addAll(vertices).strokeColor(0x00000000));
    polygons.put(coordinate, polygon);

    if (intensity == MeasurementIntensity.Poor) {
      polygon.setFillColor(0x50FC0303);
    } else if (intensity == MeasurementIntensity.Average) {
      polygon.setFillColor(0x50FCBE03);
    } else {
      polygon.setFillColor(0x5081C784);
    }
  }

  private int getTimePreference(MapsActivity activity) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
    String timePref = sharedPreferences.getString("time", "");
    int time = timePref.length() == 0 ? 5 : Integer.parseInt(timePref);

    return time * 1000;
  }

  private List<LatLng> getTileVertices(String coordinate) throws ParseException {
    MGRS topLeftMGRS = MGRS.parse(modifyCoordinateByType(coordinate, CoordinateType.TopLeft));
    LatLng topLeft = new LatLng(topLeftMGRS.toPoint().getLatitude(), topLeftMGRS.toPoint().getLongitude());

    MGRS topRightMGRS = MGRS.parse(modifyCoordinateByType(coordinate, CoordinateType.TopRight));
    LatLng topRight = new LatLng(topRightMGRS.toPoint().getLatitude(), topRightMGRS.toPoint().getLongitude());

    MGRS bottomLeftMGRS = MGRS.parse(modifyCoordinateByType(coordinate, CoordinateType.BottomLeft));
    LatLng bottomLeft = new LatLng(bottomLeftMGRS.toPoint().getLatitude(), bottomLeftMGRS.toPoint().getLongitude());

    MGRS bottomRightMGRS = MGRS.parse(modifyCoordinateByType(coordinate, CoordinateType.BottomRight));
    LatLng bottomRight = new LatLng(bottomRightMGRS.toPoint().getLatitude(), bottomRightMGRS.toPoint().getLongitude());

    List<LatLng> vertices = new ArrayList<>(4);
    vertices.add(topRight);
    vertices.add(bottomRight);
    vertices.add(bottomLeft);
    vertices.add(topLeft);

    return vertices;
  }

  private enum CoordinateType {
    TopLeft, TopRight, BottomRight, BottomLeft;
  }

  private String modifyCoordinateByType(String str, CoordinateType type) {
    String text = str.substring(5);
    String first = text.substring(0, text.length() / 2);
    String second = text.substring(text.length() / 2);

    int firstNumber = Integer.parseInt(first);
    int secondNumber = Integer.parseInt(second);

    if (type == CoordinateType.TopLeft) {
      secondNumber++;
    } else if (type == CoordinateType.TopRight) {
      firstNumber++;
      secondNumber++;
    } else if (type == CoordinateType.BottomRight) {
      firstNumber++;
    }

    return str.substring(0, 5) + String.valueOf(firstNumber) + String.valueOf(secondNumber);
  }
}
