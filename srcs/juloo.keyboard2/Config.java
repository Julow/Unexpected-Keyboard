package juloo.keyboard2;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.KeyEvent;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

final class Config
{
  // From resources
  public final float marginTop;
  public final float keyPadding;

  public final float labelTextSize;
  public final float sublabelTextSize;

  // From preferences
  public int layout; // Or '-1' for the system defaults
  public int programming_layout; // Or '-1' for none
  public float swipe_dist_px;
  public long longPressTimeout;
  public long longPressInterval;
  public float marginBottom;
  public float keyHeight;
  public float horizontalMargin;
  public float keyVerticalInterval;
  public float keyHorizontalInterval;
  public boolean preciseRepeat;
  public Set<KeyValue.Modifier> lockable_modifiers = new HashSet<KeyValue.Modifier>();
  public float characterSize; // Ratio
  public int accents; // Values are R.values.pref_accents_v_*
  public int theme; // Values are R.style.*

  // Dynamically set
  public boolean shouldOfferSwitchingToNextInputMethod;
  public boolean shouldOfferSwitchingToProgramming;
  public String actionLabel; // Might be 'null'
  public int actionId; // Meaningful only when 'actionLabel' isn't 'null'
  public boolean swapEnterActionKey; // Swap the "enter" and "action" keys
  public Set<KeyValue> extra_keys; // 'null' means all the keys

  public final IKeyEventHandler handler;

  private Config(Context context, IKeyEventHandler h)
  {
    Resources res = context.getResources();
    // static values
    marginTop = res.getDimension(R.dimen.margin_top);
    keyPadding = res.getDimension(R.dimen.key_padding);
    labelTextSize = 0.33f;
    sublabelTextSize = 0.22f;
    // default values
    layout = -1;
    programming_layout = -1;
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
    refresh(context);
    // initialized later
    shouldOfferSwitchingToNextInputMethod = false;
    shouldOfferSwitchingToProgramming = false;
    actionLabel = null;
    actionId = 0;
    swapEnterActionKey = false;
    extra_keys = null;
    handler = h;
  }

  /*
   ** Reload prefs
   */
  public void refresh(Context context)
  {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    Resources res = context.getResources();
    DisplayMetrics dm = res.getDisplayMetrics();
    // The height of the keyboard is relative to the height of the screen.
    // This is the height of the keyboard if it have 4 rows.
    int keyboardHeightPercent;
    // Scale some dimensions depending on orientation
    float horizontalIntervalScale = 1.f;
    float characterSizeScale = 1.f;
    if (res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) // Landscape mode
    {
      keyboardHeightPercent = prefs.getInt("keyboard_height_landscape", 50);
      horizontalIntervalScale = 2.f;
      characterSizeScale = 1.25f;
    }
    else
    {
      keyboardHeightPercent = prefs.getInt("keyboard_height", 35);
    }
    String layout_s = prefs.getString("layout", "system");
    layout = layout_s.equals("system") ? -1 : layoutId_of_string(layout_s);
    String prog_layout_s = prefs.getString("programming_layout", "none");
    programming_layout = prog_layout_s.equals("none") ? -1 : layoutId_of_string(prog_layout_s);
    // The swipe distance is defined relatively to the "exact physical pixels
    // per inch of the screen", which isn't affected by the scaling settings.
    // Take the mean of both dimensions as an approximation of the diagonal.
    float physical_scaling = (dm.widthPixels + dm.heightPixels) / (dm.xdpi + dm.ydpi);
    swipe_dist_px = Float.valueOf(prefs.getString("swipe_dist", "15")) * physical_scaling;;
    longPressTimeout = prefs.getInt("longpress_timeout", (int)longPressTimeout);
    longPressInterval = prefs.getInt("longpress_interval", (int)longPressInterval);
    marginBottom = getDipPref(dm, prefs, "margin_bottom", marginBottom);
    keyVerticalInterval = getDipPref(dm, prefs, "key_vertical_space", keyVerticalInterval);
    keyHorizontalInterval =
      getDipPref(dm, prefs, "key_horizontal_space", keyHorizontalInterval)
      * horizontalIntervalScale;
    // Do not substract keyVerticalInterval from keyHeight because this is done
    // during rendered.
    keyHeight = dm.heightPixels * keyboardHeightPercent / 100 / 4;
    horizontalMargin =
      getDipPref(dm, prefs, "horizontal_margin", horizontalMargin)
      + res.getDimension(R.dimen.extra_horizontal_margin);
    preciseRepeat = prefs.getBoolean("precise_repeat", preciseRepeat);
    lockable_modifiers.clear();
    if (prefs.getBoolean("lockable_shift", true)) lockable_modifiers.add(KeyValue.Modifier.SHIFT);
    if (prefs.getBoolean("lockable_ctrl", false)) lockable_modifiers.add(KeyValue.Modifier.CTRL);
    if (prefs.getBoolean("lockable_alt", false)) lockable_modifiers.add(KeyValue.Modifier.ALT);
    if (prefs.getBoolean("lockable_fn", false)) lockable_modifiers.add(KeyValue.Modifier.FN);
    if (prefs.getBoolean("lockable_meta", false)) lockable_modifiers.add(KeyValue.Modifier.META);
    if (prefs.getBoolean("lockable_sup", false)) lockable_modifiers.add(KeyValue.Modifier.SUPERSCRIPT);
    if (prefs.getBoolean("lockable_sub", false)) lockable_modifiers.add(KeyValue.Modifier.SUBSCRIPT);
    if (prefs.getBoolean("lockable_box", false)) lockable_modifiers.add(KeyValue.Modifier.BOX);
    characterSize =
      prefs.getFloat("character_size", characterSize)
      * characterSizeScale;
    accents = Integer.valueOf(prefs.getString("accents", "1"));
    theme = getThemeId(res, prefs.getString("theme", ""));
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
    final Set<KeyValue> extra_keys = new HashSet<KeyValue>(this.extra_keys);
    KeyboardData kw = original_kw.mapKeys(new KeyboardData.MapKeyValues() {
      public KeyValue apply(KeyValue key)
      {
        if (key == null)
          return null;
        boolean is_extra_key = extra_keys.contains(key);
        if (is_extra_key)
          extra_keys.remove(key);
        int flags = key.getFlags();
        if ((flags & KeyValue.FLAG_LOCALIZED) != 0 && !is_extra_key)
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
              case SWITCH_PROGRAMMING:
                return shouldOfferSwitchingToProgramming ? key : null;
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
            if (lockable_modifiers.contains(key.getModifier()))
              return key.withFlags(flags | KeyValue.FLAG_LOCK);
            break;
        }
        return key;
      }
    });
    if (extra_keys.size() > 0)
      kw = kw.addExtraKeys(extra_keys.iterator());
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

  public static int layoutId_of_string(String name)
  {
    switch (name)
    {
      case "azerty": return R.xml.azerty;
      case "bgph1": return R.xml.local_bgph1;
      case "colemak": return R.xml.colemak;
      case "dvorak": return R.xml.dvorak;
      case "neo2": return R.xml.neo2;
      case "qwerty_es": return R.xml.qwerty_es;
      case "qwerty_hu": return R.xml.qwerty_hu;
      case "qwerty_ko": return R.xml.qwerty_ko;
      case "qwerty_lv": return R.xml.qwerty_lv;
      case "qwerty_pt": return R.xml.qwerty_pt;
      case "qwerty_tr": return R.xml.qwerty_tr;
      case "qwerty": return R.xml.qwerty;
      case "qwerty_sv_se": return R.xml.qwerty_sv_se;
      case "qwertz_hu": return R.xml.qwertz_hu;
      case "qwertz": return R.xml.qwertz;
      case "ru_jcuken": return R.xml.local_ru_jcuken;
      default: return R.xml.qwerty; // The config might store an invalid layout, don't crash
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
    public void handleKeyUp(KeyValue value, Pointers.Modifiers flags);
  }
}
