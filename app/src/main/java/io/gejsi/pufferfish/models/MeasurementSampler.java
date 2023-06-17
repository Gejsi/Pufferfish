package io.gejsi.pufferfish.models;

import android.content.Context;

public abstract class MeasurementSampler {
  private boolean isRecording = false;

  // measurement data
  private double[] data;

  private Context context;

  public MeasurementSampler(Context context) {
    this.context = context;
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

  protected Context getContext() {
    return context;
  }

  public double getAverageData() {
    double sum = 0;

    for (double datum : data) {
      if (datum != 0) sum += datum;
    }

    return sum / data.length;
  }
}
