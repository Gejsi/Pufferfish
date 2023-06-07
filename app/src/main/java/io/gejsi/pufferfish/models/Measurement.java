package io.gejsi.pufferfish.models;

public class Measurement {
  private String coordinate;
  private Type type;
  private Intensity intensity;

  public Measurement(String coordinate) {
    this.coordinate = coordinate;
  }

  public Measurement(String coordinate, Type type, Intensity intensity) {
    this.coordinate = coordinate;
    this.type = type;
    this.intensity = intensity;
  }

  public String getCoordinate() {
    return coordinate;
  }

  public void setCoordinate(String coordinate) {
    this.coordinate = coordinate;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public Intensity getIntensity() {
    return intensity;
  }

  public void setIntensity(Intensity intensity) {
    this.intensity = intensity;
  }

  public enum Intensity {
    Good,
    Average,
    Poor
  }

  public enum Type {
    Noise,
    WiFi,
    LTE
  }

  @Override
  public String toString() {
    return "Measurement{" +
            "coordinate='" + coordinate + '\'' +
            ", type=" + type +
            ", intensity=" + intensity +
            '}';
  }
}

