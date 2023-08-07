package juloo.keyboard2;

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
    CHANGE_METHOD_PREV,
    ACTION,
    SWITCH_FORWARD,
    SWITCH_BACKWARD,
    SWITCH_GREEKMATH,
    CAPS_LOCK,
    SWITCH_VOICE_TYPING,
  }

  // Must be evaluated in the reverse order of their values.
  public static enum Modifier
  {
    SHIFT,
    CTRL,
    ALT,
    META,
    DOUBLE_AIGU,
    DOT_ABOVE,
    DOT_BELOW,
    GRAVE,
    AIGU,
    CIRCONFLEXE,
    TILDE,
    CEDILLE,
    TREMA,
    HORN,
    HOOK_ABOVE,
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
    BREVE,
    BAR,
    FN, // Must be placed last to be applied first
  }

  public static enum Editing
  {
    COPY,
    PASTE,
    CUT,
    SELECT_ALL,
    PASTE_PLAIN,
    UNDO,
    REDO,
    // Android context menu actions
    REPLACE,
    SHARE,
    ASSIST,
    AUTOFILL,
  }

  public static enum Placeholder
  {
    REMOVED,
    F11,
    F12,
    SHINDOT,
    SINDOT,
    OLE,
    METEG
  }

  public static enum Kind
  {
    Char, String, Keyevent, Event, Modifier, Editing, Placeholder
  }

  // Behavior flags.
  public static final int FLAG_LATCH = (1 << 20);
  public static final int FLAG_LOCK = (1 << 21);
  // Special keys are not repeated and don't clear latched modifiers.
  public static final int FLAG_SPECIAL = (1 << 22);
  // Free flag: (1 << 23);
  // Rendering flags.
  public static final int FLAG_KEY_FONT = (1 << 24); // special font file
  public static final int FLAG_SMALLER_FONT = (1 << 25); // 25% smaller symbols
  public static final int FLAG_SECONDARY = (1 << 26); // dimmer
  // Used by [Pointers].
  public static final int FLAG_LOCKED = (1 << 28);
  public static final int FLAG_FAKE_PTR = (1 << 29);

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

  /** This field encodes three things: Kind, flags and value. */
  private final int _code;

  public Kind getKind()
  {
    return Kind.values()[(_code & KIND_BITS) >>> 29];
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

  /** Defined only when [getKind() == Kind.Editing]. */
  public Editing getEditing()
  {
    return Editing.values()[(_code & VALUE_BITS)];
  }

  public Placeholder getPlaceholder()
  {
    return Placeholder.values()[(_code & VALUE_BITS)];
  }

  /* Update the char and the symbol. */
  public KeyValue withChar(char c)
  {
    return new KeyValue(String.valueOf(c), Kind.Char, c, getFlags());
  }

  public KeyValue withString(String s)
  {
    return new KeyValue(s, Kind.String, 0, getFlags());
  }

  public KeyValue withSymbol(String s)
  {
    return new KeyValue(s, (_code & KIND_BITS), (_code & VALUE_BITS), getFlags());
  }

  public KeyValue withKeyevent(int code)
  {
    return new KeyValue(_symbol, Kind.Keyevent, code, getFlags());
  }

  public KeyValue withFlags(int f)
  {
    return new KeyValue(_symbol, (_code & KIND_BITS), (_code & VALUE_BITS), f);
  }

  @Override
  public boolean equals(Object obj)
  {
    return sameKey((KeyValue)obj);
  }

  /** Type-safe alternative to [equals]. */
  public boolean sameKey(KeyValue snd)
  {
    if (snd == null)
      return false;
    return _symbol.equals(snd._symbol) && _code == snd._code;
  }

  @Override
  public int hashCode()
  {
    return _symbol.hashCode() + _code;
  }

  public KeyValue(String s, int kind, int value, int flags)
  {
    check((kind & ~KIND_BITS) == 0);
    check((flags & ~FLAGS_BITS) == 0);
    check((value & ~VALUE_BITS) == 0);
    _symbol = s;
    _code = kind | flags | value;
  }

  public KeyValue(String s, Kind k, int v, int f)
  {
    this(s, (k.ordinal() << 29), v, f);
  }

  private static KeyValue charKey(String symbol, char c, int flags)
  {
    return new KeyValue(symbol, Kind.Char, c, flags);
  }

  private static KeyValue modifierKey(String symbol, Modifier m, int flags)
  {
    if (symbol.length() > 1)
      flags |= FLAG_SMALLER_FONT;
    return new KeyValue(symbol, Kind.Modifier, m.ordinal(),
        FLAG_LATCH | FLAG_SPECIAL | FLAG_SECONDARY | flags);
  }

  private static KeyValue modifierKey(int symbol, Modifier m, int flags)
  {
    return modifierKey(String.valueOf((char)symbol), m, flags | FLAG_KEY_FONT);
  }

  private static KeyValue diacritic(int symbol, Modifier m)
  {
    return new KeyValue(String.valueOf((char)symbol), Kind.Modifier, m.ordinal(),
        FLAG_LATCH | FLAG_SPECIAL | FLAG_KEY_FONT);
  }

  private static KeyValue eventKey(String symbol, Event e, int flags)
  {
    return new KeyValue(symbol, Kind.Event, e.ordinal(), flags | FLAG_SPECIAL | FLAG_SECONDARY);
  }

  private static KeyValue eventKey(int symbol, Event e, int flags)
  {
    return eventKey(String.valueOf((char)symbol), e, flags | FLAG_KEY_FONT);
  }

  private static KeyValue keyeventKey(String symbol, int code, int flags)
  {
    return new KeyValue(symbol, Kind.Keyevent, code, flags | FLAG_SECONDARY);
  }

  private static KeyValue keyeventKey(int symbol, int code, int flags)
  {
    return keyeventKey(String.valueOf((char)symbol), code, flags | FLAG_KEY_FONT);
  }

  private static KeyValue editingKey(String symbol, Editing action, int flags)
  {
    return new KeyValue(symbol, Kind.Editing, action.ordinal(),
        flags | FLAG_SPECIAL | FLAG_SECONDARY);
  }

  private static KeyValue editingKey(String symbol, Editing action)
  {
    return editingKey(symbol, action, FLAG_SMALLER_FONT);
  }

  private static KeyValue editingKey(int symbol, Editing action)
  {
    return editingKey(String.valueOf((char)symbol), action, FLAG_KEY_FONT);
  }

  /** A key that do nothing but has a unique ID. */
  private static KeyValue placeholderKey(Placeholder id)
  {
    return new KeyValue("", Kind.Placeholder, id.ordinal(), 0);
  }

  /** Make a key that types a string. */
  public static KeyValue makeStringKey(String str)
  {
    if (str.length() == 1)
      return new KeyValue(str, Kind.Char, str.charAt(0), 0);
    else
      return new KeyValue(str, Kind.String, 0, FLAG_SMALLER_FONT);
  }

  public static KeyValue getKeyByName(String name)
  {
    switch (name)
    {
      /* These symbols have special meaning when in `res/xml` and are escaped in
         standard layouts. The backslash is not stripped when parsed from the
         custom layout option. */
      case "\\?": return makeStringKey("?");
      case "\\#": return makeStringKey("#");
      case "\\@": return makeStringKey("@");
      case "\\\\": return makeStringKey("\\");

      case "shift": return modifierKey(0xE00A, Modifier.SHIFT, 0);
      case "ctrl": return modifierKey("Ctrl", Modifier.CTRL, 0);
      case "alt": return modifierKey("Alt", Modifier.ALT, 0);
      case "accent_aigu": return diacritic(0xE050, Modifier.AIGU);
      case "accent_caron": return diacritic(0xE051, Modifier.CARON);
      case "accent_cedille": return diacritic(0xE052, Modifier.CEDILLE);
      case "accent_circonflexe": return diacritic(0xE053, Modifier.CIRCONFLEXE);
      case "accent_grave": return diacritic(0xE054, Modifier.GRAVE);
      case "accent_macron": return diacritic(0xE055, Modifier.MACRON);
      case "accent_ring": return diacritic(0xE056, Modifier.RING);
      case "accent_tilde": return diacritic(0xE057, Modifier.TILDE);
      case "accent_trema": return diacritic(0xE058, Modifier.TREMA);
      case "accent_ogonek": return diacritic(0xE059, Modifier.OGONEK);
      case "accent_dot_above": return diacritic(0xE05A, Modifier.DOT_ABOVE);
      case "accent_double_aigu": return diacritic(0xE05B, Modifier.DOUBLE_AIGU);
      case "accent_slash": return diacritic(0xE05C, Modifier.SLASH);
      case "accent_arrow_right": return diacritic(0xE05D, Modifier.ARROW_RIGHT);
      case "accent_breve": return diacritic(0xE05E, Modifier.BREVE);
      case "accent_bar": return diacritic(0xE05F, Modifier.BAR);
      case "accent_dot_below": return diacritic(0xE060, Modifier.DOT_BELOW);
      case "accent_horn": return diacritic(0xE061, Modifier.HORN);
      case "accent_hook_above": return diacritic(0xE062, Modifier.HOOK_ABOVE);
      case "superscript": return modifierKey("Sup", Modifier.SUPERSCRIPT, 0);
      case "subscript": return modifierKey("Sub", Modifier.SUBSCRIPT, 0);
      case "ordinal": return modifierKey("Ord", Modifier.ORDINAL, 0);
      case "arrows": return modifierKey("Arr", Modifier.ARROWS, 0);
      case "box": return modifierKey("Box", Modifier.BOX, 0);
      case "fn": return modifierKey("Fn", Modifier.FN, 0);
      case "meta": return modifierKey("Meta", Modifier.META, 0);

      case "config": return eventKey(0xE004, Event.CONFIG, FLAG_SMALLER_FONT);
      case "switch_text": return eventKey("ABC", Event.SWITCH_TEXT, FLAG_SMALLER_FONT);
      case "switch_numeric": return eventKey("123+", Event.SWITCH_NUMERIC, FLAG_SMALLER_FONT);
      case "switch_emoji": return eventKey(0xE001, Event.SWITCH_EMOJI, FLAG_SMALLER_FONT);
      case "switch_back_emoji": return eventKey("ABC", Event.SWITCH_BACK_EMOJI, 0);
      case "switch_forward": return eventKey(0xE013, Event.SWITCH_FORWARD, FLAG_SMALLER_FONT);
      case "switch_backward": return eventKey(0xE014, Event.SWITCH_BACKWARD, FLAG_SMALLER_FONT);
      case "switch_greekmath": return eventKey("πλ∇¬", Event.SWITCH_GREEKMATH, FLAG_SMALLER_FONT);
      case "change_method": return eventKey(0xE009, Event.CHANGE_METHOD, FLAG_SMALLER_FONT);
      case "change_method_prev": return eventKey(0xE009, Event.CHANGE_METHOD_PREV, FLAG_SMALLER_FONT);
      case "action": return eventKey("Action", Event.ACTION, FLAG_SMALLER_FONT); // Will always be replaced
      case "capslock": return eventKey(0xE012, Event.CAPS_LOCK, 0);
      case "voice_typing": return eventKey(0xE015, Event.SWITCH_VOICE_TYPING, FLAG_SMALLER_FONT);

      case "esc": return keyeventKey("Esc", KeyEvent.KEYCODE_ESCAPE, FLAG_SMALLER_FONT);
      case "enter": return keyeventKey(0xE00E, KeyEvent.KEYCODE_ENTER, 0);
      case "up": return keyeventKey(0xE005, KeyEvent.KEYCODE_DPAD_UP, 0);
      case "right": return keyeventKey(0xE006, KeyEvent.KEYCODE_DPAD_RIGHT, 0);
      case "down": return keyeventKey(0xE007, KeyEvent.KEYCODE_DPAD_DOWN, 0);
      case "left": return keyeventKey(0xE008, KeyEvent.KEYCODE_DPAD_LEFT, 0);
      case "page_up": return keyeventKey(0xE002, KeyEvent.KEYCODE_PAGE_UP, 0);
      case "page_down": return keyeventKey(0xE003, KeyEvent.KEYCODE_PAGE_DOWN, 0);
      case "home": return keyeventKey(0xE00B, KeyEvent.KEYCODE_MOVE_HOME, 0);
      case "end": return keyeventKey(0xE00C, KeyEvent.KEYCODE_MOVE_END, 0);
      case "backspace": return keyeventKey(0xE011, KeyEvent.KEYCODE_DEL, 0);
      case "delete": return keyeventKey(0xE010, KeyEvent.KEYCODE_FORWARD_DEL, 0);
      case "insert": return keyeventKey("Ins", KeyEvent.KEYCODE_INSERT, FLAG_SMALLER_FONT);
      case "f1": return keyeventKey("F1", KeyEvent.KEYCODE_F1, 0);
      case "f2": return keyeventKey("F2", KeyEvent.KEYCODE_F2, 0);
      case "f3": return keyeventKey("F3", KeyEvent.KEYCODE_F3, 0);
      case "f4": return keyeventKey("F4", KeyEvent.KEYCODE_F4, 0);
      case "f5": return keyeventKey("F5", KeyEvent.KEYCODE_F5, 0);
      case "f6": return keyeventKey("F6", KeyEvent.KEYCODE_F6, 0);
      case "f7": return keyeventKey("F7", KeyEvent.KEYCODE_F7, 0);
      case "f8": return keyeventKey("F8", KeyEvent.KEYCODE_F8, 0);
      case "f9": return keyeventKey("F9", KeyEvent.KEYCODE_F9, 0);
      case "f10": return keyeventKey("F10", KeyEvent.KEYCODE_F10, 0);
      case "f11": return keyeventKey("F11", KeyEvent.KEYCODE_F11, FLAG_SMALLER_FONT);
      case "f12": return keyeventKey("F12", KeyEvent.KEYCODE_F12, FLAG_SMALLER_FONT);
      case "tab": return keyeventKey(0xE00F, KeyEvent.KEYCODE_TAB, FLAG_SMALLER_FONT);

      case "\\t": return charKey("\\t", '\t', 0); // Send the tab character
      case "space": return charKey("\r", ' ', FLAG_KEY_FONT | FLAG_SECONDARY);
      case "nbsp": return charKey("\u237d", '\u00a0', FLAG_SMALLER_FONT);

      /* bidi */
      case "lrm": return charKey("↱", '\u200e', 0); // Send left-to-right mark
      case "rlm": return charKey("↰", '\u200f', 0); // Send right-to-left mark
      case "b(": return charKey("(", ')', 0);
      case "b)": return charKey(")", '(', 0);
      case "b[": return charKey("[", ']', 0);
      case "b]": return charKey("]", '[', 0);
      case "b{": return charKey("{", '}', 0);
      case "b}": return charKey("}", '{', 0);
      case "blt": return charKey("<", '>', 0);
      case "bgt": return charKey(">", '<', 0);

      case "removed": return placeholderKey(Placeholder.REMOVED);
      case "f11_placeholder": return placeholderKey(Placeholder.F11);
      case "f12_placeholder": return placeholderKey(Placeholder.F12);

      /* hebrew niqqud */
      case "qamats": return charKey("\u05E7\u05B8", '\u05B8', 0); // kamatz
      case "patah": return charKey("\u05E4\u05B7", '\u05B7', 0); // patach
      case "sheva": return charKey("\u05E9\u05B0", '\u05B0', 0);
      case "dagesh": return charKey("\u05D3\u05BC", '\u05BC', 0); // or mapiq
      case "hiriq": return charKey("\u05D7\u05B4", '\u05B4', 0);
      case "segol": return charKey("\u05E1\u05B6", '\u05B6', 0);
      case "tsere": return charKey("\u05E6\u05B5", '\u05B5', 0);
      case "holam": return charKey("\u05D5\u05B9", '\u05B9', 0);
      case "qubuts": return charKey("\u05E7\u05BB", '\u05BB', 0); // kubuts
      case "hataf_patah": return charKey("\u05D7\u05B2\u05E4\u05B7", '\u05B2', 0); // reduced patach
      case "hataf_qamats": return charKey("\u05D7\u05B3\u05E7\u05B8", '\u05B3', 0); // reduced kamatz
      case "hataf_segol": return charKey("\u05D7\u05B1\u05E1\u05B6", '\u05B1', 0); // reduced segol
      case "shindot": return charKey("\u05E9\u05C1", '\u05C1', 0);
      case "shindot_placeholder": return placeholderKey(Placeholder.SHINDOT);
      case "sindot": return charKey("\u05E9\u05C2", '\u05C2', 0);
      case "sindot_placeholder": return placeholderKey(Placeholder.SINDOT);
      /* hebrew punctuation */
      case "geresh": return charKey("\u05F3", '\u05F3', 0);
      case "gershayim": return charKey("\u05F4", '\u05F4', 0);
      case "maqaf": return charKey("\u05BE", '\u05BE', 0);
      /* hebrew biblical */
      case "rafe": return charKey("\u05E4\u05BF", '\u05BF', 0);
      case "ole": return charKey("\u05E2\u05AB", '\u05AB', 0);
      case "ole_placeholder": return placeholderKey(Placeholder.OLE);
      case "meteg": return charKey("\u05DE\u05BD", '\u05BD', 0); // or siluq or sof-pasuq
      case "meteg_placeholder": return placeholderKey(Placeholder.METEG);
      /* intending/preventing ligature - supported by many scripts*/
      case "zwj": return charKey("zwj", '\u200D', 0); // zero-width joiner (provides ligature)
      case "zwnj": return charKey("zwnj", '\u200C', 0); // zero-width non joiner (prevents unintended ligature)

      case "copy": return editingKey(0xE030, Editing.COPY);
      case "paste": return editingKey(0xE032, Editing.PASTE);
      case "cut": return editingKey(0xE031, Editing.CUT);
      case "selectAll": return editingKey(0xE033, Editing.SELECT_ALL);
      case "shareText": return editingKey(0xE034, Editing.SHARE);
      case "pasteAsPlainText": return editingKey(0xE035, Editing.PASTE_PLAIN);
      case "undo": return editingKey(0xE036, Editing.UNDO);
      case "redo": return editingKey(0xE037, Editing.REDO);
      case "replaceText": return editingKey("repl", Editing.REPLACE);
      case "textAssist": return editingKey(0xE038, Editing.ASSIST);
      case "autofill": return editingKey("auto", Editing.AUTOFILL);
      default: return makeStringKey(name);
    }
  }

  // Substitute for [assert], which has no effect on Android.
  private static void check(boolean b)
  {
    if (!b)
      throw new RuntimeException("Assertion failure");
  }
}
