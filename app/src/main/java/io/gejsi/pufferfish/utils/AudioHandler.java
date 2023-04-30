package io.gejsi.pufferfish.utils;

import com.google.android.gms.maps.GoogleMap;

import io.gejsi.pufferfish.controllers.MapsActivity;

public class AudioHandler {
  public static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

  private boolean audioPermissionGranted;

  public boolean getAudioPermissionGranted() {
    return audioPermissionGranted;
  }

  public void setAudioPermissionGranted(boolean locationPermissionGranted) {
    this.audioPermissionGranted = locationPermissionGranted;
  }

  private MapsActivity activity;
  private GoogleMap map;

  public AudioHandler(MapsActivity activity, GoogleMap googleMap) {
    this.activity = activity;
    this.map = googleMap;
  }
}
