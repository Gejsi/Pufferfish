package io.gejsi.pufferfish.handlers;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class SettingsUtils {
  public static int getAverageLengthPreference(Context context) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    String averagePref = sharedPreferences.getString("average", "");
    return averagePref.length() == 0 ? 10 : Integer.parseInt(averagePref);
  }

  public static int getTimePreference(Context context) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    String timePref = sharedPreferences.getString("time", "");
    int time = timePref.length() == 0 ? 5 : Integer.parseInt(timePref);

    return time * 1000;
  }

  public static int getBackgroundTimePreference(Context context) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    String backgroundTimePref = sharedPreferences.getString("background", "");
    int time = backgroundTimePref.length() == 0 ? 2 : Integer.parseInt(backgroundTimePref);

    return time * 1000;
  }
}
