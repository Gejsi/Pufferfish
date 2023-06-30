package io.gejsi.pufferfish.utils;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import io.gejsi.pufferfish.R;

public class NotificationUtils {
  private final Context context;

  private NotificationCompat.Builder builder;

  public NotificationUtils(Context context) {
    this.context = context;
  }

  public void createNotificationChannel() {
    String CHANNEL_ID = "bg";
    // create the NotificationChannel, but only on API 26+ because
    // the NotificationChannel class is new and not in the support library
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      CharSequence name = "Background measurements";
      int importance = NotificationManager.IMPORTANCE_DEFAULT;
      NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
      NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
      notificationManager.createNotificationChannel(channel);
    }

    builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_record)
            .setContentTitle("New tile visited")
            .setStyle(new NotificationCompat.BigTextStyle()
                    .bigText("A tile has been visited for the first time and a new recording has been saved."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true);
  }

  @SuppressLint("MissingPermission")
  public void sendNotification() {
    // id is set as 0 to avoid spamming different notifications,
    // because they all display the same content.
    NotificationManagerCompat.from(context).notify(0, builder.build());
  }
}
