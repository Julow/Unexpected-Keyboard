package juloo.keyboard2;

import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import java.util.HashMap;

class KeyModifier
{
  /** Cache key is KeyValue's name */
  private static HashMap<KeyValue, HashMap<Pointers.Modifiers, KeyValue>> _cache =
    new HashMap<KeyValue, HashMap<Pointers.Modifiers, KeyValue>>();

  /** Modify a key according to modifiers. */
  public static KeyValue modify(KeyValue k, Pointers.Modifiers mods)
  {
    if (k == null)
      return null;
    int n_mods = mods.size();
    HashMap<Pointers.Modifiers, KeyValue> ks = cacheEntry(k);
    KeyValue r = ks.get(mods);
    if (r == null)
    {
      r = k;
      /* Order: Fn, Shift, accents */
      for (int i = 0; i < n_mods; i++)
        r = modify(r, mods.get(i));
      ks.put(mods, r);
    }
    /* Keys with an empty string are placeholder keys. */
    return (r.getString().length() == 0) ? null : r;
  }

  public static KeyValue modify(KeyValue k, KeyValue.Modifier mod)
  {
    switch (mod)
    {
      case CTRL:
      case ALT:
      case META: return turn_into_keyevent(k);
      case FN: return apply_fn(k);
      case SHIFT: return apply_shift(k);
      case GRAVE: return apply_map_or_dead_char(k, map_char_grave, '\u02CB');
      case AIGU: return apply_map_or_dead_char(k, map_char_aigu, '\u00B4');
      case CIRCONFLEXE: return apply_map_or_dead_char(k, map_char_circonflexe, '\u02C6');
      case TILDE: return apply_map_or_dead_char(k, map_char_tilde, '\u02DC');
      case CEDILLE: return apply_map_or_dead_char(k, map_char_cedille, '\u00B8');
      case TREMA: return apply_map_or_dead_char(k, map_char_trema, '\u00A8');
      case CARON: return apply_map_or_dead_char(k, map_char_caron, '\u02C7');
      case RING: return apply_map_or_dead_char(k, map_char_ring, '\u02DA');
      case MACRON: return apply_map_or_dead_char(k, map_char_macron, '\u00AF');
      case OGONEK: return apply_map_or_dead_char(k, map_char_ogonek, '\u02DB');
      case DOT_ABOVE: return apply_map_or_dead_char(k, map_char_dot_above, '\u02D9');
      case BREVE: return apply_dead_char(k, '\u02D8');
      case DOUBLE_AIGU: return apply_map_char(k, map_char_double_aigu);
      case ORDINAL: return apply_map_char(k, map_char_ordinal);
      case SUPERSCRIPT: return apply_map_char(k, map_char_superscript);
      case SUBSCRIPT: return apply_map_char(k, map_char_subscript);
      case ARROWS: return apply_map_char(k, map_char_arrows);
      case BOX: return apply_map_char(k, map_char_box);
      case SLASH: return apply_map_char(k, map_char_slash);
      case BAR: return apply_map_char(k, map_char_bar);
      case ARROW_RIGHT: return apply_combining(k, "\u20D7");
      case DOT_BELOW: return apply_map_char(k, map_char_dot_below);
      case HORN: return apply_map_char(k, map_char_horn);
      case HOOK_ABOVE: return apply_map_char(k, map_char_hook_above);
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
          case CHANGE_METHOD_PREV:
            return KeyValue.getKeyByName("change_method");
        }
        break;
    }
    return k;
  }

  private static KeyValue apply_map_char(KeyValue k, Map_char map)
  {
    switch (k.getKind())
    {
      case Char:
        char kc = k.getChar();
        char c = map.apply(kc);
        return (kc == c) ? k : k.withChar(c);
      default: return k;
    }
  }

  private static KeyValue apply_dead_char(KeyValue k, char dead_char)
  {
    switch (k.getKind())
    {
      case Char:
        char kc = k.getChar();
        char c = (char)KeyCharacterMap.getDeadChar(dead_char, kc);
        return (c == 0 || kc == c) ? k : k.withChar(c);
      default: return k;
    }
  }

  /** Apply a [Map_char] or fallback to [apply_dead_char]. */
  private static KeyValue apply_map_or_dead_char(KeyValue k, Map_char map, char dead_char)
  {
    switch (k.getKind())
    {
      case Char:
        char kc = k.getChar();
        char c = map.apply(kc);
        if (kc == c)
          return apply_dead_char(k, dead_char);
        return k.withChar(c);
      default: return k;
    }
  }

  private static KeyValue apply_combining(KeyValue k, String combining)
  {
    switch (k.getKind())
    {
      case Char:
        return k.withString(String.valueOf(k.getChar()) + combining);
      default: return k;
    }
  }

  private static KeyValue apply_shift(KeyValue k)
  {
    switch (k.getKind())
    {
      case Char:
        char kc = k.getChar();
        char c = map_char_shift(kc);
        if (kc == c)
          c = Character.toUpperCase(kc);
        return (kc == c) ? k : k.withChar(c);
      case String:
        return k.withString(k.getString().toUpperCase());
      default: return k;
    }
  }

  private static KeyValue apply_fn(KeyValue k)
  {
    String name = null;
    switch (k.getKind())
    {
      case Char: name = apply_fn_char(k.getChar()); break;
      case Keyevent: name = apply_fn_keyevent(k.getKeyevent()); break;
      case Event: name = apply_fn_event(k.getEvent()); break;
      case Placeholder: name = apply_fn_placeholder(k.getPlaceholder()); break;
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
      case 'p': return "₱";
      case 'h': return "₴";
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

  /* Lookup the cache entry for a key. Create it needed. */
  private static HashMap<Pointers.Modifiers, KeyValue> cacheEntry(KeyValue k)
  {
    HashMap<Pointers.Modifiers, KeyValue> ks = _cache.get(k);
    if (ks == null)
    {
      ks = new HashMap<Pointers.Modifiers, KeyValue>();
      _cache.put(k, ks);
    }
    return ks;
  }

  private static abstract class Map_char
  {
    public abstract char apply(char c);
  }

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
      default: return c;
    }
  }

  private static final Map_char map_char_aigu =
    new Map_char() {
      public char apply(char c)
      {
        switch (c)
        {
          // Composite characters: 'j́'
          case 'a': return 'á';
          case 'e': return 'é';
          case 'i': return 'í';
          case 'l': return 'ĺ';
          case 'o': return 'ó';
          case 'r': return 'ŕ';
          case 's': return 'ś';
          case 'u': return 'ú';
          case 'y': return 'ý';
          default: return c;
        }
      }
    };

  private static final Map_char map_char_caron =
    new Map_char() {
      public char apply(char c)
      {
        switch (c)
        {
          case 'c': return 'č';
          case 'd': return 'ď';
          case 'e': return 'ě';
          case 'l': return 'ľ';
          case 'n': return 'ň';
          case 'r': return 'ř';
          case 's': return 'š';
          case 't': return 'ť';
          case 'z': return 'ž';
          default: return c;
        }
      }
    };

  private static final Map_char map_char_cedille =
    new Map_char() {
      public char apply(char c)
      {
        switch (c)
        {
          case 'c': return 'ç';
          case 's': return 'ş';
          case 'g': return 'ģ';
          default: return c;
        }
      }
    };

  private static final Map_char map_char_circonflexe =
    new Map_char() {
      public char apply(char c)
      {
        switch (c)
        {
          case 'a': return 'â';
          case 'e': return 'ê';
          case 'i': return 'î';
          case 'o': return 'ô';
          case 'u': return 'û';
          default: return c;
        }
      }
    };

  private static final Map_char map_char_dot_above =
    new Map_char() {
      public char apply(char c)
      {
        switch (c)
        {
          case 'ė': return 'ė';
          default: return c;
        }
      }
    };

  private static final Map_char map_char_grave =
    new Map_char() {
      public char apply(char c)
      {
        switch (c)
        {
          case 'a': return 'à';
          case 'e': return 'è';
          case 'i': return 'ì';
          case 'o': return 'ò';
          case 'u': return 'ù';
          default: return c;
        }
      }
    };

  private static final Map_char map_char_macron =
    new Map_char() {
      public char apply(char c)
      {
        switch (c)
        {
          case 'a': return 'ā';
          case 'e': return 'ē';
          case 'i': return 'ī';
          case 'u': return 'ū';
          default: return c;
        }
      }
    };

  private static final Map_char map_char_ogonek =
    new Map_char() {
      public char apply(char c)
      {
        switch (c)
        {
          case 'a': return 'ą';
          case 'e': return 'ę';
          case 'i': return 'į';
          case 'k': return 'ķ';
          case 'l': return 'ļ';
          case 'n': return 'ņ';
          case 'u': return 'ų';
          default: return c;
        }
      }
    };

  private static final Map_char map_char_ring =
    new Map_char() {
      public char apply(char c)
      {
        switch (c)
        {
          case 'a': return 'å';
          case 'u': return 'ů';
          default: return c;
        }
      }
    };

  private static final Map_char map_char_tilde =
    new Map_char() {
      public char apply(char c)
      {
        switch (c)
        {
          case 'a': return 'ã';
          case 'o': return 'õ';
          case 'n': return 'ñ';
          default: return c;
        }
      }
    };

  private static final Map_char map_char_trema =
    new Map_char() {
      public char apply(char c)
      {
        switch (c)
        {
          case 'a': return 'ä';
          case 'e': return 'ë';
          case 'i': return 'ï';
          case 'o': return 'ö';
          case 'u': return 'ü';
          case 'y': return 'ÿ';
          default: return c;
        }
      }
    };

  private static final Map_char map_char_double_aigu =
    new Map_char() {
      public char apply(char c)
      {
        switch (c)
        {
          // Composite characters: a̋ e̋ i̋ m̋ ӳ
          case 'o': return 'ő';
          case 'u': return 'ű';
          case ' ': return '˝';
          default: return c;
        }
      }
    };

  private static final Map_char map_char_ordinal =
    new Map_char() {
      public char apply(char c)
      {
        switch (c)
        {
          case 'a': return 'ª';
          case 'o': return 'º';
          case '1': return 'ª';
          case '2': return 'º';
          case '3': return 'ⁿ';
          case '4': return 'ᵈ';
          case '5': return 'ᵉ';
          case '6': return 'ʳ';
          case '7': return 'ˢ';
          case '8': return 'ᵗ';
          case '9': return 'ʰ';
          case '*': return '°';
          default: return c;
        }
      }
    };

  private static final Map_char map_char_superscript =
    new Map_char() {
      public char apply(char c)
      {
        switch (c)
        {
          case '1': return '¹';
          case '2': return '²';
          case '3': return '³';
          case '4': return '⁴';
          case '5': return '⁵';
          case '6': return '⁶';
          case '7': return '⁷';
          case '8': return '⁸';
          case '9': return '⁹';
          case '0': return '⁰';
          case '+': return '⁺';
          case '-': return '⁻';
          case '=': return '⁼';
          case '(': return '⁽';
          case ')': return '⁾';
          case 'a': return 'ᵃ';
          case 'b': return 'ᵇ';
          case 'c': return 'ᶜ';
          case 'd': return 'ᵈ';
          case 'e': return 'ᵉ';
          case 'f': return 'ᶠ';
          case 'g': return 'ᵍ';
          case 'h': return 'ʰ';
          case 'i': return 'ⁱ';
          case 'j': return 'ʲ';
          case 'k': return 'ᵏ';
          case 'l': return 'ˡ';
          case 'm': return 'ᵐ';
          case 'n': return 'ⁿ';
          case 'o': return 'ᵒ';
          case 'p': return 'ᵖ';
          case 'r': return 'ʳ';
          case 's': return 'ˢ';
          case 't': return 'ᵗ';
          case 'u': return 'ᵘ';
          case 'v': return 'ᵛ';
          case 'w': return 'ʷ';
          case 'x': return 'ˣ';
          case 'y': return 'ʸ';
          case 'z': return 'ᶻ';
          default: return c;
        }
      }
    };

  private static final Map_char map_char_subscript =
    new Map_char() {
      public char apply(char c)
      {
        switch (c)
        {
          case '1': return '₁';
          case '2': return '₂';
          case '3': return '₃';
          case '4': return '₄';
          case '5': return '₅';
          case '6': return '₆';
          case '7': return '₇';
          case '8': return '₈';
          case '9': return '₉';
          case '0': return '₀';
          case '+': return '₊';
          case '-': return '₋';
          case '=': return '₌';
          case '(': return '₍';
          case ')': return '₎';
          case 'a': return 'ₐ';
          case 'e': return 'ₑ';
          case 'h': return 'ₕ';
          case 'i': return 'ᵢ';
          case 'j': return 'ⱼ';
          case 'k': return 'ₖ';
          case 'l': return 'ₗ';
          case 'm': return 'ₘ';
          case 'n': return 'ₙ';
          case 'o': return 'ₒ';
          case 'p': return 'ₚ';
          case 'r': return 'ᵣ';
          case 's': return 'ₛ';
          case 't': return 'ₜ';
          case 'u': return 'ᵤ';
          case 'v': return 'ᵥ';
          case 'x': return 'ₓ';
          default: return c;
        }
      }
    };

  private static final Map_char map_char_arrows =
    new Map_char() {
      public char apply(char c)
      {
        switch (c)
        {
          case '1': return '↙';
          case '2': return '↓';
          case '3': return '↘';
          case '4': return '←';
          case '6': return '→';
          case '7': return '↖';
          case '8': return '↑';
          case '9': return '↗';
          default: return c;
        }
      }
    };

  private static final Map_char map_char_box =
    new Map_char() {
      public char apply(char c)
      {
        switch (c)
        {
          case '1': return '└';
          case '2': return '┴';
          case '3': return '┘';
          case '4': return '├';
          case '5': return '┼';
          case '6': return '┤';
          case '7': return '┌';
          case '8': return '┬';
          case '9': return '┐';
          case '0': return '─';
          case '.': return '│';
          default: return c;
        }
      }
    };

  private static final Map_char map_char_slash =
    new Map_char() {
      public char apply(char c)
      {
        switch (c)
        {
          case 'a': return 'ⱥ';
          case 'b': return '␢';
          case 'c': return 'ȼ';
          case 'e': return 'ɇ';
          case 'g': return 'ꞡ';
          case 'k': return 'ꝃ';
          case 'l': return 'ł';
          case 'n': return 'ꞥ';
          case 'o': return 'ø';
          case 'r': return 'ꞧ';
          case 's': return 'ꞩ';
          case 't': return 'ⱦ';
          default: return c;
        }
      }
    };

  private static final Map_char map_char_bar =
    new Map_char() {
      public char apply(char c)
      {
        switch (c)
        {
          case 'b': return 'ƀ';
          case 'c': return 'ꞓ';
          case 'd': return 'đ';
          case 'g': return 'ǥ';
          case 'i': return 'ɨ';
          case 'j': return 'ɉ';
          case 'k': return 'ꝁ';
          case 'l': return 'ƚ';
          case 'o': return 'ɵ';
          case 'p': return 'ᵽ';
          case 'q': return 'ꝗ';
          case 'r': return 'ɍ';
          case 't': return 'ŧ';
          case 'u': return 'ʉ';
          case 'y': return 'ɏ';
          case 'z': return 'ƶ';
          default: return c;
        }
      }
    };

  private static final Map_char map_char_dot_below =
          new Map_char() {
            public char apply(char c)
            {
              switch (c)
              {
                case 'a': return 'ạ';
                case 'ă': return 'ặ';
                case 'â': return 'ậ';
                case 'e': return 'ẹ';
                case 'ê': return 'ệ';
                case 'i': return 'ị';
                case 'o': return 'ọ';
                case 'ô': return 'ộ';
                case 'ơ': return 'ợ';
                case 'u': return 'ụ';
                case 'ư': return 'ự';
                case 'y': return 'ỵ';
                default: return c;
              }
            }
          };
  private static final Map_char map_char_horn =
          new Map_char() {
            public char apply(char c)
            {
              switch (c)
              {
                case 'o': return 'ơ';
                case 'ó': return 'ớ';
                case 'ò': return 'ờ';
                case 'ỏ': return 'ở';
                case 'õ': return 'ỡ';
                case 'ọ': return 'ợ';
                case 'u': return 'ư';
                case 'ú': return 'ứ';
                case 'ù': return 'ừ';
                case 'ủ': return 'ử';
                case 'ũ': return 'ữ';
                case 'ụ': return 'ự';
                default: return c;
              }
            }
          };

  private static final Map_char map_char_hook_above =
          new Map_char() {
            public char apply(char c)
            {
              switch (c)
              {
                case 'a': return 'ả';
                case 'ă': return 'ẳ';
                case 'â': return 'ẩ';
                case 'e': return 'ẻ';
                case 'ê': return 'ể';
                case 'i': return 'ỉ';
                case 'o': return 'ỏ';
                case 'ô': return 'ổ';
                case 'ơ': return 'ở';
                case 'u': return 'ủ';
                case 'ư': return 'ử';
                case 'y': return 'ỷ';
                default: return c;
              }
            }
          };
}
