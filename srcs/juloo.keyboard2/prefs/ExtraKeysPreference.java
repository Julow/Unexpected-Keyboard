package juloo.keyboard2.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
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
      case "selectAll": id = R.string.key_descr_selectAll; break;
      case "shareText": id = R.string.key_descr_shareText; break;
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

  static KeyboardData.PreferredPos key_preferred_pos(String key_name)
  {
    switch (key_name)
    {
      case "cut":
        return new KeyboardData.PreferredPos(KeyValue.getKeyByName("x"),
            new KeyboardData.KeyPos[]{
              new KeyboardData.KeyPos(2, 2, 8),
              new KeyboardData.KeyPos(2, -1, 8),
              new KeyboardData.KeyPos(-1, -1, 8),
            });
      case "copy":
        return new KeyboardData.PreferredPos(KeyValue.getKeyByName("c"),
            new KeyboardData.KeyPos[]{
              new KeyboardData.KeyPos(2, 3, 8),
              new KeyboardData.KeyPos(2, -1, 8),
              new KeyboardData.KeyPos(-1, -1, 8),
            });
      case "paste":
        return new KeyboardData.PreferredPos(KeyValue.getKeyByName("v"),
            new KeyboardData.KeyPos[]{
              new KeyboardData.KeyPos(2, 4, 8),
              new KeyboardData.KeyPos(2, -1, 8),
              new KeyboardData.KeyPos(-1, -1, 8),
            });
      case "undo":
        return new KeyboardData.PreferredPos(KeyValue.getKeyByName("z"),
            new KeyboardData.KeyPos[]{
              new KeyboardData.KeyPos(2, 1, 8),
              new KeyboardData.KeyPos(2, -1, 8),
              new KeyboardData.KeyPos(-1, -1, 8),
            });
      case "redo":
        return new KeyboardData.PreferredPos(KeyValue.getKeyByName("y"),
            new KeyboardData.KeyPos[]{
              new KeyboardData.KeyPos(0, -1, 8),
              new KeyboardData.KeyPos(-1, -1, 8),
            });
      case "f11_placeholder":
        return new KeyboardData.PreferredPos(KeyValue.getKeyByName("9"),
            new KeyboardData.KeyPos[]{
              new KeyboardData.KeyPos(0, 8, 3),
              new KeyboardData.KeyPos(0, 8, 4),
              new KeyboardData.KeyPos(0, -1, 3),
              new KeyboardData.KeyPos(0, -1, 4),
            });
      case "f12_placeholder":
        return new KeyboardData.PreferredPos(KeyValue.getKeyByName("0"),
            new KeyboardData.KeyPos[]{
              new KeyboardData.KeyPos(0, 9, 3),
              new KeyboardData.KeyPos(0, 9, 4),
              new KeyboardData.KeyPos(0, -1, 3),
              new KeyboardData.KeyPos(0, -1, 4),
            });
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
