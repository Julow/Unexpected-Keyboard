package juloo.keyboard2;

import android.content.Context;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.TypedValue;

final class Config
{
  // From resources
  public final float marginTop;
  public final float keyPadding;
  public final float keyVerticalInterval;
  public final float keyHorizontalInterval;

  // From preferences
  public int layout; // Or '-1' for the system defaults
  public float subValueDist;
  public boolean vibrateEnabled;
  public long vibrateDuration;
  public long longPressTimeout;
  public long longPressInterval;
  public float marginBottom;
  public float keyHeight;
  public float horizontalMargin;
  public boolean preciseRepeat;
  public float characterSize; // Ratio
  public int accents; // Values are R.values.pref_accents_v_*
  public int theme; // Values are R.style.*

  // Dynamically set
  public boolean shouldOfferSwitchingToNextInputMethod;
  public int accent_flags_to_remove;

  public final IKeyEventHandler handler;

  private Config(Context context, IKeyEventHandler h)
  {
    Resources res = context.getResources();
    // static values
    marginTop = res.getDimension(R.dimen.margin_top);
    keyPadding = res.getDimension(R.dimen.key_padding);
    keyVerticalInterval = res.getDimension(R.dimen.key_vertical_interval);
    keyHorizontalInterval = res.getDimension(R.dimen.key_horizontal_interval);
    // default values
    layout = -1;
    subValueDist = 10f;
    vibrateEnabled = true;
    vibrateDuration = 20;
    longPressTimeout = 600;
    longPressInterval = 65;
    marginBottom = res.getDimension(R.dimen.margin_bottom);
    keyHeight = res.getDimension(R.dimen.key_height);
    horizontalMargin = res.getDimension(R.dimen.horizontal_margin);
    preciseRepeat = true;
    characterSize = 1.f;
    accents = 1;
    // from prefs
    refresh(context);
    // initialized later
    shouldOfferSwitchingToNextInputMethod = false;
    accent_flags_to_remove = 0;
    handler = h;
  }

  /*
   ** Reload prefs
   */
  public void refresh(Context context)
  {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    DisplayMetrics dm = context.getResources().getDisplayMetrics();
    layout = layoutId_of_string(prefs.getString("layout", "system")); 
    subValueDist = getDipPrefFloat(dm, prefs, "sub_value_dist", subValueDist);
    vibrateEnabled = prefs.getBoolean("vibrate_enabled", vibrateEnabled);
    vibrateDuration = prefs.getInt("vibrate_duration", (int)vibrateDuration);
    longPressTimeout = prefs.getInt("longpress_timeout", (int)longPressTimeout);
    longPressInterval = prefs.getInt("longpress_interval", (int)longPressInterval);
    marginBottom = getDipPrefInt(dm, prefs, "margin_bottom", marginBottom);
    keyHeight = getDipPrefInt(dm, prefs, "key_height", keyHeight);
    horizontalMargin = getDipPrefInt(dm, prefs, "horizontal_margin", horizontalMargin);
    preciseRepeat = prefs.getBoolean("precise_repeat", preciseRepeat);
    characterSize = prefs.getFloat("character_size", characterSize); 
    accents = Integer.valueOf(prefs.getString("accents", "1"));
    theme = themeId_of_string(prefs.getString("theme", ""));
  }

  private float getDipPrefInt(DisplayMetrics dm, SharedPreferences prefs, String pref_name, float def)
  {
    int value = prefs.getInt(pref_name, -1);
    if (value < 0)
      return (def);
    return (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, dm));
  }

  private float getDipPrefFloat(DisplayMetrics dm, SharedPreferences prefs, String pref_name, float def)
  {
    float value = prefs.getFloat(pref_name, -1.f);
    if (value < 0.f)
      return (def);
    return (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, dm));
  }

  public static int layoutId_of_string(String name)
  {
    switch (name)
    {
      case "azerty": return R.xml.azerty;
      case "qwerty": return R.xml.qwerty;
      case "system": default: return -1;
    }
  }

  /* Used for the accents option. */
  public static int accentFlag_of_name(String name)
  {
    switch (name)
    {
      case "grave": return KeyValue.FLAG_ACCENT1;
      case "aigu": return KeyValue.FLAG_ACCENT2;
      case "circonflexe": return KeyValue.FLAG_ACCENT3;
      case "tilde": return KeyValue.FLAG_ACCENT4;
      case "cedille": return KeyValue.FLAG_ACCENT5;
      case "trema": return KeyValue.FLAG_ACCENT6;
      case "ring": return KeyValue.FLAG_ACCENT_RING;
      default: throw new RuntimeException(name);
    }
  }

  public static int themeId_of_string(String name)
  {
    switch (name)
    {
      case "light": return R.style.Light;
      case "black": return R.style.Black;
      default: case "dark": return R.style.Dark;
    }
  }

  private static Config _globalConfig = null;

  public static void initGlobalConfig(Context context, IKeyEventHandler handler)
  {
    _globalConfig = new Config(context, handler);
  }

  public static Config globalConfig()
  {
    return _globalConfig;
  }

  public static interface IKeyEventHandler
  {
    public void handleKeyUp(KeyValue value, int flags);
  }
}
