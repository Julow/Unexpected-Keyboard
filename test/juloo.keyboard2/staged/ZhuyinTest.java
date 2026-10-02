package juloo.keyboard2.staged;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.List;
import org.junit.Test;

public class ZhuyinTest
{
  /** ㄅ ㄆ ㄇ ㄈ ㄉ ㄊ ㄋ ㄌ ㄍ ㄎ ㄏ ㄐ ㄑ ㄒ ㄓ ㄔ ㄕ ㄖ ㄗ ㄘ ㄙ ㄚ ㄛ ㄜ ㄝ ㄞ ㄟ
      ㄠ ㄡ ㄢ ㄣ ㄤ ㄥ ㄦ ㄧ ㄨ ㄩ */
  static Zhuyin typed(String symbols)
  {
    Zhuyin zhuyin = new Zhuyin();
    for (int i = 0; i < symbols.length(); i++)
      assertTrue("symbol not consumed: " + symbols.charAt(i),
          zhuyin.type(String.valueOf(symbols.charAt(i))));
    return zhuyin;
  }

  static List<String> candidates(String symbols)
  {
    return typed(symbols).candidates();
  }

  @Test
  public void compose_symbols()
  {
    Zhuyin zhuyin = typed("ㄋㄧ");
    assertEquals("ㄋㄧ", zhuyin.text());
    assertEquals("你", candidates("ㄋㄧ").get(0));
  }

  @Test
  public void syllables()
  {
    assertEquals("[ni]", typed("ㄋㄧ").syllables().toString());
    assertEquals("[zhong]", typed("ㄓㄨㄥ").syllables().toString());
    assertEquals("[guo]", typed("ㄍㄨㄛ").syllables().toString());
    assertEquals("[er]", typed("ㄦ").syllables().toString());
    assertEquals("[ni, hao]", typed("ㄋㄧㄏㄠ").syllables().toString());
  }

  @Test
  public void tone_marks_end_a_syllable()
  {
    assertEquals("[ni]", typed("ㄋㄧˇ").syllables().toString());
    assertEquals("你", candidates("ㄋㄧˇ").get(0));
    assertEquals("[ni, hao]", typed("ㄋㄧˇㄏㄠˇ").syllables().toString());
    assertEquals("你好", candidates("ㄋㄧˇㄏㄠˇ").get(0));
    assertEquals("[ma]", typed("ㄇㄚ˙").syllables().toString());
  }

  @Test
  public void traditional_characters_are_offered()
  {
    // The table is ordered for Simplified Chinese, Zhuyin offers the
    // traditional variant.
    assertEquals("愛", candidates("ㄞ").get(0));
    assertEquals("國", candidates("ㄍㄨㄛ").get(0));
    // 是 is the same character in both scripts and stays first.
    assertEquals("是", candidates("ㄕ").get(0));
    assertEquals("時", candidates("ㄕ").get(1));
  }

  @Test
  public void nothing_is_offered_while_typing()
  {
    assertEquals(0, candidates("").size());
    assertEquals(0, candidates("ㄋ").size());
    assertEquals(0, candidates("ㄋㄧㄏ").size());
    // ㄅ cannot be followed by another initial.
    assertEquals(0, candidates("ㄅㄆ").size());
  }

  @Test
  public void backspace_and_reset()
  {
    Zhuyin zhuyin = typed("ㄋㄧㄏ");
    assertTrue(zhuyin.backspace());
    assertEquals("ㄋㄧ", zhuyin.text());
    assertEquals("你", zhuyin.candidates().get(0));
    zhuyin.reset();
    assertEquals("", zhuyin.text());
    assertFalse(zhuyin.backspace());
  }

  @Test
  public void other_keys_are_not_consumed()
  {
    Zhuyin zhuyin = typed("ㄋㄧ");
    assertFalse(zhuyin.type("1"));
    assertFalse(zhuyin.type(" "));
    assertFalse(zhuyin.type("ni"));
    assertEquals("ㄋㄧ", zhuyin.text());
  }
}
