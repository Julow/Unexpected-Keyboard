package juloo.keyboard2.staged;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
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

  static List<Composer.Candidate> candidates(String symbols)
  {
    return typed(symbols).candidates();
  }

  /** The text of every candidate. */
  static List<String> texts(List<Composer.Candidate> candidates)
  {
    List<String> out = new ArrayList<String>();
    for (Composer.Candidate c : candidates)
      out.add(c.text);
    return out;
  }

  static List<String> texts(String symbols)
  {
    return texts(candidates(symbols));
  }

  /** The annotation of the candidate whose text is [text], or [null]. */
  static String annotation_of(List<Composer.Candidate> candidates, String text)
  {
    for (Composer.Candidate c : candidates)
      if (c.text.equals(text))
        return c.annotation;
    return null;
  }

  @Test
  public void candidates_carry_their_tone()
  {
    List<Composer.Candidate> ying = candidates("ㄧㄥ");
    // 應 is read yīng: the first tone is shown with its own mark, ˉ, which is
    // not typed.
    assertEquals("ˉ", annotation_of(ying, "應"));
    // The tone of the other characters is shown above them.
    assertEquals("ˊ", annotation_of(ying, "營")); // yíng
    assertEquals("ˇ", annotation_of(ying, "影")); // yǐng
    assertEquals("ˋ", annotation_of(ying, "映")); // yìng
  }

  @Test
  public void a_typed_tone_pins_the_character()
  {
    // ㄧㄥˇ is yǐng, whose common character is 影 alone.
    List<Composer.Candidate> ying = candidates("ㄧㄥˇ");
    assertEquals(1, ying.size());
    assertEquals("影", ying.get(0).text);
    assertEquals("ˇ", ying.get(0).annotation);
  }

  @Test
  public void compose_symbols()
  {
    Zhuyin zhuyin = typed("ㄋㄧ");
    assertEquals("ㄋㄧ", zhuyin.text());
    assertEquals("你", texts("ㄋㄧ").get(0));
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
    assertEquals("你", texts("ㄋㄧˇ").get(0));
    assertEquals("[ni, hao]", typed("ㄋㄧˇㄏㄠˇ").syllables().toString());
    assertEquals("你好", texts("ㄋㄧˇㄏㄠˇ").get(0));
    assertEquals("[ma]", typed("ㄇㄚ˙").syllables().toString());
  }

  @Test
  public void traditional_characters_are_offered()
  {
    // The table is ordered for Simplified Chinese, Zhuyin offers the
    // traditional variant.
    assertEquals("愛", texts("ㄞ").get(0));
    assertEquals("國", texts("ㄍㄨㄛ").get(0));
    // 是 is the same character in both scripts and stays first.
    assertEquals("是", texts("ㄕ").get(0));
    assertEquals("時", texts("ㄕ").get(1));
  }

  @Test
  public void both_scripts_are_offered_with_traditional_first()
  {
    List<String> ying = texts("ㄧㄥ");
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
    assertEquals("你", zhuyin.candidates().get(0).text);
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
    assertEquals("你", zhuyin.candidates().get(0).text);
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
