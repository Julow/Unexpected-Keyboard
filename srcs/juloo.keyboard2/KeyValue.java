package juloo.keyboard2;

import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import java.util.HashMap;

class KeyValue
{
  /** Values for the [code] field. */

  public static final int EVENT_CONFIG = -2;
  public static final int EVENT_SWITCH_TEXT = -3;
  public static final int EVENT_SWITCH_NUMERIC = -4;
  public static final int EVENT_SWITCH_EMOJI = -5;
  public static final int EVENT_SWITCH_BACK_EMOJI = -6;
  public static final int EVENT_CHANGE_METHOD = -7;
  public static final int EVENT_ACTION = -8;
  public static final int EVENT_SWITCH_PROGRAMMING = -9;

  // Modifiers
  // Must be evaluated in the reverse order of their values.
  public static final int MOD_SHIFT = -100;
  public static final int MOD_FN = -101;
  public static final int MOD_CTRL = -102;
  public static final int MOD_ALT = -103;
  public static final int MOD_META = -104;

  // Dead-keys
  public static final int MOD_DOUBLE_AIGU = -200;
  public static final int MOD_DOT_ABOVE = -201;
  public static final int MOD_GRAVE = -202;
  public static final int MOD_AIGU = -203;
  public static final int MOD_CIRCONFLEXE = -204;
  public static final int MOD_TILDE = -205;
  public static final int MOD_CEDILLE = -206;
  public static final int MOD_TREMA = -207;
  public static final int MOD_SUPERSCRIPT = -208;
  public static final int MOD_SUBSCRIPT = -209;
  public static final int MOD_RING = -210;
  public static final int MOD_CARON = -211;
  public static final int MOD_MACRON = -212;
  public static final int MOD_ORDINAL = -213;
  public static final int MOD_ARROWS = -214;
  public static final int MOD_BOX = -215;
  public static final int MOD_OGONEK = -216;
  public static final int MOD_SLASH = -217;
  public static final int MOD_ARROW_RIGHT = -218;

  // Behavior flags
  public static final int FLAG_LATCH = 1;
  public static final int FLAG_LOCK = (1 << 1);
  // Special keys are not repeated and don't clear latched modifiers
  public static final int FLAG_SPECIAL = (1 << 2);
  public static final int FLAG_PRECISE_REPEAT = (1 << 4);

  // Rendering flags
  public static final int FLAG_KEY_FONT = (1 << 5);
  public static final int FLAG_SMALLER_FONT = (1 << 6);

  // Internal flags
  public static final int FLAG_LOCKED = (1 << 7);

  // Language specific keys that are removed from the keyboard by default
  public static final int FLAG_LOCALIZED = (1 << 8);

  // Kind flags
  public static final int KIND_CHAR = (0 << 30);
  public static final int KIND_STRING = (1 << 30);
  public static final int KIND_EVENT = (2 << 30);
  public static final int KIND_MODIFIER = (3 << 30);

  public static final int KIND_FLAGS = (0b11 << 30);

  public final String name;
  private final String _symbol;

  /** Describe what the key does when it isn't a simple character.
      The meaning of this value depends on [_flags & KIND_FLAGS], which
      corresponds to the [Kind] enum. */
  private final int _code;
  private final int _flags;

  public static enum Kind
  {
    Char, String, Event, Modifier
  }

  public Kind getKind()
  {
    switch (_flags & KIND_FLAGS)
    {
      case KIND_CHAR: return Kind.Char;
      case KIND_STRING: return Kind.String;
      case KIND_EVENT: return Kind.Event;
      case KIND_MODIFIER: return Kind.Modifier;
      default: throw new RuntimeException("Corrupted kind flags");
    }
  }

  public int getFlags()
  {
    return _flags;
  }

  public boolean hasFlags(int has)
  {
    return ((_flags & has) != 0);
  }

  /** The string to render on the keyboard.
      When [getKind() == Kind.String], also the string to send. */
  public String getString()
  {
    return _symbol;
  }

  /** The char to be sent when the key is pressed.
      Defined only when [getKind() == Kind.Char]. */
  public char getChar()
  {
    return (char)_code;
  }

  /** An Android event or one of the [EVENT_*] constants.
      Defined only when [getKind() == Kind.Event]. */
  public int getEvent()
  {
    return _code;
  }

  /** Modifier activated by this key.
      Defined only when [getKind() == Kind.Modifier]. */
  public int getModifier()
  {
    return _code;
  }

  /* Update the char and the symbol. */
  public KeyValue withChar(char c)
  {
    return new KeyValue(name, String.valueOf(c), KIND_CHAR, c, _flags);
  }

  public KeyValue withString(String s)
  {
    return new KeyValue(name, s, KIND_STRING, 0, _flags);
  }

  public KeyValue withNameAndSymbol(String n, String s)
  {
    return new KeyValue(n, s, (_flags & KIND_FLAGS), _code, _flags);
  }

  public KeyValue withEvent(int e)
  {
    return new KeyValue(name, _symbol, KIND_EVENT, e, _flags);
  }

  public KeyValue withFlags(int f)
  {
    return new KeyValue(name, _symbol, (_flags & KIND_FLAGS), _code, f);
  }

  private static HashMap<String, KeyValue> keys = new HashMap<String, KeyValue>();

  public KeyValue(String n, String s, int kind, int c, int f)
  {
    assert((kind & ~KIND_FLAGS) == 0);
    name = n;
    _symbol = s;
    _code = c;
    _flags = (f & ~KIND_FLAGS) | kind;
  }

  private static String stripPrefix(String s, String prefix)
  {
    if (s.startsWith(prefix))
      return s.substring(prefix.length());
    else
      return null;
  }

  public static KeyValue getKeyByName(String name)
  {
    if (name == null)
      return null;
    KeyValue kv = KeyValue.keys.get(name);
    if (kv != null)
      return kv;
    String localized = stripPrefix(name, "loc ");
    if (localized != null)
    {
      kv = getKeyByName(localized);
      return kv.withFlags(kv._flags | FLAG_LOCALIZED);
    }
    if (name.length() == 1)
      return new KeyValue(name, name, KIND_CHAR, name.charAt(0), 0);
    else
      return new KeyValue(name, name, KIND_STRING, 0, 0);
  }

  private static void addKey(String name, String symbol, int kind, int code, int flags)
  {
    keys.put(name, new KeyValue(name, symbol, kind, code, flags));
  }

  private static void addCharKey(String name, String symbol, char c, int flags)
  {
    addKey(name, symbol, KIND_CHAR, c, flags);
  }

  private static void addModifierKey(String name, String symbol, int code, int extra_flags)
  {
    assert(code >= 100 && code < 300);
    addKey(name, symbol, KIND_MODIFIER, code,
        FLAG_LATCH | FLAG_SPECIAL | extra_flags);
  }

  private static void addSpecialKey(String name, String symbol, int event, int flags)
  {
    addKey(name, symbol, KIND_EVENT, event, flags | FLAG_SPECIAL);
  }

  private static void addEventKey(String name, String symbol, int event, int flags)
  {
    addKey(name, symbol, KIND_EVENT, event, flags);
  }

  static
  {
    addModifierKey("shift", "\n", // Can't write u000A because Java is stupid
        MOD_SHIFT, FLAG_KEY_FONT | FLAG_SMALLER_FONT);
    addModifierKey("ctrl", "Ctrl", MOD_CTRL, FLAG_SMALLER_FONT);
    addModifierKey("alt", "Alt", MOD_ALT, FLAG_SMALLER_FONT);
    addModifierKey("accent_aigu", "\u0050", MOD_AIGU, FLAG_KEY_FONT);
    addModifierKey("accent_caron", "\u0051", MOD_CARON, FLAG_KEY_FONT);
    addModifierKey("accent_cedille", "\u0052", MOD_CEDILLE, FLAG_KEY_FONT);
    addModifierKey("accent_circonflexe", "\u0053", MOD_CIRCONFLEXE, FLAG_KEY_FONT);
    addModifierKey("accent_grave", "\u0054", MOD_GRAVE, FLAG_KEY_FONT);
    addModifierKey("accent_macron", "\u0055", MOD_MACRON, FLAG_KEY_FONT);
    addModifierKey("accent_ring", "\u0056", MOD_RING, FLAG_KEY_FONT);
    addModifierKey("accent_tilde", "\u0057", MOD_TILDE, FLAG_KEY_FONT);
    addModifierKey("accent_trema", "\u0058", MOD_TREMA, FLAG_KEY_FONT);
    addModifierKey("accent_ogonek", "\u0059", MOD_OGONEK, FLAG_KEY_FONT);
    addModifierKey("accent_dot_above", "\u005a", MOD_DOT_ABOVE, FLAG_KEY_FONT);
    addModifierKey("accent_double_aigu", "\u005b", MOD_DOUBLE_AIGU, FLAG_KEY_FONT);
    addModifierKey("accent_slash", "\134", // Can't write u005c
        MOD_SLASH, FLAG_KEY_FONT);
    addModifierKey("accent_arrow_right", "\u005d", MOD_ARROW_RIGHT, FLAG_KEY_FONT);
    addModifierKey("superscript", "Sup", MOD_SUPERSCRIPT, FLAG_SMALLER_FONT);
    addModifierKey("subscript", "Sub", MOD_SUBSCRIPT, FLAG_SMALLER_FONT);
    addModifierKey("ordinal", "Ord", MOD_ORDINAL, FLAG_SMALLER_FONT);
    addModifierKey("arrows", "Arr", MOD_ARROWS, FLAG_SMALLER_FONT);
    addModifierKey("box", "Box", MOD_BOX, FLAG_SMALLER_FONT);
    addModifierKey("fn", "Fn", MOD_FN, FLAG_SMALLER_FONT);
    addModifierKey("meta", "Meta", MOD_META, FLAG_SMALLER_FONT);

    addSpecialKey("config", "\u0004", EVENT_CONFIG, FLAG_KEY_FONT | FLAG_SMALLER_FONT);
    addSpecialKey("switch_text", "ABC", EVENT_SWITCH_TEXT, FLAG_SMALLER_FONT);
    addSpecialKey("switch_numeric", "123+", EVENT_SWITCH_NUMERIC, FLAG_SMALLER_FONT);
    addSpecialKey("switch_emoji", "\u0001" , EVENT_SWITCH_EMOJI, FLAG_KEY_FONT | FLAG_SMALLER_FONT);
    addSpecialKey("switch_back_emoji", "ABC", EVENT_SWITCH_BACK_EMOJI, 0);
    addSpecialKey("switch_programming", "Prog", EVENT_SWITCH_PROGRAMMING, FLAG_SMALLER_FONT);
    addSpecialKey("change_method", "\u0009", EVENT_CHANGE_METHOD, FLAG_KEY_FONT | FLAG_SMALLER_FONT);
    addSpecialKey("action", "Action", EVENT_ACTION, FLAG_SMALLER_FONT); // Will always be replaced

    addEventKey("esc", "Esc", KeyEvent.KEYCODE_ESCAPE, FLAG_SMALLER_FONT);
    addEventKey("enter", "\u000E", KeyEvent.KEYCODE_ENTER, FLAG_KEY_FONT);
    addEventKey("up", "\u0005", KeyEvent.KEYCODE_DPAD_UP, FLAG_KEY_FONT | FLAG_PRECISE_REPEAT);
    addEventKey("right", "\u0006", KeyEvent.KEYCODE_DPAD_RIGHT, FLAG_KEY_FONT | FLAG_PRECISE_REPEAT);
    addEventKey("down", "\u0007", KeyEvent.KEYCODE_DPAD_DOWN, FLAG_KEY_FONT | FLAG_PRECISE_REPEAT);
    addEventKey("left", "\u0008", KeyEvent.KEYCODE_DPAD_LEFT, FLAG_KEY_FONT | FLAG_PRECISE_REPEAT);
    addEventKey("page_up", "\u0002", KeyEvent.KEYCODE_PAGE_UP, FLAG_KEY_FONT);
    addEventKey("page_down", "\u0003", KeyEvent.KEYCODE_PAGE_DOWN, FLAG_KEY_FONT);
    addEventKey("home", "\u000B", KeyEvent.KEYCODE_MOVE_HOME, FLAG_KEY_FONT);
    addEventKey("end", "\u000C", KeyEvent.KEYCODE_MOVE_END, FLAG_KEY_FONT);
    addEventKey("backspace", "\u0011", KeyEvent.KEYCODE_DEL, FLAG_KEY_FONT);
    addEventKey("delete", "\u0010", KeyEvent.KEYCODE_FORWARD_DEL, FLAG_KEY_FONT);
    addEventKey("insert", "Ins", KeyEvent.KEYCODE_INSERT, FLAG_SMALLER_FONT);
    addEventKey("f1", "F1", KeyEvent.KEYCODE_F1, 0);
    addEventKey("f2", "F2", KeyEvent.KEYCODE_F2, 0);
    addEventKey("f3", "F3", KeyEvent.KEYCODE_F3, 0);
    addEventKey("f4", "F4", KeyEvent.KEYCODE_F4, 0);
    addEventKey("f5", "F5", KeyEvent.KEYCODE_F5, 0);
    addEventKey("f6", "F6", KeyEvent.KEYCODE_F6, 0);
    addEventKey("f7", "F7", KeyEvent.KEYCODE_F7, 0);
    addEventKey("f8", "F8", KeyEvent.KEYCODE_F8, 0);
    addEventKey("f9", "F9", KeyEvent.KEYCODE_F9, 0);
    addEventKey("f10", "F10", KeyEvent.KEYCODE_F10, 0);
    addEventKey("f11", "F11", KeyEvent.KEYCODE_F11, FLAG_SMALLER_FONT);
    addEventKey("f12", "F12", KeyEvent.KEYCODE_F12, FLAG_SMALLER_FONT);
    addEventKey("tab", "\u000F", KeyEvent.KEYCODE_TAB, FLAG_KEY_FONT | FLAG_SMALLER_FONT);

    addCharKey("\\t", "\\t", '\t', 0); // Send the tab character
    addCharKey("space", "\r", ' ', FLAG_KEY_FONT);
    addCharKey("nbsp", "\u237d", '\u00a0', FLAG_KEY_FONT | FLAG_SMALLER_FONT);
  }
}
