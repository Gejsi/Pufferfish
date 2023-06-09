package io.gejsi.pufferfish.controllers;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class MeasurementService extends Service {
  public static boolean isServiceRunning = false;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    isServiceRunning = true;

    new Thread(() -> {
      while (isServiceRunning) {
        Log.d("Background", "Service is running...");
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }).start();

    return super.onStartCommand(intent, flags, startId);
  }

  public static void stopService() {
    isServiceRunning = false;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    stopService();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}