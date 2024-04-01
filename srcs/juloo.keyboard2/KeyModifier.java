package juloo.keyboard2;

import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import java.util.HashMap;

public final class KeyModifier
{
  /** Cache key is KeyValue's name */
  private static HashMap<KeyValue, HashMap<Pointers.Modifiers, KeyValue>> _cache =
    new HashMap<KeyValue, HashMap<Pointers.Modifiers, KeyValue>>();

  /** The optional modmap takes priority over modifiers usual behaviors. Set to
      [null] to disable. */
  private static KeyboardData.Modmap _modmap = null;
  public static void set_modmap(KeyboardData.Modmap mm) { _modmap = mm; }

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
      case CTRL:
      case ALT:
      case META: return turn_into_keyevent(k);
      case FN: return apply_fn(k);
      case SHIFT: return apply_shift(k);
      case GRAVE: return apply_map_char(k, map_char_grave);
      case AIGU: return apply_map_char(k, map_char_aigu);
      case CIRCONFLEXE: return apply_map_char(k, map_char_circonflexe);
      case TILDE: return apply_map_char(k, map_char_tilde);
      case CEDILLE: return apply_map_char(k, map_char_cedille);
      case TREMA: return apply_map_char(k, map_char_trema);
      case CARON: return apply_map_char(k, map_char_caron);
      case RING: return apply_map_char(k, map_char_ring);
      case MACRON: return apply_map_char(k, map_char_macron);
      case OGONEK: return apply_map_char(k, map_char_ogonek);
      case DOT_ABOVE: return apply_map_char(k, map_char_dot_above);
      case BREVE: return apply_map_char(k, map_char_breve);
      case DOUBLE_AIGU: return apply_map_char(k, map_char_double_aigu);
      case ORDINAL: return apply_map_char(k, map_char_ordinal);
      case SUPERSCRIPT: return apply_map_char(k, map_char_superscript);
      case SUBSCRIPT: return apply_map_char(k, map_char_subscript);
      case ARROWS: return apply_map_char(k, map_char_arrows);
      case BOX: return apply_map_char(k, map_char_box);
      case SLASH: return apply_map_char(k, map_char_slash);
      case BAR: return apply_map_char(k, map_char_bar);
      case ARROW_RIGHT: return apply_map_char(k, map_char_arrow_right);
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
        if (modified == null)
          return k;
        return KeyValue.makeStringKey(modified, k.getFlags());
      default: return k;
    }
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

  /** Return [null] if the dead char do not apply. */
  private static String map_dead_char(char c, char dead_char)
  {
    char modified = (char)KeyCharacterMap.getDeadChar(dead_char, c);
    return (modified == 0 || modified == c) ? null : String.valueOf(modified);
  }

  private static final Map_char map_char_aigu =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          case 'a': return "á";
          case 'e': return "é";
          case 'i': return "í";
          case 'l': return "ĺ";
          case 'o': return "ó";
          case 'r': return "ŕ";
          case 's': return "ś";
          case 'u': return "ú";
          case 'y': return "ý";
          // Combining diacritic
          case 'j':
          // Russian vowels
          case 'у': case 'е': case 'а': case 'о': case 'и':
          case 'ы': case 'э': case 'ю': case 'я':
            return c + "\u0301";
          default: return map_dead_char(c, '\u00B4');
        }
      }
    };

  private static final Map_char map_char_caron =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          case 'c': return "č";
          case 'd': return "ď";
          case 'e': return "ě";
          case 'l': return "ľ";
          case 'n': return "ň";
          case 'r': return "ř";
          case 's': return "š";
          case 't': return "ť";
          case 'z': return "ž";
          default: return map_dead_char(c, '\u02C7');
        }
      }
    };

  private static final Map_char map_char_cedille =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          case 'c': return "ç";
          case 's': return "ş";
          case 'g': return "ģ";
          default: return map_dead_char(c, '\u00B8');
        }
      }
    };

  private static final Map_char map_char_circonflexe =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          case 'a': return "â";
          case 'e': return "ê";
          case 'i': return "î";
          case 'o': return "ô";
          case 'u': return "û";
          default: return map_dead_char(c, '\u02C6');
        }
      }
    };

  private static final Map_char map_char_dot_above =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          case 'ė': return "ė";
          default: return map_dead_char(c, '\u02D9');
        }
      }
    };

  private static final Map_char map_char_grave =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          case 'a': return "à";
          case 'e': return "è";
          case 'i': return "ì";
          case 'o': return "ò";
          case 'u': return "ù";
          default: return map_dead_char(c, '\u02CB');
        }
      }
    };

  private static final Map_char map_char_macron =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          case 'a': return "ā";
          case 'e': return "ē";
          case 'i': return "ī";
          case 'u': return "ū";
          default: return map_dead_char(c, '\u00AF');
        }
      }
    };

  private static final Map_char map_char_ogonek =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          case 'a': return "ą";
          case 'e': return "ę";
          case 'i': return "į";
          case 'k': return "ķ";
          case 'l': return "ļ";
          case 'n': return "ņ";
          case 'u': return "ų";
          default: return map_dead_char(c, '\u02DB');
        }
      }
    };

  private static final Map_char map_char_ring =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          case 'a': return "å";
          case 'u': return "ů";
          default: return map_dead_char(c, '\u02DA');
        }
      }
    };

  private static final Map_char map_char_tilde =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          case 'a': return "ã";
          case 'o': return "õ";
          case 'n': return "ñ";
          default: return map_dead_char(c, '\u02DC');
        }
      }
    };

  private static final Map_char map_char_trema =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          case 'a': return "ä";
          case 'e': return "ë";
          case 'i': return "ï";
          case 'o': return "ö";
          case 'u': return "ü";
          case 'y': return "ÿ";
          default: return map_dead_char(c, '\u00A8');
        }
      }
    };

  private static final Map_char map_char_breve =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          default: return map_dead_char(c, '\u02D8');
        }
      }
    };

  private static final Map_char map_char_double_aigu =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          case 'o': return "ő";
          case 'u': return "ű";
          case ' ': return "˝";
          // Combining diacritic
          case 'a':
          case 'e':
          case 'i':
          case 'm':
          case 'y':
            return c + "\u030b";
          default: return null;
        }
      }
    };

  private static final Map_char map_char_ordinal =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          case 'a': return "ª";
          case 'o': return "º";
          case '1': return "ª";
          case '2': return "º";
          case '3': return "ⁿ";
          case '4': return "ᵈ";
          case '5': return "ᵉ";
          case '6': return "ʳ";
          case '7': return "ˢ";
          case '8': return "ᵗ";
          case '9': return "ʰ";
          case '*': return "°";
          default: return null;
        }
      }
    };

  private static final Map_char map_char_superscript =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          case '1': return "¹";
          case '2': return "²";
          case '3': return "³";
          case '4': return "⁴";
          case '5': return "⁵";
          case '6': return "⁶";
          case '7': return "⁷";
          case '8': return "⁸";
          case '9': return "⁹";
          case '0': return "⁰";
          case '+': return "⁺";
          case '-': return "⁻";
          case '=': return "⁼";
          case '(': return "⁽";
          case ')': return "⁾";
          case 'a': return "ᵃ";
          case 'b': return "ᵇ";
          case 'c': return "ᶜ";
          case 'd': return "ᵈ";
          case 'e': return "ᵉ";
          case 'f': return "ᶠ";
          case 'g': return "ᵍ";
          case 'h': return "ʰ";
          case 'i': return "ⁱ";
          case 'j': return "ʲ";
          case 'k': return "ᵏ";
          case 'l': return "ˡ";
          case 'm': return "ᵐ";
          case 'n': return "ⁿ";
          case 'o': return "ᵒ";
          case 'p': return "ᵖ";
          case 'r': return "ʳ";
          case 's': return "ˢ";
          case 't': return "ᵗ";
          case 'u': return "ᵘ";
          case 'v': return "ᵛ";
          case 'w': return "ʷ";
          case 'x': return "ˣ";
          case 'y': return "ʸ";
          case 'z': return "ᶻ";
          default: return null;
        }
      }
    };

  private static final Map_char map_char_subscript =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          case '1': return "₁";
          case '2': return "₂";
          case '3': return "₃";
          case '4': return "₄";
          case '5': return "₅";
          case '6': return "₆";
          case '7': return "₇";
          case '8': return "₈";
          case '9': return "₉";
          case '0': return "₀";
          case '+': return "₊";
          case '-': return "₋";
          case '=': return "₌";
          case '(': return "₍";
          case ')': return "₎";
          case 'a': return "ₐ";
          case 'e': return "ₑ";
          case 'h': return "ₕ";
          case 'i': return "ᵢ";
          case 'j': return "ⱼ";
          case 'k': return "ₖ";
          case 'l': return "ₗ";
          case 'm': return "ₘ";
          case 'n': return "ₙ";
          case 'o': return "ₒ";
          case 'p': return "ₚ";
          case 'r': return "ᵣ";
          case 's': return "ₛ";
          case 't': return "ₜ";
          case 'u': return "ᵤ";
          case 'v': return "ᵥ";
          case 'x': return "ₓ";
          default: return null;
        }
      }
    };

  private static final Map_char map_char_arrows =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          case '1': return "↙";
          case '2': return "↓";
          case '3': return "↘";
          case '4': return "←";
          case '6': return "→";
          case '7': return "↖";
          case '8': return "↑";
          case '9': return "↗";
          default: return null;
        }
      }
    };

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

  private static final Map_char map_char_box =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          case '1': return "└";
          case '2': return "┴";
          case '3': return "┘";
          case '4': return "├";
          case '5': return "┼";
          case '6': return "┤";
          case '7': return "┌";
          case '8': return "┬";
          case '9': return "┐";
          case '0': return "─";
          case '.': return "│";
          default: return null;
        }
      }
    };

  private static final Map_char map_char_slash =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          case 'a': return "ⱥ";
          case 'b': return "␢";
          case 'c': return "ȼ";
          case 'e': return "ɇ";
          case 'g': return "ꞡ";
          case 'k': return "ꝃ";
          case 'l': return "ł";
          case 'n': return "ꞥ";
          case 'o': return "ø";
          case 'r': return "ꞧ";
          case 's': return "ꞩ";
          case 't': return "ⱦ";
          default: return null;
        }
      }
    };

  private static final Map_char map_char_bar =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          case 'b': return "ƀ";
          case 'c': return "ꞓ";
          case 'd': return "đ";
          case 'g': return "ǥ";
          case 'i': return "ɨ";
          case 'j': return "ɉ";
          case 'k': return "ꝁ";
          case 'l': return "ƚ";
          case 'o': return "ɵ";
          case 'p': return "ᵽ";
          case 'q': return "ꝗ";
          case 'r': return "ɍ";
          case 't': return "ŧ";
          case 'u': return "ʉ";
          case 'y': return "ɏ";
          case 'z': return "ƶ";
          default: return null;
        }
      }
    };

  private static final Map_char map_char_dot_below =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          case 'a': return "ạ";
          case 'ă': return "ặ";
          case 'â': return "ậ";
          case 'e': return "ẹ";
          case 'ê': return "ệ";
          case 'i': return "ị";
          case 'o': return "ọ";
          case 'ô': return "ộ";
          case 'ơ': return "ợ";
          case 'u': return "ụ";
          case 'ư': return "ự";
          case 'y': return "ỵ";
          default: return null;
        }
      }
    };

  private static final Map_char map_char_horn =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          case 'o': return "ơ";
          case 'ó': return "ớ";
          case 'ò': return "ờ";
          case 'ỏ': return "ở";
          case 'õ': return "ỡ";
          case 'ọ': return "ợ";
          case 'u': return "ư";
          case 'ú': return "ứ";
          case 'ù': return "ừ";
          case 'ủ': return "ử";
          case 'ũ': return "ữ";
          case 'ụ': return "ự";
          default: return null;
        }
      }
    };

  private static final Map_char map_char_hook_above =
    new Map_char() {
      public String apply(char c)
      {
        switch (c)
        {
          case 'a': return "ả";
          case 'ă': return "ẳ";
          case 'â': return "ẩ";
          case 'e': return "ẻ";
          case 'ê': return "ể";
          case 'i': return "ỉ";
          case 'o': return "ỏ";
          case 'ô': return "ổ";
          case 'ơ': return "ở";
          case 'u': return "ủ";
          case 'ư': return "ử";
          case 'y': return "ỷ";
          default: return null;
        }
      }
    };

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
