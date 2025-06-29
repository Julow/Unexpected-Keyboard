package juloo.keyboard2;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import androidx.window.layout.WindowInfoTracker;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

  // From preferences
  /** [null] represent the [system] layout. */
  public List<KeyboardData> layouts;
  public boolean show_numpad = false;
  // From the 'numpad_layout' option, also apply to the numeric pane.
  public boolean inverse_numpad = false;
  public boolean add_number_row;
  public boolean number_row_symbols;
  public float swipe_dist_px;
  public float slide_step_px;
  // Let the system handle vibration when false.
  public boolean vibrate_custom;
  // Control the vibration if [vibrate_custom] is true.
  public long vibrate_duration;
  public long longPressTimeout;
  public long longPressInterval;
  public boolean keyrepeat_enabled;
  public float margin_bottom;
  public int keyboardHeightPercent;
  public int screenHeightPixels;
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
  public NumberLayout selected_number_layout;
  public boolean borderConfig;
  public int circle_sensitivity;
  public boolean clipboard_history_enabled;

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
  public boolean foldable_unfolded = false;
  /** Index in 'layouts' of the currently used layout. See
      [get_current_layout()] and [set_current_layout()]. */
  int current_layout_portrait;
  int current_layout_landscape;
  int current_layout_unfolded_portrait;
  int current_layout_unfolded_landscape;

  private Config(SharedPreferences prefs, Resources res, IKeyEventHandler h, Boolean foldableUnfolded)
  {
    _prefs = prefs;
    // static values
    marginTop = res.getDimension(R.dimen.margin_top);
    keyPadding = res.getDimension(R.dimen.key_padding);
    labelTextSize = 0.33f;
    sublabelTextSize = 0.22f;
    // from prefs
    refresh(res, foldableUnfolded);
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
  public void refresh(Resources res, Boolean foldableUnfolded)
  {
    DisplayMetrics dm = res.getDisplayMetrics();
    orientation_landscape = res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    foldable_unfolded = foldableUnfolded;

    float characterSizeScale = 1.f;
    String show_numpad_s = _prefs.getString("show_numpad", "never");
    show_numpad = "always".equals(show_numpad_s);
    if (orientation_landscape)
    {
      if ("landscape".equals(show_numpad_s))
        show_numpad = true;
      keyboardHeightPercent = _prefs.getInt(foldable_unfolded ? "keyboard_height_landscape_unfolded" : "keyboard_height_landscape", 50);
      characterSizeScale = 1.25f;
    }
    else
    {
      keyboardHeightPercent = _prefs.getInt(foldable_unfolded ? "keyboard_height_unfolded" : "keyboard_height", 35);
    }
    layouts = LayoutsPreference.load_from_preferences(res, _prefs);
    inverse_numpad = _prefs.getString("numpad_layout", "default").equals("low_first");
    String number_row = _prefs.getString("number_row", "no_number_row");
    add_number_row = !number_row.equals("no_number_row");
    number_row_symbols = number_row.equals("symbols");
    // The baseline for the swipe distance correspond to approximately the
    // width of a key in portrait mode, as most layouts have 10 columns.
    // Multipled by the DPI ratio because most swipes are made in the diagonals.
    // The option value uses an unnamed scale where the baseline is around 25.
    float dpi_ratio = Math.max(dm.xdpi, dm.ydpi) / Math.min(dm.xdpi, dm.ydpi);
    float swipe_scaling = Math.min(dm.widthPixels, dm.heightPixels) / 10.f * dpi_ratio;
    float swipe_dist_value = Float.valueOf(_prefs.getString("swipe_dist", "15"));
    swipe_dist_px = swipe_dist_value / 25.f * swipe_scaling;
    slide_step_px = 0.4f * swipe_scaling;
    vibrate_custom = _prefs.getBoolean("vibrate_custom", false);
    vibrate_duration = _prefs.getInt("vibrate_duration", 20);
    longPressTimeout = _prefs.getInt("longpress_timeout", 600);
    longPressInterval = _prefs.getInt("longpress_interval", 65);
    keyrepeat_enabled = _prefs.getBoolean("keyrepeat_enabled", true);
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
    screenHeightPixels = dm.heightPixels;
    horizontal_margin =
      get_dip_pref_oriented(dm, "horizontal_margin", 3, 28);
    double_tap_lock_shift = _prefs.getBoolean("lock_double_tap", false);
    characterSize =
      _prefs.getFloat("character_size", 1.15f)
      * characterSizeScale;
    theme = getThemeId(res, _prefs.getString("theme", ""));
    autocapitalisation = _prefs.getBoolean("autocapitalisation", true);
    switch_input_immediate = _prefs.getBoolean("switch_input_immediate", false);
    extra_keys_param = ExtraKeysPreference.get_extra_keys(_prefs);
    extra_keys_custom = CustomExtraKeysPreference.get(_prefs);
    selected_number_layout = NumberLayout.valueOf(_prefs.getString("number_entry_layout",  "pin").toUpperCase());
    current_layout_portrait = _prefs.getInt("current_layout_portrait", 0);
    current_layout_landscape = _prefs.getInt("current_layout_landscape", 0);
    current_layout_unfolded_portrait = _prefs.getInt("current_layout_unfolded_portrait", 0);
    current_layout_unfolded_landscape = _prefs.getInt("current_layout_unfolded_landscape", 0);
    circle_sensitivity = Integer.valueOf(_prefs.getString("circle_sensitivity", "2"));
    clipboard_history_enabled = _prefs.getBoolean("clipboard_history_enabled", false);
  }

  public int get_current_layout()
  {
    if (foldable_unfolded) {
      return (orientation_landscape)
              ? current_layout_unfolded_landscape : current_layout_unfolded_portrait;
    } else {
      return (orientation_landscape)
              ? current_layout_landscape : current_layout_portrait;
    }
  }

  public void set_current_layout(int l)
  {
    if (foldable_unfolded) {
      if (orientation_landscape)
        current_layout_unfolded_landscape = l;
      else
        current_layout_unfolded_portrait = l;
    } else {
      if (orientation_landscape)
        current_layout_landscape = l;
      else
        current_layout_portrait = l;
    }

    SharedPreferences.Editor e = _prefs.edit();
    e.putInt("current_layout_portrait", current_layout_portrait);
    e.putInt("current_layout_landscape", current_layout_landscape);
    e.putInt("current_layout_unfolded_portrait", current_layout_unfolded_portrait);
    e.putInt("current_layout_unfolded_landscape", current_layout_unfolded_landscape);
    e.apply();
  }

  public void set_clipboard_history_enabled(boolean e)
  {
    clipboard_history_enabled = e;
    _prefs.edit().putBoolean("clipboard_history_enabled", e).commit();
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
    final String suffix;
    if (foldable_unfolded) {
      suffix = orientation_landscape ? "_landscape_unfolded" : "_portrait_unfolded";
    } else {
      suffix = orientation_landscape ? "_landscape" : "_portrait";
    }

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
      case "rosepine": return R.style.RosePine;
      default:
      case "system":
        if ((night_mode & Configuration.UI_MODE_NIGHT_NO) != 0)
          return R.style.Light;
        return R.style.Dark;
    }
  }

  private static Config _globalConfig = null;

  public static void initGlobalConfig(SharedPreferences prefs, Resources res,
      IKeyEventHandler handler, Boolean foldableUnfolded)
  {
    migrate(prefs);
    _globalConfig = new Config(prefs, res, handler, foldableUnfolded);
    LayoutModifier.init(_globalConfig, res);
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

  private static int CONFIG_VERSION = 3;

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
    switch (saved_version)
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
        // Fallthrough
      case 1:
        boolean add_number_row = prefs.getBoolean("number_row", false);
        e.putString("number_row", add_number_row ? "no_symbols" : "no_number_row");
        // Fallthrough
      case 2:
        if (!prefs.contains("number_entry_layout")) {
          e.putString("number_entry_layout", prefs.getBoolean("pin_entry_enabled", true) ? "pin" : "number");
        }
        // Fallthrough
      case 3:
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
