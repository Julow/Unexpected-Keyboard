package juloo.keyboard2;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity
{
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    detectSystemTheme();
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.settings);
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
}
