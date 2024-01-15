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
    "voice_typing",
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
    "€",
    "ß",
    "£",
    "§",
    "†",
    "ª",
    "º",
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
  };

  /** Whether an extra key is enabled by default. */
  public static boolean default_checked(String name)
  {
    switch (name)
    {
      case "voice_typing":
      case "change_method":
        return true;
      default:
        return false;
    }
  }

  /** Text that describe a key. Might be null. */
  static String key_description(Resources res, String name)
  {
    int id = 0;
    switch (name)
    {
      case "capslock": id = R.string.key_descr_capslock; break;
      case "switch_greekmath": id = R.string.key_descr_switch_greekmath; break;
      case "change_method": id = R.string.key_descr_change_method; break;
      case "voice_typing": id = R.string.key_descr_voice_typing; break;
      case "copy": id = R.string.key_descr_copy; break;
      case "paste": id = R.string.key_descr_paste; break;
      case "cut": id = R.string.key_descr_cut; break;
      case "selectAll": id = R.string.key_descr_selectAll; break;
      case "shareText": id = R.string.key_descr_shareText; break;
      case "pasteAsPlainText": id = R.string.key_descr_pasteAsPlainText; break;
      case "undo": id = R.string.key_descr_undo; break;
      case "redo": id = R.string.key_descr_redo; break;
      case "ª": id = R.string.key_descr_ª; break;
      case "º": id = R.string.key_descr_º; break;
      case "superscript": id = R.string.key_descr_superscript; break;
      case "subscript": id = R.string.key_descr_subscript; break;
      case "page_up": id = R.string.key_descr_page_up; break;
      case "page_down": id = R.string.key_descr_page_down; break;
      case "home": id = R.string.key_descr_home; break;
      case "end": id = R.string.key_descr_end; break;
    }
    if (id == 0)
      return null;
    return res.getString(id);
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
        ks.put(KeyValue.getKeyByName(key_name), KeyboardData.PreferredPos.DEFAULT);
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
    boolean _key_font;

    public ExtraKeyCheckBoxPreference(Context ctx, String key_name,
        boolean default_checked)
    {
      super(ctx);
      KeyValue kv = KeyValue.getKeyByName(key_name);
      String title = kv.getString();
      String descr = key_description(ctx.getResources(), key_name);
      if (descr != null)
        title += " (" + descr + ")";
      setKey(pref_key_of_key_name(key_name));
      setDefaultValue(default_checked);
      setTitle(title);
      _key_font = kv.hasFlags(KeyValue.FLAG_KEY_FONT);
    }

    @Override
    protected void onBindView(View view)
    {
      super.onBindView(view);
      TextView title = (TextView)view.findViewById(android.R.id.title);
      title.setTypeface(_key_font ? Theme.getKeyFont(getContext()) : null);
    }
  }
}
