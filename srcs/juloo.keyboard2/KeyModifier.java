package juloo.keyboard2;

import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import java.util.HashMap;

public final class KeyModifier
{
  /** The optional modmap takes priority over modifiers usual behaviors. Set to
      [null] to disable. */
  private static KeyboardData.Modmap _modmap = null;
  public static void set_modmap(KeyboardData.Modmap mm)
  {
    _modmap = mm;
  }

  /** Modify a key according to modifiers. */
  public static KeyValue modify(KeyValue k, Pointers.Modifiers mods)
  {
    if (k == null)
      return null;
    int n_mods = mods.size();
    KeyValue r = k;
    for (int i = 0; i < n_mods; i++)
      r = modify(r, mods.get(i));
    /* Keys with an empty string are placeholder keys. */
    if (r.getString().length() == 0)
      return null;
    return r;
  }

  public static KeyValue modify(KeyValue k, KeyValue mod)
  {
    switch (mod.getKind())
    {
      case Modifier:
        return modify(k, mod.getModifier());
      case Compose_pending:
        return ComposeKey.apply(mod.getPendingCompose(), k);
      case Hangul_initial:
        if (k.equals(mod)) // Allow typing the initial in letter form
          return KeyValue.makeStringKey(k.getString(), KeyValue.FLAG_GREYED);
        return combine_hangul_initial(k, mod.getHangulPrecomposed());
      case Hangul_medial:
        return combine_hangul_medial(k, mod.getHangulPrecomposed());
    }
    return k;
  }

  public static KeyValue modify(KeyValue k, KeyValue.Modifier mod)
  {
    switch (mod)
    {
      case CTRL: return apply_ctrl(k);
      case ALT:
      case META: return turn_into_keyevent(k);
      case FN: return apply_fn(k);
      case GESTURE: return apply_gesture(k);
      case SHIFT: return apply_shift(k);
      case GRAVE: return apply_compose_or_dead_char(k, ComposeKeyData.accent_grave, '\u02CB');
      case AIGU: return apply_compose_or_dead_char(k, ComposeKeyData.accent_aigu, '\u00B4');
      case CIRCONFLEXE: return apply_compose_or_dead_char(k, ComposeKeyData.accent_circonflexe, '\u02C6');
      case TILDE: return apply_compose_or_dead_char(k, ComposeKeyData.accent_tilde, '\u02DC');
      case CEDILLE: return apply_compose_or_dead_char(k, ComposeKeyData.accent_cedille, '\u00B8');
      case TREMA: return apply_compose_or_dead_char(k, ComposeKeyData.accent_trema, '\u00A8');
      case CARON: return apply_compose_or_dead_char(k, ComposeKeyData.accent_caron, '\u02C7');
      case RING: return apply_compose_or_dead_char(k, ComposeKeyData.accent_ring, '\u02DA');
      case MACRON: return apply_compose_or_dead_char(k, ComposeKeyData.accent_macron, '\u00AF');
      case OGONEK: return apply_compose_or_dead_char(k, ComposeKeyData.accent_ogonek, '\u02DB');
      case DOT_ABOVE: return apply_compose_or_dead_char(k, ComposeKeyData.accent_dot_above, '\u02D9');
      case BREVE: return apply_dead_char(k, '\u02D8');
      case DOUBLE_AIGU: return apply_compose(k, ComposeKeyData.accent_double_aigu);
      case ORDINAL: return apply_compose(k, ComposeKeyData.accent_ordinal);
      case SUPERSCRIPT: return apply_compose(k, ComposeKeyData.accent_superscript);
      case SUBSCRIPT: return apply_compose(k, ComposeKeyData.accent_subscript);
      case ARROWS: return apply_compose(k, ComposeKeyData.accent_arrows);
      case BOX: return apply_compose(k, ComposeKeyData.accent_box);
      case SLASH: return apply_compose(k, ComposeKeyData.accent_slash);
      case BAR: return apply_compose(k, ComposeKeyData.accent_bar);
      case DOT_BELOW: return apply_compose(k, ComposeKeyData.accent_dot_below);
      case HORN: return apply_compose(k, ComposeKeyData.accent_horn);
      case HOOK_ABOVE: return apply_compose(k, ComposeKeyData.accent_hook_above);
      case ARROW_RIGHT: return apply_map_char(k, map_char_arrow_right);
      default: return k;
    }
  }

  /** Modify a key after a long press. */
  public static KeyValue modify_long_press(KeyValue k)
  {
    switch (k.getKind())
    {
      case Event:
        switch (k.getEvent())
        {
          case CHANGE_METHOD_AUTO:
            return KeyValue.getKeyByName("change_method");
          case SWITCH_VOICE_TYPING:
            return KeyValue.getKeyByName("voice_typing_chooser");
        }
        break;
    }
    return k;
  }

  public static Map_char modify_numpad_script(String numpad_script)
  {
    if (numpad_script == null)
      return map_char_none;
    switch (numpad_script)
    {
      case "hindu-arabic": return map_char_numpad_hindu;
      case "bengali": return map_char_numpad_bengali;
      case "devanagari": return map_char_numpad_devanagari;
      case "persian": return map_char_numpad_persian;
      case "gujarati": return map_char_numpad_gujarati;
      default: return map_char_none;
    }
  }

  private static KeyValue apply_map_char(KeyValue k, Map_char map)
  {
    switch (k.getKind())
    {
      case Char:
        char kc = k.getChar();
        String modified = map.apply(kc);
        if (modified != null)
          return KeyValue.makeStringKey(modified, k.getFlags());
    }
    return k;
  }

  /** Apply the given compose state or fallback to the dead_char. */
  private static KeyValue apply_compose_or_dead_char(KeyValue k, int state, char dead_char)
  {
    switch (k.getKind())
    {
      case Char:
        char c = k.getChar();
        KeyValue r = ComposeKey.apply(state, c);
        if (r != null)
          return r;
    }
    return apply_dead_char(k, dead_char);
  }

  private static KeyValue apply_compose(KeyValue k, int state)
  {
    switch (k.getKind())
    {
      case Char:
        KeyValue r = ComposeKey.apply(state, k.getChar());
        if (r != null)
          return r;
    }
    return k;
  }

  private static KeyValue apply_dead_char(KeyValue k, char dead_char)
  {
    switch (k.getKind())
    {
      case Char:
        char c = k.getChar();
        char modified = (char)KeyCharacterMap.getDeadChar(dead_char, c);
        if (modified != 0 && modified != c)
          return KeyValue.makeStringKey(String.valueOf(modified));
    }
    return k;
  }

  private static KeyValue apply_shift(KeyValue k)
  {
    if (_modmap != null)
    {
      KeyValue mapped = _modmap.shift.get(k);
      if (mapped != null)
        return mapped;
    }
    switch (k.getKind())
    {
      case Char:
        char kc = k.getChar();
        char c = map_char_shift(kc);
        if (kc == c)
          c = Character.toUpperCase(kc);
        return (kc == c) ? k : k.withChar(c);
      case String:
        String s = Utils.capitalize_string(k.getString());
        return KeyValue.makeStringKey(s, k.getFlags());
      default: return k;
    }
  }

  private static KeyValue apply_fn(KeyValue k)
  {
    if (_modmap != null)
    {
      KeyValue mapped = _modmap.fn.get(k);
      if (mapped != null)
        return mapped;
    }
    String name = null;
    switch (k.getKind())
    {
      case Char: name = apply_fn_char(k.getChar()); break;
      case Keyevent: name = apply_fn_keyevent(k.getKeyevent()); break;
      case Event: name = apply_fn_event(k.getEvent()); break;
      case Placeholder: name = apply_fn_placeholder(k.getPlaceholder()); break;
      case Cursor_move: name = apply_fn_cursormove(k.getCursorMove()); break;
    }
    return (name == null) ? k : KeyValue.getKeyByName(name);
  }

  private static String apply_fn_keyevent(int code)
  {
    switch (code)
    {
      case KeyEvent.KEYCODE_DPAD_UP: return "page_up";
      case KeyEvent.KEYCODE_DPAD_DOWN: return "page_down";
      case KeyEvent.KEYCODE_DPAD_LEFT: return "home";
      case KeyEvent.KEYCODE_DPAD_RIGHT: return "end";
      case KeyEvent.KEYCODE_ESCAPE: return "insert";
      case KeyEvent.KEYCODE_TAB: return "\\t";
      case KeyEvent.KEYCODE_PAGE_UP:
      case KeyEvent.KEYCODE_PAGE_DOWN:
      case KeyEvent.KEYCODE_MOVE_HOME:
      case KeyEvent.KEYCODE_MOVE_END: return "removed";
      default: return null;
    }
  }

  private static String apply_fn_event(KeyValue.Event ev)
  {
    switch (ev)
    {
      case SWITCH_NUMERIC: return "switch_greekmath";
      default: return null;
    }
  }

  private static String apply_fn_placeholder(KeyValue.Placeholder p)
  {
    switch (p)
    {
      case F11: return "f11";
      case F12: return "f12";
      case SHINDOT: return "shindot";
      case SINDOT: return "sindot";
      case OLE: return "ole";
      case METEG: return "meteg";
      default: return null;
    }
  }

  private static String apply_fn_cursormove(short cur)
  {
    switch (cur)
    {
      case -1 : return "home"; // cursor_left
      case 1 : return "end"; // cursor_right
      default: return null;
    }
  }

  /** Return the name of modified key, or [null]. */
  private static String apply_fn_char(char c)
  {
    switch (c)
    {
      case '1': return "f1";
      case '2': return "f2";
      case '3': return "f3";
      case '4': return "f4";
      case '5': return "f5";
      case '6': return "f6";
      case '7': return "f7";
      case '8': return "f8";
      case '9': return "f9";
      case '0': return "f10";
      case '<': return "«";
      case '>': return "»";
      case '{': return "‹";
      case '}': return "›";
      case '[': return "‘";
      case ']': return "’";
      case '(': return "“";
      case ')': return "”";
      case '\'': return "‚";
      case '"': return "„";
      case '-': return "–";
      case '_': return "—";
      case '^': return "¬";
      case '%': return "‰";
      case '=': return "≈";
      case 'u': return "µ";
      case 'a': return "æ";
      case 'o': return "œ";
      case '*': return "°";
      case '.': return "…";
      case ',': return "·";
      case '!': return "¡";
      case '?': return "¿";
      case '|': return "¦";
      case '§': return "¶";
      case '†': return "‡";
      case '×': return "∙";
      case ' ': return "nbsp";
      // arrows
      case '↖': return "⇖";
      case '↑': return "⇑";
      case '↗': return "⇗";
      case '←': return "⇐";
      case '→': return "⇒";
      case '↙': return "⇙";
      case '↓': return "⇓";
      case '↘': return "⇘";
      case '↔': return "⇔";
      case '↕': return "⇕";
      // Currency symbols
      case 'e': return "€";
      case 'l': return "£";
      case 'r': return "₹";
      case 'y': return "¥";
      case 'c': return "¢";
      case 'p': return "₽";
      case 'b': return "₱";
      case 'h': return "₴";
      case 'z': return "₿";
      case '€': case '£': return "removed"; // Avoid showing these twice
      // alternating greek letters
      case 'π': return "ϖ";
      case 'θ': return "ϑ";
      case 'Θ': return "ϴ";
      case 'ε': return "ϵ";
      case 'β': return "ϐ";
      case 'ρ': return "ϱ";
      case 'σ': return "ς";
      case 'γ': return "ɣ";
      case 'φ': return "ϕ";
      case 'υ': return "ϒ";
      case 'κ': return "ϰ";
      // alternating math characters
      case '∪': return "⋃";
      case '∩': return "⋂";
      case '∃': return "∄";
      case '∈': return "∉";
      case '∫': return "∮";
      case 'Π': return "∏";
      case 'Σ': return "∑";
      case '∨': return "⋁";
      case '∧': return "⋀";
      case '⊷': return "⊶";
      case '⊂': return "⊆";
      case '⊃': return "⊇";
      case '±': return "∓";
      // hebrew niqqud
      case 'ק': return "qamats"; // kamatz
      case 'ר': return "hataf_qamats"; // reduced kamatz
      case 'ו': return "holam";
      case 'ם': return "rafe";
      case 'פ': return "patah"; // patach
      case 'ש': return "sheva";
      case 'ד': return "dagesh"; // or mapiq
      case 'ח': return "hiriq";
      case 'ף': return "hataf_patah"; // reduced patach
      case 'ז': return "qubuts"; // kubuts
      case 'ס': return "segol";
      case 'ב': return "hataf_segol"; // reduced segol
      case 'צ': return "tsere";
      // Devanagari symbols
      case 'ए': return "ऍ";
      case 'े': return "ॅ";
      case 'ऐ': return "ऎ";
      case 'ै': return "ॆ";
      case 'ऋ': return "ॠ";
      case 'ृ': return "ॄ";
      case 'ळ': return "ऴ";
      case 'र': return "ऱ";
      case 'क': return "क़";
      case 'ख': return "ख़";
      case 'ग': return "ग़";
      case 'घ': return "ॻ";
      case 'ढ': return "ढ़";
      case 'न': return "ऩ";
      case 'ड': return "ड़";
      case 'ट': return "ॸ";
      case 'ण': return "ॾ";
      case 'फ': return "फ़";
      case 'ऌ': return "ॡ";
      case 'ॢ': return "ॣ";
      case 'औ': return "ॵ";
      case 'ौ': return "ॏ";
      case 'ओ': return "ऒ";
      case 'ो': return "ॊ";
      case 'च': return "ॼ";
      case 'ज': return "ज़";
      case 'ब': return "ॿ";
      case 'व': return "ॺ";
      case 'य': return "य़";
      case 'अ': return "ॲ";
      case 'आ': return "ऑ";
      case 'ा': return "ॉ";
      case 'झ': return "ॹ";
      case 'ई': return "ॴ";
      case 'ी': return "ऻ";
      case 'इ': return "ॳ";
      case 'ि': return "ऺ";
      case 'उ': return "ॶ";
      case 'ऊ': return "ॷ";
      case 'ु': return "ऄ";
      case 'ष': return "क्ष";
      case 'थ': return "त्र";
      case 'द': return "द्र";
      case 'प': return "प्र";
      case 'श': return "श्र";
      case 'छ': return "श्च";
      case 'ँ': return "ऀ";
      case '₹': return "₨";
      case 'ॖ': return "ॗ";
      case '॓': return "॔";
      case '॰': return "ॱ";
      case '।': return "॥";
      case 'ं': return "ॕ";
      case '़': return "ॎ";
      case 'ऽ': return "ॽ";
      // Persian numbers
      case '۱': return "f1";
      case '۲': return "f2";
      case '۳': return "f3";
      case '۴': return "f4";
      case '۵': return "f5";
      case '۶': return "f6";
      case '۷': return "f7";
      case '۸': return "f8";
      case '۹': return "f9";
      case '۰': return "f10";
      // Arabic numbers
      case '١': return "f1";
      case '٢': return "f2";
      case '٣': return "f3";
      case '٤': return "f4";
      case '٥': return "f5";
      case '٦': return "f6";
      case '٧': return "f7";
      case '٨': return "f8";
      case '٩': return "f9";
      case '٠': return "f10";
      default: return null;
    }
  }

  private static KeyValue apply_ctrl(KeyValue k)
  {
    if (_modmap != null)
    {
      KeyValue mapped = _modmap.ctrl.get(k);
      // Do not return the modified character right away, first turn it into a
      // key event.
      if (mapped != null)
        k = mapped;
    }
    return turn_into_keyevent(k);
  }

  private static KeyValue turn_into_keyevent(KeyValue k)
  {
    if (k.getKind() != KeyValue.Kind.Char)
      return k;
    int e;
    switch (k.getChar())
    {
      case 'a': e = KeyEvent.KEYCODE_A; break;
      case 'b': e = KeyEvent.KEYCODE_B; break;
      case 'c': e = KeyEvent.KEYCODE_C; break;
      case 'd': e = KeyEvent.KEYCODE_D; break;
      case 'e': e = KeyEvent.KEYCODE_E; break;
      case 'f': e = KeyEvent.KEYCODE_F; break;
      case 'g': e = KeyEvent.KEYCODE_G; break;
      case 'h': e = KeyEvent.KEYCODE_H; break;
      case 'i': e = KeyEvent.KEYCODE_I; break;
      case 'j': e = KeyEvent.KEYCODE_J; break;
      case 'k': e = KeyEvent.KEYCODE_K; break;
      case 'l': e = KeyEvent.KEYCODE_L; break;
      case 'm': e = KeyEvent.KEYCODE_M; break;
      case 'n': e = KeyEvent.KEYCODE_N; break;
      case 'o': e = KeyEvent.KEYCODE_O; break;
      case 'p': e = KeyEvent.KEYCODE_P; break;
      case 'q': e = KeyEvent.KEYCODE_Q; break;
      case 'r': e = KeyEvent.KEYCODE_R; break;
      case 's': e = KeyEvent.KEYCODE_S; break;
      case 't': e = KeyEvent.KEYCODE_T; break;
      case 'u': e = KeyEvent.KEYCODE_U; break;
      case 'v': e = KeyEvent.KEYCODE_V; break;
      case 'w': e = KeyEvent.KEYCODE_W; break;
      case 'x': e = KeyEvent.KEYCODE_X; break;
      case 'y': e = KeyEvent.KEYCODE_Y; break;
      case 'z': e = KeyEvent.KEYCODE_Z; break;
      case '0': e = KeyEvent.KEYCODE_0; break;
      case '1': e = KeyEvent.KEYCODE_1; break;
      case '2': e = KeyEvent.KEYCODE_2; break;
      case '3': e = KeyEvent.KEYCODE_3; break;
      case '4': e = KeyEvent.KEYCODE_4; break;
      case '5': e = KeyEvent.KEYCODE_5; break;
      case '6': e = KeyEvent.KEYCODE_6; break;
      case '7': e = KeyEvent.KEYCODE_7; break;
      case '8': e = KeyEvent.KEYCODE_8; break;
      case '9': e = KeyEvent.KEYCODE_9; break;
      case '`': e = KeyEvent.KEYCODE_GRAVE; break;
      case '-': e = KeyEvent.KEYCODE_MINUS; break;
      case '=': e = KeyEvent.KEYCODE_EQUALS; break;
      case '[': e = KeyEvent.KEYCODE_LEFT_BRACKET; break;
      case ']': e = KeyEvent.KEYCODE_RIGHT_BRACKET; break;
      case '\\': e = KeyEvent.KEYCODE_BACKSLASH; break;
      case ';': e = KeyEvent.KEYCODE_SEMICOLON; break;
      case '\'': e = KeyEvent.KEYCODE_APOSTROPHE; break;
      case '/': e = KeyEvent.KEYCODE_SLASH; break;
      case '@': e = KeyEvent.KEYCODE_AT; break;
      case '+': e = KeyEvent.KEYCODE_PLUS; break;
      case ',': e = KeyEvent.KEYCODE_COMMA; break;
      case '.': e = KeyEvent.KEYCODE_PERIOD; break;
      case '*': e = KeyEvent.KEYCODE_STAR; break;
      case '#': e = KeyEvent.KEYCODE_POUND; break;
      case '(': e = KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN; break;
      case ')': e = KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN; break;
      case ' ': e = KeyEvent.KEYCODE_SPACE; break;
      default: return k;
    }
    return k.withKeyevent(e);
  }

  /** Modify a key affected by a round-trip or a clockwise circle gesture. */
  private static KeyValue apply_gesture(KeyValue k)
  {
    KeyValue shifted = apply_shift(k);
    if (shifted == null || shifted.equals(k))
      return apply_fn(k);
    return shifted;
  }

  public static abstract class Map_char
  {
    /** Modify a char or return [null] if the modifier do not apply. Return a
        [String] that can contains combining diacritics. */
    public abstract String apply(char c);
  }

  private static final Map_char map_char_none =
    new Map_char() {
      public String apply(char _c) { return null; }
    };

  private static char map_char_shift(char c)
  {
    switch (c)
    {
      case '↙': return '⇙';
      case '↓': return '⇓';
      case '↘': return '⇘';
      case '←': return '⇐';
      case '→': return '⇒';
      case '↖': return '⇖';
      case '↑': return '⇑';
      case '↗': return '⇗';
      case '└': return '╚';
      case '┴': return '╩';
      case '┘': return '╝';
      case '├': return '╠';
      case '┼': return '╬';
      case '┤': return '╣';
      case '┌': return '╔';
      case '┬': return '╦';
      case '┐': return '╗';
      case '─': return '═';
      case '│': return '║';
      case 'ß': return 'ẞ';
      /* In Turkish, upper case of 'iı' is 'İI' but Java's toUpperCase will
         return 'II'. To make 'İ' accessible, make it the shift of 'ı'. This
         has the inconvenient of swapping i and ı on the keyboard. */
      case 'ı': return 'İ';
      case '₹': return '₨';
      // Gujarati alternate characters
      case 'અ': return 'આ';
      case 'ઇ': return 'ઈ';
      case 'િ': return 'ી';
      case 'ઉ': return 'ઊ';
      case 'ુ': return 'ૂ';
      case 'એ': return 'ઐ';
      case 'ે': return 'ૈ';
      case 'ઓ': return 'ઔ';
      case 'ો': return 'ૌ';
      case 'ક': return 'ખ';
      case 'ગ': return 'ઘ';
      case 'ચ': return 'છ';
      case 'જ': return 'ઝ';
      case 'ટ': return 'ઠ';
      case 'ડ': return 'ઢ';
      case 'ન': return 'ણ';
      case 'ત': return 'થ';
      case 'દ': return 'ધ';
      case 'પ': return 'ફ';
      case 'બ': return 'ભ';
      case 'મ': return 'ં';
      case 'લ': return 'ળ';
      case 'સ': return 'શ';
      case 'હ': return 'ઃ';
      default: return c;
    }
  }

  private static final Map_char map_char_arrow_right =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          default: return c + "\u20D7";
        }
      }
    };

  // Used with Arabic despite the name; called "Hindi numerals" in Arabic
  // map_char_numpad_devanagari is used in Hindi
  private static final Map_char map_char_numpad_hindu =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          case '0': return "٠";
          case '1': return "١";
          case '2': return "٢";
          case '3': return "٣";
          case '4': return "٤";
          case '5': return "٥";
          case '6': return "٦";
          case '7': return "٧";
          case '8': return "٨";
          case '9': return "٩";
          default: return null;
        }
      }
    };

  private static final Map_char map_char_numpad_bengali =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          case '0': return "০";
          case '1': return "১";
          case '2': return "২";
          case '3': return "৩";
          case '4': return "৪";
          case '5': return "৫";
          case '6': return "৬";
          case '7': return "৭";
          case '8': return "৮";
          case '9': return "৯";
          default: return null;
        }
      }
    };

  private static final Map_char map_char_numpad_devanagari =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          case '0': return "०";
          case '1': return "१";
          case '2': return "२";
          case '3': return "३";
          case '4': return "४";
          case '5': return "५";
          case '6': return "६";
          case '7': return "७";
          case '8': return "८";
          case '9': return "९";
          default: return null;
        }
      }
    };

  private static final Map_char map_char_numpad_persian =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          case '0': return "۰";
          case '1': return "۱";
          case '2': return "۲";
          case '3': return "۳";
          case '4': return "۴";
          case '5': return "۵";
          case '6': return "۶";
          case '7': return "۷";
          case '8': return "۸";
          case '9': return "۹";
          default: return null;
        }
      }
    };

  private static final Map_char map_char_numpad_gujarati =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          case '0': return "૦";
          case '1': return "૧";
          case '2': return "૨";
          case '3': return "૩";
          case '4': return "૪";
          case '5': return "૫";
          case '6': return "૬";
          case '7': return "૭";
          case '8': return "૮";
          case '9': return "૯";
          default: return null;
        }
      }
    };

  /** Compose the precomposed initial with the medial [kv]. */
  private static KeyValue combine_hangul_initial(KeyValue kv, int precomposed)
  {
    switch (kv.getKind())
    {
      case Char:
        return combine_hangul_initial(kv, kv.getChar(), precomposed);
      case Hangul_initial:
        // No initials are expected to compose, grey out
        return kv.withFlags(kv.getFlags() | KeyValue.FLAG_GREYED);
      default:
        return kv;
    }
  }

  private static KeyValue combine_hangul_initial(KeyValue kv, char medial,
      int precomposed)
  {
    int medial_idx;
    switch (medial)
    {
      // Vowels
      case 'ㅏ': medial_idx = 0; break;
      case 'ㅐ': medial_idx = 1; break;
      case 'ㅑ': medial_idx = 2; break;
      case 'ㅒ': medial_idx = 3; break;
      case 'ㅓ': medial_idx = 4; break;
      case 'ㅔ': medial_idx = 5; break;
      case 'ㅕ': medial_idx = 6; break;
      case 'ㅖ': medial_idx = 7; break;
      case 'ㅗ': medial_idx = 8; break;
      case 'ㅘ': medial_idx = 9; break;
      case 'ㅙ': medial_idx = 10; break;
      case 'ㅚ': medial_idx = 11; break;
      case 'ㅛ': medial_idx = 12; break;
      case 'ㅜ': medial_idx = 13; break;
      case 'ㅝ': medial_idx = 14; break;
      case 'ㅞ': medial_idx = 15; break;
      case 'ㅟ': medial_idx = 16; break;
      case 'ㅠ': medial_idx = 17; break;
      case 'ㅡ': medial_idx = 18; break;
      case 'ㅢ': medial_idx = 19; break;
      case 'ㅣ': medial_idx = 20; break;
      // Grey-out uncomposable characters
      default: return kv.withFlags(kv.getFlags() | KeyValue.FLAG_GREYED);
    }
    return KeyValue.makeHangulMedial(precomposed, medial_idx);
  }

  /** Combine the precomposed medial with the final [kv]. */
  private static KeyValue combine_hangul_medial(KeyValue kv, int precomposed)
  {
    switch (kv.getKind())
    {
      case Char:
        return combine_hangul_medial(kv, kv.getChar(), precomposed);
      case Hangul_initial:
        // Finals that can also be initials have this kind.
        return combine_hangul_medial(kv, kv.getString().charAt(0), precomposed);
      default:
        return kv;
    }
  }

  private static KeyValue combine_hangul_medial(KeyValue kv, char c,
      int precomposed)
  {
    int final_idx;
    switch (c)
    {
      case ' ': final_idx = 0; break;
      case 'ㄱ': final_idx = 1; break;
      case 'ㄲ': final_idx = 2; break;
      case 'ㄳ': final_idx = 3; break;
      case 'ㄴ': final_idx = 4; break;
      case 'ㄵ': final_idx = 5; break;
      case 'ㄶ': final_idx = 6; break;
      case 'ㄷ': final_idx = 7; break;
      case 'ㄹ': final_idx = 8; break;
      case 'ㄺ': final_idx = 9; break;
      case 'ㄻ': final_idx = 10; break;
      case 'ㄼ': final_idx = 11; break;
      case 'ㄽ': final_idx = 12; break;
      case 'ㄾ': final_idx = 13; break;
      case 'ㄿ': final_idx = 14; break;
      case 'ㅀ': final_idx = 15; break;
      case 'ㅁ': final_idx = 16; break;
      case 'ㅂ': final_idx = 17; break;
      case 'ㅄ': final_idx = 18; break;
      case 'ㅅ': final_idx = 19; break;
      case 'ㅆ': final_idx = 20; break;
      case 'ㅇ': final_idx = 21; break;
      case 'ㅈ': final_idx = 22; break;
      case 'ㅊ': final_idx = 23; break;
      case 'ㅋ': final_idx = 24; break;
      case 'ㅌ': final_idx = 25; break;
      case 'ㅍ': final_idx = 26; break;
      case 'ㅎ': final_idx = 27; break;
      // Grey-out uncomposable characters
      default: return kv.withFlags(kv.getFlags() | KeyValue.FLAG_GREYED);
    }
    return KeyValue.makeHangulFinal(precomposed, final_idx);
  }
}
