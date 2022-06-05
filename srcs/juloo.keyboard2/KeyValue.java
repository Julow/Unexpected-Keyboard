package juloo.keyboard2;

import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import java.util.HashMap;

final class KeyValue
{
  public static enum Event
  {
    CONFIG,
    SWITCH_TEXT,
    SWITCH_NUMERIC,
    SWITCH_EMOJI,
    SWITCH_BACK_EMOJI,
    CHANGE_METHOD,
    ACTION,
    SWITCH_PROGRAMMING
  }

  // Must be evaluated in the reverse order of their values.
  public static enum Modifier
  {
    SHIFT,
    FN,
    CTRL,
    ALT,
    META,
    DOUBLE_AIGU,
    DOT_ABOVE,
    GRAVE,
    AIGU,
    CIRCONFLEXE,
    TILDE,
    CEDILLE,
    TREMA,
    SUPERSCRIPT,
    SUBSCRIPT,
    RING,
    CARON,
    MACRON,
    ORDINAL,
    ARROWS,
    BOX,
    OGONEK,
    SLASH,
    ARROW_RIGHT
  }

  // Behavior flags.
  public static final int FLAG_LATCH = (1 << 20);
  public static final int FLAG_LOCK = (1 << 21);
  // Special keys are not repeated and don't clear latched modifiers.
  public static final int FLAG_SPECIAL = (1 << 22);
  public static final int FLAG_PRECISE_REPEAT = (1 << 23);
  // Rendering flags.
  public static final int FLAG_KEY_FONT = (1 << 24);
  public static final int FLAG_SMALLER_FONT = (1 << 25);
  // Used by [Pointers].
  public static final int FLAG_LOCKED = (1 << 26);
  // Language specific keys that are removed from the keyboard by default.
  public static final int FLAG_LOCALIZED = (1 << 27);

  // Kinds
  public static final int KIND_CHAR = (0 << 29);
  public static final int KIND_STRING = (1 << 29);
  public static final int KIND_KEYEVENT = (2 << 29);
  public static final int KIND_EVENT = (3 << 29);
  public static final int KIND_MODIFIER = (4 << 29);

  // Ranges for the different components
  private static final int FLAGS_BITS = (0b111111111 << 20); // 9 bits wide
  private static final int KIND_BITS = (0b111 << 29); // 3 bits wide
  private static final int VALUE_BITS = ~(FLAGS_BITS | KIND_BITS); // 20 bits wide
  static
  {
    check((FLAGS_BITS & KIND_BITS) == 0); // No overlap
    check((FLAGS_BITS | KIND_BITS | VALUE_BITS) == ~0); // No holes
  }

  private final String _symbol;

  /** This field encodes three things:
      - The kind
      - The flags
      - The value for Char, Event and Modifier keys.
      */
  private final int _code;

  public static enum Kind
  {
    Char, String, Keyevent, Event, Modifier
  }

  public Kind getKind()
  {
    switch (_code & KIND_BITS)
    {
      case KIND_CHAR: return Kind.Char;
      case KIND_STRING: return Kind.String;
      case KIND_KEYEVENT: return Kind.Keyevent;
      case KIND_EVENT: return Kind.Event;
      case KIND_MODIFIER: return Kind.Modifier;
      default: throw new RuntimeException("Corrupted kind flags");
    }
  }

  public int getFlags()
  {
    return (_code & FLAGS_BITS);
  }

  public boolean hasFlags(int has)
  {
    return ((_code & has) == has);
  }

  /** The string to render on the keyboard.
      When [getKind() == Kind.String], also the string to send. */
  public String getString()
  {
    return _symbol;
  }

  /** Defined only when [getKind() == Kind.Char]. */
  public char getChar()
  {
    return (char)(_code & VALUE_BITS);
  }

  /** Defined only when [getKind() == Kind.Keyevent]. */
  public int getKeyevent()
  {
    return (_code & VALUE_BITS);
  }

  /** Defined only when [getKind() == Kind.Event]. */
  public Event getEvent()
  {
    return Event.values()[(_code & VALUE_BITS)];
  }

  /** Defined only when [getKind() == Kind.Modifier]. */
  public Modifier getModifier()
  {
    return Modifier.values()[(_code & VALUE_BITS)];
  }

  /* Update the char and the symbol. */
  public KeyValue withChar(char c)
  {
    return new KeyValue(String.valueOf(c), KIND_CHAR, c, getFlags());
  }

  public KeyValue withString(String s)
  {
    return new KeyValue(s, KIND_STRING, 0, getFlags());
  }

  public KeyValue withSymbol(String s)
  {
    return new KeyValue(s, (_code & KIND_BITS), (_code & VALUE_BITS), getFlags());
  }

  public KeyValue withKeyevent(int code)
  {
    return new KeyValue(_symbol, KIND_KEYEVENT, code, getFlags());
  }

  public KeyValue withFlags(int f)
  {
    return new KeyValue(_symbol, (_code & KIND_BITS), (_code & VALUE_BITS), f);
  }

  @Override
  public boolean equals(Object obj)
  {
    KeyValue snd = (KeyValue)obj;
    return _symbol.equals(snd._symbol) && _code == snd._code;
  }

  @Override
  public int hashCode()
  {
    return _symbol.hashCode() + _code;
  }

  private static HashMap<String, KeyValue> keys = new HashMap<String, KeyValue>();

  public KeyValue(String s, int kind, int value, int flags)
  {
    check((kind & ~KIND_BITS) == 0);
    check((flags & ~FLAGS_BITS) == 0);
    check((value & ~VALUE_BITS) == 0);
    _symbol = s;
    _code = kind | flags | value;
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
      return kv.withFlags(kv.getFlags() | FLAG_LOCALIZED);
    }
    if (name.length() == 1)
      return new KeyValue(name, KIND_CHAR, name.charAt(0), 0);
    else
      return new KeyValue(name, KIND_STRING, 0, 0);
  }

  private static void addKey(String name, String symbol, int kind, int code, int flags)
  {
    keys.put(name, new KeyValue(symbol, kind, code, flags));
  }

  private static void addCharKey(String name, String symbol, char c, int flags)
  {
    addKey(name, symbol, KIND_CHAR, c, flags);
  }

  private static void addModifierKey(String name, String symbol, Modifier m, int extra_flags)
  {
    addKey(name, symbol, KIND_MODIFIER, m.ordinal(),
        FLAG_LATCH | FLAG_SPECIAL | extra_flags);
  }

  private static void addEventKey(String name, String symbol, Event e, int flags)
  {
    addKey(name, symbol, KIND_EVENT, e.ordinal(), flags | FLAG_SPECIAL);
  }

  private static void addKeyeventKey(String name, String symbol, int code, int flags)
  {
    addKey(name, symbol, KIND_KEYEVENT, code, flags);
  }

  private static void addPlaceholderKey(String name)
  {
    addKey(name, "", KIND_STRING, 0, 0);
  }

  static
  {
    addModifierKey("shift", "\n", // Can't write u000A because Java is stupid
        Modifier.SHIFT, FLAG_KEY_FONT | FLAG_SMALLER_FONT);
    addModifierKey("ctrl", "Ctrl", Modifier.CTRL, FLAG_SMALLER_FONT);
    addModifierKey("alt", "Alt", Modifier.ALT, FLAG_SMALLER_FONT);
    addModifierKey("accent_aigu", "\u0050", Modifier.AIGU, FLAG_KEY_FONT);
    addModifierKey("accent_caron", "\u0051", Modifier.CARON, FLAG_KEY_FONT);
    addModifierKey("accent_cedille", "\u0052", Modifier.CEDILLE, FLAG_KEY_FONT);
    addModifierKey("accent_circonflexe", "\u0053", Modifier.CIRCONFLEXE, FLAG_KEY_FONT);
    addModifierKey("accent_grave", "\u0054", Modifier.GRAVE, FLAG_KEY_FONT);
    addModifierKey("accent_macron", "\u0055", Modifier.MACRON, FLAG_KEY_FONT);
    addModifierKey("accent_ring", "\u0056", Modifier.RING, FLAG_KEY_FONT);
    addModifierKey("accent_tilde", "\u0057", Modifier.TILDE, FLAG_KEY_FONT);
    addModifierKey("accent_trema", "\u0058", Modifier.TREMA, FLAG_KEY_FONT);
    addModifierKey("accent_ogonek", "\u0059", Modifier.OGONEK, FLAG_KEY_FONT);
    addModifierKey("accent_dot_above", "\u005a", Modifier.DOT_ABOVE, FLAG_KEY_FONT);
    addModifierKey("accent_double_aigu", "\u005b", Modifier.DOUBLE_AIGU, FLAG_KEY_FONT);
    addModifierKey("accent_slash", "\134", // Can't write u005c
        Modifier.SLASH, FLAG_KEY_FONT);
    addModifierKey("accent_arrow_right", "\u005d", Modifier.ARROW_RIGHT, FLAG_KEY_FONT);
    addModifierKey("superscript", "Sup", Modifier.SUPERSCRIPT, FLAG_SMALLER_FONT);
    addModifierKey("subscript", "Sub", Modifier.SUBSCRIPT, FLAG_SMALLER_FONT);
    addModifierKey("ordinal", "Ord", Modifier.ORDINAL, FLAG_SMALLER_FONT);
    addModifierKey("arrows", "Arr", Modifier.ARROWS, FLAG_SMALLER_FONT);
    addModifierKey("box", "Box", Modifier.BOX, FLAG_SMALLER_FONT);
    addModifierKey("fn", "Fn", Modifier.FN, FLAG_SMALLER_FONT);
    addModifierKey("meta", "Meta", Modifier.META, FLAG_SMALLER_FONT);

    addEventKey("config", "\u0004", Event.CONFIG, FLAG_KEY_FONT | FLAG_SMALLER_FONT);
    addEventKey("switch_text", "ABC", Event.SWITCH_TEXT, FLAG_SMALLER_FONT);
    addEventKey("switch_numeric", "123+", Event.SWITCH_NUMERIC, FLAG_SMALLER_FONT);
    addEventKey("switch_emoji", "\u0001" , Event.SWITCH_EMOJI, FLAG_KEY_FONT | FLAG_SMALLER_FONT);
    addEventKey("switch_back_emoji", "ABC", Event.SWITCH_BACK_EMOJI, 0);
    addEventKey("switch_programming", "Prog", Event.SWITCH_PROGRAMMING, FLAG_SMALLER_FONT);
    addEventKey("change_method", "\u0009", Event.CHANGE_METHOD, FLAG_KEY_FONT | FLAG_SMALLER_FONT);
    addEventKey("action", "Action", Event.ACTION, FLAG_SMALLER_FONT); // Will always be replaced

    addKeyeventKey("esc", "Esc", KeyEvent.KEYCODE_ESCAPE, FLAG_SMALLER_FONT);
    addKeyeventKey("enter", "\u000E", KeyEvent.KEYCODE_ENTER, FLAG_KEY_FONT);
    addKeyeventKey("up", "\u0005", KeyEvent.KEYCODE_DPAD_UP, FLAG_KEY_FONT | FLAG_PRECISE_REPEAT);
    addKeyeventKey("right", "\u0006", KeyEvent.KEYCODE_DPAD_RIGHT, FLAG_KEY_FONT | FLAG_PRECISE_REPEAT);
    addKeyeventKey("down", "\u0007", KeyEvent.KEYCODE_DPAD_DOWN, FLAG_KEY_FONT | FLAG_PRECISE_REPEAT);
    addKeyeventKey("left", "\u0008", KeyEvent.KEYCODE_DPAD_LEFT, FLAG_KEY_FONT | FLAG_PRECISE_REPEAT);
    addKeyeventKey("page_up", "\u0002", KeyEvent.KEYCODE_PAGE_UP, FLAG_KEY_FONT);
    addKeyeventKey("page_down", "\u0003", KeyEvent.KEYCODE_PAGE_DOWN, FLAG_KEY_FONT);
    addKeyeventKey("home", "\u000B", KeyEvent.KEYCODE_MOVE_HOME, FLAG_KEY_FONT);
    addKeyeventKey("end", "\u000C", KeyEvent.KEYCODE_MOVE_END, FLAG_KEY_FONT);
    addKeyeventKey("backspace", "\u0011", KeyEvent.KEYCODE_DEL, FLAG_KEY_FONT);
    addKeyeventKey("delete", "\u0010", KeyEvent.KEYCODE_FORWARD_DEL, FLAG_KEY_FONT);
    addKeyeventKey("insert", "Ins", KeyEvent.KEYCODE_INSERT, FLAG_SMALLER_FONT);
    addKeyeventKey("f1", "F1", KeyEvent.KEYCODE_F1, 0);
    addKeyeventKey("f2", "F2", KeyEvent.KEYCODE_F2, 0);
    addKeyeventKey("f3", "F3", KeyEvent.KEYCODE_F3, 0);
    addKeyeventKey("f4", "F4", KeyEvent.KEYCODE_F4, 0);
    addKeyeventKey("f5", "F5", KeyEvent.KEYCODE_F5, 0);
    addKeyeventKey("f6", "F6", KeyEvent.KEYCODE_F6, 0);
    addKeyeventKey("f7", "F7", KeyEvent.KEYCODE_F7, 0);
    addKeyeventKey("f8", "F8", KeyEvent.KEYCODE_F8, 0);
    addKeyeventKey("f9", "F9", KeyEvent.KEYCODE_F9, 0);
    addKeyeventKey("f10", "F10", KeyEvent.KEYCODE_F10, 0);
    addKeyeventKey("f11", "F11", KeyEvent.KEYCODE_F11, FLAG_SMALLER_FONT);
    addKeyeventKey("f12", "F12", KeyEvent.KEYCODE_F12, FLAG_SMALLER_FONT);
    addKeyeventKey("tab", "\u000F", KeyEvent.KEYCODE_TAB, FLAG_KEY_FONT | FLAG_SMALLER_FONT);

    addCharKey("\\t", "\\t", '\t', 0); // Send the tab character
    addCharKey("space", "\r", ' ', FLAG_KEY_FONT);
    addCharKey("nbsp", "\u237d", '\u00a0', FLAG_KEY_FONT | FLAG_SMALLER_FONT);

    addPlaceholderKey("removed");
    addPlaceholderKey("f11_placeholder");
    addPlaceholderKey("f12_placeholder");
  }

  // Substitute for [assert], which has no effect on Android.
  private static void check(boolean b)
  {
    if (!b)
      throw new RuntimeException("Assertion failure");
  }
}
