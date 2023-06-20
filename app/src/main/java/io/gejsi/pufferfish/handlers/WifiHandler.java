package io.gejsi.pufferfish.handlers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import java.util.List;

import io.gejsi.pufferfish.models.MeasurementSampler;
import io.gejsi.pufferfish.utils.SettingsUtils;

public class WifiHandler extends MeasurementSampler {
  public WifiHandler(Context context) {
    super(context);
  }

  @SuppressLint("MissingPermission")
  public void start() {
    WifiManager wifiManager = (WifiManager) this.getContext().getSystemService(Context.WIFI_SERVICE);

    this.setRecording(true);
    int averageLength = SettingsUtils.getAverageLengthPreference(getContext());
    this.setData(new double[averageLength]);

    new Thread(() -> {
      for (int n = 0; this.isRecording(); n++) {
        List<ScanResult> wifiList = wifiManager.getScanResults();
        int level = findMaxWifiLevel(wifiList);
        double[] data = this.getData();
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

  @Override
  public void stop() {
    this.setRecording(false);
  }
}
