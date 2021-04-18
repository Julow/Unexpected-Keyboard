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
    switch (k.char_)
    {
      case '1': return makeFnKey("F1", KeyEvent.KEYCODE_F1);
      case '2': return makeFnKey("F2", KeyEvent.KEYCODE_F2);
      case '3': return makeFnKey("F3", KeyEvent.KEYCODE_F3);
      case '4': return makeFnKey("F4", KeyEvent.KEYCODE_F4);
      case '5': return makeFnKey("F5", KeyEvent.KEYCODE_F5);
      case '6': return makeFnKey("F6", KeyEvent.KEYCODE_F6);
      case '7': return makeFnKey("F7", KeyEvent.KEYCODE_F7);
      case '8': return makeFnKey("F8", KeyEvent.KEYCODE_F8);
      case '9': return makeFnKey("F9", KeyEvent.KEYCODE_F9);
      case '0': return makeFnKey("F10", KeyEvent.KEYCODE_F10);
      default: return null;
    }
  }

  private static KeyValue makeFnKey(String symbol, int eventCode)
  {
    return new KeyValue(symbol, symbol, KeyValue.CHAR_NONE, eventCode, 0);
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
