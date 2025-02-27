package juloo.keyboard2;

import android.view.KeyEvent;
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
    assertEquals(KeyValue.getSpecialKeyByName("tab"),
        KeyValue.keyeventKey(0xE00F, KeyEvent.KEYCODE_TAB, KeyValue.FLAG_KEY_FONT | KeyValue.FLAG_SMALLER_FONT));
    assertEquals(KeyValue.getSpecialKeyByName("tab").withSymbol("t"),
        KeyValue.keyeventKey("t", KeyEvent.KEYCODE_TAB, 0));
    assertEquals(KeyValue.getSpecialKeyByName("tab").withSymbol("tab"),
        KeyValue.keyeventKey("tab", KeyEvent.KEYCODE_TAB, KeyValue.FLAG_SMALLER_FONT));
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
