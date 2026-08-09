package juloo.keyboard2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class PersonalDictionaryTest
{
  static List<PersonalDictionary.Entry> words(String... ws)
  {
    List<PersonalDictionary.Entry> l = new ArrayList<PersonalDictionary.Entry>();
    for (String w : ws)
      l.add(new PersonalDictionary.Entry(w, ""));
    return l;
  }

  @Test
  public void prefix_and_exact_matching()
  {
    PersonalDictionary d = new PersonalDictionary(words(
      "Nafisa", "Ashu", "aardvark", "Ashley"));
    // Case-insensitive prefix; stored case preserved; insertion order.
    assertEquals(Arrays.asList("Ashu", "Ashley"), d.query("as", 3));
    // Exact match; no other word has "ashu" as a prefix.
    assertEquals(Arrays.asList("Ashu"), d.query("ashu", 3));
    // 'a' prefix returns a-words in insertion order.
    assertEquals(Arrays.asList("Ashu", "aardvark", "Ashley"), d.query("a", 5));
    // No match.
    assertEquals(Arrays.asList(), d.query("z", 3));
    // Respects max.
    assertEquals(Arrays.asList("Ashu"), d.query("a", 1));
    // max <= 0 returns nothing.
    assertEquals(Arrays.asList(), d.query("a", 0));
  }

  @Test
  public void word_matches_follow_typed_capitalization()
  {
    PersonalDictionary d = new PersonalDictionary(words("aardvark", "Ashu"));
    // Upper-case typed prefix capitalizes lower-case stored words.
    assertEquals(Arrays.asList("Aardvark", "Ashu"), d.query("A", 5));
    assertEquals(Arrays.asList("Aardvark"), d.query("Aa", 3));
    // Lower-case typed prefix leaves stored casing untouched.
    assertEquals(Arrays.asList("aardvark", "Ashu"), d.query("a", 5));
  }

  @Test
  public void shortcut_expands_exactly()
  {
    List<PersonalDictionary.Entry> es = words("Ashu");
    es.add(new PersonalDictionary.Entry("gareth@example.com", "aa"));
    PersonalDictionary d = new PersonalDictionary(es);
    // Exact shortcut match expands, case-insensitively.
    assertEquals(Arrays.asList("gareth@example.com"), d.query("aa", 3));
    assertEquals(Arrays.asList("gareth@example.com"), d.query("AA", 3));
    // Expansion is returned verbatim, never capitalized.
    assertEquals(Arrays.asList("gareth@example.com"), d.query("Aa", 3));
    // Longer text does not fire the shortcut.
    assertEquals(Arrays.asList(), d.query("aaa", 3));
    // Shortcut entries still behave as words for prefix matching.
    assertEquals(Arrays.asList("gareth@example.com"), d.query("gareth", 3));
  }

  @Test
  public void shortcut_ranks_before_word_matches()
  {
    List<PersonalDictionary.Entry> es = words("aardvark");
    es.add(new PersonalDictionary.Entry("gareth@example.com", "aa"));
    PersonalDictionary d = new PersonalDictionary(es);
    assertEquals(Arrays.asList("gareth@example.com", "aardvark"),
      d.query("aa", 3));
  }

  @Test
  public void case_insensitive_duplicates_returned_once()
  {
    PersonalDictionary d = new PersonalDictionary(words(
      "Ashu", "ashu", "ASHU"));
    // First stored casing wins; later case variants are dropped.
    assertEquals(Arrays.asList("Ashu"), d.query("ashu", 3));
    assertEquals(Arrays.asList("Ashu"), d.query("as", 3));
  }

  @Test
  public void empty_dictionary()
  {
    PersonalDictionary d = new PersonalDictionary(null);
    assertTrue(d.is_empty());
    assertEquals(Arrays.asList(), d.query("a", 3));
  }
}
