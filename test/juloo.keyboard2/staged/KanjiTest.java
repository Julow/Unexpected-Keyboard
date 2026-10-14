package juloo.keyboard2.staged;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class KanjiTest
{
  /** A small dictionary that exercises the cost model. Costs are Mozc's. */
  static final String FIXTURE =
    "こんぴゅーた\tコンピュータ\t3000\n"
    + "ご\tご\t0\n"
    + "にほん\t日本\t730\n"
    + "にほんご\t日本語\t3793\n"
    + "の\tの\t0\n"
    + "の\t之\t10\n"
    + "わたし\tわたし\t0\n"
    + "わたし\t私\t263\n";

  static void load(String text)
  {
    KanjiDictionary.load(text);
  }

  static List<String> candidates(String reading) throws Exception
  {
    load(FIXTURE);
    return texts(Kanji.candidates(reading));
  }

  /** The text of every candidate. */
  static List<String> texts(List<Composer.Candidate> candidates)
  {
    List<String> out = new ArrayList<String>();
    for (Composer.Candidate c : candidates)
      out.add(c.text);
    return out;
  }

  @Test
  public void a_longer_word_beats_a_cut() throws Exception
  {
    // 日本語 is one word, 日本 + ご would be two.
    assertEquals("日本語", candidates("にほんご").get(0));
  }

  @Test
  public void a_kanji_beats_the_kana_of_the_same_reading() throws Exception
  {
    assertEquals("私", candidates("わたし").get(0));
    // The kana is still offered.
    assertEquals("わたし", candidates("わたし").get(1));
  }

  @Test
  public void a_single_kana_stays_kana() throws Exception
  {
    // 之 is cheaper than the kana of the same reading, a particle is not
    // converted on its own.
    assertEquals("の", candidates("の").get(0));
  }

  @Test
  public void a_katakana_word_is_converted() throws Exception
  {
    // The only reading of the entry, so it wins over leaving the kana.
    assertEquals("コンピュータ", candidates("こんぴゅーた").get(0));
  }

  @Test
  public void nothing_to_convert() throws Exception
  {
    // A reading that is not in the dictionary is left as it is.
    assertEquals(1, candidates("はれ").size());
    assertEquals("はれ", candidates("はれ").get(0));
    // Katakana is not converted, and neither is an empty reading.
    assertEquals(0, candidates("コンピュータ").size());
    assertEquals(0, candidates("").size());
  }

  @Test
  public void no_dictionary() throws Exception
  {
    load("");
    assertEquals(0, Kanji.candidates("にほんご").size());
  }

  @Test
  public void the_reading_is_always_offered() throws Exception
  {
    List<String> candidates = candidates("にほんご");
    assertTrue(candidates.toString(), candidates.contains("にほんご"));
    assertEquals("にほんご", candidates.get(candidates.size() - 1));
  }

  /** More words of a same reading than the three candidates the view used to
      be limited to, so that the list is not silently truncated. */
  static final String WIDE_FIXTURE =
    "か\tか\t0\n"
    + "か\t下\t100\n"
    + "か\t化\t200\n"
    + "か\t可\t300\n"
    + "か\t佳\t400\n"
    + "か\t加\t500\n";

  @Test
  public void the_list_is_not_truncated() throws Exception
  {
    load(WIDE_FIXTURE);
    List<String> candidates = texts(Kanji.candidates("か"));
    assertTrue(candidates.toString(), candidates.size() > 3);
    assertEquals("か", candidates.get(0));
  }

  /** A reading with two kanji and the kana, to exercise the furigana. */
  static final String FURIGANA_FIXTURE =
    "わたし\tわたし\t0\n"
    + "わたし\t私\t263\n"
    + "わたし\t渡し\t500\n";

  /** The annotation of the candidate whose text is [text]. */
  static String annotation_of(List<Composer.Candidate> candidates, String text)
  {
    for (Composer.Candidate c : candidates)
      if (c.text.equals(text))
        return c.annotation;
    throw new AssertionError("no candidate " + text);
  }

  @Test
  public void alternatives_carry_their_reading_as_furigana() throws Exception
  {
    load(FURIGANA_FIXTURE);
    List<Composer.Candidate> candidates = Kanji.candidates("わたし");
    // The winning cut carries its reading too, or a reading with no
    // alternative would leave the whole row bare.
    assertEquals("私", candidates.get(0).text);
    assertEquals("わたし", candidates.get(0).annotation);
    // 渡し replaces 私 and carries the reading of the word it replaces.
    assertEquals("わたし", annotation_of(candidates, "渡し"));
    // The kana alternative is written with the reading already.
    assertNull(annotation_of(candidates, "わたし"));
  }

  @Test
  public void a_conversion_without_alternative_is_not_bare() throws Exception
  {
    // 日本 has no other word of its reading in this dictionary, so the winning
    // cut is the only cell that could explain it.
    load(FIXTURE);
    List<Composer.Candidate> candidates = Kanji.candidates("にほん");
    assertEquals("日本", candidates.get(0).text);
    assertEquals("にほん", candidates.get(0).annotation);
    // The kana of the same reading stays bare.
    assertNull(annotation_of(candidates, "にほん"));
  }
}
