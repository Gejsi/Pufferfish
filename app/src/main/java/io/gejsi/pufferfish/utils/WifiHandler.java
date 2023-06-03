package io.gejsi.pufferfish.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import androidx.preference.PreferenceManager;

import com.google.android.gms.maps.GoogleMap;

import java.util.List;

import io.gejsi.pufferfish.controllers.MapsActivity;

public class WifiHandler {
  private boolean wifiPermissionGranted;
  private boolean isRecording = false;

  // measurement data
  double[] data;

  public void setWifiPermissionGranted(boolean wifiPermissionGranted) {
    this.wifiPermissionGranted = wifiPermissionGranted;
  }

  private MapsActivity activity;
  private GoogleMap map;

  public WifiHandler(MapsActivity activity, GoogleMap googleMap) {
    this.activity = activity;
    this.map = googleMap;
  }

  @SuppressLint("MissingPermission")
  public void start() {
    if (!wifiPermissionGranted) {
      return;
    }

    WifiManager wifiManager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

    isRecording = true;

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
    String averagePref = sharedPreferences.getString("average", "");
    int averageLength = averagePref.length() == 0 ? 10 : Integer.parseInt(averagePref);

    data = new double[averageLength];

    new Thread(() -> {
      for (int n = 0; isRecording; n++) {
        List<ScanResult> wifiList = wifiManager.getScanResults();
        int level = findMaxWifiLevel(wifiList);
        data[n % averageLength] = level;
      }
    }).start();
  }

  private int findMaxWifiLevel(List<ScanResult> wifiList) {
    int maxLevel = 0;
    for (ScanResult scanResult : wifiList) {
      int level = WifiManager.calculateSignalLevel(scanResult.level, 5);

      if (level > maxLevel) {
        maxLevel = level;
      }
    }
    return maxLevel;
  }

  public void stop() {
    isRecording = false;
  }

  public double getData() {
    double sum = 0;

    for (int i = 0; i < data.length; i++) {
      if (data[i] != 0) sum += data[i];
    }

    return sum / data.length;
  }
}
