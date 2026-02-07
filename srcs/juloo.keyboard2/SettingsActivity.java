package juloo.keyboard2;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.json.JSONException;

public class SettingsActivity extends PreferenceActivity
{
  private static final int REQUEST_CODE_BACKUP = 101;
  private static final int REQUEST_CODE_RESTORE = 102;

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
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

    boolean foldableDevice = FoldStateTracker.isFoldableDevice(this);
    findPreference("margin_bottom_portrait_unfolded").setEnabled(foldableDevice);
    findPreference("margin_bottom_landscape_unfolded").setEnabled(foldableDevice);
    findPreference("horizontal_margin_portrait_unfolded").setEnabled(foldableDevice);
    findPreference("horizontal_margin_landscape_unfolded").setEnabled(foldableDevice);
    findPreference("keyboard_height_unfolded").setEnabled(foldableDevice);
    findPreference("keyboard_height_landscape_unfolded").setEnabled(foldableDevice);
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

  @Override
  public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference)
  {
    String key = preference.getKey();
    if ("backup".equals(key))
    {
      Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
      intent.addCategory(Intent.CATEGORY_OPENABLE);
      intent.setType("application/zip");
      String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date());
      intent.putExtra(Intent.EXTRA_TITLE, "unexpected_keyboard_backup_" + timeStamp + ".zip");
      startActivityForResult(intent, REQUEST_CODE_BACKUP);
      return true;
    }
    else if ("restore".equals(key))
    {
      Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
      intent.addCategory(Intent.CATEGORY_OPENABLE);
      intent.setType("application/zip");
      startActivityForResult(intent, REQUEST_CODE_RESTORE);
      return true;
    }
    return super.onPreferenceTreeClick(preferenceScreen, preference);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == RESULT_OK && data != null && data.getData() != null)
    {
      Uri uri = data.getData();
      if (requestCode == REQUEST_CODE_BACKUP)
      {
        try
        {
          BackupManager.exportData(this, uri);
          Toast.makeText(this, R.string.backup_success, Toast.LENGTH_SHORT).show();
        }
        catch (Exception e)
        {
          e.printStackTrace();
          Toast.makeText(this, R.string.backup_failed, Toast.LENGTH_SHORT).show();
        }
      }
      else if (requestCode == REQUEST_CODE_RESTORE)
      {
        try
        {
          BackupManager.importData(this, uri, getPreferenceManager());
          Toast.makeText(this, R.string.restore_success, Toast.LENGTH_SHORT).show();
          recreate();
        }
        catch (Exception e)
        {
          e.printStackTrace();
          Toast.makeText(this, R.string.restore_failed, Toast.LENGTH_SHORT).show();
        }
      }
    }
  }
}
