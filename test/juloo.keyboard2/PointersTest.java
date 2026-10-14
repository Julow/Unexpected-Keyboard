package juloo.keyboard2;

import juloo.keyboard2.KeyValue;
import juloo.keyboard2.Pointers;
import org.junit.Test;
import static org.junit.Assert.*;

public class PointersTest
{
  public PointersTest() {}

  /** Sticky keys are locked as soon as they latch, this is what makes them
      stay activated until pressed again. */
  @Test
  public void sticky_modifier() throws Exception
  {
    int flags = Pointers.pointer_flags_of_kv(
        KeyValue.getKeyByName("accent_small_caps"), false);
    assertTrue((flags & Pointers.FLAG_P_LATCHABLE) != 0);
    assertTrue((flags & Pointers.FLAG_P_LOCKED) != 0);
  }

  @Test
  public void regular_modifiers() throws Exception
  {
    int flags = Pointers.pointer_flags_of_kv(
        KeyValue.getKeyByName("accent_aigu"), false);
    assertTrue((flags & Pointers.FLAG_P_LATCHABLE) != 0);
    assertEquals(0, flags & Pointers.FLAG_P_LOCKED);
    // Whether double tap locks the shift key depends on the configuration.
    assertTrue((Pointers.pointer_flags_of_kv(KeyValue.SHIFT, true)
          & Pointers.FLAG_P_DOUBLE_TAP_LOCK) != 0);
    assertEquals(0, Pointers.pointer_flags_of_kv(KeyValue.SHIFT, false)
        & Pointers.FLAG_P_DOUBLE_TAP_LOCK);
  }
}
