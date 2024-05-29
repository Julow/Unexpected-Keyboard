package juloo.keyboard2;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import juloo.keyboard2.prefs.CustomExtraKeysPreference;
import juloo.keyboard2.prefs.ExtraKeysPreference;
import juloo.keyboard2.prefs.LayoutsPreference;

public final class Config
{
  private final SharedPreferences _prefs;

  // From resources
  public final float marginTop;
  public final float keyPadding;

  public final float labelTextSize;
  public final float sublabelTextSize;

  public final KeyboardData.Row bottom_row;
  public final KeyboardData.Row number_row;
  public final KeyboardData num_pad;

  // From preferences
  /** [null] represent the [system] layout. */
  public List<KeyboardData> layouts;
  public boolean show_numpad = false;
  // From the 'numpad_layout' option, also apply to the numeric pane.
  public boolean inverse_numpad = false;
  public boolean add_number_row;
  public float swipe_dist_px;
  public float slide_step_px;
  // Let the system handle vibration when false.
  public boolean vibrate_custom;
  // Control the vibration if [vibrate_custom] is true.
  public long vibrate_duration;
  public long longPressTimeout;
  public long longPressInterval;
  public float margin_bottom;
  public float keyHeight;
  public float horizontal_margin;
  public float key_vertical_margin;
  public float key_horizontal_margin;
  public int labelBrightness; // 0 - 255
  public int keyboardOpacity; // 0 - 255
  public float customBorderRadius; // 0 - 1
  public float customBorderLineWidth; // dp
  public int keyOpacity; // 0 - 255
  public int keyActivatedOpacity; // 0 - 255
  public boolean double_tap_lock_shift;
  public float characterSize; // Ratio
  public int theme; // Values are R.style.*
  public boolean autocapitalisation;
  public boolean switch_input_immediate;
  public boolean pin_entry_enabled;
  public boolean borderConfig;

  // Dynamically set
  public boolean shouldOfferVoiceTyping;
  public String actionLabel; // Might be 'null'
  public int actionId; // Meaningful only when 'actionLabel' isn't 'null'
  public boolean swapEnterActionKey; // Swap the "enter" and "action" keys
  public ExtraKeys extra_keys_subtype;
  public Map<KeyValue, KeyboardData.PreferredPos> extra_keys_param;
  public Map<KeyValue, KeyboardData.PreferredPos> extra_keys_custom;

  public final IKeyEventHandler handler;
  public boolean orientation_landscape = false;
  /** Index in 'layouts' of the currently used layout. See
      [get_current_layout()] and [set_current_layout()]. */
  int current_layout_portrait;
  int current_layout_landscape;

  private Config(SharedPreferences prefs, Resources res, IKeyEventHandler h)
  {
    _prefs = prefs;
    // static values
    marginTop = res.getDimension(R.dimen.margin_top);
    keyPadding = res.getDimension(R.dimen.key_padding);
    labelTextSize = 0.33f;
    sublabelTextSize = 0.22f;
    try
    {
      number_row = KeyboardData.load_number_row(res);
      bottom_row = KeyboardData.load_bottom_row(res);
      num_pad = KeyboardData.load_num_pad(res);
    }
    catch (Exception e)
    {
      throw new RuntimeException(e.getMessage()); // Not recoverable
    }
    // from prefs
    refresh(res);
    // initialized later
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
    float characterSizeScale = 1.f;
    String show_numpad_s = _prefs.getString("show_numpad", "never");
    show_numpad = "always".equals(show_numpad_s);
    if (orientation_landscape)
    {
      if ("landscape".equals(show_numpad_s))
        show_numpad = true;
      keyboardHeightPercent = _prefs.getInt("keyboard_height_landscape", 50);
      characterSizeScale = 1.25f;
    }
    else
    {
      keyboardHeightPercent = _prefs.getInt("keyboard_height", 35);
    }
    layouts = LayoutsPreference.load_from_preferences(res, _prefs);
    inverse_numpad = _prefs.getString("numpad_layout", "default").equals("low_first");
    add_number_row = _prefs.getBoolean("number_row", false);
    // The baseline for the swipe distance correspond to approximately the
    // width of a key in portrait mode, as most layouts have 10 columns.
    // Multipled by the DPI ratio because most swipes are made in the diagonals.
    // The option value uses an unnamed scale where the baseline is around 25.
    float dpi_ratio = Math.max(dm.xdpi, dm.ydpi) / Math.min(dm.xdpi, dm.ydpi);
    float swipe_scaling = Math.min(dm.widthPixels, dm.heightPixels) / 10.f * dpi_ratio;
    float swipe_dist_value = Float.valueOf(_prefs.getString("swipe_dist", "15"));
    swipe_dist_px = swipe_dist_value / 25.f * swipe_scaling;
    slide_step_px = 0.2f * swipe_scaling;
    vibrate_custom = _prefs.getBoolean("vibrate_custom", false);
    vibrate_duration = _prefs.getInt("vibrate_duration", 20);
    longPressTimeout = _prefs.getInt("longpress_timeout", 600);
    longPressInterval = _prefs.getInt("longpress_interval", 65);
    margin_bottom = get_dip_pref_oriented(dm, "margin_bottom", 7, 3);
    key_vertical_margin = get_dip_pref(dm, "key_vertical_margin", 1.5f) / 100;
    key_horizontal_margin = get_dip_pref(dm, "key_horizontal_margin", 2) / 100;
    // Label brightness is used as the alpha channel
    labelBrightness = _prefs.getInt("label_brightness", 100) * 255 / 100;
    // Keyboard opacity
    keyboardOpacity = _prefs.getInt("keyboard_opacity", 100) * 255 / 100;
    keyOpacity = _prefs.getInt("key_opacity", 100) * 255 / 100;
    keyActivatedOpacity = _prefs.getInt("key_activated_opacity", 100) * 255 / 100;
    // keyboard border settings
    borderConfig = _prefs.getBoolean("border_config", false);
    customBorderRadius = _prefs.getInt("custom_border_radius", 0) / 100.f;
    customBorderLineWidth = get_dip_pref(dm, "custom_border_line_width", 0);
    // Do not substract key_vertical_margin from keyHeight because this is done
    // during rendering.
    keyHeight = dm.heightPixels * keyboardHeightPercent / 100 / 4;
    horizontal_margin =
      get_dip_pref_oriented(dm, "horizontal_margin", 3, 28);
    double_tap_lock_shift = _prefs.getBoolean("lock_double_tap", false);
    characterSize =
      _prefs.getFloat("character_size", 1.f)
      * characterSizeScale;
    theme = getThemeId(res, _prefs.getString("theme", ""));
    autocapitalisation = _prefs.getBoolean("autocapitalisation", true);
    switch_input_immediate = _prefs.getBoolean("switch_input_immediate", false);
    extra_keys_param = ExtraKeysPreference.get_extra_keys(_prefs);
    extra_keys_custom = CustomExtraKeysPreference.get(_prefs);
    pin_entry_enabled = _prefs.getBoolean("pin_entry_enabled", true);
    current_layout_portrait = _prefs.getInt("current_layout_portrait", 0);
    current_layout_landscape = _prefs.getInt("current_layout_landscape", 0);
  }

  public int get_current_layout()
  {
    return (orientation_landscape)
      ? current_layout_landscape : current_layout_portrait;
  }

  public void set_current_layout(int l)
  {
    if (orientation_landscape)
      current_layout_landscape = l;
    else
      current_layout_portrait = l;
    SharedPreferences.Editor e = _prefs.edit();
    e.putInt("current_layout_portrait", current_layout_portrait);
    e.putInt("current_layout_landscape", current_layout_landscape);
    e.apply();
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
   *  - Add the extra keys
   */
  public KeyboardData modify_layout(KeyboardData kw)
  {
    final KeyValue action_key = action_key();
    // Extra keys are removed from the set as they are encountered during the
    // first iteration then automatically added.
    final Map<KeyValue, KeyboardData.PreferredPos> extra_keys = new HashMap<KeyValue, KeyboardData.PreferredPos>();
    final Set<KeyValue> remove_keys = new HashSet<KeyValue>();
    // Make sure the config key is accessible to avoid being locked in a custom
    // layout.
    extra_keys.put(KeyValue.getKeyByName("config"), KeyboardData.PreferredPos.ANYWHERE);
    extra_keys.putAll(extra_keys_param);
    extra_keys.putAll(extra_keys_custom);
    if (extra_keys_subtype != null)
    {
      Set<KeyValue> present = new HashSet<KeyValue>();
      present.addAll(kw.getKeys().keySet());
      present.addAll(extra_keys_param.keySet());
      present.addAll(extra_keys_custom.keySet());
      extra_keys_subtype.compute(extra_keys,
          new ExtraKeys.Query(kw.script, present));
    }
    KeyboardData.Row added_number_row = null;
    if (add_number_row && !show_numpad)
      added_number_row = modify_number_row(number_row, kw);
    if (added_number_row != null)
      remove_keys.addAll(added_number_row.getKeys(0).keySet());
    if (kw.bottom_row)
      kw = kw.insert_row(bottom_row, kw.rows.size());
    kw = kw.mapKeys(new KeyboardData.MapKeyValues() {
      public KeyValue apply(KeyValue key, boolean localized)
      {
        boolean is_extra_key = extra_keys.containsKey(key);
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
              case CHANGE_METHOD_PICKER:
                if (switch_input_immediate)
                  return KeyValue.getKeyByName("change_method_prev");
                return key;
              case ACTION:
                return (swapEnterActionKey && action_key != null) ?
                  KeyValue.getKeyByName("enter") : action_key;
              case SWITCH_FORWARD:
                return (layouts.size() > 1) ? key : null;
              case SWITCH_BACKWARD:
                return (layouts.size() > 2) ? key : null;
              case SWITCH_VOICE_TYPING:
              case SWITCH_VOICE_TYPING_CHOOSER:
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
      kw = kw.addNumPad(modify_numpad(num_pad, kw));
    if (added_number_row != null)
      kw = kw.insert_row(added_number_row, 0);
    if (extra_keys.size() > 0)
      kw = kw.addExtraKeys(extra_keys.entrySet().iterator());
    return kw;
  }

  /** Handle the numpad layout. The [main_kw] is used to adapt the numpad to
      the main layout's script. */
  public KeyboardData modify_numpad(KeyboardData kw, KeyboardData main_kw)
  {
    final KeyValue action_key = action_key();
    final KeyModifier.Map_char map_digit = KeyModifier.modify_numpad_script(main_kw.numpad_script);
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
            char prev_c = key.getChar();
            char c = prev_c;
            if (inverse_numpad)
              c = inverse_numpad_char(c);
            String modified = map_digit.apply(c);
            if (modified != null) // Was modified by script
              return KeyValue.makeStringKey(modified);
            if (prev_c != c) // Was inverted
              return key.withChar(c);
            break;
        }
        return key;
      }
    });
  }

  static KeyboardData.MapKeyValues numpad_script_map(String numpad_script)
  {
    final KeyModifier.Map_char map_digit = KeyModifier.modify_numpad_script(numpad_script);
    return new KeyboardData.MapKeyValues() {
      public KeyValue apply(KeyValue key, boolean localized)
      {
        switch (key.getKind())
        {
          case Char:
            String modified = map_digit.apply(key.getChar());
            if (modified != null)
              return KeyValue.makeStringKey(modified);
            break;
        }
        return key;
      }
    };
  }

  /** Modify the pin entry layout. [main_kw] is used to map the digits into the
      same script. */
  public KeyboardData modify_pinentry(KeyboardData kw, KeyboardData main_kw)
  {
    return kw.mapKeys(numpad_script_map(main_kw.numpad_script));
  }

  /** Modify the number row according to [main_kw]'s script. */
  public KeyboardData.Row modify_number_row(KeyboardData.Row row,
      KeyboardData main_kw)
  {
    return row.mapKeys(numpad_script_map(main_kw.numpad_script));
  }

  private float get_dip_pref(DisplayMetrics dm, String pref_name, float def)
  {
    float value;
    try { value = _prefs.getInt(pref_name, -1); }
    catch (Exception e) { value = _prefs.getFloat(pref_name, -1f); }
    if (value < 0f)
      value = def;
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
    int night_mode = res.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    switch (theme_name)
    {
      case "light": return R.style.Light;
      case "black": return R.style.Black;
      case "altblack": return R.style.AltBlack;
      case "dark": return R.style.Dark;
      case "white": return R.style.White;
      case "epaper": return R.style.ePaper;
      case "desert": return R.style.Desert;
      case "jungle": return R.style.Jungle;
      case "monetlight": return R.style.MonetLight;
      case "monetdark": return R.style.MonetDark;
      case "monet":
        if ((night_mode & Configuration.UI_MODE_NIGHT_NO) != 0)
          return R.style.MonetLight;
        return R.style.MonetDark;
      default:
      case "system":
        if ((night_mode & Configuration.UI_MODE_NIGHT_NO) != 0)
          return R.style.Light;
        return R.style.Dark;
    }
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
    migrate(prefs);
    _globalConfig = new Config(prefs, res, handler);
  }

  public static Config globalConfig()
  {
    return _globalConfig;
  }

  public static SharedPreferences globalPrefs()
  {
    return _globalConfig._prefs;
  }

  public static interface IKeyEventHandler
  {
    public void key_down(KeyValue value, boolean is_swipe);
    public void key_up(KeyValue value, Pointers.Modifiers mods);
    public void mods_changed(Pointers.Modifiers mods);
  }

  /** Config migrations. */

  private static int CONFIG_VERSION = 1;

  public static void migrate(SharedPreferences prefs)
  {
    int saved_version = prefs.getInt("version", 0);
    Logs.debug_config_migration(saved_version, CONFIG_VERSION);
    if (saved_version == CONFIG_VERSION)
      return;
    SharedPreferences.Editor e = prefs.edit();
    e.putInt("version", CONFIG_VERSION);
    // Migrations might run on an empty [prefs] for new installs, in this case
    // they set the default values of complex options.
    switch (saved_version) // Fallback switch
    {
      case 0:
        // Primary, secondary and custom layout options are merged into the new
        // Layouts option. This also sets the default value.
        List<LayoutsPreference.Layout> l = new ArrayList<LayoutsPreference.Layout>();
        l.add(migrate_layout(prefs.getString("layout", "system")));
        String snd_layout = prefs.getString("second_layout", "none");
        if (snd_layout != null && !snd_layout.equals("none"))
          l.add(migrate_layout(snd_layout));
        String custom_layout = prefs.getString("custom_layout", "");
        if (custom_layout != null && !custom_layout.equals(""))
          l.add(LayoutsPreference.CustomLayout.parse(custom_layout));
        LayoutsPreference.save_to_preferences(e, l);
      case 1:
      default: break;
    }
    e.apply();
  }

  private static LayoutsPreference.Layout migrate_layout(String name)
  {
    if (name == null || name.equals("system"))
      return new LayoutsPreference.SystemLayout();
    return new LayoutsPreference.NamedLayout(name);
  }
}
