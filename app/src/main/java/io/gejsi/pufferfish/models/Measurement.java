package io.gejsi.pufferfish.models;

import mil.nga.grid.features.Point;
import mil.nga.mgrs.MGRS;

public class Measurement {
  private int acousticNoiseIntensity = -1;
  private int wifiStrength = -1;
  private int lteStrength = -1;
  private MGRS mgrs;

  public Measurement(double latitude, double longitude) {
    this.mgrs = MGRS.from(Point.point(longitude, latitude));
  }

  public MGRS getMGRS() {
    return mgrs;
  }

  public int getAcousticNoiseIntensity() {
    return acousticNoiseIntensity;
  }

  public int getWifiStrength() {
    return wifiStrength;
  }

  public int getLteStrength() {
    return lteStrength;
  }

  public void setAcousticNoiseIntensity(int acousticNoiseIntensity) {
    this.acousticNoiseIntensity = acousticNoiseIntensity;
  }

  public void setWifiStrength(int wifiStrength) {
    this.wifiStrength = wifiStrength;
  }

  public void setLteStrength(int lteStrength) {
    this.lteStrength = lteStrength;
  }
}

