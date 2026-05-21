package juloo.keyboard2;

import android.view.KeyEvent;
import juloo.keyboard2.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ModmapTest
{
  public ModmapTest() {}

  @Test
  public void test()
  {
    Modmap mm = new Modmap();
    mm.add(Modmap.M.Shift, KeyValue.getKeyByName("a"), KeyValue.getKeyByName("b"));
    mm.add(Modmap.M.Fn, KeyValue.getKeyByName("c"), KeyValue.getKeyByName("d"));
    Utils.apply(mm, "a", KeyValue.Modifier.SHIFT, "b");
    Utils.apply(mm, "a", KeyValue.Modifier.FN, "æ");
    Utils.apply(mm, "c", KeyValue.Modifier.FN, "d");
  }

  @Test
  public void keyevent_mappings()
  {
    Modmap mm = new Modmap();
    mm.add(Modmap.M.Ctrl, KeyValue.getKeyByName("љ"), KeyValue.getKeyByName("љ:q"));
    Utils.apply(mm, "a", KeyValue.Modifier.CTRL, KeyValue.getKeyByName("a").withKeyevent(29));
    Utils.apply(mm, "љ", KeyValue.Modifier.CTRL, KeyValue.getKeyByName("љ").withKeyevent(45));
  }

  @Test
  public void hangul_shift_mappings()
  {
    Utils.apply(null, "ㅂ", KeyValue.Modifier.SHIFT, "ㅃ");
    Utils.apply(null, "ㅈ", KeyValue.Modifier.SHIFT, "ㅉ");
    Utils.apply(null, "ㄷ", KeyValue.Modifier.SHIFT, "ㄸ");
    Utils.apply(null, "ㄱ", KeyValue.Modifier.SHIFT, "ㄲ");
    Utils.apply(null, "ㅅ", KeyValue.Modifier.SHIFT, "ㅆ");
    Utils.apply(null, "ㅐ", KeyValue.Modifier.SHIFT, "ㅒ");
    Utils.apply(null, "ㅔ", KeyValue.Modifier.SHIFT, "ㅖ");
    Utils.apply(null, "ㅁ", KeyValue.Modifier.SHIFT, "ㅁ");
    Utils.apply(null, "ㅏ", KeyValue.Modifier.SHIFT, "ㅏ");
  }

  static class Utils
  {
    static void apply(Modmap mm, String a, KeyValue.Modifier mod, String expected)
    {
      apply(mm, a, mod, KeyValue.getKeyByName(expected));
    }

    static void apply(Modmap mm, String a, KeyValue.Modifier mod, KeyValue expected)
    {
      KeyModifier.set_modmap(mm);
      KeyValue b = KeyModifier.modify(KeyValue.getKeyByName(a), mod);
      KeyModifier.set_modmap(null);
      assertEquals(b, expected);
    }
  }
}
