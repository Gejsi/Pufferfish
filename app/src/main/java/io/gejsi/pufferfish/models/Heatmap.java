package io.gejsi.pufferfish.models;

import java.util.Map;

import mil.nga.mgrs.grid.GridType;

public class Heatmap {
  private String timestamp;
  private Measurement.Type measurementType;
  private Map<String, Measurement> measurements;

  private GridType gridType;


  public Heatmap() {
  }

  public Heatmap(String timestamp, Measurement.Type measurementType, Map<String, Measurement> measurements, GridType gridType) {
    this.timestamp = timestamp;
    this.measurementType = measurementType;
    this.measurements = measurements;
    this.gridType = gridType;
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

  public GridType getGridType() {
    return gridType;
  }

  public void setGridType(GridType gridType) {
    this.gridType = gridType;
  }
}
