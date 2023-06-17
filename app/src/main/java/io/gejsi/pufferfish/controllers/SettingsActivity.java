package io.gejsi.pufferfish.controllers;

import android.os.Bundle;
import android.text.InputType;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;

import io.gejsi.pufferfish.R;

public class SettingsActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.settings_activity);
    if (savedInstanceState == null) {
      getSupportFragmentManager().beginTransaction().replace(R.id.settings, new SettingsFragment()).commit();
    }
  }

  public static class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
      setPreferencesFromResource(R.xml.root_preferences, rootKey);

      EditTextPreference averagePref = findPreference("average");
      if (averagePref != null)
        averagePref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));

      EditTextPreference timePref = findPreference("time");
      if (timePref != null)
        timePref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
    }
  }
}