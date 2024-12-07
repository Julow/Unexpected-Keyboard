package juloo.keyboard2;

import juloo.keyboard2.KeyValue;
import org.junit.Test;
import static org.junit.Assert.*;

public class KeyValueTest
{
  public KeyValueTest() {}

  @Test
  public void equals()
  {
    assertEquals(KeyValue.makeStringKeyWithSymbol("Foo", "Symbol", 0), KeyValue.makeStringKeyWithSymbol("Foo", "Symbol", 0));
  }
}
