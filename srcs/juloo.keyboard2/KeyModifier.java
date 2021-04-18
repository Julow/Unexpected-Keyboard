package juloo.keyboard2;

import android.util.SparseArray;
import android.view.KeyCharacterMap;
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
    char c = handleChar(k.char_, flags);
    if (c == k.char_) // Don't override the symbol if the char didn't change
      r = k;
    else
      r = k.withCharAndSymbol(String.valueOf(c), c);
    ks.put(flags, r);
    return r;
  }

  private static char handleChar(char c, int flags)
  {
    if (c == KeyValue.CHAR_NONE)
      return c;
    if ((flags & KeyValue.FLAG_SHIFT) != 0) // Shift
      c = Character.toUpperCase(c);
    if ((flags & KeyValue.FLAGS_ACCENTS) != 0) // Accents, after shift is applied
      c = handleAccentChar(c, flags);
    return c;
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
