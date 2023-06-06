package io.gejsi.pufferfish.handlers;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.google.android.gms.maps.GoogleMap;

import io.gejsi.pufferfish.controllers.MapsActivity;

public abstract class MeasurementHandler {
  private boolean isRecording = false;

  // measurement data
  private double[] data;

  private MapsActivity activity;
  private GoogleMap map;

  public MeasurementHandler(MapsActivity activity) {
    this.activity = activity;
  }

  public abstract void start();

  public abstract void stop();

  protected boolean isRecording() {
    return isRecording;
  }

  protected void setRecording(boolean recording) {
    isRecording = recording;
  }

  protected double[] getData() {
    return data;
  }

  protected void setData(double[] data) {
    this.data = data;
  }

  protected MapsActivity getActivity() {
    return activity;
  }

  public double getAverageData() {
    double sum = 0;

    for (double datum : data) {
      if (datum != 0) sum += datum;
    }

    return sum / data.length;
  }

  protected int getAverageLengthPreference() {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
    String averagePref = sharedPreferences.getString("average", "");
    return averagePref.length() == 0 ? 10 : Integer.parseInt(averagePref);
  }
}
