package io.gejsi.pufferfish.models;

import java.util.Map;

public class Heatmap {
  private String timestamp;
  private Measurement.Type measurementType;
  private Map<String, Measurement> measurements;


  public Heatmap() {}

  public Heatmap(String timestamp, Measurement.Type measurementType, Map<String, Measurement> measurements) {
    this.timestamp = timestamp;
    this.measurementType = measurementType;
    this.measurements = measurements;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public Measurement.Type getMeasurementType() {
    return measurementType;
  }

  public void setMeasurementType(Measurement.Type measurementType) {
    this.measurementType = measurementType;
  }

  public Map<String, Measurement> getMeasurements() {
    return measurements;
  }

  public void setMeasurements(Map<String, Measurement> measurements) {
    this.measurements = measurements;
  }
}
