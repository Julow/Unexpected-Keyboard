package juloo.keyboard2;

import android.content.res.Resources;
import android.view.KeyEvent;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class LayoutModifier
{
  static Config globalConfig;
  static KeyboardData.Row bottom_row;
  static KeyboardData.Row number_row;
  static KeyboardData num_pad;

  /** Update the layout according to the configuration.
   *  - Remove the switching key if it isn't needed
   *  - Remove "localized" keys from other locales (not in 'extra_keys')
   *  - Replace the action key to show the right label
   *  - Swap the enter and action keys
   *  - Add the optional numpad and number row
   *  - Add the extra keys
   */
  public static KeyboardData modify_layout(KeyboardData kw)
  {
    // Extra keys are removed from the set as they are encountered during the
    // first iteration then automatically added.
    final TreeMap<KeyValue, KeyboardData.PreferredPos> extra_keys = new TreeMap<KeyValue, KeyboardData.PreferredPos>();
    final Set<KeyValue> remove_keys = new HashSet<KeyValue>();
    // Make sure the config key is accessible to avoid being locked in a custom
    // layout.
    extra_keys.put(KeyValue.getKeyByName("config"), KeyboardData.PreferredPos.ANYWHERE);
    extra_keys.putAll(globalConfig.extra_keys_param);
    extra_keys.putAll(globalConfig.extra_keys_custom);
    // Number row and numpads are added after the modification pass to allow
    // removing the number keys from the main layout.
    KeyboardData.Row added_number_row = null;
    KeyboardData added_numpad = null;
    if (globalConfig.show_numpad)
    {
      added_numpad = modify_numpad(num_pad, kw);
      remove_keys.addAll(added_numpad.getKeys().keySet());
    }
    else if (globalConfig.add_number_row && !kw.embedded_number_row) // The numpad removes the number row
    {
      added_number_row = modify_number_row(number_row, kw);
      remove_keys.addAll(added_number_row.getKeys(0).keySet());
    }
    // Add the bottom row before computing the extra keys
    if (kw.bottom_row)
      kw = kw.insert_row(bottom_row, kw.rows.size());
    // Compose keys to add to the layout
    // 'extra_keys_keyset' reflects changes made to 'extra_keys'
    Set<KeyValue> extra_keys_keyset = extra_keys.keySet();
    // 'kw_keys' contains the keys present on the layout without any extra keys
    Set<KeyValue> kw_keys = kw.getKeys().keySet();
    if (globalConfig.extra_keys_subtype != null && kw.locale_extra_keys)
    {
      Set<KeyValue> present = new HashSet<KeyValue>(kw_keys);
      present.addAll(extra_keys_keyset);
      globalConfig.extra_keys_subtype.compute(extra_keys,
          new ExtraKeys.Query(kw.script, present));
    }
    kw = kw.mapKeys(new KeyboardData.MapKeyValues() {
      public KeyValue apply(KeyValue key, boolean localized)
      {
        if (localized && !extra_keys.containsKey(key))
          return null;
        if (remove_keys.contains(key))
          return null;
        return modify_key(key);
      }
    });
    if (added_numpad != null)
      kw = kw.addNumPad(added_numpad);
    // Add extra keys that are not on the layout (including 'loc' keys)
    extra_keys_keyset.removeAll(kw_keys);
    if (extra_keys.size() > 0)
      kw = kw.addExtraKeys(extra_keys.entrySet().iterator());
    // Avoid adding extra keys to the number row
    if (added_number_row != null)
      kw = kw.insert_row(added_number_row, 0);
    return kw;
  }

  /** Handle the numpad layout. The [main_kw] is used to adapt the numpad to
      the main layout's script. */
  public static KeyboardData modify_numpad(KeyboardData kw, KeyboardData main_kw)
  {
    final int map_digit = KeyModifier.modify_numpad_script(main_kw.numpad_script);
    return kw.mapKeys(new KeyboardData.MapKeyValues() {
      public KeyValue apply(KeyValue key, boolean localized)
      {
        switch (key.getKind())
        {
          case Char:
            char prev_c = key.getChar();
            char c = prev_c;
            if (globalConfig.inverse_numpad)
              c = inverse_numpad_char(c);
            if (map_digit != -1)
            {
              KeyValue modified = ComposeKey.apply(map_digit, c);
              if (modified != null) // Was modified by script
                return modified;
            }
            if (prev_c != c) // Was inverted
              return key.withChar(c);
            return key; // Don't fallback into [modify_key]
        }
        return modify_key(key);
      }
    });
  }

  /** Modify the pin entry layout. [main_kw] is used to map the digits into the
      same script. */
  public static KeyboardData modify_pinentry(KeyboardData kw, KeyboardData main_kw)
  {
    KeyboardData.MapKeyValues m = numpad_script_map(main_kw.numpad_script);
    return m == null ? kw : kw.mapKeys(m);
  }

  /** Modify the number row according to [main_kw]'s script. */
  static KeyboardData.Row modify_number_row(KeyboardData.Row row,
      KeyboardData main_kw)
  {
    KeyboardData.MapKeyValues m = numpad_script_map(main_kw.numpad_script);
    return m == null ? row : row.mapKeys(m);
  }

  static KeyboardData.MapKeyValues numpad_script_map(String numpad_script)
  {
    final int map_digit = KeyModifier.modify_numpad_script(numpad_script);
    if (map_digit == -1)
      return null;
    return new KeyboardData.MapKeyValues() {
      public KeyValue apply(KeyValue key, boolean localized)
      {
        KeyValue modified = ComposeKey.apply(map_digit, key);
        return (modified != null) ? modified : key;
      }
    };
  }

  /** Modify keys on the main layout and on the numpad according to the config.
   */
  static KeyValue modify_key(KeyValue orig)
  {
    switch (orig.getKind())
    {
      case Event:
        switch (orig.getEvent())
        {
          case CHANGE_METHOD_PICKER:
            if (globalConfig.switch_input_immediate)
              return KeyValue.getKeyByName("change_method_prev");
            break;
          case ACTION:
            if (globalConfig.actionLabel == null)
              return null; // Remove the action key
            if (globalConfig.swapEnterActionKey)
              return KeyValue.getKeyByName("enter");
            return KeyValue.makeActionKey(globalConfig.actionLabel);
          case SWITCH_FORWARD:
            return (globalConfig.layouts.size() > 1) ? orig : null;
          case SWITCH_BACKWARD:
            return (globalConfig.layouts.size() > 2) ? orig : null;
          case SWITCH_VOICE_TYPING:
          case SWITCH_VOICE_TYPING_CHOOSER:
            return globalConfig.shouldOfferVoiceTyping ? orig : null;
        }
        break;
      case Keyevent:
        switch (orig.getKeyevent())
        {
          case KeyEvent.KEYCODE_ENTER:
            if (globalConfig.swapEnterActionKey && globalConfig.actionLabel != null)
              return KeyValue.makeActionKey(globalConfig.actionLabel);
            break;
        }
        break;
    }
    return orig;
  }

  static char inverse_numpad_char(char c)
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

  public static void init(Config globalConfig_, Resources res)
  {
    globalConfig = globalConfig_;
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
  }
}
