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
    SWITCH_PROGRAMMING,
    SWITCH_GREEKMATH,
    CAPS_LOCK,
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
    ARROW_RIGHT,
    BREVE
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
  public static final int FLAG_FAKE_PTR = (1 << 27);

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

  public static KeyValue getKeyByName(String name)
  {
    KeyValue kv = keys.get(name);
    if (kv != null)
      return kv;
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

  private static void addModifierKey(String name, String symbol, Modifier m, int flags)
  {
    if (symbol.length() > 1)
      flags |= FLAG_SMALLER_FONT;
    addKey(name, symbol, KIND_MODIFIER, m.ordinal(),
        FLAG_LATCH | FLAG_SPECIAL | flags);
  }

  private static void addModifierKey(String name, int symbol, Modifier m, int flags)
  {
    addModifierKey(name, String.valueOf((char)symbol), m, flags | FLAG_KEY_FONT);
  }

  private static void addDiacritic(String name, int symbol, Modifier m)
  {
    addModifierKey(name, symbol, m, 0);
  }

  private static void addEventKey(String name, String symbol, Event e, int flags)
  {
    addKey(name, symbol, KIND_EVENT, e.ordinal(), flags | FLAG_SPECIAL);
  }

  private static void addEventKey(String name, int symbol, Event e, int flags)
  {
    addEventKey(name, String.valueOf((char)symbol), e, flags | FLAG_KEY_FONT);
  }

  private static void addKeyeventKey(String name, String symbol, int code, int flags)
  {
    addKey(name, symbol, KIND_KEYEVENT, code, flags);
  }

  private static void addKeyeventKey(String name, int symbol, int code, int flags)
  {
    addKeyeventKey(name, String.valueOf((char)symbol), code, flags | FLAG_KEY_FONT);
  }

  // Within VALUE_BITS
  private static int placeholder_unique_id = 0;

  /** Use a unique id as the value because the symbol is shared between every
      placeholders (it is the empty string). */
  private static void addPlaceholderKey(String name)
  {
    addKey(name, "", KIND_STRING, placeholder_unique_id++, 0);
  }

  static
  {
    addModifierKey("shift", 0x0A, Modifier.SHIFT, 0);
    addModifierKey("ctrl", "Ctrl", Modifier.CTRL, 0);
    addModifierKey("alt", "Alt", Modifier.ALT, 0);
    addDiacritic("accent_aigu", 0x50, Modifier.AIGU);
    addDiacritic("accent_caron", 0x51, Modifier.CARON);
    addDiacritic("accent_cedille", 0x52, Modifier.CEDILLE);
    addDiacritic("accent_circonflexe", 0x53, Modifier.CIRCONFLEXE);
    addDiacritic("accent_grave", 0x54, Modifier.GRAVE);
    addDiacritic("accent_macron", 0x55, Modifier.MACRON);
    addDiacritic("accent_ring", 0x56, Modifier.RING);
    addDiacritic("accent_tilde", 0x57, Modifier.TILDE);
    addDiacritic("accent_trema", 0x58, Modifier.TREMA);
    addDiacritic("accent_ogonek", 0x59, Modifier.OGONEK);
    addDiacritic("accent_dot_above", 0x5A, Modifier.DOT_ABOVE);
    addDiacritic("accent_double_aigu", 0x5B, Modifier.DOUBLE_AIGU);
    addDiacritic("accent_slash", 0x5C, Modifier.SLASH);
    addDiacritic("accent_arrow_right", 0x5D, Modifier.ARROW_RIGHT);
    addDiacritic("accent_breve", 0x5E, Modifier.BREVE);
    addModifierKey("superscript", "Sup", Modifier.SUPERSCRIPT, 0);
    addModifierKey("subscript", "Sub", Modifier.SUBSCRIPT, 0);
    addModifierKey("ordinal", "Ord", Modifier.ORDINAL, 0);
    addModifierKey("arrows", "Arr", Modifier.ARROWS, 0);
    addModifierKey("box", "Box", Modifier.BOX, 0);
    addModifierKey("fn", "Fn", Modifier.FN, 0);
    addModifierKey("meta", "Meta", Modifier.META, 0);

    addEventKey("config", 0x04, Event.CONFIG, FLAG_SMALLER_FONT);
    addEventKey("switch_text", "ABC", Event.SWITCH_TEXT, FLAG_SMALLER_FONT);
    addEventKey("switch_numeric", "123+", Event.SWITCH_NUMERIC, FLAG_SMALLER_FONT);
    addEventKey("switch_emoji", 0x01, Event.SWITCH_EMOJI, FLAG_SMALLER_FONT);
    addEventKey("switch_back_emoji", "ABC", Event.SWITCH_BACK_EMOJI, 0);
    addEventKey("switch_programming", "Prog", Event.SWITCH_PROGRAMMING, FLAG_SMALLER_FONT);
    addEventKey("switch_greekmath", "πλ∇¬", Event.SWITCH_GREEKMATH, FLAG_SMALLER_FONT);
    addEventKey("change_method", 0x09, Event.CHANGE_METHOD, FLAG_SMALLER_FONT);
    addEventKey("action", "Action", Event.ACTION, FLAG_SMALLER_FONT); // Will always be replaced
    addEventKey("capslock", 0x12, Event.CAPS_LOCK, 0);

    addKeyeventKey("esc", "Esc", KeyEvent.KEYCODE_ESCAPE, FLAG_SMALLER_FONT);
    addKeyeventKey("enter", 0x0E, KeyEvent.KEYCODE_ENTER, 0);
    addKeyeventKey("up", 0x05, KeyEvent.KEYCODE_DPAD_UP, FLAG_PRECISE_REPEAT);
    addKeyeventKey("right", 0x06, KeyEvent.KEYCODE_DPAD_RIGHT, FLAG_PRECISE_REPEAT);
    addKeyeventKey("down", 0x07, KeyEvent.KEYCODE_DPAD_DOWN, FLAG_PRECISE_REPEAT);
    addKeyeventKey("left", 0x08, KeyEvent.KEYCODE_DPAD_LEFT, FLAG_PRECISE_REPEAT);
    addKeyeventKey("page_up", 0x02, KeyEvent.KEYCODE_PAGE_UP, 0);
    addKeyeventKey("page_down", 0x03, KeyEvent.KEYCODE_PAGE_DOWN, 0);
    addKeyeventKey("home", 0x0B, KeyEvent.KEYCODE_MOVE_HOME, 0);
    addKeyeventKey("end", 0x0C, KeyEvent.KEYCODE_MOVE_END, 0);
    addKeyeventKey("backspace", 0x11, KeyEvent.KEYCODE_DEL, 0);
    addKeyeventKey("delete", 0x10, KeyEvent.KEYCODE_FORWARD_DEL, 0);
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
    addKeyeventKey("tab", 0x0F, KeyEvent.KEYCODE_TAB, FLAG_SMALLER_FONT);

    addCharKey("\\t", "\\t", '\t', 0); // Send the tab character
    addCharKey("space", "\r", ' ', FLAG_KEY_FONT);
    addCharKey("nbsp", "\u237d", '\u00a0', FLAG_SMALLER_FONT);

    addPlaceholderKey("removed");
    addPlaceholderKey("f11_placeholder");
    addPlaceholderKey("f12_placeholder");
  }

  static final HashMap<String, String> keys_descr = new HashMap<String, String>();

  /* Some keys have a description attached. Return [null] if otherwise. */
  public static String getKeyDescription(String name)
  {
    return keys_descr.get(name);
  }

  static void addKeyDescr(String name, String descr)
  {
    keys_descr.put(name, descr);
  }

  static {
    /* Keys description is shown in the settings. */
    addKeyDescr("capslock", "Caps lock");
    addKeyDescr("switch_greekmath", "Greek & math symbols");
  }

  // Substitute for [assert], which has no effect on Android.
  private static void check(boolean b)
  {
    if (!b)
      throw new RuntimeException("Assertion failure");
  }
}
