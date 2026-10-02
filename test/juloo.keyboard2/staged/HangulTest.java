package juloo.keyboard2.staged;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class HangulTest
{
  /** Type every key in order and return the composed text. */
  static String type(String... keys)
  {
    Hangul hangul = new Hangul();
    for (String key : keys)
      assertTrue("key not consumed: " + key, hangul.type(key));
    return hangul.text();
  }

  static Hangul typed(String... keys)
  {
    Hangul hangul = new Hangul();
    for (String key : keys)
      hangul.type(key);
    return hangul;
  }

  @Test
  public void initial_and_medial()
  {
    assertEquals("가", type("ㄱ", "ㅏ"));
    assertEquals("아", type("ㅇ", "ㅏ"));
    assertEquals("아", type("ㅏ"));
  }

  @Test
  public void final_consonant()
  {
    assertEquals("간", type("ㄱ", "ㅏ", "ㄴ"));
    assertEquals("한글", type("ㅎ", "ㅏ", "ㄴ", "ㄱ", "ㅡ", "ㄹ"));
    assertEquals("안녕", type("ㅇ", "ㅏ", "ㄴ", "ㄴ", "ㅕ", "ㅇ"));
  }

  @Test
  public void a_final_moves_to_the_next_syllable()
  {
    // 사람: the ㄹ of 살 must become the initial of 라.
    assertEquals("사람", type("ㅅ", "ㅏ", "ㄹ", "ㅏ", "ㅁ"));
    assertEquals("가나", type("ㄱ", "ㅏ", "ㄴ", "ㅏ"));
  }

  @Test
  public void compound_finals()
  {
    assertEquals("값", type("ㄱ", "ㅏ", "ㅂ", "ㅅ"));
    assertEquals("갑사", type("ㄱ", "ㅏ", "ㅂ", "ㅅ", "ㅏ"));
    assertEquals("값이", type("ㄱ", "ㅏ", "ㅂ", "ㅅ", "ㅇ", "ㅣ"));
    assertEquals("갆", type("ㄱ", "ㅏ", "ㄴ", "ㅎ"));
    // 많 + ㅇ + ㅣ, the final ㄶ is kept because the next key is a consonant.
    assertEquals("많이", type("ㅁ", "ㅏ", "ㄴ", "ㅎ", "ㅇ", "ㅣ"));
    // The ㄴ is kept and the ㅎ moves when a vowel follows directly.
    assertEquals("간하", type("ㄱ", "ㅏ", "ㄴ", "ㅎ", "ㅏ"));
  }

  @Test
  public void compound_vowels_are_assembled()
  {
    assertEquals("과", type("ㄱ", "ㅗ", "ㅏ"));
    assertEquals("과", type("ㄱ", "ㅘ"));
    assertEquals("왜", type("ㅇ", "ㅗ", "ㅐ"));
    assertEquals("의", type("ㅇ", "ㅡ", "ㅣ"));
    assertEquals("의", type("ㅇ", "ㅢ"));
  }

  @Test
  public void consonant_without_a_vowel()
  {
    assertEquals("ㄱ", type("ㄱ"));
    assertEquals("ㄱㄴ", type("ㄱ", "ㄴ"));
    assertEquals("간ㄷ", type("ㄱ", "ㅏ", "ㄴ", "ㄷ"));
  }

  @Test
  public void backspace_erases_one_jamo()
  {
    Hangul hangul = typed("ㄱ", "ㅏ", "ㄴ");
    assertEquals("간", hangul.text());
    assertTrue(hangul.backspace());
    assertEquals("가", hangul.text());
    assertTrue(hangul.backspace());
    assertEquals("ㄱ", hangul.text());
    assertTrue(hangul.backspace());
    assertEquals("", hangul.text());
    assertFalse(hangul.backspace());
  }

  @Test
  public void other_keys_are_not_consumed()
  {
    Hangul hangul = typed("ㄱ", "ㅏ");
    assertFalse(hangul.type("1"));
    assertFalse(hangul.type(" "));
    // A syllable is not a jamo, but a string of jamo is accepted as is.
    assertFalse(hangul.type("가"));
    assertFalse(hangul.type("ㄱa"));
    assertEquals("가", hangul.text());
  }

  @Test
  public void reset()
  {
    Hangul hangul = typed("ㄱ", "ㅏ");
    hangul.reset();
    assertEquals("", hangul.text());
  }
}
