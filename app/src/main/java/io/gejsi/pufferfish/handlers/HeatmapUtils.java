package io.gejsi.pufferfish.handlers;

import android.app.Activity;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.gejsi.pufferfish.models.Measurement;

public class HeatmapUtils {
  public static void saveHeatmap(Activity activity, Measurement.Type measurementType, List<Measurement> measurements) {
    if (measurements.isEmpty()) {
      Toast.makeText(activity, "No measurements have been taken yet. Cannot save the heatmap.", Toast.LENGTH_SHORT).show();
      return;
    }

    String timestamp = new SimpleDateFormat("dd-MM-yyyy_HH:mm:ss", Locale.getDefault()).format(new Date());

    // Create the JSON array for measurements
    JSONArray measurementsArray = new JSONArray();

    // Iterate over your measurements and add them to the array
    for (Measurement measurement : measurements) {
      try {
        // Create a JSON object for each measurement
        JSONObject measurementObject = new JSONObject();
        measurementObject.put("coordinate", measurement.getCoordinate());
        measurementObject.put("type", measurement.getType().toString());
        measurementObject.put("intensity", measurement.getIntensity().toString());

        // Add the measurement object to the array
        measurementsArray.put(measurementObject);
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }

    JSONObject rootObject = new JSONObject();
    try {
      rootObject.put("timestamp", timestamp);
      rootObject.put("type", measurementType);
      rootObject.put("measurements", measurementsArray);
    } catch (JSONException e) {
      e.printStackTrace();
    }

    String fileName = "Heatmap_" + measurementType + "_" + timestamp + ".json";
    try {
      File file = new File(activity.getFilesDir(), fileName);
      FileWriter fileWriter = new FileWriter(file);
      fileWriter.write(rootObject.toString());
      fileWriter.close();
      Toast.makeText(activity, "Heatmap saved", Toast.LENGTH_SHORT).show();
    } catch (IOException e) {
      e.printStackTrace();
      Toast.makeText(activity, "Error saving heatmap", Toast.LENGTH_SHORT).show();
    }
  }
}
