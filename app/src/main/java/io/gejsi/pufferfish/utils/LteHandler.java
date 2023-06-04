package io.gejsi.pufferfish.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthLte;
import android.telephony.TelephonyManager;

import com.google.android.gms.maps.GoogleMap;

import java.util.List;

import io.gejsi.pufferfish.controllers.MapsActivity;

public class LteHandler extends MeasurementHandler {
  public LteHandler(MapsActivity activity, GoogleMap googleMap) {
    super(activity, googleMap);
  }

  @SuppressLint("MissingPermission")
  public void start() {
    TelephonyManager telephonyManager = (TelephonyManager) this.getActivity().getSystemService(Context.TELEPHONY_SERVICE);

    this.setRecording(true);
    int averageLength = this.getAverageLengthPreference();
    this.setData(new double[averageLength]);

    new Thread(() -> {
      for (int n = 0; this.isRecording(); n++) {
        List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
        if (cellInfoList != null) {
          for (CellInfo cellInfo : cellInfoList) {
            if (cellInfo instanceof CellInfoLte) {
              CellSignalStrengthLte signalStrengthLte = ((CellInfoLte) cellInfo).getCellSignalStrength();
              int level = signalStrengthLte.getLevel();
              double[] data = getData();
              data[n % averageLength] = level;
              break;
            }
          }
        }
      }
    }).start();
  }

  @Override
  public void stop() {
    this.setRecording(false);
  }
}
