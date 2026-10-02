package juloo.keyboard2.staged;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.zip.GZIPOutputStream;
import org.junit.Test;

public class KanjiDictionaryTest
{
  static final String FIXTURE =
    "ご\tご\t0\n"
    + "にほん\t日本\t730\n"
    + "にほんご\t日本語\t3793\n"
    + "ほん\t本\t500\n"
    + "わたし\tわたし\t0\n";

  static InputStream gzipped(String text) throws Exception
  {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    GZIPOutputStream out = new GZIPOutputStream(bytes);
    out.write(text.getBytes("UTF-8"));
    out.close();
    return new ByteArrayInputStream(bytes.toByteArray());
  }

  static InputStream plain(String text) throws Exception
  {
    return new ByteArrayInputStream(text.getBytes("UTF-8"));
  }

  @Test
  public void entries_are_found_by_reading() throws Exception
  {
    assertEquals(5, KanjiDictionary.load(gzipped(FIXTURE)));
    assertTrue(KanjiDictionary.is_loaded());
    int i = KanjiDictionary.find("にほん");
    assertEquals("にほん", KanjiDictionary.reading(i));
    assertEquals("日本", KanjiDictionary.word(i));
    assertEquals(730, KanjiDictionary.cost(i));
  }

  @Test
  public void the_first_entry_of_a_reading_is_returned() throws Exception
  {
    KanjiDictionary.load(gzipped(FIXTURE));
    // "にほん" and "にほんご" both start with にほん, the exact one comes first.
    int i = KanjiDictionary.find("にほん");
    assertEquals("日本", KanjiDictionary.word(i));
    assertEquals("にほんご", KanjiDictionary.reading(i + 1));
  }

  @Test
  public void unknown_reading() throws Exception
  {
    KanjiDictionary.load(gzipped(FIXTURE));
    assertEquals(-1, KanjiDictionary.find("さくら"));
    assertEquals(-1, KanjiDictionary.find(""));
  }

  @Test
  public void the_dictionary_is_replaced_on_each_load() throws Exception
  {
    KanjiDictionary.load(gzipped(FIXTURE));
    assertEquals(1, KanjiDictionary.load(plain("さくら\t桜\t100\n")));
    assertEquals(1, KanjiDictionary.count());
    assertEquals("桜", KanjiDictionary.word(KanjiDictionary.find("さくら")));
    assertEquals(-1, KanjiDictionary.find("にほん"));
  }

  @Test
  public void plain_text_is_read_as_well() throws Exception
  {
    // The resource is gzipped, but a build could store it uncompressed.
    KanjiDictionary.load(plain(FIXTURE));
    assertFalse(KanjiDictionary.find("にほん") < 0);
  }
}
