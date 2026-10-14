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
  public void both_scripts_are_offered_with_traditional_first()
  {
    List<String> ying = candidates("ㄧㄥ");
    // The list is not capped: 英 is reachable, it is not among the three most
    // frequent characters of "ying".
    assertTrue(ying.size() > 3);
    assertTrue(ying.contains("英"));
    // Traditional candidates come first, the simplified ones are appended
    // after all of them.
    assertEquals("應", ying.get(0));
    assertTrue(ying.indexOf("英") < ying.indexOf("应"));
  }

  @Test
  public void nothing_is_offered_while_typing()
  {
    assertEquals(0, candidates("").size());
    assertEquals(0, candidates("ㄋ").size());
    assertEquals(0, candidates("ㄋㄧㄏ").size());
  }

  @Test
  public void only_the_combinations_that_are_syllables_are_accepted()
  {
    // ㄅ cannot be followed by another initial.
    Zhuyin zhuyin = typed("ㄅ");
    assertFalse(zhuyin.type("ㄆ"));
    assertEquals("ㄅ", zhuyin.text());
    // ㄅ is never followed by ㄩ.
    assertFalse(zhuyin.type("ㄩ"));
    assertEquals("ㄅ", zhuyin.text());
    // A refused key leaves the composition usable.
    assertTrue(zhuyin.type("ㄚ"));
    assertEquals("ㄅㄚ", zhuyin.text());
    assertTrue(zhuyin.candidates().size() > 0);
  }

  @Test
  public void a_tone_mark_does_not_start_a_syllable()
  {
    Zhuyin empty = new Zhuyin();
    assertFalse(empty.type("ˇ"));
    assertEquals("", empty.text());
    // Nor can it follow an incomplete syllable.
    Zhuyin incomplete = typed("ㄋ");
    assertFalse(incomplete.type("ˇ"));
    assertEquals("ㄋ", incomplete.text());
  }

  @Test
  public void an_incomplete_syllable_is_accepted()
  {
    Zhuyin zhuyin = typed("ㄋ");
    assertEquals("ㄋ", zhuyin.text());
    assertEquals(0, zhuyin.candidates().size());
    assertTrue(zhuyin.type("ㄧ"));
    assertTrue(zhuyin.type("ˇ"));
    assertEquals("你", zhuyin.candidates().get(0));
  }

  @Test
  public void only_the_keys_that_can_continue_are_accepted()
  {
    // Nothing is composed: every key is accepted, so nothing is greyed out.
    Zhuyin empty = new Zhuyin();
    assertTrue(empty.accepts("ㄅ"));
    assertTrue(empty.accepts("?"));
    // ㄅ can take a medial or a final, but not another initial.
    Zhuyin zhuyin = typed("ㄅ");
    assertTrue(zhuyin.accepts("ㄚ"));
    assertTrue(zhuyin.accepts("ㄨ"));
    assertFalse(zhuyin.accepts("ㄆ"));
    assertFalse(zhuyin.accepts("ㄩ"));
    assertFalse(zhuyin.accepts("ˇ"));
    assertFalse(zhuyin.accepts("1"));
    // accepts() does not modify the composition.
    assertEquals("ㄅ", zhuyin.text());
  }

  @Test
  public void accepts_follows_the_composition()
  {
    Zhuyin zhuyin = typed("ㄋㄧ");
    assertTrue(zhuyin.accepts("ˇ"));
    assertTrue(zhuyin.accepts("ㄏ"));
    assertFalse(zhuyin.accepts("a"));
    assertTrue(zhuyin.type("ㄏ"));
    // ㄏ cannot take another initial.
    assertFalse(zhuyin.accepts("ㄅ"));
    assertEquals("ㄋㄧㄏ", zhuyin.text());
  }

  @Test
  public void the_longest_spelling_is_read_first()
  {
    assertEquals("[bie]", typed("ㄅㄧㄝ").syllables().toString());
    assertEquals("[xia]", typed("ㄒㄧㄚ").syllables().toString());
    assertEquals("[xian]", typed("ㄒㄧㄢ").syllables().toString());
    // But a shorter spelling is tried when it is the only one that completes,
    // ㄅㄨㄞ is "bu" followed by "ai".
    assertEquals("[bu, ai]", typed("ㄅㄨㄞ").syllables().toString());
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
