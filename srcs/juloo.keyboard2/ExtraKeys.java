package juloo.keyboard2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ExtraKeys
{
  List<ExtraKey> _ks;

  public ExtraKeys()
  {
    _ks = new ArrayList<ExtraKey>();
  }

  public void parse_and_add_keys_for_script(String script, String extra_keys_str)
  {
    _ks.addAll(parse_extra_keys(script, extra_keys_str));
  }

  /** Add the keys that should be added to the keyboard into [dst]. */
  public void compute(Set<KeyValue> dst, Query q)
  {
    for (ExtraKey k : _ks)
    {
      if (k.should_add(q))
        dst.add(k.kv);
    }
  }

  public static List<ExtraKey> parse_extra_keys(String script, String str)
  {
    List<ExtraKey> dst = new ArrayList<ExtraKey>();
    String[] ks = str.split("\\|");
    for (int i = 0; i < ks.length; i++)
      dst.add(ExtraKey.parse(ks[i], script));
    return dst;
  }

  final static class ExtraKey
  {
    /** The key to add. */
    final KeyValue kv;
    /** The key will be added to layouts of the same script. If null, might be
        added to layouts of any script. */
    final String script;
    /** The key will not be added to layout that already contain all the
        alternatives. */
    final List<KeyValue> alternatives;

    ExtraKey(KeyValue kv_, String script_, List<KeyValue> alts_)
    {
      kv = kv_;
      script = script_;
      alternatives = alts_;
    }

    /** Whether the key should be added to the keyboard. */
    public boolean should_add(Query q)
    {
      return
        (q.script == null || script == null || q.script.equals(script))
        && (alternatives.size() == 0 || !q.present.containsAll(alternatives));
    }

    /** Extra keys are of the form "key name" or "key name:alt 1:alt 2". */
    public static ExtraKey parse(String str, String script)
    {
      String[] strs = str.split(":");
      KeyValue kv = KeyValue.getKeyByName(strs[0]);
      KeyValue[] alts = new KeyValue[strs.length-1];
      for (int i = 1; i < strs.length; i++)
        alts[i-1] = KeyValue.getKeyByName(strs[i]);
      return new ExtraKey(kv, script, Arrays.asList(alts));
    }
  }

  public final static class Query
  {
    /** Script of the current layout. Might be null. */
    final String script;
    /** Keys present on the layout. */
    final Set<KeyValue> present;

    public Query(String script_, Set<KeyValue> present_)
    {
      script = script_;
      present = present_;
    }
  }
}
