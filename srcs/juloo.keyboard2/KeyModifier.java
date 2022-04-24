package juloo.keyboard2;

import android.util.SparseArray;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import java.util.HashMap;

class KeyModifier
{
  /** Cache key is KeyValue's name */
  private static HashMap<String, SparseArray<KeyValue>> _cache =
    new HashMap<String, SparseArray<KeyValue>>();

  /** Represents a removed key, because a cache entry can't be [null]. */
  private static final KeyValue removed_key = KeyValue.getKeyByName("removed");

  /** Modify a key according to modifiers. */
  public static KeyValue handleFlags(KeyValue k, int flags)
  {
    if (k == null)
      return null;
    SparseArray<KeyValue> ks = cacheEntry(k);
    KeyValue r = ks.get(flags);
    if (r == null) // Cold cache
    {
      r = k;
      r = handleFn(r, flags);
      r = handleShift(r, flags);
      r = handleAccents(r, flags);
      ks.put(flags, r);
    }
    return (r == removed_key) ? null : r;
  }

  private static KeyValue handleAccents(KeyValue k, int flags)
  {
    if (k.char_ == KeyValue.CHAR_NONE || (flags & KeyValue.FLAGS_ACCENTS) == 0)
      return k;
    char c = handleAccentChar(k.char_, flags);
    if (c == 0 || c == k.char_)
      return k;
    return k.withCharAndSymbol(c);
  }

  private static KeyValue handleShift(KeyValue k, int flags)
  {
    if ((flags & KeyValue.FLAG_SHIFT) == 0)
      return k;
    char c = k.char_;
    if (k.char_ != KeyValue.CHAR_NONE)
      c = Character.toUpperCase(c);
    if (c == k.char_) // Used to have more rules if toUpperCase() did nothing
      return k;
    return k.withCharAndSymbol(c);
  }

  private static char handleAccentChar(char c, int flags)
  {
    switch ((flags & KeyValue.FLAGS_ACCENTS))
    {
      case KeyValue.FLAG_ACCENT1:
        return (char)KeyCharacterMap.getDeadChar('\u02CB', c);
      case KeyValue.FLAG_ACCENT2:
        return (char)KeyCharacterMap.getDeadChar('\u00B4', c);
      case KeyValue.FLAG_ACCENT3:
        return (char)KeyCharacterMap.getDeadChar('\u02C6', c);
      case KeyValue.FLAG_ACCENT4:
        return (char)KeyCharacterMap.getDeadChar('\u02DC', c);
      case KeyValue.FLAG_ACCENT5:
        return (char)KeyCharacterMap.getDeadChar('\u00B8', c);
      case KeyValue.FLAG_ACCENT6:
        return (char)KeyCharacterMap.getDeadChar('\u00A8', c);
      case KeyValue.FLAG_ACCENT_CARON:
        return (char)KeyCharacterMap.getDeadChar('\u02C7', c);
      case KeyValue.FLAG_ACCENT_RING:
        return (char)KeyCharacterMap.getDeadChar('\u02DA', c);
      case KeyValue.FLAG_ACCENT_MACRON:
        return (char)KeyCharacterMap.getDeadChar('\u00AF', c);
      case KeyValue.FLAG_ACCENT_OGONEK:
        return (char)KeyCharacterMap.getDeadChar('\u02DB', c);
      case KeyValue.FLAG_ACCENT_DOT_ABOVE:
        return (char)KeyCharacterMap.getDeadChar('\u02D9', c);
      case KeyValue.FLAG_ACCENT_DOUBLE_AIGU:
        switch (c)
        {
          // Composite characters: a̋ e̋ i̋ m̋ ӳ
          case 'o': return 'ő';
          case 'u': return 'ű';
          case ' ': return '˝';
          default: return c;
        }
      case KeyValue.FLAG_ACCENT_ORDINAL:
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
      case KeyValue.FLAG_ACCENT_SUPERSCRIPT:
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
      case KeyValue.FLAG_ACCENT_SUBSCRIPT:
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
      case KeyValue.FLAG_ACCENT_ARROWS:
        if ((flags & KeyValue.FLAG_SHIFT) == 0)
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
        else
        {
          switch (c)
          {
            case '1': return '⇙';
            case '2': return '⇓';
            case '3': return '⇘';
            case '4': return '⇐';
            case '6': return '⇒';
            case '7': return '⇖';
            case '8': return '⇑';
            case '9': return '⇗';
            default: return c;
          }
        }
      case KeyValue.FLAG_ACCENT_BOX:
        if ((flags & KeyValue.FLAG_SHIFT) == 0)
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
        else
        {
          switch (c)
          {
            case '1': return '╚';
            case '2': return '╩';
            case '3': return '╝';
            case '4': return '╠';
            case '5': return '╬';
            case '6': return '╣';
            case '7': return '╔';
            case '8': return '╦';
            case '9': return '╗';
            case '0': return '═';
            case '.': return '║';
            default: return c;
          }
        }
      default: return c; // Can't happen
    }
  }

  private static KeyValue handleFn(KeyValue k, int flags)
  {
    if ((flags & KeyValue.FLAG_FN) == 0)
    {
      switch (k.name)
      {
        // Remove some keys when Fn is *not* activated
        case "f11_placeholder":
        case "f12_placeholder": return removed_key;
        default: return k;
      }
    }
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
      case "↖": name = "⇖"; break;
      case "↑": name = "⇑"; break;
      case "↗": name = "⇗"; break;
      case "←": name = "⇐"; break;
      case "→": name = "⇒"; break;
      case "↙": name = "⇙"; break;
      case "↓": name = "⇓"; break;
      case "↘": name = "⇘"; break;
      // Currency symbols
      case "e": name = "€"; break;
      case "l": name = "£"; break;
      case "r": name = "₹"; break;
      case "y": name = "¥"; break;
      case "c": name = "¢"; break;
      case "p": name = "₱"; break;
      case "€": case "£": return removed_key; // Avoid showing these twice
      default: return k;
    }
    return KeyValue.getKeyByName(name);
  }

  /* Lookup the cache entry for a key. Create it needed. */
  private static SparseArray<KeyValue> cacheEntry(KeyValue k)
  {
    SparseArray<KeyValue> ks = _cache.get(k.name);
    if (ks == null)
    {
      ks = new SparseArray<KeyValue>();
      _cache.put(k.name, ks);
    }
    return ks;
  }
}
