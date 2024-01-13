package juloo.keyboard2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ExtraKeys
{
  public static final ExtraKeys EMPTY = new ExtraKeys(Collections.EMPTY_LIST);

  Collection<ExtraKey> _ks;

  public ExtraKeys(Collection<ExtraKey> ks)
  {
    _ks = ks;
  }

  /** Add the keys that should be added to the keyboard into [dst]. Keys
      already added to [dst] might have an impact, see [ExtraKey.compute].  */
  public void compute(Map<KeyValue, KeyboardData.PreferredPos> dst, Query q)
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
    /** The key next to which to add. Might be [null]. */
    final KeyValue next_to;

    ExtraKey(KeyValue kv_, String script_, List<KeyValue> alts_, KeyValue next_to_)
    {
      kv = kv_;
      script = script_;
      alternatives = alts_;
      next_to = next_to_;
    }

    /** Whether the key should be added to the keyboard. */
    public void compute(Map<KeyValue, KeyboardData.PreferredPos> dst, Query q)
    {
      // Add the alternative if it's the only one. The list of alternatives is
      // enforced to be complete by the merging step. The same [kv] will not
      // appear again in the list of extra keys with a different list of
      // alternatives.
      // Selecting the dead key in the "Add key to the keyboard" option would
      // disable this behavior for a key.
      boolean use_alternative = (alternatives.size() == 1 && !dst.containsKey(kv));
      if
        ((q.script == null || script == null || q.script.equals(script))
        && (alternatives.size() == 0 || !q.present.containsAll(alternatives)))
      {
        KeyValue kv_ = use_alternative ? alternatives.get(0) : kv;
        KeyboardData.PreferredPos pos = KeyboardData.PreferredPos.DEFAULT;
        if (next_to != null)
        {
          pos = new KeyboardData.PreferredPos(pos);
          pos.next_to = next_to;
        }
        dst.put(kv_, pos);
      }
    }

    /** Return a new key from two. [kv] are expected to be equal. [script] is
        generalized to [null] on any conflict. [alternatives] are concatenated.
        */
    public ExtraKey merge_with(ExtraKey k2)
    {
      String script_ = one_or_none(script, k2.script);
      List<KeyValue> alts = new ArrayList<KeyValue>(alternatives);
      KeyValue next_to_ = one_or_none(next_to, k2.next_to);
      alts.addAll(k2.alternatives);
      return new ExtraKey(kv, script_, alts, next_to_);
    }

    /** If one of [a] or [b] is null, return the other. If [a] and [b] are
        equal, return [a]. Otherwise, return null. */
    <E> E one_or_none(E a, E b)
    {
      return (a == null) ? b : (b == null || a.equals(b)) ? a : null;
    }

    /** Extra keys are of the form "key name" or "key name:alt1:alt2@next_to". */
    public static ExtraKey parse(String str, String script)
    {
      String[] split_on_at = str.split("@", 2);
      String[] key_names = split_on_at[0].split(":");
      KeyValue kv = KeyValue.getKeyByName(key_names[0]);
      KeyValue[] alts = new KeyValue[key_names.length-1];
      for (int i = 1; i < key_names.length; i++)
        alts[i-1] = KeyValue.getKeyByName(key_names[i]);
      KeyValue next_to = null;
      if (split_on_at.length > 1)
        next_to = KeyValue.getKeyByName(split_on_at[1]);
      return new ExtraKey(kv, script, Arrays.asList(alts), next_to);
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
