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
    assertEquals(KeyValue.makeStringKey("Foo").withSymbol("Symbol"),
        KeyValue.makeMacro("Symbol", new KeyValue[] { KeyValue.makeStringKey("Foo") }, 0));
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
    assertEquals(apply_numpad_script("tamil"), "௦௧௨௩௪௫௬௭௮௯");
  }
  String apply_numpad_script(String script)
  {
    StringBuilder b = new StringBuilder();
    int map = KeyModifier.modify_numpad_script(script);
    for (char c : "0123456789".toCharArray())
      b.append(ComposeKey.apply(map, c).getChar());
    return b.toString();
  }
}
