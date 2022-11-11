package juloo.keyboard2;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity
{
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    detectSystemTheme();
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.settings);
    PreferenceManager.getDefaultSharedPreferences(this)
      .registerOnSharedPreferenceChangeListener(this.new OnPreferencesChange());
  }

  /** The default theme is [Theme.DeviceDefault], which is dark. Detect if the
      system is using light theme. */
  void detectSystemTheme()
  {
    if (Build.VERSION.SDK_INT >= 14)
    {
      int ui_mode = getResources().getConfiguration().uiMode;
      if ((ui_mode & Configuration.UI_MODE_NIGHT_NO) != 0)
        setTheme(android.R.style.Theme_DeviceDefault_Light);
    }
  }

  /** See DirectBootAwarePreferences. */
  class OnPreferencesChange implements SharedPreferences.OnSharedPreferenceChangeListener
  {
    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String _key)
    {
      DirectBootAwarePreferences
        .copy_preferences_to_protected_storage(SettingsActivity.this, prefs);
    }
  }
}
