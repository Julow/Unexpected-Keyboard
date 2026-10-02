package juloo.keyboard2.staged;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.zip.GZIPOutputStream;
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

  static void load(String text) throws Exception
  {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    GZIPOutputStream out = new GZIPOutputStream(bytes);
    out.write(text.getBytes("UTF-8"));
    out.close();
    KanjiDictionary.load(new ByteArrayInputStream(bytes.toByteArray()));
  }

  static List<String> candidates(String reading) throws Exception
  {
    load(FIXTURE);
    return Kanji.candidates(reading, 3);
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
    assertEquals(0, Kanji.candidates("にほんご", 3).size());
  }

  @Test
  public void the_reading_is_always_offered() throws Exception
  {
    List<String> candidates = candidates("にほんご");
    assertTrue(candidates.toString(), candidates.contains("にほんご"));
    assertTrue(candidates.size() <= 3);
    assertEquals("にほんご", candidates.get(candidates.size() - 1));
  }
}
