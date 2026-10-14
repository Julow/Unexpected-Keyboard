package juloo.keyboard2.staged;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class KanjiDictionaryTest
{
  static final String FIXTURE =
    "ご\tご\t0\n"
    + "にほん\t日本\t730\n"
    + "にほんご\t日本語\t3793\n"
    + "ほん\t本\t500\n"
    + "わたし\tわたし\t0\n";

  @Test
  public void entries_are_found_by_reading()
  {
    assertEquals(5, KanjiDictionary.load(FIXTURE));
    assertTrue(KanjiDictionary.is_loaded());
    int i = KanjiDictionary.find("にほん");
    assertEquals("にほん", KanjiDictionary.reading(i));
    assertEquals("日本", KanjiDictionary.word(i));
    assertEquals(730, KanjiDictionary.cost(i));
  }

  @Test
  public void the_first_entry_of_a_reading_is_returned()
  {
    KanjiDictionary.load(FIXTURE);
    // "にほん" and "にほんご" both start with にほん, the exact one comes first.
    int i = KanjiDictionary.find("にほん");
    assertEquals("日本", KanjiDictionary.word(i));
    assertEquals("にほんご", KanjiDictionary.reading(i + 1));
  }

  @Test
  public void unknown_reading()
  {
    KanjiDictionary.load(FIXTURE);
    assertEquals(-1, KanjiDictionary.find("さくら"));
    assertEquals(-1, KanjiDictionary.find(""));
  }

  @Test
  public void the_dictionary_is_replaced_on_each_load()
  {
    KanjiDictionary.load(FIXTURE);
    assertEquals(1, KanjiDictionary.load("さくら\t桜\t100\n"));
    assertEquals(1, KanjiDictionary.count());
    assertEquals("桜", KanjiDictionary.word(KanjiDictionary.find("さくら")));
    assertEquals(-1, KanjiDictionary.find("にほん"));
  }

  @Test
  public void a_line_that_is_not_an_entry_is_skipped()
  {
    // Sorted by reading, as the dictionary is: a skipped line does not move
    // the entries that follow it out of order.
    assertEquals(2, KanjiDictionary.load(
          "これはエントリではない\n"
          + "にほん\t日本\t730\n"
          + "ほん\t本\tnull\n"
          + "ほん\t本\t500\n"));
    assertEquals(2, KanjiDictionary.count());
    assertEquals("本", KanjiDictionary.word(KanjiDictionary.find("ほん")));
    assertEquals(500, KanjiDictionary.cost(KanjiDictionary.find("ほん")));
  }

  @Test
  public void an_empty_dictionary_is_not_loaded()
  {
    assertEquals(0, KanjiDictionary.load(""));
    assertEquals(0, KanjiDictionary.count());
    assertEquals(false, KanjiDictionary.is_loaded());
  }
}
