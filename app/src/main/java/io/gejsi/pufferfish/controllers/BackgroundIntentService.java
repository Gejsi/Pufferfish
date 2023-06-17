package io.gejsi.pufferfish.controllers;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class BackgroundIntentService extends IntentService {
  public static volatile boolean shouldContinue = true;

  public BackgroundIntentService() {
    super("BackgroundIntentService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    // check the condition
    if (!shouldContinue) {
      stopSelf();
      return;
    }

    new Thread(() -> {
      while (shouldContinue) {
        Log.d("Test", "onHandleIntent: ");

        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }).start();
  }
}