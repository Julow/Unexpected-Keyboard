package juloo.keyboard2;

import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import java.util.HashMap;

class KeyValue
{
  public static final int EVENT_NONE = -1;
  public static final int EVENT_CONFIG = -2;
  public static final int EVENT_SWITCH_TEXT = -3;
  public static final int EVENT_SWITCH_NUMERIC = -4;
  public static final int EVENT_SWITCH_EMOJI = -5;
  public static final int EVENT_SWITCH_BACK_EMOJI = -6;
  public static final int EVENT_CHANGE_METHOD = -7;
  public static final int EVENT_ACTION = -8;
  public static final char CHAR_NONE = '\0';

  // Behavior flags
  public static final int FLAG_LATCH = 1;
  public static final int FLAG_LOCK = (1 << 1);
  public static final int FLAG_NOREPEAT = (1 << 2);
  public static final int FLAG_NOCHAR = (1 << 3);
  public static final int FLAG_PRECISE_REPEAT = (1 << 4);

  // Rendering flags
  public static final int FLAG_KEY_FONT = (1 << 5);
  public static final int FLAG_SMALLER_FONT = (1 << 6);

  // Internal flags
  public static final int FLAG_LOCKED = (1 << 8);

  // Modifier flags
  public static final int FLAG_CTRL = (1 << 10);
  public static final int FLAG_SHIFT = (1 << 11);
  public static final int FLAG_ALT = (1 << 12);
  public static final int FLAG_FN = (1 << 13);
  public static final int FLAG_META = (1 << 14);

  // Accent flags
  public static final int FLAG_ACCENT1 = (1 << 16); // Grave
  public static final int FLAG_ACCENT2 = (1 << 17); // Aigu
  public static final int FLAG_ACCENT3 = (1 << 18); // Circonflexe
  public static final int FLAG_ACCENT4 = (1 << 19); // Tilde
  public static final int FLAG_ACCENT5 = (1 << 20); // Cédille
  public static final int FLAG_ACCENT6 = (1 << 21); // Tréma
  public static final int FLAG_ACCENT_SUPERSCRIPT = (1 << 22);
  public static final int FLAG_ACCENT_SUBSCRIPT = (1 << 23);
  public static final int FLAG_ACCENT_RING = (1 << 24);
  public static final int FLAG_ACCENT_CARON = (1 << 26);
  public static final int FLAG_ACCENT_MACRON = (1 << 27);
  public static final int FLAG_ACCENT_ORDINAL = (1 << 28);

  public static final int FLAGS_ACCENTS = FLAG_ACCENT1 | FLAG_ACCENT2 |
    FLAG_ACCENT3 | FLAG_ACCENT4 | FLAG_ACCENT5 | FLAG_ACCENT6 |
    FLAG_ACCENT_CARON | FLAG_ACCENT_MACRON | FLAG_ACCENT_SUPERSCRIPT |
    FLAG_ACCENT_SUBSCRIPT | FLAG_ACCENT_ORDINAL | FLAG_ACCENT_RING;

  // Language specific keys that are removed from the keyboard by default
  public static final int FLAG_LOCALIZED = (1 << 25);

  public final String name;
  public final String symbol;
  public final char char_;
  public final int eventCode;
  public final int flags;

  /* Update the char and the symbol. */
  public KeyValue withCharAndSymbol(char c)
  {
    return withCharAndSymbol(String.valueOf(c), c);
  }

  public KeyValue withCharAndSymbol(String s, char c)
  {
    return new KeyValue(name, s, c, eventCode, flags);
  }

  public KeyValue withNameAndSymbol(String n, String s)
  {
    return new KeyValue(n, s, char_, eventCode, flags);
  }

  public KeyValue withFlags(int f)
  {
    return new KeyValue(name, symbol, char_, eventCode, f);
  }

  private static HashMap<String, KeyValue> keys = new HashMap<String, KeyValue>();

  public KeyValue(String n, String s, char c, int e, int f)
  {
    name = n;
    symbol = s;
    char_ = c;
    eventCode = e;
    flags = f;
  }

  public static KeyValue getKeyByName(String name)
  {
    if (name == null)
      return null;
    KeyValue kv = KeyValue.keys.get(name);
    if (kv != null)
      return kv;
    char c = (name.length() == 1) ? name.charAt(0) : CHAR_NONE;
    return new KeyValue(name, name, c, EVENT_NONE, 0);
  }

  private static void addKey(String name, String symbol, char c, int event, int flags)
  {
    keys.put(name, new KeyValue(name, symbol, c, event, flags));
  }

  private static void addCharKey(char c, int event, int flags)
  {
    String name = String.valueOf(c);
    addKey(name, name, c, event, flags);
  }

  private static void addCharKey(char c, int event)
  {
    addCharKey(c, event, 0);
  }

  private static void addModifierKey(String name, String symbol, int extra_flags)
  {
    addKey(name, symbol, CHAR_NONE, EVENT_NONE,
        FLAG_LATCH | FLAG_NOCHAR | FLAG_NOREPEAT | extra_flags);
  }

  private static void addSpecialKey(String name, String symbol, int event)
  {
    addSpecialKey(name, symbol, event, 0);
  }

  private static void addSpecialKey(String name, String symbol, int event, int flags)
  {
    addKey(name, symbol, CHAR_NONE, event, flags | FLAG_NOREPEAT);
  }

  private static void addEventKey(String name, String symbol, int event)
  {
    addEventKey(name, symbol, event, 0);
  }

  private static void addEventKey(String name, String symbol, int event, int flags)
  {
    addKey(name, symbol, CHAR_NONE, event, flags);
  }

  static
  {
    addModifierKey("shift", "\n", // Can't write u000A because Java is stupid
        FLAG_SHIFT | FLAG_KEY_FONT | FLAG_SMALLER_FONT);
    addModifierKey("ctrl", "Ctrl", FLAG_CTRL | FLAG_SMALLER_FONT);
    addModifierKey("alt", "Alt", FLAG_ALT | FLAG_SMALLER_FONT);
    addModifierKey("accent_aigu", "\u0050", FLAG_ACCENT2 | FLAG_KEY_FONT | FLAG_LOCALIZED);
    addModifierKey("accent_caron", "\u0051", FLAG_ACCENT_CARON | FLAG_KEY_FONT | FLAG_LOCALIZED);
    addModifierKey("accent_cedille", "\u0052", FLAG_ACCENT5 | FLAG_KEY_FONT | FLAG_LOCALIZED);
    addModifierKey("accent_circonflexe", "\u0053", FLAG_ACCENT3 | FLAG_KEY_FONT | FLAG_LOCALIZED);
    addModifierKey("accent_grave", "\u0054", FLAG_ACCENT1 | FLAG_KEY_FONT | FLAG_LOCALIZED);
    addModifierKey("accent_macron", "\u0055", FLAG_ACCENT_MACRON | FLAG_KEY_FONT | FLAG_LOCALIZED);
    addModifierKey("accent_ring", "\u0056", FLAG_ACCENT_RING | FLAG_KEY_FONT | FLAG_LOCALIZED);
    addModifierKey("accent_tilde", "\u0057", FLAG_ACCENT4 | FLAG_KEY_FONT | FLAG_LOCALIZED);
    addModifierKey("accent_trema", "\u0058", FLAG_ACCENT6 | FLAG_KEY_FONT | FLAG_LOCALIZED);
    addModifierKey("superscript", "Sup", FLAG_ACCENT_SUPERSCRIPT | FLAG_SMALLER_FONT);
    addModifierKey("subscript", "Sub", FLAG_ACCENT_SUBSCRIPT | FLAG_SMALLER_FONT);
    addModifierKey("ordinal", "Ord", FLAG_ACCENT_ORDINAL | FLAG_SMALLER_FONT);
    addModifierKey("fn", "Fn", FLAG_FN | FLAG_SMALLER_FONT);
    addModifierKey("meta", "◆", FLAG_META);

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
    addCharKey('ß', EVENT_NONE, FLAG_LOCALIZED);
    addCharKey('€', EVENT_NONE, FLAG_LOCALIZED);
    addCharKey('£', EVENT_NONE, FLAG_LOCALIZED);

    addSpecialKey("config", "\u0004", EVENT_CONFIG, FLAG_KEY_FONT | FLAG_SMALLER_FONT);
    addSpecialKey("switch_text", "ABC", EVENT_SWITCH_TEXT);
    addSpecialKey("switch_numeric", "123+", EVENT_SWITCH_NUMERIC);
    addSpecialKey("switch_emoji", "\u0001" , EVENT_SWITCH_EMOJI, FLAG_KEY_FONT | FLAG_SMALLER_FONT);
    addSpecialKey("switch_back_emoji", "ABC", EVENT_SWITCH_BACK_EMOJI);
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
    addEventKey("f1", "F1", KeyEvent.KEYCODE_F1);
    addEventKey("f2", "F2", KeyEvent.KEYCODE_F2);
    addEventKey("f3", "F3", KeyEvent.KEYCODE_F3);
    addEventKey("f4", "F4", KeyEvent.KEYCODE_F4);
    addEventKey("f5", "F5", KeyEvent.KEYCODE_F5);
    addEventKey("f6", "F6", KeyEvent.KEYCODE_F6);
    addEventKey("f7", "F7", KeyEvent.KEYCODE_F7);
    addEventKey("f8", "F8", KeyEvent.KEYCODE_F8);
    addEventKey("f9", "F9", KeyEvent.KEYCODE_F9);
    addEventKey("f10", "F10", KeyEvent.KEYCODE_F10);
    addEventKey("f11", "F11", KeyEvent.KEYCODE_F11, FLAG_SMALLER_FONT);
    addEventKey("f12", "F12", KeyEvent.KEYCODE_F12, FLAG_SMALLER_FONT);
    addEventKey("tab", "\u000F", KeyEvent.KEYCODE_TAB, FLAG_KEY_FONT | FLAG_SMALLER_FONT);

    addKey("\\t", "\\t", '\t', EVENT_NONE, 0); // Send the tab character
    addKey("space", "\r", ' ', KeyEvent.KEYCODE_SPACE, FLAG_KEY_FONT | FLAG_SMALLER_FONT);
    addKey("nbsp", "\u237d", '\u00a0', EVENT_NONE, FLAG_KEY_FONT | FLAG_SMALLER_FONT);
  }
}
