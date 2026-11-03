package juloo.keyboard2;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SettingsActivity extends PreferenceActivity
{
  // Request code for file picker
  private static final int REQUEST_EXPORT = 1001;
  private static final int REQUEST_IMPORT = 1002;

  private SharedPreferences sharedPreferences;
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    this.sharedPreferences = getPreferenceManager().getSharedPreferences();
    // The preferences can't be read when in direct-boot mode. Avoid crashing
    // and don't allow changing the settings.
    // Run the config migration on this prefs as it might be different from the
    // one used by the keyboard, which have been migrated.
    try
    {
      Config.migrate(sharedPreferences);
    }
    catch (Exception _e) { fallbackEncrypted(); return; }
    addPreferencesFromResource(R.xml.settings);

    final Preference importDataPreference = findPreference("settings_import");
    importDataPreference.setOnPreferenceClickListener((Preference p) -> {
      Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
      intent.addCategory(Intent.CATEGORY_OPENABLE);
      intent.setType("text/*");
      this.startActivityForResult(Intent.createChooser(intent, getString(R.string.pref_settings_import)), REQUEST_IMPORT);

      return true;
    });

    final Preference exportDataPreference = findPreference("settings_export");
    exportDataPreference.setOnPreferenceClickListener((final Preference p) -> {
      Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
      intent.addCategory(Intent.CATEGORY_OPENABLE);
      intent.setType("text/plain");
      intent.putExtra(Intent.EXTRA_TITLE, "unexpected-keyboard-prefs.txt");
      this.startActivityForResult(Intent.createChooser(intent, getString(R.string.pref_settings_export)), REQUEST_EXPORT);

      return true;
    });

    boolean foldableDevice = FoldStateTracker.isFoldableDevice(this);
    findPreference("margin_bottom_portrait_unfolded").setEnabled(foldableDevice);
    findPreference("margin_bottom_landscape_unfolded").setEnabled(foldableDevice);
    findPreference("horizontal_margin_portrait_unfolded").setEnabled(foldableDevice);
    findPreference("horizontal_margin_landscape_unfolded").setEnabled(foldableDevice);
    findPreference("keyboard_height_unfolded").setEnabled(foldableDevice);
    findPreference("keyboard_height_landscape_unfolded").setEnabled(foldableDevice);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (resultCode != RESULT_OK || data == null) {
      return;
    }

    Uri uri = data.getData();
    if (uri != null) {
      if (requestCode == REQUEST_IMPORT) {
        importFromFile(uri);
      }
      if (requestCode == REQUEST_EXPORT) {
        exportToFile(uri);
      }
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
          sharedPreferences);
    super.onStop();
  }

  private void exportToFile(Uri uri) {
    try (OutputStream stream = this.getContentResolver().openOutputStream(uri);
         OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
      Map<String, ?> allPrefs = sharedPreferences.getAll();
      JSONArray json = new JSONArray();
      for (String key : allPrefs.keySet()) {
        Object value = allPrefs.get(key);
        if (value == null) continue;
        String valueType = value.getClass().getSimpleName();
        JSONObject entry = new JSONObject();
        entry.put("key", key);
        entry.put("type", valueType);
        entry.put("value", value.toString());
        json.put(entry);
      }
      writer.write(json.toString(2));

    } catch (Exception e) {
      Log.e("Settings", "Error exporting prefs", e);
      post_toast(R.string.export_import_fail);
    }
  }
  private void importFromFile(Uri uri) {
    try (InputStream inputStream = this.getContentResolver().openInputStream(uri);
         BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

      // Clear all existing preferences
      SharedPreferences.Editor editor = sharedPreferences.edit();

      StringBuilder fileContent = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        fileContent.append(line);
      }

      JSONArray json = new JSONArray(fileContent.toString());
      for (int i = 0; i < json.length(); i++) {
        JSONObject entry = json.getJSONObject(i);
        String key = entry.getString("key");
        String type = entry.getString("type");
        String value = entry.getString("value");
        switch (type) {
          case "Integer":
            editor.putInt(key, Integer.parseInt(value));
            break;
          case "Float":
            editor.putFloat(key, Float.parseFloat(value));
            break;
          case "Boolean":
            editor.putBoolean(key, Boolean.parseBoolean(value));
            break;
          case "String":
            editor.putString(key, value);
            break;
        }
      }
      editor.apply();

      // Refresh UI
      onCreate(null);
    } catch (Exception e) {
      Log.e("Settings", "Error importing prefs", e);
      post_toast(R.string.export_import_fail);
    }
  }

  private void post_toast(int msg_id)
  {
    Toast.makeText(this, msg_id, Toast.LENGTH_SHORT).show();
  }
}
