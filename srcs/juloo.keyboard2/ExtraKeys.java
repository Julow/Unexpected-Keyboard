package juloo.keyboard2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ExtraKeys
{
  Map<String, List<KeyValue>> _keys_per_script;

  public ExtraKeys()
  {
    _keys_per_script = new HashMap<String, List<KeyValue>>();
  }

  public void add_keys_for_script(String script, List<KeyValue> kvs)
  {
    List<KeyValue> ks = _keys_per_script.get(script);
    if (ks == null) ks = new ArrayList<KeyValue>();
    ks.addAll(kvs);
    _keys_per_script.put(script, ks);
  }

  /** Add the keys that should be added to the keyboard into [dst]. [null] is
      a valid script. */
  public void compute(Set<KeyValue> dst, String script)
  {
    if (script == null)
    {
      for (String sc : _keys_per_script.keySet())
        get_keys_of_script(dst, sc);
    }
    else
    {
      get_keys_of_script(dst, null);
      get_keys_of_script(dst, script);
    }
  }

  void get_keys_of_script(Set<KeyValue> dst, String script)
  {
    List<KeyValue> ks = _keys_per_script.get(script);
    if (ks != null)
      dst.addAll(ks);
  }

  public static List<KeyValue> parse_extra_keys(String str)
  {
    List<KeyValue> dst = new ArrayList<KeyValue>();
    String[] ks = str.split("\\|");
    for (int i = 0; i < ks.length; i++)
      dst.add(KeyValue.getKeyByName(ks[i]));
    return dst;
  }
}
