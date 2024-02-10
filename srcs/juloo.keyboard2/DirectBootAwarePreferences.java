package juloo.keyboard2;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build.VERSION;
import android.preference.PreferenceManager;
import java.util.Map;
import java.util.Set;

@TargetApi(24)
public final class DirectBootAwarePreferences
{
  /* On API >= 24, preferences are read from the device protected storage. This
   * storage is less protected than the default, no personnal or sensitive
   * information is stored there (only the keyboard settings). This storage is
   * accessible during boot and allow the keyboard to read its settings and
   * allow typing the storage password. */
  public static SharedPreferences get_shared_preferences(Context context)
  {
    if (VERSION.SDK_INT < 24)
      return PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences prefs = get_protected_prefs(context);
    check_need_migration(context, prefs);
    return prefs;
  }

  /* Copy shared preferences to device protected storage. Not using
   * [Context.moveSharedPreferencesFrom] because the settings activity still
   * use [PreferenceActivity], which can't work on a non-default shared
   * preference file. */
  public static void copy_preferences_to_protected_storage(Context context,
      SharedPreferences src)
  {
    if (VERSION.SDK_INT >= 24)
      copy_shared_preferences(src, get_protected_prefs(context));
  }

  static SharedPreferences get_protected_prefs(Context context)
  {
    String pref_name =
      PreferenceManager.getDefaultSharedPreferencesName(context);
    return context.createDeviceProtectedStorageContext()
      .getSharedPreferences(pref_name, Context.MODE_PRIVATE);
  }

  static void check_need_migration(Context app_context,
      SharedPreferences protected_prefs)
  {
    if (!protected_prefs.getBoolean("need_migration", true))
      return;
    SharedPreferences prefs;
    try
    {
      prefs = PreferenceManager.getDefaultSharedPreferences(app_context);
    }
    catch (Exception e)
    {
      // Device is locked, migrate later.
      return;
    }
    prefs.edit().putBoolean("need_migration", false).commit();
    copy_shared_preferences(prefs, protected_prefs);
  }

  static void copy_shared_preferences(SharedPreferences src, SharedPreferences dst)
  {
    SharedPreferences.Editor e = dst.edit();
    Map<String, ?> entries = src.getAll();
    for (String k : entries.keySet())
    {
      Object v = entries.get(k);
      if (v instanceof Boolean)
        e.putBoolean(k, (Boolean)v);
      else if (v instanceof Float)
        e.putFloat(k, (Float)v);
      else if (v instanceof Integer)
        e.putInt(k, (Integer)v);
      else if (v instanceof Long)
        e.putLong(k, (Long)v);
      else if (v instanceof String)
        e.putString(k, (String)v);
      else if (v instanceof Set)
        e.putStringSet(k, (Set<String>)v);
    }
    e.commit();
  }
}
