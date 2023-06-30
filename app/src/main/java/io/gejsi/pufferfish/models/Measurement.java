package io.gejsi.pufferfish.models;

import androidx.annotation.NonNull;

import com.google.firebase.database.Exclude;

public class Measurement {
  private String coordinate;
  private Type type;
  private Intensity intensity;

  public Measurement() {
  }

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

  @Exclude
  public void setIntensity(double data) {
    if (type == Type.Noise) {
      if (data < 10) intensity = Intensity.Good;
      else if (data >= 10 && data <= 30) intensity = Intensity.Average;
      else intensity = Intensity.Bad;
    } else if (type == Type.WiFi || type == Type.LTE) {
      if (data >= 3) intensity = Intensity.Good;
      else if (data == 2) intensity = Intensity.Average;
      else intensity = Intensity.Bad;
    } else {
      throw new IllegalArgumentException("Cannot set intensity since no measurement type has been provided.");
    }
  }

  public enum Intensity {
    Good,
    Average,
    Bad
  }

  public enum Type {
    Noise,
    WiFi,
    LTE
  }

  @NonNull
  @Override
  public String toString() {
    return "Measurement{" +
            "coordinate='" + coordinate + '\'' +
            ", type=" + type +
            ", intensity=" + intensity +
            '}';
  }
}

