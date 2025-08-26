package juloo.keyboard2;

import java.lang.reflect.Array;
import java.util.Map;
import java.util.TreeMap;

/** Stores key combinations that are applied by [KeyModifier]. */
public final class Modmap
{
  public enum M { Shift, Fn, Ctrl, Gesture }

  Map<KeyValue, KeyValue>[] _map;

  public Modmap()
  {
    _map = (Map<KeyValue, KeyValue>[])Array.newInstance(TreeMap.class,
        M.values().length);
  }

  public void add(M m, KeyValue a, KeyValue b)
  {
    int i = m.ordinal();
    if (_map[i] == null)
      _map[i] = new TreeMap<KeyValue, KeyValue>();
    _map[i].put(a, b);
  }

  public KeyValue get(M m, KeyValue a)
  {
    Map<KeyValue, KeyValue> mm = _map[m.ordinal()];
    return (mm == null) ? null : mm.get(a);
  }
}
