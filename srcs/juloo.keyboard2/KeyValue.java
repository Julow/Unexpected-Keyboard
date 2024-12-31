package juloo.keyboard2;

import android.view.KeyEvent;

public final class KeyValue implements Comparable<KeyValue>
{
  public static enum Event
  {
    CONFIG,
    SWITCH_TEXT,
    SWITCH_NUMERIC,
    SWITCH_EMOJI,
    SWITCH_BACK_EMOJI,
    SWITCH_CLIPBOARD,
    SWITCH_BACK_CLIPBOARD,
    CHANGE_METHOD_PICKER,
    CHANGE_METHOD_AUTO,
    ACTION,
    SWITCH_FORWARD,
    SWITCH_BACKWARD,
    SWITCH_GREEKMATH,
    CAPS_LOCK,
    SWITCH_VOICE_TYPING,
    SWITCH_VOICE_TYPING_CHOOSER,
    KEY_CODE,
  }

  // Must be evaluated in the reverse order of their values.
  public static enum Modifier
  {
    SHIFT,
    GESTURE,
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
    FN,
  } // Last is be applied first

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
    Char, String, Keyevent, Event, Compose_pending, Hangul_initial,
    Hangul_medial, Modifier, Editing, Placeholder,
    Cursor_move // Value is encoded as a 16-bit integer
  }

  private static final int FLAGS_OFFSET = 19;
  private static final int KIND_OFFSET = 28;

  // Behavior flags.
  public static final int FLAG_LATCH = (1 << FLAGS_OFFSET << 0);
  // Key can be locked by typing twice
  public static final int FLAG_LOCK = (1 << FLAGS_OFFSET << 1);
  // Special keys are not repeated and don't clear latched modifiers.
  public static final int FLAG_SPECIAL = (1 << FLAGS_OFFSET << 2);
  // Whether the symbol should be greyed out. For example, keys that are not
  // part of the pending compose sequence.
  public static final int FLAG_GREYED = (1 << FLAGS_OFFSET << 3);
  // Rendering flags.
  public static final int FLAG_KEY_FONT = (1 << FLAGS_OFFSET << 4); // special font file
  public static final int FLAG_SECONDARY = (1 << FLAGS_OFFSET << 6); // dimmer
  public static final int FLAG_SMALLER_FONT = (1 << FLAGS_OFFSET << 5); // 25% smaller symbols
  // Used by [Pointers].
  // Free: (1 << FLAGS_OFFSET << 7)
  // Free: (1 << FLAGS_OFFSET << 8)

  // Ranges for the different components
  private static final int FLAGS_BITS =
    FLAG_LATCH | FLAG_LOCK | FLAG_SPECIAL | FLAG_GREYED | FLAG_KEY_FONT |
    FLAG_SMALLER_FONT | FLAG_SECONDARY;
  private static final int KIND_BITS = (0b1111 << KIND_OFFSET); // 4 bits wide
  private static final int VALUE_BITS = ~(FLAGS_BITS | KIND_BITS); // 20 bits wide

  static
  {
    check((FLAGS_BITS & KIND_BITS) == 0); // No overlap
    check((FLAGS_BITS | KIND_BITS | VALUE_BITS) == ~0); // No holes
    // No kind is out of range
    check((((Kind.values().length - 1) << KIND_OFFSET) & ~KIND_BITS) == 0);
  }

  private final String _symbol;
  private final boolean _long_name;

  /** This field encodes three things: Kind, flags and value. */
  private final int _code;

  public Kind getKind()
  {
    return Kind.values()[(_code & KIND_BITS) >>> KIND_OFFSET];
  }

  public int getFlags()
  {
    return (_code & FLAGS_BITS);
  }

  public boolean hasFlagsAny(int has)
  {
    return ((_code & has) != 0);
  }
  public boolean is_long_name()
  {
    return this._long_name;
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

  /** Defined only when [getKind() == Kind.Placeholder]. */
  public Placeholder getPlaceholder()
  {
    return Placeholder.values()[(_code & VALUE_BITS)];
  }

  /** Defined only when [getKind() == Kind.Compose_pending]. */
  public int getPendingCompose()
  {
    return (_code & VALUE_BITS);
  }

  /** Defined only when [getKind()] is [Kind.Hangul_initial] or
      [Kind.Hangul_medial]. */
  public int getHangulPrecomposed()
  {
    return (_code & VALUE_BITS);
  }

  /** Defined only when [getKind() == Kind.Cursor_move]. */
  public short getCursorMove()
  {
    return (short)(_code & VALUE_BITS);
  }

  /* Update the char and the symbol. */
  public KeyValue withChar(char c)
  {
    return new KeyValue(String.valueOf(c), Kind.Char, c, getFlags());
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

  public int compareTo(KeyValue snd)
  {
    // Compare the kind and value first, then the flags.
    int d = (_code & ~FLAGS_BITS) - (snd._code & ~FLAGS_BITS);
    if (d != 0)
      return d;
    d = _code - snd._code;
    if (d != 0)
      return d;
    return _symbol.compareTo(snd._symbol);
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
    _symbol = s;
    _code = (kind & KIND_BITS) | (flags & FLAGS_BITS) | (value & VALUE_BITS);
    _long_name = false;
  }
  public KeyValue(String s, int kind, int value, int flags, boolean long_name)
  {
    _symbol = s;
    _code = (kind & KIND_BITS) | (flags & FLAGS_BITS) | (value & VALUE_BITS);
    _long_name = long_name;
  }

  public KeyValue(String s, Kind k, int v, int f)
  {
    this(s, (k.ordinal() << KIND_OFFSET), v, f);
  }
  public KeyValue(String s, Kind k, int v, int f,boolean long_name)
  {
    this(s, (k.ordinal() << KIND_OFFSET), v, f,long_name);
  }

  private static KeyValue charKey(String symbol, char c, int flags)
  {
    return new KeyValue(symbol, Kind.Char, c, flags);
  }

  private static KeyValue charKey(int symbol, char c, int flags)
  {
    return charKey(String.valueOf((char)symbol), c, flags);
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

  /** A key that moves the cursor [d] times to the right. If [d] is negative,
      it moves the cursor [abs(d)] times to the left. */
  public static KeyValue cursorMoveKey(int d)
  {
    int symbol = (d < 0) ? 0xE008 : 0xE006;
    return new KeyValue(String.valueOf((char)symbol), Kind.Cursor_move,
        ((short)d) & 0xFFFF,
        FLAG_SPECIAL | FLAG_SECONDARY | FLAG_KEY_FONT);
  }

  /** A key that do nothing but has a unique ID. */
  private static KeyValue placeholderKey(Placeholder id)
  {
    return new KeyValue("", Kind.Placeholder, id.ordinal(), 0);
  }

  public static KeyValue makeStringKey(String str)
  {
    return makeStringKey(str, 0);
  }

  public static KeyValue makeCharKey(char c)
  {
    return new KeyValue(String.valueOf(c), Kind.Char, c, 0);
  }

  public static KeyValue makeComposePending(String symbol, int state, int flags)
  {
    return new KeyValue(symbol, Kind.Compose_pending, state,
        flags | FLAG_LATCH);
  }

  public static KeyValue makeComposePending(int symbol, int state, int flags)
  {
    return makeComposePending(String.valueOf((char)symbol), state,
        flags | FLAG_KEY_FONT);
  }

  public static KeyValue makeHangulInitial(String symbol, int initial_idx)
  {
    return new KeyValue(symbol, Kind.Hangul_initial, initial_idx * 588 + 44032,
        FLAG_LATCH);
  }

  public static KeyValue makeHangulMedial(int precomposed, int medial_idx)
  {
    precomposed += medial_idx * 28;
    return new KeyValue(String.valueOf((char)precomposed), Kind.Hangul_medial,
        precomposed, FLAG_LATCH);
  }

  public static KeyValue makeHangulFinal(int precomposed, int final_idx)
  {
    precomposed += final_idx;
    return KeyValue.makeCharKey((char)precomposed);
  }

  /** Make a key that types a string. A char key is returned for a string of
      length 1. */
  public static KeyValue makeStringKey(String str, int flags)
  {
    if (str.length() == 1)
      return new KeyValue(str, Kind.Char, str.charAt(0), flags);
    else
      return new KeyValue(str, Kind.String, 0, flags | FLAG_SMALLER_FONT);
  }

  /** Make a modifier key for passing to [KeyModifier]. */
  public static KeyValue makeInternalModifier(Modifier mod)
  {
    return new KeyValue("", Kind.Modifier, mod.ordinal(), 0);
  }

  public static KeyValue getKeyByName(String name)
  {
    switch (name)
    {
      /* These symbols have special meaning when in `srcs/layouts` and are
         escaped in standard layouts. The backslash is not stripped when parsed
         from the custom layout option. */
      case "\\?": return makeStringKey("?");
      case "\\#": return makeStringKey("#");
      case "\\@": return makeStringKey("@");
      case "\\\\": return makeStringKey("\\");

      /* Modifiers and dead-keys */
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

      /* Special event keys */
      case "config": return eventKey(0xE004, Event.CONFIG, FLAG_SMALLER_FONT);
      case "switch_text": return eventKey("ABC", Event.SWITCH_TEXT, FLAG_SMALLER_FONT);
      case "switch_numeric": return eventKey("123+", Event.SWITCH_NUMERIC, FLAG_SMALLER_FONT);
      case "switch_emoji": return eventKey(0xE001, Event.SWITCH_EMOJI, FLAG_SMALLER_FONT);
      case "switch_back_emoji": return eventKey("ABC", Event.SWITCH_BACK_EMOJI, 0);
      case "switch_clipboard": return eventKey(0xE017, Event.SWITCH_CLIPBOARD, 0);
      case "switch_back_clipboard": return eventKey("ABC", Event.SWITCH_BACK_CLIPBOARD, 0);
      case "switch_forward": return eventKey(0xE013, Event.SWITCH_FORWARD, FLAG_SMALLER_FONT);
      case "switch_backward": return eventKey(0xE014, Event.SWITCH_BACKWARD, FLAG_SMALLER_FONT);
      case "switch_greekmath": return eventKey("πλ∇¬", Event.SWITCH_GREEKMATH, FLAG_SMALLER_FONT);
      case "change_method": return eventKey(0xE009, Event.CHANGE_METHOD_PICKER, FLAG_SMALLER_FONT);
      case "change_method_prev": return eventKey(0xE009, Event.CHANGE_METHOD_AUTO, FLAG_SMALLER_FONT);
      case "action": return eventKey("Action", Event.ACTION, FLAG_SMALLER_FONT); // Will always be replaced
      case "capslock": return eventKey(0xE012, Event.CAPS_LOCK, 0);
      case "voice_typing": return eventKey(0xE015, Event.SWITCH_VOICE_TYPING, FLAG_SMALLER_FONT);
      case "voice_typing_chooser": return eventKey(0xE015, Event.SWITCH_VOICE_TYPING_CHOOSER, FLAG_SMALLER_FONT);

      /* Key events */
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
      case "menu": return keyeventKey("Menu", KeyEvent.KEYCODE_MENU, FLAG_SMALLER_FONT);

      /* Spaces */
      case "\\t": return charKey("\\t", '\t', 0); // Send the tab character
      case "\\n": return charKey("\\n", '\n', 0); // Send the newline character
      case "space": return charKey(0xE00D, ' ', FLAG_KEY_FONT | FLAG_SMALLER_FONT | FLAG_GREYED);
      case "nbsp": return charKey("\u237d", '\u00a0', FLAG_SMALLER_FONT);
      case "nnbsp": return charKey("\u2423", '\u202F', FLAG_SMALLER_FONT);

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
      case "zwnj":
      case "halfspace": return charKey("⸽", '\u200C', 0); // zero-width non joiner

      /* Editing keys */
      case "copy": return editingKey(0xE030, Editing.COPY);
      case "paste": return editingKey(0xE032, Editing.PASTE);
      case "cut": return editingKey(0xE031, Editing.CUT);
      case "selectAll": return editingKey(0xE033, Editing.SELECT_ALL);
      case "shareText": return editingKey(0xE034, Editing.SHARE);
      case "pasteAsPlainText": return editingKey(0xE035, Editing.PASTE_PLAIN);
      case "undo": return editingKey(0xE036, Editing.UNDO);
      case "redo": return editingKey(0xE037, Editing.REDO);
      case "cursor_left": return cursorMoveKey(-1);
      case "cursor_right": return cursorMoveKey(1);
      // These keys are not used
      case "replaceText": return editingKey("repl", Editing.REPLACE);
      case "textAssist": return editingKey(0xE038, Editing.ASSIST);
      case "autofill": return editingKey("auto", Editing.AUTOFILL);

      /* The compose key */
      case "compose": return makeComposePending(0xE016, ComposeKeyData.compose, FLAG_SECONDARY | FLAG_SMALLER_FONT | FLAG_SPECIAL);

      /* Placeholder keys */
      case "removed": return placeholderKey(Placeholder.REMOVED);
      case "f11_placeholder": return placeholderKey(Placeholder.F11);
      case "f12_placeholder": return placeholderKey(Placeholder.F12);

      // Korean Hangul
      case "ㄱ": return makeHangulInitial("ㄱ", 0);
      case "ㄲ": return makeHangulInitial("ㄲ", 1);
      case "ㄴ": return makeHangulInitial("ㄴ", 2);
      case "ㄷ": return makeHangulInitial("ㄷ", 3);
      case "ㄸ": return makeHangulInitial("ㄸ", 4);
      case "ㄹ": return makeHangulInitial("ㄹ", 5);
      case "ㅁ": return makeHangulInitial("ㅁ", 6);
      case "ㅂ": return makeHangulInitial("ㅂ", 7);
      case "ㅃ": return makeHangulInitial("ㅃ", 8);
      case "ㅅ": return makeHangulInitial("ㅅ", 9);
      case "ㅆ": return makeHangulInitial("ㅆ", 10);
      case "ㅇ": return makeHangulInitial("ㅇ", 11);
      case "ㅈ": return makeHangulInitial("ㅈ", 12);
      case "ㅉ": return makeHangulInitial("ㅉ", 13);
      case "ㅊ": return makeHangulInitial("ㅊ", 14);
      case "ㅋ": return makeHangulInitial("ㅋ", 15);
      case "ㅌ": return makeHangulInitial("ㅌ", 16);
      case "ㅍ": return makeHangulInitial("ㅍ", 17);
      case "ㅎ": return makeHangulInitial("ㅎ", 18);

      /* Fallback to a int keycode if possible or string key that types its name */
      default:
        String uppername = name.toUpperCase();
        if(uppername.startsWith("KEYCODE_")){
          int keycode = mapStringToKeycode(uppername);
          String keyname = uppername.split("KEYCODE_")[1];
          if (0 == keycode) keyname = "UNKNOWN";
          return new KeyValue(keyname, Kind.Keyevent, keycode,  FLAG_SMALLER_FONT, true);
        }else{
          return makeStringKey(name);
        }
    }
  }

  // Substitute for [assert], which has no effect on Android.
  private static void check(boolean b)
  {
    if (!b)
      throw new RuntimeException("Assertion failure");
  }
  private static int mapStringToKeycode(String str){
    switch (str){
      case "KEYCODE_UNKNOWN"         :return 0;
      case "KEYCODE_SOFT_LEFT"       :return 1;
      case "KEYCODE_SOFT_RIGHT"      :return 2;
      case "KEYCODE_HOME"            :return 3;
      case "KEYCODE_BACK"            :return 4;
      case "KEYCODE_CALL"            :return 5;
      case "KEYCODE_ENDCALL"         :return 6;
      case "KEYCODE_0"               :return 7;
      case "KEYCODE_1"               :return 8;
      case "KEYCODE_2"               :return 9;
      case "KEYCODE_3"               :return 10;
      case "KEYCODE_4"               :return 11;
      case "KEYCODE_5"               :return 12;
      case "KEYCODE_6"               :return 13;
      case "KEYCODE_7"               :return 14;
      case "KEYCODE_8"               :return 15;
      case "KEYCODE_9"               :return 16;
      case "KEYCODE_STAR"            :return 17;
      case "KEYCODE_POUND"           :return 18;
      case "KEYCODE_DPAD_UP"         :return 19;
      case "KEYCODE_DPAD_DOWN"       :return 20;
      case "KEYCODE_DPAD_LEFT"       :return 21;
      case "KEYCODE_DPAD_RIGHT"      :return 22;
      case "KEYCODE_DPAD_CENTER"     :return 23;
      case "KEYCODE_VOLUME_UP"       :return 24;
      case "KEYCODE_VOLUME_DOWN"     :return 25;
      case "KEYCODE_POWER"           :return 26;
      case "KEYCODE_CAMERA"          :return 27;
      case "KEYCODE_CLEAR"           :return 28;
      case "KEYCODE_A"               :return 29;
      case "KEYCODE_B"               :return 30;
      case "KEYCODE_C"               :return 31;
      case "KEYCODE_D"               :return 32;
      case "KEYCODE_E"               :return 33;
      case "KEYCODE_F"               :return 34;
      case "KEYCODE_G"               :return 35;
      case "KEYCODE_H"               :return 36;
      case "KEYCODE_I"               :return 37;
      case "KEYCODE_J"               :return 38;
      case "KEYCODE_K"               :return 39;
      case "KEYCODE_L"               :return 40;
      case "KEYCODE_M"               :return 41;
      case "KEYCODE_N"               :return 42;
      case "KEYCODE_O"               :return 43;
      case "KEYCODE_P"               :return 44;
      case "KEYCODE_Q"               :return 45;
      case "KEYCODE_R"               :return 46;
      case "KEYCODE_S"               :return 47;
      case "KEYCODE_T"               :return 48;
      case "KEYCODE_U"               :return 49;
      case "KEYCODE_V"               :return 50;
      case "KEYCODE_W"               :return 51;
      case "KEYCODE_X"               :return 52;
      case "KEYCODE_Y"               :return 53;
      case "KEYCODE_Z"               :return 54;
      case "KEYCODE_COMMA"           :return 55;
      case "KEYCODE_PERIOD"          :return 56;
      case "KEYCODE_ALT_LEFT"        :return 57;
      case "KEYCODE_ALT_RIGHT"       :return 58;
      case "KEYCODE_SHIFT_LEFT"      :return 59;
      case "KEYCODE_SHIFT_RIGHT"     :return 60;
      case "KEYCODE_TAB"             :return 61;
      case "KEYCODE_SPACE"           :return 62;
      case "KEYCODE_SYM"             :return 63;
      case "KEYCODE_EXPLORER"        :return 64;
      case "KEYCODE_ENVELOPE"        :return 65;
      case "KEYCODE_ENTER"           :return 66;
      case "KEYCODE_DEL"             :return 67;
      case "KEYCODE_GRAVE"           :return 68;
      case "KEYCODE_MINUS"           :return 69;
      case "KEYCODE_EQUALS"          :return 70;
      case "KEYCODE_LEFT_BRACKET"    :return 71;
      case "KEYCODE_RIGHT_BRACKET"   :return 72;
      case "KEYCODE_BACKSLASH"       :return 73;
      case "KEYCODE_SEMICOLON"       :return 74;
      case "KEYCODE_APOSTROPHE"      :return 75;
      case "KEYCODE_SLASH"           :return 76;
      case "KEYCODE_AT"              :return 77;
      case "KEYCODE_NUM"             :return 78;
      case "KEYCODE_HEADSETHOOK"     :return 79;
      case "KEYCODE_FOCUS"           :return 80;   // *Camera* focus
      case "KEYCODE_PLUS"            :return 81;
      case "KEYCODE_MENU"            :return 82;
      case "KEYCODE_NOTIFICATION"    :return 83;
      case "KEYCODE_SEARCH"          :return 84;
      case "KEYCODE_MEDIA_PLAY_PAUSE":return 85;
      case "KEYCODE_MEDIA_STOP"      :return 86;
      case "KEYCODE_MEDIA_NEXT"      :return 87;
      case "KEYCODE_MEDIA_PREVIOUS"  :return 88;
      case "KEYCODE_MEDIA_REWIND"    :return 89;
      case "KEYCODE_MEDIA_FAST_FORWARD" :return 90;
      case "KEYCODE_MUTE"            :return 91;
      case "KEYCODE_PAGE_UP"         :return 92;
      case "KEYCODE_PAGE_DOWN"       :return 93;
      case "KEYCODE_PICTSYMBOLS"     :return 94;   // switch symbol-sets (Emoji,Kao-moji)
      case "KEYCODE_SWITCH_CHARSET"  :return 95;   // switch char-sets (Kanji,Katakana)
      case "KEYCODE_BUTTON_A"        :return 96;
      case "KEYCODE_BUTTON_B"        :return 97;
      case "KEYCODE_BUTTON_C"        :return 98;
      case "KEYCODE_BUTTON_X"        :return 99;
      case "KEYCODE_BUTTON_Y"        :return 100;
      case "KEYCODE_BUTTON_Z"        :return 101;
      case "KEYCODE_BUTTON_L1"       :return 102;
      case "KEYCODE_BUTTON_R1"       :return 103;
      case "KEYCODE_BUTTON_L2"       :return 104;
      case "KEYCODE_BUTTON_R2"       :return 105;
      case "KEYCODE_BUTTON_THUMBL"   :return 106;
      case "KEYCODE_BUTTON_THUMBR"   :return 107;
      case "KEYCODE_BUTTON_START"    :return 108;
      case "KEYCODE_BUTTON_SELECT"   :return 109;
      case "KEYCODE_BUTTON_MODE"     :return 110;
      case "KEYCODE_ESCAPE"          :return 111;
      case "KEYCODE_FORWARD_DEL"     :return 112;
      case "KEYCODE_CTRL_LEFT"       :return 113;
      case "KEYCODE_CTRL_RIGHT"      :return 114;
      case "KEYCODE_CAPS_LOCK"       :return 115;
      case "KEYCODE_SCROLL_LOCK"     :return 116;
      case "KEYCODE_META_LEFT"       :return 117;
      case "KEYCODE_META_RIGHT"      :return 118;
      case "KEYCODE_FUNCTION"        :return 119;
      case "KEYCODE_SYSRQ"           :return 120;
      case "KEYCODE_BREAK"           :return 121;
      case "KEYCODE_MOVE_HOME"       :return 122;
      case "KEYCODE_MOVE_END"        :return 123;
      case "KEYCODE_INSERT"          :return 124;
      case "KEYCODE_FORWARD"         :return 125;
      case "KEYCODE_MEDIA_PLAY"      :return 126;
      case "KEYCODE_MEDIA_PAUSE"     :return 127;
      case "KEYCODE_MEDIA_CLOSE"     :return 128;
      case "KEYCODE_MEDIA_EJECT"     :return 129;
      case "KEYCODE_MEDIA_RECORD"    :return 130;
      case "KEYCODE_F1"              :return 131;
      case "KEYCODE_F2"              :return 132;
      case "KEYCODE_F3"              :return 133;
      case "KEYCODE_F4"              :return 134;
      case "KEYCODE_F5"              :return 135;
      case "KEYCODE_F6"              :return 136;
      case "KEYCODE_F7"              :return 137;
      case "KEYCODE_F8"              :return 138;
      case "KEYCODE_F9"              :return 139;
      case "KEYCODE_F10"             :return 140;
      case "KEYCODE_F11"             :return 141;
      case "KEYCODE_F12"             :return 142;
      case "KEYCODE_NUM_LOCK"        :return 143;
      case "KEYCODE_NUMPAD_0"        :return 144;
      case "KEYCODE_NUMPAD_1"        :return 145;
      case "KEYCODE_NUMPAD_2"        :return 146;
      case "KEYCODE_NUMPAD_3"        :return 147;
      case "KEYCODE_NUMPAD_4"        :return 148;
      case "KEYCODE_NUMPAD_5"        :return 149;
      case "KEYCODE_NUMPAD_6"        :return 150;
      case "KEYCODE_NUMPAD_7"        :return 151;
      case "KEYCODE_NUMPAD_8"        :return 152;
      case "KEYCODE_NUMPAD_9"        :return 153;
      case "KEYCODE_NUMPAD_DIVIDE"   :return 154;
      case "KEYCODE_NUMPAD_MULTIPLY" :return 155;
      case "KEYCODE_NUMPAD_SUBTRACT" :return 156;
      case "KEYCODE_NUMPAD_ADD"      :return 157;
      case "KEYCODE_NUMPAD_DOT"      :return 158;
      case "KEYCODE_NUMPAD_COMMA"    :return 159;
      case "KEYCODE_NUMPAD_ENTER"    :return 160;
      case "KEYCODE_NUMPAD_EQUALS"   :return 161;
      case "KEYCODE_NUMPAD_LEFT_PAREN" :return 162;
      case "KEYCODE_NUMPAD_RIGHT_PAREN" :return 163;
      case "KEYCODE_VOLUME_MUTE"     :return 164;
      case "KEYCODE_INFO"            :return 165;
      case "KEYCODE_CHANNEL_UP"      :return 166;
      case "KEYCODE_CHANNEL_DOWN"    :return 167;
      case "KEYCODE_ZOOM_IN"         :return 168;
      case "KEYCODE_ZOOM_OUT"        :return 169;
      case "KEYCODE_TV"              :return 170;
      case "KEYCODE_WINDOW"          :return 171;
      case "KEYCODE_GUIDE"           :return 172;
      case "KEYCODE_DVR"             :return 173;
      case "KEYCODE_BOOKMARK"        :return 174;
      case "KEYCODE_CAPTIONS"        :return 175;
      case "KEYCODE_SETTINGS"        :return 176;
      case "KEYCODE_TV_POWER"        :return 177;
      case "KEYCODE_TV_INPUT"        :return 178;
      case "KEYCODE_STB_POWER"       :return 179;
      case "KEYCODE_STB_INPUT"       :return 180;
      case "KEYCODE_AVR_POWER"       :return 181;
      case "KEYCODE_AVR_INPUT"       :return 182;
      case "KEYCODE_PROG_RED"        :return 183;
      case "KEYCODE_PROG_GREEN"      :return 184;
      case "KEYCODE_PROG_YELLOW"     :return 185;
      case "KEYCODE_PROG_BLUE"       :return 186;
      case "KEYCODE_APP_SWITCH"      :return 187;
      case "KEYCODE_BUTTON_1"        :return 188;
      case "KEYCODE_BUTTON_2"        :return 189;
      case "KEYCODE_BUTTON_3"        :return 190;
      case "KEYCODE_BUTTON_4"        :return 191;
      case "KEYCODE_BUTTON_5"        :return 192;
      case "KEYCODE_BUTTON_6"        :return 193;
      case "KEYCODE_BUTTON_7"        :return 194;
      case "KEYCODE_BUTTON_8"        :return 195;
      case "KEYCODE_BUTTON_9"        :return 196;
      case "KEYCODE_BUTTON_10"       :return 197;
      case "KEYCODE_BUTTON_11"       :return 198;
      case "KEYCODE_BUTTON_12"       :return 199;
      case "KEYCODE_BUTTON_13"       :return 200;
      case "KEYCODE_BUTTON_14"       :return 201;
      case "KEYCODE_BUTTON_15"       :return 202;
      case "KEYCODE_BUTTON_16"       :return 203;
      case "KEYCODE_LANGUAGE_SWITCH" :return 204;
      case "KEYCODE_MANNER_MODE"     :return 205;
      case "KEYCODE_3D_MODE"         :return 206;
      case "KEYCODE_CONTACTS"        :return 207;
      case "KEYCODE_CALENDAR"        :return 208;
      case "KEYCODE_MUSIC"           :return 209;
      case "KEYCODE_CALCULATOR"      :return 210;
      case "KEYCODE_ZENKAKU_HANKAKU" :return 211;
      case "KEYCODE_EISU"            :return 212;
      case "KEYCODE_MUHENKAN"        :return 213;
      case "KEYCODE_HENKAN"          :return 214;
      case "KEYCODE_KATAKANA_HIRAGANA" :return 215;
      case "KEYCODE_YEN"             :return 216;
      case "KEYCODE_RO"              :return 217;
      case "KEYCODE_KANA"            :return 218;
      case "KEYCODE_ASSIST"          :return 219;
      case "KEYCODE_BRIGHTNESS_DOWN" :return 220;
      case "KEYCODE_BRIGHTNESS_UP"   :return 221;
      case "KEYCODE_MEDIA_AUDIO_TRACK" :return 222;
      case "KEYCODE_SLEEP"           :return 223;
      case "KEYCODE_WAKEUP"          :return 224;
      case "KEYCODE_PAIRING"         :return 225;
      case "KEYCODE_MEDIA_TOP_MENU"  :return 226;
      case "KEYCODE_11"              :return 227;
      case "KEYCODE_12"              :return 228;
      case "KEYCODE_LAST_CHANNEL"    :return 229;
      case "KEYCODE_TV_DATA_SERVICE" :return 230;
      case "KEYCODE_VOICE_ASSIST" :return 231;
      case "KEYCODE_TV_RADIO_SERVICE" :return 232;
      case "KEYCODE_TV_TELETEXT" :return 233;
      case "KEYCODE_TV_NUMBER_ENTRY" :return 234;
      case "KEYCODE_TV_TERRESTRIAL_ANALOG" :return 235;
      case "KEYCODE_TV_TERRESTRIAL_DIGITAL" :return 236;
      case "KEYCODE_TV_SATELLITE" :return 237;
      case "KEYCODE_TV_SATELLITE_BS" :return 238;
      case "KEYCODE_TV_SATELLITE_CS" :return 239;
      case "KEYCODE_TV_SATELLITE_SERVICE" :return 240;
      case "KEYCODE_TV_NETWORK" :return 241;
      case "KEYCODE_TV_ANTENNA_CABLE" :return 242;
      case "KEYCODE_TV_INPUT_HDMI_1" :return 243;
      case "KEYCODE_TV_INPUT_HDMI_2" :return 244;
      case "KEYCODE_TV_INPUT_HDMI_3" :return 245;
      case "KEYCODE_TV_INPUT_HDMI_4" :return 246;
      case "KEYCODE_TV_INPUT_COMPOSITE_1" :return 247;
      case "KEYCODE_TV_INPUT_COMPOSITE_2" :return 248;
      case "KEYCODE_TV_INPUT_COMPONENT_1" :return 249;
      case "KEYCODE_TV_INPUT_COMPONENT_2" :return 250;
      case "KEYCODE_TV_INPUT_VGA_1" :return 251;
      case "KEYCODE_TV_AUDIO_DESCRIPTION" :return 252;
      case "KEYCODE_TV_AUDIO_DESCRIPTION_MIX_UP" :return 253;
      case "KEYCODE_TV_AUDIO_DESCRIPTION_MIX_DOWN" :return 254;
      case "KEYCODE_TV_ZOOM_MODE" :return 255;
      case "KEYCODE_TV_CONTENTS_MENU" :return 256;
      case "KEYCODE_TV_MEDIA_CONTEXT_MENU" :return 257;
      case "KEYCODE_TV_TIMER_PROGRAMMING" :return 258;
      case "KEYCODE_HELP" :return 259;
      case "KEYCODE_NAVIGATE_PREVIOUS" :return 260;
      case "KEYCODE_NAVIGATE_NEXT"   :return 261;
      case "KEYCODE_NAVIGATE_IN"     :return 262;
      case "KEYCODE_NAVIGATE_OUT"    :return 263;
      case "KEYCODE_STEM_PRIMARY" :return 264;
      case "KEYCODE_STEM_1" :return 265;
      case "KEYCODE_STEM_2" :return 266;
      case "KEYCODE_STEM_3" :return 267;
      case "KEYCODE_DPAD_UP_LEFT"    :return 268;
      case "KEYCODE_DPAD_DOWN_LEFT"  :return 269;
      case "KEYCODE_DPAD_UP_RIGHT"   :return 270;
      case "KEYCODE_DPAD_DOWN_RIGHT" :return 271;
      case "KEYCODE_MEDIA_SKIP_FORWARD" :return 272;
      case "KEYCODE_MEDIA_SKIP_BACKWARD" :return 273;
      case "KEYCODE_MEDIA_STEP_FORWARD" :return 274;
      case "KEYCODE_MEDIA_STEP_BACKWARD" :return 275;
      case "KEYCODE_SOFT_SLEEP" :return 276;
      case "KEYCODE_CUT" :return 277;
      case "KEYCODE_COPY" :return 278;
      case "KEYCODE_PASTE" :return 279;
      case "KEYCODE_SYSTEM_NAVIGATION_UP" :return 280;
      case "KEYCODE_SYSTEM_NAVIGATION_DOWN" :return 281;
      case "KEYCODE_SYSTEM_NAVIGATION_LEFT" :return 282;
      case "KEYCODE_SYSTEM_NAVIGATION_RIGHT" :return 283;
      case "KEYCODE_ALL_APPS" :return 284;
      case "KEYCODE_REFRESH" :return 285;
      case "KEYCODE_THUMBS_UP" :return 286;
      case "KEYCODE_THUMBS_DOWN" :return 287;
      case "KEYCODE_PROFILE_SWITCH" :return 288;
      case "KEYCODE_VIDEO_APP_1" :return 289;
      case "KEYCODE_VIDEO_APP_2" :return 290;
      case "KEYCODE_VIDEO_APP_3" :return 291;
      case "KEYCODE_VIDEO_APP_4" :return 292;
      case "KEYCODE_VIDEO_APP_5" :return 293;
      case "KEYCODE_VIDEO_APP_6" :return 294;
      case "KEYCODE_VIDEO_APP_7" :return 295;
      case "KEYCODE_VIDEO_APP_8" :return 296;
      case "KEYCODE_FEATURED_APP_1" :return 297;
      case "KEYCODE_FEATURED_APP_2" :return 298;
      case "KEYCODE_FEATURED_APP_3" :return 299;
      case "KEYCODE_FEATURED_APP_4" :return 300;
      case "KEYCODE_DEMO_APP_1" :return 301;
      case "KEYCODE_DEMO_APP_2" :return 302;
      case "KEYCODE_DEMO_APP_3" :return 303;
      case "KEYCODE_DEMO_APP_4" :return 304;
      case "KEYCODE_KEYBOARD_BACKLIGHT_DOWN" :return 305;
      case "KEYCODE_KEYBOARD_BACKLIGHT_UP" :return 306;
      case "KEYCODE_KEYBOARD_BACKLIGHT_TOGGLE" :return 307;
      case "KEYCODE_STYLUS_BUTTON_PRIMARY" :return 308;
      case "KEYCODE_STYLUS_BUTTON_SECONDARY" :return 309;
      case "KEYCODE_STYLUS_BUTTON_TERTIARY" :return 310;
      case "KEYCODE_STYLUS_BUTTON_TAIL" :return 311;
      case "KEYCODE_RECENT_APPS" :return 312;
      case "KEYCODE_MACRO_1" :return 313;
      case "KEYCODE_MACRO_2" :return 314;
      case "KEYCODE_MACRO_3" :return 315;
      case "KEYCODE_MACRO_4" :return 316;
      case "KEYCODE_EMOJI_PICKER" :return 317;
      case "KEYCODE_SCREENSHOT" :return 318;
      default: return 0;
    }
  }
}
