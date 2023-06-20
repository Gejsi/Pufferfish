package io.gejsi.pufferfish.utils;

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
}
