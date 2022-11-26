package juloo.keyboard2;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.KeyEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

final class Config
{
  private final SharedPreferences _prefs;

  // From resources
  public final float marginTop;
  public final float keyPadding;

  public final float labelTextSize;
  public final float sublabelTextSize;

  // From preferences
  public KeyboardData layout; // Or 'null' for the system defaults
  public KeyboardData second_layout; // Or 'null' for none
  public KeyboardData custom_layout; // Might be 'null'
  public boolean show_numpad = false;
  public float swipe_dist_px;
  public boolean vibrateEnabled;
  public long longPressTimeout;
  public long longPressInterval;
  public float marginBottom;
  public float keyHeight;
  public float horizontalMargin;
  public float keyVerticalInterval;
  public float keyHorizontalInterval;
  public int labelBrightness; // 0 - 255
  public boolean preciseRepeat;
  public boolean double_tap_lock_shift;
  public float characterSize; // Ratio
  public int accents; // Values are R.values.pref_accents_v_*
  public int theme; // Values are R.style.*
  public boolean autocapitalisation;

  // Dynamically set
  public boolean shouldOfferSwitchingToNextInputMethod;
  public boolean shouldOfferSwitchingToSecond;
  public String actionLabel; // Might be 'null'
  public int actionId; // Meaningful only when 'actionLabel' isn't 'null'
  public boolean swapEnterActionKey; // Swap the "enter" and "action" keys
  public Set<KeyValue> extra_keys_subtype;
  public Set<KeyValue> extra_keys_param;

  public final IKeyEventHandler handler;

  private Config(SharedPreferences prefs, Resources res, IKeyEventHandler h)
  {
    _prefs = prefs;
    // static values
    marginTop = res.getDimension(R.dimen.margin_top);
    keyPadding = res.getDimension(R.dimen.key_padding);
    labelTextSize = 0.33f;
    sublabelTextSize = 0.22f;
    // default values
    layout = null;
    second_layout = null;
    custom_layout = null;
    vibrateEnabled = true;
    longPressTimeout = 600;
    longPressInterval = 65;
    marginBottom = res.getDimension(R.dimen.margin_bottom);
    keyHeight = res.getDimension(R.dimen.key_height);
    horizontalMargin = res.getDimension(R.dimen.horizontal_margin);
    keyVerticalInterval = res.getDimension(R.dimen.key_vertical_interval);
    keyHorizontalInterval = res.getDimension(R.dimen.key_horizontal_interval);
    preciseRepeat = true;
    characterSize = 1.f;
    accents = 1;
    // from prefs
    refresh(res);
    // initialized later
    shouldOfferSwitchingToNextInputMethod = false;
    shouldOfferSwitchingToSecond = false;
    actionLabel = null;
    actionId = 0;
    swapEnterActionKey = false;
    extra_keys_subtype = null;
    handler = h;
  }

  /*
   ** Reload prefs
   */
  public void refresh(Resources res)
  {
    DisplayMetrics dm = res.getDisplayMetrics();
    // The height of the keyboard is relative to the height of the screen.
    // This is the height of the keyboard if it have 4 rows.
    int keyboardHeightPercent;
    // Scale some dimensions depending on orientation
    float horizontalIntervalScale = 1.f;
    float characterSizeScale = 1.f;
    String show_numpad_s = _prefs.getString("show_numpad", "never");
    show_numpad = "always".equals(show_numpad_s);
    if (res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) // Landscape mode
    {
      if ("landscape".equals(show_numpad_s))
        show_numpad = true;
      keyboardHeightPercent = _prefs.getInt("keyboard_height_landscape", 50);
      horizontalIntervalScale = 2.f;
      characterSizeScale = 1.25f;
    }
    else
    {
      keyboardHeightPercent = _prefs.getInt("keyboard_height", 35);
    }
    layout = layout_of_string(res, _prefs.getString("layout", "none"));
    second_layout = layout_of_string(res, _prefs.getString("second_layout", "none"));
    custom_layout = KeyboardData.load_string(_prefs.getString("custom_layout", ""));
    // The baseline for the swipe distance correspond to approximately the
    // width of a key in portrait mode, as most layouts have 10 columns.
    // Multipled by the DPI ratio because most swipes are made in the diagonals.
    // The option value uses an unnamed scale where the baseline is around 25.
    float dpi_ratio = Math.max(dm.xdpi, dm.ydpi) / Math.min(dm.xdpi, dm.ydpi);
    float swipe_scaling = Math.min(dm.widthPixels, dm.heightPixels) / 10.f * dpi_ratio;
    float swipe_dist_value = Float.valueOf(_prefs.getString("swipe_dist", "15"));
    swipe_dist_px = swipe_dist_value / 25.f * swipe_scaling;
    vibrateEnabled = _prefs.getBoolean("vibrate_enabled", vibrateEnabled);
    longPressTimeout = _prefs.getInt("longpress_timeout", (int)longPressTimeout);
    longPressInterval = _prefs.getInt("longpress_interval", (int)longPressInterval);
    marginBottom = getDipPref(dm, _prefs, "margin_bottom", marginBottom);
    keyVerticalInterval = getDipPref(dm, _prefs, "key_vertical_space", keyVerticalInterval);
    keyHorizontalInterval =
      getDipPref(dm, _prefs, "key_horizontal_space", keyHorizontalInterval)
      * horizontalIntervalScale;
    // Label brightness is used as the alpha channel
    labelBrightness = _prefs.getInt("label_brightness", 100) * 255 / 100;
    // Do not substract keyVerticalInterval from keyHeight because this is done
    // during rendered.
    keyHeight = dm.heightPixels * keyboardHeightPercent / 100 / 4;
    horizontalMargin =
      getDipPref(dm, _prefs, "horizontal_margin", horizontalMargin)
      + res.getDimension(R.dimen.extra_horizontal_margin);
    preciseRepeat = _prefs.getBoolean("precise_repeat", preciseRepeat);
    double_tap_lock_shift = _prefs.getBoolean("lock_double_tap", false);
    characterSize =
      _prefs.getFloat("character_size", characterSize)
      * characterSizeScale;
    accents = Integer.valueOf(_prefs.getString("accents", "1"));
    theme = getThemeId(res, _prefs.getString("theme", ""));
    autocapitalisation = _prefs.getBoolean("autocapitalisation", true);
    extra_keys_param = ExtraKeyCheckBoxPreference.get_extra_keys(_prefs);
  }

  /** Update the layout according to the configuration.
   *  - Remove the switching key if it isn't needed
   *  - Remove "localized" keys from other locales (not in 'extra_keys')
   *  - Replace the action key to show the right label
   *  - Swap the enter and action keys
   */
  public KeyboardData modify_layout(KeyboardData original_kw)
  {
    // Update the name to avoid caching in KeyModifier
    final KeyValue action_key = (actionLabel == null) ? null :
      KeyValue.getKeyByName("action").withSymbol(actionLabel);
    // Extra keys are removed from the set as they are encountered during the
    // first iteration then automatically added.
    final Set<KeyValue> extra_keys = new HashSet<KeyValue>();
    extra_keys.addAll(extra_keys_subtype);
    extra_keys.addAll(extra_keys_param);
    KeyboardData kw = original_kw.mapKeys(new KeyboardData.MapKeyValues() {
      public KeyValue apply(KeyValue key, boolean localized)
      {
        boolean is_extra_key = extra_keys.contains(key);
        if (is_extra_key)
          extra_keys.remove(key);
        if (localized && !is_extra_key)
          return null;
        switch (key.getKind())
        {
          case Event:
            switch (key.getEvent())
            {
              case CHANGE_METHOD:
                return shouldOfferSwitchingToNextInputMethod ? key : null;
              case ACTION:
                return (swapEnterActionKey && action_key != null) ?
                  KeyValue.getKeyByName("enter") : action_key;
              case SWITCH_SECOND:
                return shouldOfferSwitchingToSecond ? key : null;
            }
            break;
          case Keyevent:
            switch (key.getKeyevent())
            {
              case KeyEvent.KEYCODE_ENTER:
                return (swapEnterActionKey && action_key != null) ? action_key : key;
            }
            break;
          case Modifier:
            switch (key.getModifier())
            {
              case SHIFT:
                if (double_tap_lock_shift)
                  return key.withFlags(key.getFlags() | KeyValue.FLAG_LOCK);
            }
            break;
        }
        return key;
      }
    });
    if (extra_keys.size() > 0)
      kw = kw.addExtraKeys(extra_keys.iterator());
    if (original_kw.num_pad && show_numpad)
      kw = kw.addNumPad();
    return kw;
  }

  private float getDipPref(DisplayMetrics dm, SharedPreferences prefs, String pref_name, float def)
  {
    float value;
    try { value = prefs.getInt(pref_name, -1); }
    catch (Exception e) { value = prefs.getFloat(pref_name, -1f); }
    if (value < 0f)
      return (def);
    return (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, dm));
  }

  private int getThemeId(Resources res, String theme_name)
  {
    switch (theme_name)
    {
      case "light": return R.style.Light;
      case "black": return R.style.Black;
      case "dark": return R.style.Dark;
      case "white": return R.style.White;
      default:
      case "system":
        if (Build.VERSION.SDK_INT >= 8)
        {
          int night_mode = res.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
          if ((night_mode & Configuration.UI_MODE_NIGHT_NO) != 0)
            return R.style.Light;
        }
        return R.style.Dark;
    }
  }

  public KeyboardData layout_of_string(Resources res, String name)
  {
    int id = R.xml.qwerty; // The config might store an invalid layout, don't crash
    switch (name)
    {
      case "system": case "none": return null;
      case "custom": if (custom_layout != null) return custom_layout; break;
      case "azerty": id = R.xml.azerty; break;
      case "bangla": id = R.xml.bangla; break;
      case "bgph1": id = R.xml.local_bgph1; break;
      case "bone": id = R.xml.bone; break;
      case "colemak": id = R.xml.colemak; break;
      case "dvorak": id = R.xml.dvorak; break;
      case "hindi": id = R.xml.hindi; break;
      case "jcuken_ua": id = R.xml.jcuken_ua; break;
      case "neo2": id = R.xml.neo2; break;
      case "qwerty": id = R.xml.qwerty; break;
      case "qwerty_el": id = R.xml.qwerty_el; break;
      case "qwerty_es": id = R.xml.qwerty_es; break;
      case "qwerty_hu": id = R.xml.qwerty_hu; break;
      case "qwerty_ko": id = R.xml.qwerty_ko; break;
      case "qwerty_lv": id = R.xml.qwerty_lv; break;
      case "qwerty_no": id = R.xml.qwerty_no; break;
      case "qwerty_pt": id = R.xml.qwerty_pt; break;
      case "qwerty_sv_se": id = R.xml.qwerty_sv_se; break;
      case "qwerty_tr": id = R.xml.qwerty_tr; break;
      case "qwertz": id = R.xml.qwertz; break;
      case "qwertz_cs": id = R.xml.qwertz_cs; break;
      case "qwertz_de": id = R.xml.qwertz_de; break;
      case "qwertz_hu": id = R.xml.qwertz_hu; break;
      case "ru_jcuken": id = R.xml.local_ru_jcuken; break;
    }
    return KeyboardData.load(res, id);
  }

  private static Config _globalConfig = null;

  public static void initGlobalConfig(SharedPreferences prefs, Resources res,
      IKeyEventHandler handler)
  {
    _globalConfig = new Config(prefs, res, handler);
  }

  public static Config globalConfig()
  {
    return _globalConfig;
  }

  public static interface IKeyEventHandler
  {
    public void key_up(KeyValue value, Pointers.Modifiers flags);
  }
}
