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

  @Test
  public void numpad_script()
  {
    assertEquals(apply_numpad_script("hindu-arabic"), "٠١٢٣٤٥٦٧٨٩");
    assertEquals(apply_numpad_script("bengali"), "০১২৩৪৫৬৭৮৯");
    assertEquals(apply_numpad_script("devanagari"), "०१२३४५६७८९");
    assertEquals(apply_numpad_script("persian"), "۰۱۲۳۴۵۶۷۸۹");
    assertEquals(apply_numpad_script("gujarati"), "૦૧૨૩૪૫૬૭૮૯");
    assertEquals(apply_numpad_script("kannada"), "೦೧೨೩೪೫೬೭೮೯");
  }
  String apply_numpad_script(String script)
  {
    StringBuilder b = new StringBuilder();
    KeyModifier.Map_char map = KeyModifier.modify_numpad_script(script);
    for (char c : "0123456789".toCharArray())
      b.append(map.apply(c));
    return b.toString();
  }
}
