package juloo.keyboard2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ExtraKeys
{
  public static final ExtraKeys EMPTY = new ExtraKeys(Collections.EMPTY_LIST);

  Collection<ExtraKey> _ks;

  public ExtraKeys(Collection<ExtraKey> ks)
  {
    _ks = ks;
  }

  /** Add the keys that should be added to the keyboard into [dst]. Keys
      already added to [dst] might have an impact, see [ExtraKey.compute].  */
  public void compute(Set<KeyValue> dst, Query q)
  {
    for (ExtraKey k : _ks)
      k.compute(dst, q);
  }

  public static ExtraKeys parse(String script, String str)
  {
    Collection<ExtraKey> dst = new ArrayList<ExtraKey>();
    String[] ks = str.split("\\|");
    for (int i = 0; i < ks.length; i++)
      dst.add(ExtraKey.parse(ks[i], script));
    return new ExtraKeys(dst);
  }

  /** Merge identical keys. This is required to decide whether to add
      alternatives. Script is generalized (set to null) on any conflict. */
  public static ExtraKeys merge(List<ExtraKeys> kss)
  {
    Map<KeyValue, ExtraKey> merged_keys = new HashMap<KeyValue, ExtraKey>();
    for (ExtraKeys ks : kss)
      for (ExtraKey k : ks._ks)
      {
        ExtraKey k2 = merged_keys.get(k.kv);
        if (k2 != null)
          k = k.merge_with(k2);
        merged_keys.put(k.kv, k);
      }
    return new ExtraKeys(merged_keys.values());
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
    public void compute(Set<KeyValue> dst, Query q)
    {
      // Add the alternative if it's the only one. The list of alternatives is
      // enforced to be complete by the merging step. The same [kv] will not
      // appear again in the list of extra keys with a different list of
      // alternatives.
      // Selecting the dead key in the "Add key to the keyboard" option would
      // disable this behavior for a key.
      boolean use_alternative = (alternatives.size() == 1 && !dst.contains(kv));
      if
        ((q.script == null || script == null || q.script.equals(script))
        && (alternatives.size() == 0 || !q.present.containsAll(alternatives)))
        dst.add(use_alternative ? alternatives.get(0) : kv);
    }

    /** Return a new key from two. [kv] are expected to be equal. [script] is
        generalized to [null] on any conflict. [alternatives] are concatenated.
        */
    public ExtraKey merge_with(ExtraKey k2)
    {
      String script_ =
        (script != null && k2.script != null && script.equals(k2.script))
        ? script : null;
      List<KeyValue> alts = new ArrayList<KeyValue>(alternatives);
      alts.addAll(k2.alternatives);
      return new ExtraKey(kv, script_, alts);
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
