package juloo.keyboard2;

import juloo.keyboard2.KeyModifier;
import juloo.keyboard2.KeyValue;

/** Utils to make writing tests easier. */
public final class TestUtils
{
  /** Evaluate a macro and return the last key. Used to test KeyModifier.
      Similar to [KeyEventHandler.evaluate_macro]. */
  public static KeyValue eval(String... ks)
  {
    Pointers.Modifiers mods = Pointers.Modifiers.EMPTY;
    KeyValue kv = null;
    for (String next_k : ks)
    {
      kv = KeyModifier.modify(KeyValue.getKeyByName(next_k), mods);
      if (kv == null) break;
      if (!kv.hasFlagsAny(KeyValue.FLAG_SPECIAL))
        mods = Pointers.Modifiers.EMPTY;
      if (kv.hasFlagsAny(KeyValue.FLAG_LATCH))
        mods = mods.with_extra_mod(kv);
    }
    return kv;
  }

  public static KeyValue key(String s)
  {
    return KeyValue.getKeyByName(s);
  }

  public static KeyValue str(String s)
  {
    return KeyValue.makeStringKey(s);
  }

  public static KeyValue str(String s, int flags)
  {
    return KeyValue.makeStringKey(s, flags);
  }
}
