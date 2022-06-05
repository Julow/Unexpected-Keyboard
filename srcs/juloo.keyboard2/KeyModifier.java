package juloo.keyboard2;

import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import java.util.HashMap;

class KeyModifier
{
  /** Cache key is KeyValue's name */
  private static HashMap<String, HashMap<Pointers.Modifiers, KeyValue>> _cache =
    new HashMap<String, HashMap<Pointers.Modifiers, KeyValue>>();

  /** Represents a removed key, because a cache entry can't be [null]. */
  private static final KeyValue removed_key = KeyValue.getKeyByName("removed");

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
      r = remove_placeholders(r);
      ks.put(mods, r);
    }
    return (r == removed_key) ? null : r;
  }

  public static KeyValue modify(KeyValue k, int mod)
  {
    switch (mod)
    {
      case KeyValue.MOD_FN: return apply_fn(k);
      case KeyValue.MOD_SHIFT: return apply_shift(k);
      case KeyValue.MOD_GRAVE: return apply_dead_char(k, '\u02CB');
      case KeyValue.MOD_AIGU: return apply_dead_char(k, '\u00B4');
      case KeyValue.MOD_CIRCONFLEXE: return apply_dead_char(k, '\u02C6');
      case KeyValue.MOD_TILDE: return apply_dead_char(k, '\u02DC');
      case KeyValue.MOD_CEDILLE: return apply_dead_char(k, '\u00B8');
      case KeyValue.MOD_TREMA: return apply_dead_char(k, '\u00A8');
      case KeyValue.MOD_CARON: return apply_dead_char(k, '\u02C7');
      case KeyValue.MOD_RING: return apply_dead_char(k, '\u02DA');
      case KeyValue.MOD_MACRON: return apply_dead_char(k, '\u00AF');
      case KeyValue.MOD_OGONEK: return apply_dead_char(k, '\u02DB');
      case KeyValue.MOD_DOT_ABOVE: return apply_dead_char(k, '\u02D9');
      case KeyValue.MOD_DOUBLE_AIGU:
        return maybe_modify_char(k, map_char_double_aigu(k.char_));
      case KeyValue.MOD_ORDINAL:
        return maybe_modify_char(k, map_char_ordinal(k.char_));
      case KeyValue.MOD_SUPERSCRIPT:
        return maybe_modify_char(k, map_char_superscript(k.char_));
      case KeyValue.MOD_SUBSCRIPT:
        return maybe_modify_char(k, map_char_subscript(k.char_));
      case KeyValue.MOD_ARROWS:
        return maybe_modify_char(k, map_char_arrows(k.char_));
      case KeyValue.MOD_BOX:
        return maybe_modify_char(k, map_char_box(k.char_));
      case KeyValue.MOD_SLASH:
        return maybe_modify_char(k, map_char_slash(k.char_));
      case KeyValue.MOD_ARROW_RIGHT: return apply_combining(k, "\u20D7");
      default: return k;
    }
  }

  private static KeyValue apply_dead_char(KeyValue k, char dead_char)
  {
    char c = k.char_;
    if (c != KeyValue.CHAR_NONE)
      c = (char)KeyCharacterMap.getDeadChar(dead_char, c);
    return maybe_modify_char(k, c);
  }

  private static KeyValue apply_combining(KeyValue k, String combining)
  {
    if (k.char_ == KeyValue.CHAR_NONE)
      return k;
    return KeyValue.getKeyByName(String.valueOf(k.char_) + combining);
  }

  private static KeyValue apply_shift(KeyValue k)
  {
    char c = k.char_;
    if (c == KeyValue.CHAR_NONE)
    {
      // The key is a string
      if (k.code == KeyValue.EVENT_NONE)
        return k.withCharAndSymbol(k.symbol.toUpperCase(), KeyValue.CHAR_NONE);
      else
        return k;
    }
    c = map_char_shift(c);
    if (c == k.char_)
      c = Character.toUpperCase(c);
    return maybe_modify_char(k, c);
  }

  private static KeyValue apply_fn(KeyValue k)
  {
    String name;
    switch (k.name)
    {
      case "1": name = "f1"; break;
      case "2": name = "f2"; break;
      case "3": name = "f3"; break;
      case "4": name = "f4"; break;
      case "5": name = "f5"; break;
      case "6": name = "f6"; break;
      case "7": name = "f7"; break;
      case "8": name = "f8"; break;
      case "9": name = "f9"; break;
      case "0": name = "f10"; break;
      case "f11_placeholder": name = "f11"; break;
      case "f12_placeholder": name = "f12"; break;
      case "up": name = "page_up"; break;
      case "down": name = "page_down"; break;
      case "left": name = "home"; break;
      case "right": name = "end"; break;
      case "<": name = "«"; break;
      case ">": name = "»"; break;
      case "{": name = "‹"; break;
      case "}": name = "›"; break;
      case "[": name = "‘"; break;
      case "]": name = "’"; break;
      case "(": name = "“"; break;
      case ")": name = "”"; break;
      case "'": name = "‚"; break;
      case "\"": name = "„"; break;
      case "-": name = "–"; break;
      case "_": name = "—"; break;
      case "^": name = "¬"; break;
      case "%": name = "‰"; break;
      case "=": name = "≈"; break;
      case "u": name = "µ"; break;
      case "a": name = "æ"; break;
      case "o": name = "œ"; break;
      case "esc": name = "insert"; break;
      case "*": name = "°"; break;
      case ".": name = "…"; break;
      case ",": name = "·"; break;
      case "!": name = "¡"; break;
      case "?": name = "¿"; break;
      case "tab": name = "\\t"; break;
      case "space": name = "nbsp"; break;
      // arrows
      case "↖": name = "⇖"; break;
      case "↑": name = "⇑"; break;
      case "↗": name = "⇗"; break;
      case "←": name = "⇐"; break;
      case "→": name = "⇒"; break;
      case "↙": name = "⇙"; break;
      case "↓": name = "⇓"; break;
      case "↘": name = "⇘"; break;
      case "↔": name = "⇔"; break;
      case "↕": name = "⇕"; break;
      // Currency symbols
      case "e": name = "€"; break;
      case "l": name = "£"; break;
      case "r": name = "₹"; break;
      case "y": name = "¥"; break;
      case "c": name = "¢"; break;
      case "p": name = "₱"; break;
      case "€": case "£": return removed_key; // Avoid showing these twice
      // alternating greek letters
      case "π": name = "ϖ"; break;
      case "θ": name = "ϑ"; break;
      case "Θ": name = "ϴ"; break;
      case "ε": name = "ϵ"; break;
      case "β": name = "ϐ"; break;
      case "ρ": name = "ϱ"; break;
      case "σ": name = "ς"; break;
      case "γ": name = "ɣ"; break;
      case "φ": name = "ϕ"; break;
      case "υ": name = "ϒ"; break;
      case "κ": name = "ϰ"; break;
      // alternating math characters
      case "∪": name = "⋃"; break;
      case "∩": name = "⋂"; break;
      case "∃": name = "∄"; break;
      case "∈": name = "∉"; break;
      case "∫": name = "∮"; break;
      case "Π": name = "∏"; break;
      case "Σ": name = "∑"; break;
      case "∨": name = "⋁"; break;
      case "∧": name = "⋀"; break;
      case "⊷": name = "⊶"; break;
      case "⊂": name = "⊆"; break;
      case "⊃": name = "⊇"; break;
      case "±": name = "∓"; break;
      default: return k;
    }
    return KeyValue.getKeyByName(name);
  }

  /** Remove placeholder keys that haven't been modified into something. */
  private static KeyValue remove_placeholders(KeyValue k)
  {
    switch (k.name)
    {
      case "f11_placeholder":
      case "f12_placeholder": return removed_key;
      default: return k;
    }
  }

  /** Helper, update [k] with the char [c] if it's not [0]. */
  private static KeyValue maybe_modify_char(KeyValue k, char c)
  {
    if (c == 0 || c == KeyValue.CHAR_NONE || c == k.char_)
      return k;
    return k.withCharAndSymbol(c);
  }

  /* Lookup the cache entry for a key. Create it needed. */
  private static HashMap<Pointers.Modifiers, KeyValue> cacheEntry(KeyValue k)
  {
    HashMap<Pointers.Modifiers, KeyValue> ks = _cache.get(k.name);
    if (ks == null)
    {
      ks = new HashMap<Pointers.Modifiers, KeyValue>();
      _cache.put(k.name, ks);
    }
    return ks;
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
      default: return c;
    }
  }

  private static char map_char_double_aigu(char c)
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

  private static char map_char_ordinal(char c)
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

  private static char map_char_superscript(char c)
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
      case 'i': return 'ⁱ';
      case '+': return '⁺';
      case '-': return '⁻';
      case '=': return '⁼';
      case '(': return '⁽';
      case ')': return '⁾';
      case 'n': return 'ⁿ';
      default: return c;
    }
  }

  private static char map_char_subscript(char c)
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
      case 'e': return 'ₑ';
      case 'a': return 'ₐ';
      case 'x': return 'ₓ';
      case 'o': return 'ₒ';
      default: return c;
    }
  }

  private static char map_char_arrows(char c)
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

  private static char map_char_box(char c)
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

  private static char map_char_slash(char c)
  {
    switch (c)
    {
      case 'a': return 'ⱥ';
      case 'c': return 'ȼ';
      case 'e': return 'ɇ';
      case 'g': return 'ꞡ';
      case 'l': return 'ł';
      case 'n': return 'ꞥ';
      case 'o': return 'ø';
      case ' ': return '/';
      default: return c;
    }
  }
}
