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
    // The preferences can't be read when in direct-boot mode. Avoid crashing
    // and don't allow changing the settings.
    // Run the config migration on this prefs as it might be different from the
    // one used by the keyboard, which have been migrated.
    try
    {
      Config.migrate(getPreferenceManager().getSharedPreferences());
    }
    catch (Exception _e) { fallbackEncrypted(); return; }
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
 
if ( Build.VERSION.SDK_INT >= 21 )
										{
												int ui_mode = getResources ( ).getConfiguration ( ).uiMode;
												if ( ( ui_mode & Configuration.UI_MODE_NIGHT_NO ) != 0 )
														{setTheme ( R.style.appTheme );}
												else
														{setTheme ( R.style.appTheme_Night );}
										}
						}

  void fallbackEncrypted()
  {
    // Can't communicate with the user here.
    finish();
  }

  protected void onStop()
  {
    DirectBootAwarePreferences
      .copy_preferences_to_protected_storage(this,
          getPreferenceManager().getSharedPreferences());
    super.onStop();
  }
}
