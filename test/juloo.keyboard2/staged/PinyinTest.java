package juloo.keyboard2.staged;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class PinyinTest
{
  static Pinyin typed(String keys)
  {
    Pinyin pinyin = new Pinyin();
    for (int i = 0; i < keys.length(); i++)
      assertTrue("key not consumed: " + keys.charAt(i),
          pinyin.type(String.valueOf(keys.charAt(i))));
    return pinyin;
  }

  static List<Composer.Candidate> candidates(String keys)
  {
    return typed(keys).candidates();
  }

  /** The text of every candidate. */
  static List<String> texts(List<Composer.Candidate> candidates)
  {
    List<String> out = new ArrayList<String>();
    for (Composer.Candidate c : candidates)
      out.add(c.text);
    return out;
  }

  static List<String> texts(String keys)
  {
    return texts(candidates(keys));
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
    List<Composer.Candidate> ying = candidates("ying");
    assertEquals("ˊ", annotation_of(ying, "营")); // yíng
    assertEquals("ˇ", annotation_of(ying, "影")); // yǐng
  }

  @Test
  public void compose_letters()
  {
    Pinyin pinyin = typed("ni");
    assertEquals("ni", pinyin.text());
    assertEquals("你", texts("ni").get(0));
  }

  @Test
  public void syllables()
  {
    assertEquals("ni", typed("ni").syllables().get(0));
    assertEquals("[ni, hao]", typed("nihao").syllables().toString());
    assertEquals("[zhong]", typed("zhong").syllables().toString());
    assertEquals("[guo]", typed("guo").syllables().toString());
    // The longest syllable wins when a sequence can be read in several ways,
    // "xian" is both 先 and 西 + 安.
    assertEquals("[xian]", typed("xian").syllables().toString());
    assertEquals("[fang, an]", typed("fangan").syllables().toString());
  }

  @Test
  public void candidates_of_several_syllables()
  {
    assertEquals("你好", texts("nihao").get(0));
    assertEquals("中国", texts("zhongguo").get(0));
    assertEquals("你号", texts("nihao").get(1));
  }

  @Test
  public void nothing_is_offered_while_typing()
  {
    // "n" is a syllable of its own (嗯) but "nih" is not a sequence of complete
    // syllables.
    assertEquals(1, candidates("n").size());
    assertEquals(0, candidates("nih").size());
    assertEquals(0, candidates("xyz").size());
    assertEquals(0, candidates("").size());
  }

  @Test
  public void umlaut_is_typed_with_v()
  {
    assertEquals("女", texts("nv").get(0));
    assertTrue(texts("lv").contains("绿"));
    // After j, q and x the umlaut is written with a plain u.
    assertEquals("去", texts("qu").get(0));
  }

  @Test
  public void both_scripts_are_offered_with_simplified_first()
  {
    List<String> ying = texts("ying");
    // The list is not capped: 英 is reachable, it is not among the three most
    // frequent characters of "ying".
    assertTrue(ying.size() > 3);
    assertTrue(ying.contains("英"));
    // Simplified candidates come first, the traditional ones are appended
    // after all of them.
    assertEquals("应", ying.get(0));
    assertTrue(ying.indexOf("英") < ying.indexOf("應"));
  }

  @Test
  public void backspace_and_reset()
  {
    Pinyin pinyin = typed("nih");
    assertTrue(pinyin.backspace());
    assertEquals("ni", pinyin.text());
    assertEquals("你", pinyin.candidates().get(0).text);
    pinyin.reset();
    assertEquals("", pinyin.text());
    assertFalse(pinyin.backspace());
  }

  @Test
  public void other_keys_are_not_consumed()
  {
    Pinyin pinyin = typed("ni");
    assertFalse(pinyin.type("1"));
    assertFalse(pinyin.type(" "));
    assertFalse(pinyin.type("n i"));
    assertEquals("ni", pinyin.text());
  }

  @Test
  public void upper_case_letters_are_accepted()
  {
    // Auto capitalisation must not break pinyin.
    assertEquals("你", texts("Ni").get(0));
    assertEquals("nihao", typed("Nihao").text());
  }
}
