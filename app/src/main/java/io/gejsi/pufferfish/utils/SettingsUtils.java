package io.gejsi.pufferfish.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import mil.nga.mgrs.grid.GridType;

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

  public static boolean getNotificationsPreference(Context context) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    return sharedPreferences.getBoolean("notifications", false);
  }

  public static GridType getGridPreference(Context context) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    String pref = sharedPreferences.getString("grid_type", GridType.TEN_METER.toString());

    if (pref.equals("one")) {
      return GridType.METER;
    } else if (pref.equals("hundred")) {
      return GridType.HUNDRED_METER;
    }

    return GridType.TEN_METER;
  }
}
