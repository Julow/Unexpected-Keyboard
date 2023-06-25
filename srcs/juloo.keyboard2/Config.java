package juloo.keyboard2;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.KeyEvent;
import java.util.HashSet;
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
  // From the 'numpad_layout' option, also apply to the numeric pane.
  public boolean inverse_numpad = false;
  public boolean number_row;
  public float swipe_dist_px;
  public float slide_step_px;
  public VibratorCompat.VibrationBehavior vibration_behavior;
  public long longPressTimeout;
  public long longPressInterval;
  public float margin_bottom;
  public float keyHeight;
  public float horizontal_margin;
  public float keyVerticalInterval;
  public float keyHorizontalInterval;
  public int labelBrightness; // 0 - 255
  public int keyboardOpacity; // 0 - 255
  public int keyOpacity; // 0 - 255
  public int keyActivatedOpacity; // 0 - 255
  public boolean double_tap_lock_shift;
  public float characterSize; // Ratio
  public int accents; // Values are R.values.pref_accents_v_*
  public int theme; // Values are R.style.*
  public boolean autocapitalisation;
  public boolean switch_input_immediate;

  // Dynamically set
  public boolean shouldOfferSwitchingToNextInputMethod;
  public boolean shouldOfferSwitchingToSecond;
  public boolean shouldOfferVoiceTyping;
  public String actionLabel; // Might be 'null'
  public int actionId; // Meaningful only when 'actionLabel' isn't 'null'
  public boolean swapEnterActionKey; // Swap the "enter" and "action" keys
  public ExtraKeys extra_keys_subtype;
  public Set<KeyValue> extra_keys_param;

  public final IKeyEventHandler handler;
  public boolean orientation_landscape = false;

  private Config(SharedPreferences prefs, Resources res, IKeyEventHandler h)
  {
    _prefs = prefs;
    // static values
    marginTop = res.getDimension(R.dimen.margin_top);
    keyPadding = res.getDimension(R.dimen.key_padding);
    labelTextSize = 0.33f;
    sublabelTextSize = 0.22f;
    // from prefs
    refresh(res);
    // initialized later
    shouldOfferSwitchingToNextInputMethod = false;
    shouldOfferSwitchingToSecond = false;
    shouldOfferVoiceTyping = false;
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
    orientation_landscape = res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    // The height of the keyboard is relative to the height of the screen.
    // This is the height of the keyboard if it have 4 rows.
    int keyboardHeightPercent;
    // Scale some dimensions depending on orientation
    float horizontalIntervalScale = 1.f;
    float characterSizeScale = 1.f;
    String show_numpad_s = _prefs.getString("show_numpad", "never");
    show_numpad = "always".equals(show_numpad_s);
    if (orientation_landscape)
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
    second_layout = tweak_secondary_layout(layout_of_string(res, _prefs.getString("second_layout", "none")));
    custom_layout = KeyboardData.load_string(_prefs.getString("custom_layout", ""));
    inverse_numpad = _prefs.getString("numpad_layout", "default").equals("low_first");
    number_row = _prefs.getBoolean("number_row", false);
    // The baseline for the swipe distance correspond to approximately the
    // width of a key in portrait mode, as most layouts have 10 columns.
    // Multipled by the DPI ratio because most swipes are made in the diagonals.
    // The option value uses an unnamed scale where the baseline is around 25.
    float dpi_ratio = Math.max(dm.xdpi, dm.ydpi) / Math.min(dm.xdpi, dm.ydpi);
    float swipe_scaling = Math.min(dm.widthPixels, dm.heightPixels) / 10.f * dpi_ratio;
    float swipe_dist_value = Float.valueOf(_prefs.getString("swipe_dist", "15"));
    swipe_dist_px = swipe_dist_value / 25.f * swipe_scaling;
    slide_step_px = swipe_dist_px / 4.f;
    vibration_behavior =
      VibratorCompat.VibrationBehavior.of_string(_prefs.getString("vibration_behavior", "system"));
    longPressTimeout = _prefs.getInt("longpress_timeout", 600);
    longPressInterval = _prefs.getInt("longpress_interval", 65);
    margin_bottom = get_dip_pref_oriented(dm, "margin_bottom", 7, 3);
    keyVerticalInterval = get_dip_pref(dm, "key_vertical_space", 2);
    keyHorizontalInterval = get_dip_pref(dm, "key_horizontal_space", 2) * horizontalIntervalScale;
    // Label brightness is used as the alpha channel
    labelBrightness = _prefs.getInt("label_brightness", 100) * 255 / 100;
    // Keyboard opacity
    keyboardOpacity = _prefs.getInt("keyboard_opacity", 100) * 255 / 100;
    keyOpacity = _prefs.getInt("key_opacity", 100) * 255 / 100;
    keyActivatedOpacity = _prefs.getInt("key_activated_opacity", 100) * 255 / 100;
    // Do not substract keyVerticalInterval from keyHeight because this is done
    // during rendered.
    keyHeight = dm.heightPixels * keyboardHeightPercent / 100 / 4;
    horizontal_margin =
      get_dip_pref_oriented(dm, "horizontal_margin", 3, 28);
    double_tap_lock_shift = _prefs.getBoolean("lock_double_tap", false);
    characterSize =
      _prefs.getFloat("character_size", 1.f)
      * characterSizeScale;
    accents = Integer.valueOf(_prefs.getString("accents", "1"));
    theme = getThemeId(res, _prefs.getString("theme", ""));
    autocapitalisation = _prefs.getBoolean("autocapitalisation", true);
    switch_input_immediate = _prefs.getBoolean("switch_input_immediate", false);
    extra_keys_param = ExtraKeyCheckBoxPreference.get_extra_keys(_prefs);
  }

  KeyValue action_key()
  {
    // Update the name to avoid caching in KeyModifier
    return (actionLabel == null) ? null :
      KeyValue.getKeyByName("action").withSymbol(actionLabel);
  }

  /** Update the layout according to the configuration.
   *  - Remove the switching key if it isn't needed
   *  - Remove "localized" keys from other locales (not in 'extra_keys')
   *  - Replace the action key to show the right label
   *  - Swap the enter and action keys
   *  - Add the optional numpad and number row
   */
  public KeyboardData modify_layout(KeyboardData kw)
  {
    final KeyValue action_key = action_key();
    // Extra keys are removed from the set as they are encountered during the
    // first iteration then automatically added.
    final Set<KeyValue> extra_keys = new HashSet<KeyValue>();
    final Set<KeyValue> remove_keys = new HashSet<KeyValue>();
    if (extra_keys_subtype != null)
      extra_keys_subtype.compute(extra_keys, kw.script);
    extra_keys.addAll(extra_keys_param);
    boolean number_row = this.number_row && !show_numpad;
    if (number_row)
      KeyboardData.number_row.getKeys(remove_keys);
    kw = kw.mapKeys(new KeyboardData.MapKeyValues() {
      public KeyValue apply(KeyValue key, boolean localized)
      {
        boolean is_extra_key = extra_keys.contains(key);
        if (is_extra_key)
          extra_keys.remove(key);
        if (localized && !is_extra_key)
          return null;
        if (remove_keys.contains(key))
          return null;
        switch (key.getKind())
        {
          case Event:
            switch (key.getEvent())
            {
              case CHANGE_METHOD:
                if (!shouldOfferSwitchingToNextInputMethod)
                  return null;
                if (switch_input_immediate)
                  return KeyValue.getKeyByName("change_method_prev");
                return key;
              case ACTION:
                return (swapEnterActionKey && action_key != null) ?
                  KeyValue.getKeyByName("enter") : action_key;
              case SWITCH_SECOND:
                return shouldOfferSwitchingToSecond ? key : null;
              case SWITCH_VOICE_TYPING:
                return shouldOfferVoiceTyping ? key : null;
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
    if (show_numpad)
      kw = kw.addNumPad();
    if (number_row)
      kw = kw.addNumberRow();
    if (extra_keys.size() > 0)
      kw = kw.addExtraKeys(extra_keys.iterator());
    return kw;
  }

  /**
   * Handle the numpad layout.
   */
  public KeyboardData modify_numpad(KeyboardData kw)
  {
    final KeyValue action_key = action_key();
    return kw.mapKeys(new KeyboardData.MapKeyValues() {
      public KeyValue apply(KeyValue key, boolean localized)
      {
        switch (key.getKind())
        {
          case Event:
            switch (key.getEvent())
            {
              case ACTION:
                return (swapEnterActionKey && action_key != null) ?
                  KeyValue.getKeyByName("enter") : action_key;
            }
            break;
          case Keyevent:
            switch (key.getKeyevent())
            {
              case KeyEvent.KEYCODE_ENTER:
                return (swapEnterActionKey && action_key != null) ? action_key : key;
            }
            break;
          case Char:
            char a = key.getChar(), b = a;
            if (inverse_numpad)
              b = inverse_numpad_char(a);
            if (a != b)
              return key.withChar(b);
            break;
        }
        return key;
      }
    });
  }

  /** Modify a layout to turn it into a secondary layout by changing the
      "switch_second" key. */
  KeyboardData tweak_secondary_layout(KeyboardData layout)
  {
    if (layout == null)
      return null;
    return layout.mapKeys(new KeyboardData.MapKeyValues() {
      public KeyValue apply(KeyValue key, boolean localized)
      {
        if (key.getKind() == KeyValue.Kind.Event
            && key.getEvent() == KeyValue.Event.SWITCH_SECOND)
          return KeyValue.getKeyByName("switch_second_back");
        return key;
      }
    });
  }

  private float get_dip_pref(DisplayMetrics dm, String pref_name, float def)
  {
    float value;
    try { value = _prefs.getInt(pref_name, -1); }
    catch (Exception e) { value = _prefs.getFloat(pref_name, -1f); }
    if (value < 0f)
      return (def);
    return (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, dm));
  }

  /** [get_dip_pref] depending on orientation. */
  float get_dip_pref_oriented(DisplayMetrics dm, String pref_base_name, float def_port, float def_land)
  {
    String suffix = orientation_landscape ? "_landscape" : "_portrait";
    float def = orientation_landscape ? def_land : def_port;
    return get_dip_pref(dm, pref_base_name + suffix, def);
  }

  private int getThemeId(Resources res, String theme_name)
  {
    switch (theme_name)
    {
      case "light": return R.style.Light;
      case "black": return R.style.Black;
      case "altblack": return R.style.AltBlack;
      case "dark": return R.style.Dark;
      case "white": return R.style.White;
      case "epaper": return R.style.ePaper;
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

  /** Might return [null] if the selected layout is "system", "custom" or if
      the name is not recognized. */
  public KeyboardData layout_of_string(Resources res, String name)
  {
    int id;
    switch (name)
    {
      case "arab_ckb": id = R.xml.arab_ckb; break;
      case "arab_fa_ir": id = R.xml.arab_ir; break;
      case "arab_ibm": id = R.xml.arab_ibm; break;
      case "arab_ibm_alt": id = R.xml.arab_ibm_alt; break;
      case "bengali_national": id = R.xml.bengali_national; break;
      case "bengali_provat": id = R.xml.bengali_provat; break;
      case "cyrl_jcuken_ru": id = R.xml.cyrl_jcuken_ru; break;
      case "cyrl_jcuken_uk": id = R.xml.cyrl_jcuken_uk; break;
      case "cyrl_yaverti": id = R.xml.cyrl_yaverti; break;
      case "deva_inscript": id = R.xml.deva_inscript; break;
      case "deva_alt": id = R.xml.deva_alt; break;
      case "grek_qwerty": id = R.xml.grek_qwerty; break;
      case "hang_dubeolsik_kr": id = R.xml.hang_dubeolsik_kr; break;
      case "hebr_1_il": id = R.xml.hebr_1_il; break;
      case "hebr_2_il": id = R.xml.hebr_2_il; break;
      case "latn_azerty_fr": id = R.xml.latn_azerty_fr; break;
      case "latn_bone": id = R.xml.latn_bone; break;
      case "latn_colemak": id = R.xml.latn_colemak; break;
      case "latn_dvorak": id = R.xml.latn_dvorak; break;
      case "latn_neo2": id = R.xml.latn_neo2; break;
      case "latn_qwerty_hu": id = R.xml.latn_qwerty_hu; break;
      case "latn_qwerty_br": id = R.xml.latn_qwerty_br; break;
      case "latn_qwerty_es": id = R.xml.latn_qwerty_es; break;
      case "latn_qwerty_lv": id = R.xml.latn_qwerty_lv; break;
      case "latn_qwerty_no": id = R.xml.latn_qwerty_no; break;
      case "latn_qwerty_pl": id = R.xml.latn_qwerty_pl; break;
      case "latn_qwerty_ro": id = R.xml.latn_qwerty_ro; break;
      case "latn_qwerty_se": id = R.xml.latn_qwerty_se; break;
      case "latn_qwerty_tr": id = R.xml.latn_qwerty_tr; break;
      case "latn_qwerty_us": id = R.xml.latn_qwerty_us; break;
      case "latn_qwerty_vi": id = R.xml.latn_qwerty_vi; break;
      case "latn_qwertz": id = R.xml.latn_qwertz; break;
      case "latn_qwertz_cz": id = R.xml.latn_qwertz_cz; break;
      case "latn_qwertz_de": id = R.xml.latn_qwertz_de; break;
      case "latn_qwertz_hu": id = R.xml.latn_qwertz_hu; break;
      case "latn_qwertz_sk": id = R.xml.latn_qwertz_sk; break;
      case "custom": return custom_layout;
      case "system": case "none": default: return null;
    }
    return KeyboardData.load(res, id);
  }

  char inverse_numpad_char(char c)
  {
    switch (c)
    {
      case '7': return '1';
      case '8': return '2';
      case '9': return '3';
      case '1': return '7';
      case '2': return '8';
      case '3': return '9';
      default: return c;
    }
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
