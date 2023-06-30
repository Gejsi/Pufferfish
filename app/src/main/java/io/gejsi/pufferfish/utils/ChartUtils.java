package io.gejsi.pufferfish.utils;

import android.app.Activity;
import android.graphics.Color;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
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
import io.gejsi.pufferfish.models.Heatmap;
import io.gejsi.pufferfish.models.Measurement;

public class ChartUtils {
  public static void drawChart(Activity activity, BarChart intensityChart, Measurement.Type measurementType) {
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    assert currentUser != null;

    FirebaseDatabase database = FirebaseDatabase.getInstance(activity.getString(R.string.db));
    DatabaseReference heatmapsRef = database.getReference("heatmaps").child(currentUser.getUid());

    CompletableFuture<DataSnapshot> dataSnapshotFuture = new CompletableFuture<>();
    heatmapsRef.addValueEventListener(new ValueEventListener() {
      @Override
      public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        dataSnapshotFuture.complete(dataSnapshot);
      }

      @Override
      public void onCancelled(@NonNull DatabaseError databaseError) {
      }
    });


    dataSnapshotFuture.thenAccept(dataSnapshot -> {
      int goodNum = 0;
      int averageNum = 0;
      int badNum = 0;

      for (DataSnapshot heatmapSnapshot : dataSnapshot.getChildren()) {
        Heatmap heatmap = heatmapSnapshot.getValue(Heatmap.class);
        if (heatmap != null && heatmap.getMeasurementType() == measurementType) {
          for (Measurement measurement : heatmap.getMeasurements().values()) {
            if (measurement.getIntensity() == Measurement.Intensity.Good)
              goodNum++;
            else if (measurement.getIntensity() == Measurement.Intensity.Average)
              averageNum++;
            else if (measurement.getIntensity() == Measurement.Intensity.Bad)
              badNum++;
          }
        }
      }

      float totalMeasurements = goodNum + averageNum + badNum;
      float goodPercentage = (goodNum / totalMeasurements) * 100;
      float averagePercentage = (averageNum / totalMeasurements) * 100;
      float badPercentage = (badNum / totalMeasurements) * 100;

      List<BarEntry> entries = new ArrayList<>();
      if (totalMeasurements != 0) {
        entries.add(new BarEntry(0f, goodPercentage));
        entries.add(new BarEntry(1f, averagePercentage));
        entries.add(new BarEntry(2f, badPercentage));

        BarDataSet dataSet = new BarDataSet(entries, "Intensity Distribution");
        dataSet.setColors(Color.GREEN, Color.YELLOW, Color.RED);
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);
        intensityChart.setData(barData);
        intensityChart.invalidate();

        ((TextView) activity.findViewById(R.id.goodMeasurementsText)).setText("Number of good measurements: " + goodNum);
        ((TextView) activity.findViewById(R.id.averageMeasurementsText)).setText("Number of average measurements: " + averageNum);
        ((TextView) activity.findViewById(R.id.badMeasurementsText)).setText("Number of poor/loud measurements: " + badNum);
        ((TextView) activity.findViewById(R.id.totalMeasurementsText)).setText("Total number of measurements: " + (int) totalMeasurements);
      } else {
        intensityChart.setData(null);
        intensityChart.invalidate();
      }
    });
  }
}
