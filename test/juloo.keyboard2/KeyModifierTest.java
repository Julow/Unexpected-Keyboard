package juloo.keyboard2;

import juloo.keyboard2.KeyModifier;
import juloo.keyboard2.KeyValue;
import org.junit.Test;
import static org.junit.Assert.*;

public class KeyModifierTest
{
  public KeyModifierTest() {}

  @Test
  public void compose() throws Exception
  {
    assertEquals(eval("compose", "space", "space"), str(" "));
    assertEquals(eval("compose", "-", "space"), str("~"));
    assertEquals(eval("compose", "space", "-"), str("~"));
  }

  // Similar to [KeyEventHandler.evaluate_macro].
  static KeyValue eval(String... ks)
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

  static KeyValue str(String s)
  {
    return KeyValue.makeStringKey(s);
  }
}
