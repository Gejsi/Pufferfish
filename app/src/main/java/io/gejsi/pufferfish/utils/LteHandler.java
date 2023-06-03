package io.gejsi.pufferfish.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthLte;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.google.android.gms.maps.GoogleMap;

import java.util.List;

import io.gejsi.pufferfish.controllers.MapsActivity;

public class LteHandler {
  private boolean permissionGranted;
  private boolean isRecording = false;

  // measurement data
  double[] data;

  public void setPermissionGranted(boolean permissionGranted) {
    this.permissionGranted = permissionGranted;
  }

  private MapsActivity activity;
  private GoogleMap map;

  public LteHandler(MapsActivity activity, GoogleMap googleMap) {
    this.activity = activity;
    this.map = googleMap;
  }

  @SuppressLint("MissingPermission")
  public void start() {
    if (!permissionGranted) {
      return;
    }

    TelephonyManager telephonyManager = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
    if (telephonyManager.getDataNetworkType() != TelephonyManager.NETWORK_TYPE_LTE) {
      Toast.makeText(activity, "Only LTE is supported.", Toast.LENGTH_SHORT).show();
      activity.finish();
    }

    isRecording = true;

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
    String averagePref = sharedPreferences.getString("average", "");
    int averageLength = averagePref.length() == 0 ? 10 : Integer.parseInt(averagePref);

    data = new double[averageLength];

    new Thread(() -> {
      for (int n = 0; isRecording; n++) {

        List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
        if (cellInfoList != null) {
          for (CellInfo cellInfo : cellInfoList) {
            if (cellInfo instanceof CellInfoLte) {
              CellSignalStrengthLte signalStrengthLte = ((CellInfoLte) cellInfo).getCellSignalStrength();
              int level = signalStrengthLte.getLevel();
              data[n % averageLength] = level;
              break;
            }
          }
        }
      }
    }).start();
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
