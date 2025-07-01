package juloo.keyboard2.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build.VERSION;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import juloo.keyboard2.*;

/** This class implements the "extra keys" preference but also defines the
    possible extra keys. */
public class ExtraKeysPreference extends PreferenceCategory
{
  /** Array of the keys that can be selected. */
  public static String[] extra_keys = new String[]
  {
    "alt",
    "meta",
    "compose",
    "voice_typing",
    "switch_clipboard",
    "accent_aigu",
    "accent_grave",
    "accent_double_aigu",
    "accent_dot_above",
    "accent_circonflexe",
    "accent_tilde",
    "accent_cedille",
    "accent_trema",
    "accent_ring",
    "accent_caron",
    "accent_macron",
    "accent_ogonek",
    "accent_breve",
    "accent_slash",
    "accent_bar",
    "accent_dot_below",
    "accent_hook_above",
    "accent_horn",
    "accent_double_grave",
    "€",
    "ß",
    "£",
    "§",
    "†",
    "ª",
    "º",
    "zwj",
    "zwnj",
    "nbsp",
    "nnbsp",
    "tab",
    "esc",
    "page_up",
    "page_down",
    "home",
    "end",
    "switch_greekmath",
    "change_method",
    "capslock",
    "copy",
    "paste",
    "cut",
    "selectAll",
    "shareText",
    "pasteAsPlainText",
    "undo",
    "redo",
    "delete_word",
    "forward_delete_word",
    "superscript",
    "subscript",
    "f11_placeholder",
    "f12_placeholder",
    "menu",
    "scroll_lock",
    "combining_dot_above",
    "combining_double_aigu",
    "combining_slash",
    "combining_arrow_right",
    "combining_breve",
    "combining_bar",
    "combining_aigu",
    "combining_caron",
    "combining_cedille",
    "combining_circonflexe",
    "combining_grave",
    "combining_macron",
    "combining_ring",
    "combining_tilde",
    "combining_trema",
    "combining_ogonek",
    "combining_dot_below",
    "combining_horn",
    "combining_hook_above",
    "combining_vertical_tilde",
    "combining_inverted_breve",
    "combining_pokrytie",
    "combining_slavonic_psili",
    "combining_slavonic_dasia",
    "combining_payerok",
    "combining_titlo",
    "combining_vzmet",
    "combining_arabic_v",
    "combining_arabic_inverted_v",
    "combining_shaddah",
    "combining_sukun",
    "combining_fatha",
    "combining_dammah",
    "combining_kasra",
    "combining_hamza_above",
    "combining_hamza_below",
    "combining_alef_above",
    "combining_fathatan",
    "combining_kasratan",
    "combining_dammatan",
    "combining_alef_below",
    "combining_kavyka",
    "combining_palatalization",
  };

  /** Whether an extra key is enabled by default. */
  public static boolean default_checked(String name)
  {
    switch (name)
    {
      case "voice_typing":
      case "change_method":
      case "switch_clipboard":
      case "compose":
      case "tab":
      case "esc":
      case "f11_placeholder":
      case "f12_placeholder":
        return true;
      default:
        return false;
    }
  }

  /** Text that describe a key. Might be null. */
  static String key_description(Resources res, String name)
  {
    int id = 0;
    String additional_info = null;
    switch (name)
    {
      case "capslock": id = R.string.key_descr_capslock; break;
      case "change_method": id = R.string.key_descr_change_method; break;
      case "compose": id = R.string.key_descr_compose; break;
      case "copy": id = R.string.key_descr_copy; break;
      case "cut": id = R.string.key_descr_cut; break;
      case "end":
        id = R.string.key_descr_end;
        additional_info = format_key_combination(new String[]{"fn", "right"});
        break;
      case "home":
        id = R.string.key_descr_home;
        additional_info = format_key_combination(new String[]{"fn", "left"});
        break;
      case "page_down":
        id = R.string.key_descr_page_down;
        additional_info = format_key_combination(new String[]{"fn", "down"});
        break;
      case "page_up":
        id = R.string.key_descr_page_up;
        additional_info = format_key_combination(new String[]{"fn", "up"});
        break;
      case "paste": id = R.string.key_descr_paste; break;
      case "pasteAsPlainText":
        id = R.string.key_descr_pasteAsPlainText;
        additional_info = format_key_combination(new String[]{"fn", "paste"});
        break;
      case "redo":
        id = R.string.key_descr_redo;
        additional_info = format_key_combination(new String[]{"fn", "undo"});
        break;
      case "delete_word":
        id = R.string.key_descr_delete_word;
        additional_info = format_key_combination_gesture(res, "backspace");
        break;
      case "forward_delete_word":
        id = R.string.key_descr_forward_delete_word;
        additional_info = format_key_combination_gesture(res, "forward_delete");
        break;
      case "selectAll": id = R.string.key_descr_selectAll; break;
      case "subscript": id = R.string.key_descr_subscript; break;
      case "superscript": id = R.string.key_descr_superscript; break;
      case "switch_greekmath": id = R.string.key_descr_switch_greekmath; break;
      case "undo": id = R.string.key_descr_undo; break;
      case "voice_typing": id = R.string.key_descr_voice_typing; break;
      case "ª": id = R.string.key_descr_ª; break;
      case "º": id = R.string.key_descr_º; break;
      case "switch_clipboard": id = R.string.key_descr_clipboard; break;
      case "zwj": id = R.string.key_descr_zwj; break;
      case "zwnj": id = R.string.key_descr_zwnj; break;
      case "nbsp": id = R.string.key_descr_nbsp; break;
      case "nnbsp": id = R.string.key_descr_nnbsp; break;

      case "accent_aigu":
      case "accent_grave":
      case "accent_double_aigu":
      case "accent_dot_above":
      case "accent_circonflexe":
      case "accent_tilde":
      case "accent_cedille":
      case "accent_trema":
      case "accent_ring":
      case "accent_caron":
      case "accent_macron":
      case "accent_ogonek":
      case "accent_breve":
      case "accent_slash":
      case "accent_bar":
      case "accent_dot_below":
      case "accent_hook_above":
      case "accent_horn":
      case "accent_double_grave":
        id = R.string.key_descr_dead_key;
        break;

      case "combining_dot_above":
      case "combining_double_aigu":
      case "combining_slash":
      case "combining_arrow_right":
      case "combining_breve":
      case "combining_bar":
      case "combining_aigu":
      case "combining_caron":
      case "combining_cedille":
      case "combining_circonflexe":
      case "combining_grave":
      case "combining_macron":
      case "combining_ring":
      case "combining_tilde":
      case "combining_trema":
      case "combining_ogonek":
      case "combining_dot_below":
      case "combining_horn":
      case "combining_hook_above":
      case "combining_vertical_tilde":
      case "combining_inverted_breve":
      case "combining_pokrytie":
      case "combining_slavonic_psili":
      case "combining_slavonic_dasia":
      case "combining_payerok":
      case "combining_titlo":
      case "combining_vzmet":
      case "combining_arabic_v":
      case "combining_arabic_inverted_v":
      case "combining_shaddah":
      case "combining_sukun":
      case "combining_fatha":
      case "combining_dammah":
      case "combining_kasra":
      case "combining_hamza_above":
      case "combining_hamza_below":
      case "combining_alef_above":
      case "combining_fathatan":
      case "combining_kasratan":
      case "combining_dammatan":
      case "combining_alef_below":
      case "combining_kavyka":
      case "combining_palatalization":
        id = R.string.key_descr_combining;
        break;
    }
    if (id == 0)
      return additional_info;
    String descr = res.getString(id);
    if (additional_info != null)
      descr += "  —  " + additional_info;
    return descr;
  }

  static String key_title(String key_name, KeyValue kv)
  {
    switch (key_name)
    {
      case "f11_placeholder": return "F11";
      case "f12_placeholder": return "F12";
    }
    return kv.getString();
  }

  /** Format a key combination */
  static String format_key_combination(String[] keys)
  {
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < keys.length; i++)
    {
      if (i > 0) out.append(" + ");
      out.append(KeyValue.getKeyByName(keys[i]).getString());
    }
    return out.toString();
  }

  /** Explain a gesture on a key */
  static String format_key_combination_gesture(Resources res, String key_name)
  {
    return res.getString(R.string.key_descr_gesture) + " + "
      + KeyValue.getKeyByName(key_name).getString();
  }

  /** Place an extra key next to the key specified by the first argument, on
      bottom-right preferably or on the bottom-left. If the specified key is not
      on the layout, place on the specified row and column. */
  static KeyboardData.PreferredPos mk_preferred_pos(String next_to_key, int row, int col, boolean prefer_bottom_right)
  {
    KeyValue next_to = (next_to_key == null) ? null : KeyValue.getKeyByName(next_to_key);
    int d1, d2; // Preferred direction and fallback direction
    if (prefer_bottom_right) { d1 = 4; d2 = 3; } else { d1 = 3; d2 = 4; }
    return new KeyboardData.PreferredPos(next_to,
            new KeyboardData.KeyPos[]{
              new KeyboardData.KeyPos(row, col, d1),
              new KeyboardData.KeyPos(row, col, d2),
              new KeyboardData.KeyPos(row, -1, d1),
              new KeyboardData.KeyPos(row, -1, d2),
              new KeyboardData.KeyPos(-1, -1, -1),
            });
  }

  static KeyboardData.PreferredPos key_preferred_pos(String key_name)
  {
    switch (key_name)
    {
      case "cut": return mk_preferred_pos("x", 2, 2, true);
      case "copy": return mk_preferred_pos("c", 2, 3, true);
      case "paste": return mk_preferred_pos("v", 2, 4, true);
      case "undo": return mk_preferred_pos("z", 2, 1, true);
      case "selectAll": return mk_preferred_pos("a", 1, 0, true);
      case "redo": return mk_preferred_pos("y", 0, 5, true);
      case "f11_placeholder": return mk_preferred_pos("9", 0, 8, false);
      case "f12_placeholder": return mk_preferred_pos("0", 0, 9, false);
      case "delete_word": return mk_preferred_pos("backspace", -1, -1, false);
      case "forward_delete_word": return mk_preferred_pos("backspace", -1, -1, true);
    }
    return KeyboardData.PreferredPos.DEFAULT;
  }

  /** Get the set of enabled extra keys. */
  public static Map<KeyValue, KeyboardData.PreferredPos> get_extra_keys(SharedPreferences prefs)
  {
    Map<KeyValue, KeyboardData.PreferredPos> ks =
      new HashMap<KeyValue, KeyboardData.PreferredPos>();
    for (String key_name : extra_keys)
    {
      if (prefs.getBoolean(pref_key_of_key_name(key_name),
            default_checked(key_name)))
        ks.put(KeyValue.getKeyByName(key_name), key_preferred_pos(key_name));
    }
    return ks;
  }

  boolean _attached = false; /** Whether it has already been attached. */

  public ExtraKeysPreference(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    setOrderingAsAdded(true);
  }

  @Override
  protected void onAttachedToActivity()
  {
    if (_attached)
      return;
    _attached = true;
    for (String key_name : extra_keys)
      addPreference(new ExtraKeyCheckBoxPreference(getContext(), key_name,
            default_checked(key_name)));
  }

  public static String pref_key_of_key_name(String key_name)
  {
    return "extra_key_" + key_name;
  }

  static class ExtraKeyCheckBoxPreference extends CheckBoxPreference
  {
    public ExtraKeyCheckBoxPreference(Context ctx, String key_name,
        boolean default_checked)
    {
      super(ctx);
      KeyValue kv = KeyValue.getKeyByName(key_name);
      String title = key_title(key_name, kv);
      String descr = key_description(ctx.getResources(), key_name);
      if (descr != null)
        title += " (" + descr + ")";
      setKey(pref_key_of_key_name(key_name));
      setDefaultValue(default_checked);
      setTitle(title);
      if (VERSION.SDK_INT >= 26)
        setSingleLineTitle(false);
    }

    @Override
    protected void onBindView(View view)
    {
      super.onBindView(view);
      TextView title = (TextView)view.findViewById(android.R.id.title);
      title.setTypeface(Theme.getKeyFont(getContext()));
    }
  }
}
