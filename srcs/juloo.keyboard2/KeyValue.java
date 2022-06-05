package juloo.keyboard2;

import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import java.util.HashMap;

class KeyValue
{
  /** Values for the [code] field. */

  public static final int EVENT_NONE = -1;
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

  /** Special value for the [_char] field. */
  public static final char CHAR_NONE = '\0';

  // Behavior flags
  public static final int FLAG_LATCH = 1;
  public static final int FLAG_LOCK = (1 << 1);
  // Special keys are not repeated and don't clear latched modifiers
  public static final int FLAG_SPECIAL = (1 << 2);
  public static final int FLAG_MODIFIER = (1 << 3);
  public static final int FLAG_PRECISE_REPEAT = (1 << 4);

  // Rendering flags
  public static final int FLAG_KEY_FONT = (1 << 5);
  public static final int FLAG_SMALLER_FONT = (1 << 6);

  // Internal flags
  public static final int FLAG_LOCKED = (1 << 7);

  // Language specific keys that are removed from the keyboard by default
  public static final int FLAG_LOCALIZED = (1 << 8);

  public final String name;
  private final String _symbol;
  private final char _char;

  /** Describe what the key does when it isn't a simple character.
      Can be one of:
      - When [FLAG_MODIFIER] is set, a modifier. See [KeyModifier].
      - [EVENT_NONE], no event is associated with the key.
      - A positive integer, an Android [KeyEvent].
      - One of the [EVENT_*] constants, an action performed in [KeyEventHandler].
      A key can have both a character and a key event associated, the key event
      is used when certain modifiers are active, the character is used
      otherwise. See [KeyEventHandler]. */
  private final int _code;
  private final int _flags;

  public static enum Kind
  {
    Char, String, Event, Modifier
  }

  public Kind getKind()
  {
    if ((_flags & FLAG_MODIFIER) != 0)
      return Kind.Modifier;
    if (_char != CHAR_NONE)
      return Kind.Char;
    if (_code != EVENT_NONE)
      return Kind.Event;
    return Kind.String;
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
    return _char;
  }

  /** An Android event or one of the [EVENT_*] constants, including
      [EVENT_NONE].
      Defined only when [getKind() == Kind.Char]. */
  public int getCharEvent()
  {
    return _code;
  }

  /** An Android event or one of the [EVENT_*] constants, except [EVENT_NONE].
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
  public KeyValue withCharAndSymbol(char c)
  {
    return withCharAndSymbol(String.valueOf(c), c);
  }

  public KeyValue withCharAndSymbol(String s, char c)
  {
    return new KeyValue(name, s, c, _code, _flags);
  }

  public KeyValue withNameAndSymbol(String n, String s)
  {
    return new KeyValue(n, s, _char, _code, _flags);
  }

  public KeyValue withFlags(int f)
  {
    return new KeyValue(name, _symbol, _char, _code, f);
  }

  private static HashMap<String, KeyValue> keys = new HashMap<String, KeyValue>();

  public KeyValue(String n, String s, char c, int e, int f)
  {
    name = n;
    _symbol = s;
    _char = c;
    _code = e;
    _flags = f;
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
    char c = (name.length() == 1) ? name.charAt(0) : CHAR_NONE;
    return new KeyValue(name, name, c, EVENT_NONE, 0);
  }

  private static void addKey(String name, String symbol, char c, int event, int flags)
  {
    keys.put(name, new KeyValue(name, symbol, c, event, flags));
  }

  private static void addCharKey(char c, int event)
  {
    String name = String.valueOf(c);
    addKey(name, name, c, event, 0);
  }

  private static void addModifierKey(String name, String symbol, int code, int extra_flags)
  {
    assert(code >= 100 && code < 300);
    addKey(name, symbol, CHAR_NONE, code,
        FLAG_LATCH | FLAG_MODIFIER | FLAG_SPECIAL | extra_flags);
  }

  private static void addSpecialKey(String name, String symbol, int event, int flags)
  {
    addKey(name, symbol, CHAR_NONE, event, flags | FLAG_SPECIAL);
  }

  private static void addEventKey(String name, String symbol, int event, int flags)
  {
    addKey(name, symbol, CHAR_NONE, event, flags);
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

    addCharKey('a', KeyEvent.KEYCODE_A);
    addCharKey('b', KeyEvent.KEYCODE_B);
    addCharKey('c', KeyEvent.KEYCODE_C);
    addCharKey('d', KeyEvent.KEYCODE_D);
    addCharKey('e', KeyEvent.KEYCODE_E);
    addCharKey('f', KeyEvent.KEYCODE_F);
    addCharKey('g', KeyEvent.KEYCODE_G);
    addCharKey('h', KeyEvent.KEYCODE_H);
    addCharKey('i', KeyEvent.KEYCODE_I);
    addCharKey('j', KeyEvent.KEYCODE_J);
    addCharKey('k', KeyEvent.KEYCODE_K);
    addCharKey('l', KeyEvent.KEYCODE_L);
    addCharKey('m', KeyEvent.KEYCODE_M);
    addCharKey('n', KeyEvent.KEYCODE_N);
    addCharKey('o', KeyEvent.KEYCODE_O);
    addCharKey('p', KeyEvent.KEYCODE_P);
    addCharKey('q', KeyEvent.KEYCODE_Q);
    addCharKey('r', KeyEvent.KEYCODE_R);
    addCharKey('s', KeyEvent.KEYCODE_S);
    addCharKey('t', KeyEvent.KEYCODE_T);
    addCharKey('u', KeyEvent.KEYCODE_U);
    addCharKey('v', KeyEvent.KEYCODE_V);
    addCharKey('w', KeyEvent.KEYCODE_W);
    addCharKey('x', KeyEvent.KEYCODE_X);
    addCharKey('y', KeyEvent.KEYCODE_Y);
    addCharKey('z', KeyEvent.KEYCODE_Z);
    addCharKey('0', KeyEvent.KEYCODE_0);
    addCharKey('1', KeyEvent.KEYCODE_1);
    addCharKey('2', KeyEvent.KEYCODE_2);
    addCharKey('3', KeyEvent.KEYCODE_3);
    addCharKey('4', KeyEvent.KEYCODE_4);
    addCharKey('5', KeyEvent.KEYCODE_5);
    addCharKey('6', KeyEvent.KEYCODE_6);
    addCharKey('7', KeyEvent.KEYCODE_7);
    addCharKey('8', KeyEvent.KEYCODE_8);
    addCharKey('9', KeyEvent.KEYCODE_9);
    addCharKey('`', KeyEvent.KEYCODE_GRAVE);
    addCharKey('-', KeyEvent.KEYCODE_MINUS);
    addCharKey('=', KeyEvent.KEYCODE_EQUALS);
    addCharKey('[', KeyEvent.KEYCODE_LEFT_BRACKET);
    addCharKey(']', KeyEvent.KEYCODE_RIGHT_BRACKET);
    addCharKey('\\', KeyEvent.KEYCODE_BACKSLASH);
    addCharKey(';', KeyEvent.KEYCODE_SEMICOLON);
    addCharKey('\'', KeyEvent.KEYCODE_APOSTROPHE);
    addCharKey('/', KeyEvent.KEYCODE_SLASH);
    addCharKey('@', KeyEvent.KEYCODE_AT);
    addCharKey('+', KeyEvent.KEYCODE_PLUS);
    addCharKey(',', KeyEvent.KEYCODE_COMMA);
    addCharKey('.', KeyEvent.KEYCODE_PERIOD);
    addCharKey('*', KeyEvent.KEYCODE_STAR);
    addCharKey('#', KeyEvent.KEYCODE_POUND);
    addCharKey('(', KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN);
    addCharKey(')', KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN);

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

    addKey("\\t", "\\t", '\t', EVENT_NONE, 0); // Send the tab character
    addKey("space", "\r", ' ', KeyEvent.KEYCODE_SPACE, FLAG_KEY_FONT);
    addKey("nbsp", "\u237d", '\u00a0', EVENT_NONE, FLAG_KEY_FONT | FLAG_SMALLER_FONT);
  }
}
