package juloo.keyboard2;

import android.view.KeyEvent;
import juloo.keyboard2.*;
import org.junit.Test;
import static juloo.keyboard2.TestUtils.*;
import static org.junit.Assert.*;

public class ModmapTest
{
  public ModmapTest() {}

  @Test
  public void test()
  {
    Modmap mm = new Modmap();
    mm.add(Modmap.M.Shift, key("a"), key("b"));
    mm.add(Modmap.M.Fn, key("c"), key("d"));
    Utils.apply(mm, "a", KeyValue.Modifier.SHIFT, "b");
    Utils.apply(mm, "a", KeyValue.Modifier.FN, "æ");
    Utils.apply(mm, "c", KeyValue.Modifier.FN, "d");
  }

  @Test
  public void keyevent_mappings()
  {
    Modmap mm = new Modmap();
    mm.add(Modmap.M.Ctrl, key("љ"), key("љ:q"));
    Utils.apply(mm, "a", KeyValue.Modifier.CTRL, key("a").withKeyevent(29));
    Utils.apply(mm, "љ", KeyValue.Modifier.CTRL, key("љ").withKeyevent(45));
  }

  static class Utils
  {
    static void apply(Modmap mm, String a, KeyValue.Modifier mod, String expected)
    {
      apply(mm, a, mod, key(expected));
    }

    static void apply(Modmap mm, String a, KeyValue.Modifier mod, KeyValue expected)
    {
      KeyModifier.set_modmap(mm);
      KeyValue b = KeyModifier.modify(key(a), mod);
      KeyModifier.set_modmap(null);
      assertEquals(b, expected);
    }
  }
}
