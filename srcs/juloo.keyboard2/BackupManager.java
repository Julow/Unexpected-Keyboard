package juloo.keyboard2;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.json.JSONException;
import org.json.JSONObject;

public class BackupManager {
    private static final String TAG = "BackupManager";
    private static final String SETTINGS_FILE_NAME = "settings.json";
    private static final String SNIPPETS_FILE_NAME = "snippets.json";
    private static final String SNIPPETS_PREF_NAME = "pinned_clipboards";

    public static void exportData(Context context, Uri uri) throws IOException, JSONException {
        try (OutputStream os = context.getContentResolver().openOutputStream(uri);
                ZipOutputStream zos = new ZipOutputStream(os)) {

            // 1. Export Settings
            SharedPreferences settingsPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            JSONObject settingsJson = prefsToJson(settingsPrefs);
            addToZip(zos, SETTINGS_FILE_NAME, settingsJson.toString());

            // 2. Export Snippets
            SharedPreferences snippetsPrefs = context.getSharedPreferences(SNIPPETS_PREF_NAME, Context.MODE_PRIVATE);
            JSONObject snippetsJson = prefsToJson(snippetsPrefs);
            addToZip(zos, SNIPPETS_FILE_NAME, snippetsJson.toString());
        }
    }

    public static void importData(Context context, Uri uri, PreferenceManager prefManager)
            throws IOException, JSONException {
        try (InputStream is = context.getContentResolver().openInputStream(uri);
                ZipInputStream zis = new ZipInputStream(is)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Log.d(TAG, "Restoring entry: " + entry.getName());
                if (SETTINGS_FILE_NAME.equals(entry.getName())) {
                    String json = readZipEntry(zis);
                    restorePrefs(context, PreferenceManager.getDefaultSharedPreferences(context), new JSONObject(json),
                            prefManager);
                    // Reload config to apply changes
                    Config.globalConfig().refresh(context.getResources(), FoldStateTracker.isFoldableDevice(context));
                } else if (SNIPPETS_FILE_NAME.equals(entry.getName())) {
                    String json = readZipEntry(zis);
                    restorePrefs(context, context.getSharedPreferences(SNIPPETS_PREF_NAME, Context.MODE_PRIVATE),
                            new JSONObject(json), null);
                    // Reload snippets
                    SnippetManager.get(context).reload();
                }
            }
        }
    }

    private static JSONObject prefsToJson(SharedPreferences prefs) throws JSONException {
        JSONObject json = new JSONObject();
        Map<String, ?> allEntries = prefs.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            json.put(entry.getKey(), entry.getValue());
        }
        return json;
    }

    private static void addToZip(ZipOutputStream zos, String fileName, String content) throws IOException {
        ZipEntry entry = new ZipEntry(fileName);
        zos.putNextEntry(entry);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(zos));
        writer.write(content);
        writer.flush();
        // Do not close writer as it closes zos
        zos.closeEntry();
    }

    private static String readZipEntry(ZipInputStream zis) throws IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = zis.read(buffer)) > 0) {
            bos.write(buffer, 0, len);
        }
        return bos.toString("UTF-8");
    }

    private static void restorePrefs(Context context, SharedPreferences prefs, JSONObject json,
            PreferenceManager prefManager) throws JSONException {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();

        java.util.Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = json.get(key);

            // Try to deduce type from Preference definitions if available
            if (prefManager != null) {
                android.preference.Preference p = prefManager.findPreference(key);
                if (p instanceof juloo.keyboard2.prefs.SlideBarPreference) {
                    // Force Float
                    if (value instanceof Number) {
                        editor.putFloat(key, ((Number) value).floatValue());
                        continue;
                    }
                } else if (p instanceof juloo.keyboard2.prefs.IntSlideBarPreference) {
                    // Force Int
                    if (value instanceof Number) {
                        editor.putInt(key, ((Number) value).intValue());
                        continue;
                    }
                }
            }

            // Fallback to JSON type guessing
            if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
            } else if (value instanceof Float) {
                editor.putFloat(key, ((Number) value).floatValue());
            } else if (value instanceof Integer) {
                // FAST FAIL SAFEGUARD: specific known float keys
                if ("character_size".equals(key) || "key_vertical_margin".equals(key)
                        || "key_horizontal_margin".equals(key)) {
                    Log.w(TAG, "Restoring key " + key + " as Float (forced override from Integer)");
                    editor.putFloat(key, ((Number) value).floatValue());
                } else {
                    editor.putInt(key, (Integer) value);
                }
            } else if (value instanceof Long) {
                editor.putLong(key, (Long) value);
            } else if (value instanceof String) {
                editor.putString(key, (String) value);
            } else if (value instanceof Double) {
                editor.putFloat(key, ((Double) value).floatValue());
            } else {
                Log.w(TAG, "Skipping unknown type for key: " + key + ", value: " + value);
            }
        }
        editor.commit();
    }
}
