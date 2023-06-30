package io.gejsi.pufferfish.utils;

import android.app.Activity;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.gejsi.pufferfish.R;
import io.gejsi.pufferfish.models.Heatmap;
import io.gejsi.pufferfish.models.Measurement;
import mil.nga.mgrs.grid.GridType;

public class HeatmapUtils {
  public static boolean saveHeatmap(Activity activity, Measurement.Type measurementType, Collection<Measurement> measurements, GridType gridType, String existingFile) {
    if (measurements.isEmpty()) {
      Toast.makeText(activity, "No measurements have been taken yet. Cannot save the heatmap.", Toast.LENGTH_SHORT).show();
      return false;
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
      if (existingFile != null) {
        // gets the timestamp from the filename if the heatmap is being edited
        String[] fileParts = existingFile.substring(0, existingFile.length() - ".json".length()).split("_");
        String existingTimestamp = fileParts[2] + "_" + fileParts[3];
        rootObject.put("timestamp", existingTimestamp);
      } else {
        // otherwise, get the current timestamp
        rootObject.put("timestamp", timestamp);
      }
      rootObject.put("type", measurementType);
      rootObject.put("gridType", gridType);
      rootObject.put("measurements", measurementsArray);
    } catch (JSONException e) {
      e.printStackTrace();
    }

    String fileName = existingFile != null ? existingFile : "Heatmap_" + measurementType + "_" + timestamp + ".json";
    try {
      File file = new File(activity.getFilesDir(), fileName);
      FileWriter fileWriter = new FileWriter(file);
      fileWriter.write(rootObject.toString());
      fileWriter.close();
      Toast.makeText(activity, "Heatmap saved", Toast.LENGTH_SHORT).show();
    } catch (IOException e) {
      e.printStackTrace();
      Toast.makeText(activity, "Error while saving the heatmap", Toast.LENGTH_SHORT).show();
    }

    return true;
  }

  public static boolean updateHeatmap(Activity activity, String heatmapKey, Map<String, Measurement> measurements) {
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    assert currentUser != null;
    FirebaseDatabase database = FirebaseDatabase.getInstance(activity.getString(R.string.db));
    DatabaseReference heatmapsRef = database.getReference("heatmaps").child(currentUser.getUid());

    Map<String, Object> objectMeasurements = new HashMap<>();
    objectMeasurements.put("measurements", measurements);
    heatmapsRef.child(heatmapKey).updateChildren(objectMeasurements).addOnSuccessListener(__ -> {
      Toast.makeText(activity, "Heatmap updated", Toast.LENGTH_SHORT).show();
    }).addOnFailureListener(__ -> {
      Toast.makeText(activity, "Error while updating the heatmap", Toast.LENGTH_SHORT).show();
    });

    return true;
  }

  public static Heatmap loadHeatmap(Activity activity, String fileName) {
    String timestamp = "";
    Measurement.Type measurementType = null;
    Map<String, Measurement> measurements = new HashMap<>();
    GridType gridType = null;

    try {
      File file = new File(activity.getFilesDir(), fileName);
      InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(file));
      BufferedReader br = new BufferedReader(inputStreamReader);
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line);
      }
      br.close();

      // Parse the JSON string
      String jsonString = sb.toString();
      JSONObject rootObject = new JSONObject(jsonString);
      timestamp = rootObject.getString("timestamp");
      measurementType = Measurement.Type.valueOf(rootObject.getString("type"));
      gridType = GridType.valueOf(rootObject.getString("gridType"));

      JSONArray measurementsArray = rootObject.getJSONArray("measurements");
      for (int i = 0; i < measurementsArray.length(); i++) {
        JSONObject measurementObject = measurementsArray.getJSONObject(i);
        String coordinate = measurementObject.getString("coordinate");
        Measurement.Type type = Measurement.Type.valueOf(measurementObject.getString("type"));
        Measurement.Intensity intensity = Measurement.Intensity.valueOf(measurementObject.getString("intensity"));
        measurements.put(coordinate, new Measurement(coordinate, type, intensity));
      }
    } catch (IOException | JSONException e) {
      e.printStackTrace();
      Toast.makeText(activity, "Error while loading the heatmap", Toast.LENGTH_SHORT).show();
    }

    return new Heatmap(timestamp, measurementType, measurements, gridType);
  }

  public static void syncHeatmap(Activity activity, String fileName) {
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

    if (currentUser == null) {
      Toast.makeText(activity, "Cannot sync as a guest. Please, login.", Toast.LENGTH_SHORT).show();
      return;
    }

    Heatmap heatmap = loadHeatmap(activity, fileName);

    FirebaseDatabase database = FirebaseDatabase.getInstance(activity.getString(R.string.db));
    DatabaseReference heatmapsRef = database.getReference("heatmaps").child(currentUser.getUid());

    Query query = heatmapsRef.orderByChild("timestamp").equalTo(heatmap.getTimestamp());
    query.addListenerForSingleValueEvent(new ValueEventListener() {
      @Override
      public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        if (dataSnapshot.exists()) {
          // heatmap already exists, update it
          for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            String heatmapKey = snapshot.getKey();

            if (heatmapKey != null)
              heatmapsRef.child(heatmapKey).setValue(heatmap).addOnSuccessListener(activity, __ -> {
                Toast.makeText(activity, "Heatmap successfully updated.", Toast.LENGTH_SHORT).show();
              });
          }
        } else {
          // heatmap is new, push it
          heatmapsRef.child(heatmap.getTimestamp()).setValue(heatmap).addOnSuccessListener(activity, __ -> {
            Toast.makeText(activity, "Heatmap successfully synced.", Toast.LENGTH_SHORT).show();
          });
        }
      }

      @Override
      public void onCancelled(@NonNull DatabaseError databaseError) {
        Toast.makeText(activity, "Error while syncing the heatmap.", Toast.LENGTH_SHORT).show();
      }
    });
  }

  public static CompletableFuture<Heatmap> fetchHeatmap(Activity activity, String onlineTimestamp) {
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    assert currentUser != null;

    FirebaseDatabase database = FirebaseDatabase.getInstance(activity.getString(R.string.db));
    DatabaseReference heatmapsRef = database.getReference("heatmaps").child(currentUser.getUid());
    CompletableFuture<Heatmap> heatmapFuture = new CompletableFuture<>();

    ValueEventListener valueEventListener = new ValueEventListener() {
      @Override
      public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        for (DataSnapshot heatmapSnapshot : dataSnapshot.getChildren()) {
          Heatmap heatmap = heatmapSnapshot.getValue(Heatmap.class);
          if (heatmap != null && heatmap.getTimestamp().equals(onlineTimestamp)) {
            heatmapFuture.complete(heatmap);
            return;
          }
        }

        Toast.makeText(activity, "Heatmap not found.", Toast.LENGTH_SHORT).show();
        heatmapFuture.complete(null);
      }

      @Override
      public void onCancelled(@NonNull DatabaseError databaseError) {
        Toast.makeText(activity, "Error while fetching the heatmap.", Toast.LENGTH_SHORT).show();
        activity.finish();
      }
    };

    heatmapsRef.addValueEventListener(valueEventListener);

    return heatmapFuture;
  }

}
