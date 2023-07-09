package juloo.keyboard2;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceGroup;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import java.util.HashSet;
import java.util.Set;

/** This class implements the "extra keys" preference but also defines the
    possible extra keys. */
public class ExtraKeysPreference extends PreferenceGroup
{
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
    "switch_greekmath",
    "capslock",
    "copy",
    "paste",
    "cut",
    "selectAll",
    "shareText",
    "pasteAsPlainText",
    "undo",
    "redo",
    "replaceText",
    "textAssist",
    "autofill",
  };

  /** Whether an extra key is enabled by default. */
  public static boolean default_checked(String name)
  {
    switch (name)
    {
      case "voice_typing":
        return true;
      default:
        return false;
    }
  }

  /** Get the set of enabled extra keys. */
  public static Set<KeyValue> get_extra_keys(SharedPreferences prefs)
  {
    HashSet<KeyValue> ks = new HashSet<KeyValue>();
    for (String key_name : extra_keys)
    {
      if (prefs.getBoolean(pref_key_of_key_name(key_name),
            default_checked(key_name)))
        ks.add(KeyValue.getKeyByName(key_name));
    }
    return ks;
  }

  boolean _attached; /** Whether it has already been attached. */

  public ExtraKeysPreference(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    Resources res = context.getResources();
    setOrderingAsAdded(true);
    setLayoutResource(R.layout.extra_keys_preference);
  }

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

  final class ExtraKeyCheckBoxPreference extends CheckBoxPreference
  {
    boolean _key_font;

    public ExtraKeyCheckBoxPreference(Context context, String key_name,
        boolean default_checked)
    {
      super(context);
      KeyValue kv = KeyValue.getKeyByName(key_name);
      String title = kv.getString();
      String descr = KeyValue.getKeyDescription(key_name);
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
