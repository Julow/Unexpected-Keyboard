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
    if ((r = handleChar(k, flags)) != null) ;
    else if ((r = handleFn(k, flags)) != null) ;
    else r = k;
    ks.put(flags, r);
    return r;
  }

  /* Returns [null] if had no effect. */
  private static KeyValue handleChar(KeyValue k, int flags)
  {
    char c = k.char_;
    if (c == KeyValue.CHAR_NONE)
      return null;
    if ((flags & KeyValue.FLAG_SHIFT) != 0) // Shift
      c = Character.toUpperCase(c);
    if ((flags & KeyValue.FLAGS_ACCENTS) != 0) // Accents, after shift is applied
      c = handleAccentChar(c, flags);
    if (c == k.char_)
      return null;
    return k.withCharAndSymbol(String.valueOf(c), c);
  }

  private static char handleAccentChar(char c, int flags)
  {
    char accent;
    switch ((flags & KeyValue.FLAGS_ACCENTS))
    {
      case KeyValue.FLAG_ACCENT1: accent = '\u02CB'; break;
      case KeyValue.FLAG_ACCENT2: accent = '\u00B4'; break;
      case KeyValue.FLAG_ACCENT3: accent = '\u02C6'; break;
      case KeyValue.FLAG_ACCENT4: accent = '\u02DC'; break;
      case KeyValue.FLAG_ACCENT5: accent = '\u00B8'; break;
      case KeyValue.FLAG_ACCENT6: accent = '\u00A8'; break;
      default: return c; // Can't happen
    }
    char r = (char)KeyCharacterMap.getDeadChar(accent, (int)c);
    return (r == 0) ? c : r;
  }

  private static KeyValue handleFn(KeyValue k, int flags)
  {
    if ((flags & KeyValue.FLAG_FN) == 0)
      return null;
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
      default: return null;
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
