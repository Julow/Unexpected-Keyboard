package juloo.keyboard2;

import android.util.SparseArray;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import java.util.HashMap;

class KeyModifier
{
  /* Cache key is KeyValue's name */
  private static HashMap<String, SparseArray<KeyValue>> _cache =
    new HashMap<String, SparseArray<KeyValue>>();

  /* Modify a key according to modifiers. */
  public static KeyValue handleFlags(KeyValue k, int flags)
  {
    if (flags == 0) // No modifier
      return k;
    SparseArray<KeyValue> ks = cacheEntry(k);
    KeyValue r = ks.get(flags);
    if (r != null) // Found in cache
      return r;
    r = k;
    r = handleFn(r, flags);
    r = handleShift(r, flags);
    r = handleAccents(r, flags);
    ks.put(flags, r);
    return r;
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
    if (c == k.char_) // More rules if toUpperCase() did nothing
      switch (k.symbol)
      {
        case "→": c = '⇒'; break;
        case "←": c = '⇐'; break;
        default: return k;
      }
    return k.withCharAndSymbol(c);
  }

  private static char handleAccentChar(char c, int flags)
  {
    switch ((flags & KeyValue.FLAGS_ACCENTS))
    {
      case KeyValue.FLAG_ACCENT1:
        return (char)KeyCharacterMap.getDeadChar('\u02CB', c);
      case KeyValue.FLAG_ACCENT2:
        switch (c)
        {
          case '`': return '´';
          case '<': return '‘';
          case '>': return '‘';
          default: return (char)KeyCharacterMap.getDeadChar('\u00B4', c);
        }
      case KeyValue.FLAG_ACCENT3:
        switch (c)
        {
          case '*': return '°';
          default: return (char)KeyCharacterMap.getDeadChar('\u02C6', c);
        }
      case KeyValue.FLAG_ACCENT4:
        switch (c)
        {
          case '?': return '¿';
          case '!': return '¡';
          default: return (char)KeyCharacterMap.getDeadChar('\u02DC', c);
        }
      case KeyValue.FLAG_ACCENT5:
        switch (c)
        {
          case 'u': return 'µ';
          case '"': return '„';
          case '-': return '¬';
          case 'a': return 'æ';
          case 'o': return 'œ';
          default: return (char)KeyCharacterMap.getDeadChar('\u00B8', c);
        }
      case KeyValue.FLAG_ACCENT6:
        switch (c)
        {
          case '-': return '÷';
          default: return (char)KeyCharacterMap.getDeadChar('\u00A8', c);
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
      default: return c; // Can't happen
    }
  }

  private static KeyValue handleFn(KeyValue k, int flags)
  {
    if ((flags & KeyValue.FLAG_FN) == 0)
      return k;
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
      case "up": name = "page_up"; break;
      case "down": name = "page_down"; break;
      case "left": name = "home"; break;
      case "right": name = "end"; break;
      case ">": name = "→"; break;
      case "<": name = "←"; break;
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
