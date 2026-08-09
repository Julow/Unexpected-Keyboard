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

  /** Combined suggestions as assembled by the keyboard: shortcut expansions
      first, then word matches. */
  static List<String> suggest(PersonalDictionary d, String typed, int max)
  {
    boolean capitalize =
      typed.length() > 0 && Character.isUpperCase(typed.charAt(0));
    List<String> out = new ArrayList<String>(d.query_shortcuts(typed, max));
    out.addAll(d.query_word_matches(typed, capitalize, max - out.size()));
    return out;
  }

  @Test
  public void prefix_and_exact_matching()
  {
    PersonalDictionary d = new PersonalDictionary(words(
      "Nafisa", "Ashu", "aardvark", "Ashley"));
    // Case-insensitive prefix; stored case preserved; insertion order.
    assertEquals(Arrays.asList("Ashu", "Ashley"),
        d.query_word_matches("as", false, 3));
    // Exact match; no other word has "ashu" as a prefix.
    assertEquals(Arrays.asList("Ashu"),
        d.query_word_matches("ashu", false, 3));
    // 'a' prefix returns a-words in insertion order.
    assertEquals(Arrays.asList("Ashu", "aardvark", "Ashley"),
        d.query_word_matches("a", false, 5));
    // No match.
    assertEquals(Arrays.asList(), d.query_word_matches("z", false, 3));
    // Respects max.
    assertEquals(Arrays.asList("Ashu"), d.query_word_matches("a", false, 1));
    // max <= 0 returns nothing.
    assertEquals(Arrays.asList(), d.query_word_matches("a", false, 0));
    assertEquals(Arrays.asList(), d.query_shortcuts("a", 0));
  }

  @Test
  public void word_matches_follow_typed_capitalization()
  {
    PersonalDictionary d = new PersonalDictionary(words("aardvark", "Ashu"));
    // Capitalize flag capitalizes lower-case stored words.
    assertEquals(Arrays.asList("Aardvark", "Ashu"),
        d.query_word_matches("a", true, 5));
    // Without the flag, stored casing is preserved.
    assertEquals(Arrays.asList("aardvark", "Ashu"),
        d.query_word_matches("a", false, 5));
  }

  @Test
  public void matching_applies_dictionary_substitutions()
  {
    PersonalDictionary d = new PersonalDictionary(words("café"));
    // Typing without diacritics matches the accented stored word.
    assertEquals(Arrays.asList("café"), d.query_word_matches("cafe", false, 3));
    // Typing with diacritics also matches.
    assertEquals(Arrays.asList("café"), d.query_word_matches("café", false, 3));
  }

  @Test
  public void shortcut_expands_exactly()
  {
    List<PersonalDictionary.Entry> es = words("Ashu");
    es.add(new PersonalDictionary.Entry("gareth@example.com", "aa"));
    PersonalDictionary d = new PersonalDictionary(es);
    // Exact shortcut match expands, case-insensitively.
    assertEquals(Arrays.asList("gareth@example.com"), d.query_shortcuts("aa", 3));
    assertEquals(Arrays.asList("gareth@example.com"), d.query_shortcuts("AA", 3));
    // Expansion is returned verbatim, never capitalized.
    assertEquals(Arrays.asList("gareth@example.com"),
        suggest(d, "Aa", 3));
    // Longer text does not fire the shortcut.
    assertEquals(Arrays.asList(), d.query_shortcuts("aaa", 3));
    // Shortcut entries still behave as words for prefix matching.
    assertEquals(Arrays.asList("gareth@example.com"),
        d.query_word_matches("gareth", false, 3));
  }

  @Test
  public void shortcut_ranks_before_word_matches()
  {
    List<PersonalDictionary.Entry> es = words("aardvark");
    es.add(new PersonalDictionary.Entry("gareth@example.com", "aa"));
    PersonalDictionary d = new PersonalDictionary(es);
    assertEquals(Arrays.asList("gareth@example.com", "aardvark"),
        suggest(d, "aa", 3));
  }

  @Test
  public void case_insensitive_duplicates_returned_once()
  {
    PersonalDictionary d = new PersonalDictionary(words(
      "Ashu", "ashu", "ASHU"));
    // First stored casing wins; later case variants are dropped.
    assertEquals(Arrays.asList("Ashu"),
        d.query_word_matches("ashu", false, 3));
    assertEquals(Arrays.asList("Ashu"),
        d.query_word_matches("as", false, 3));
  }

  @Test
  public void empty_dictionary()
  {
    PersonalDictionary d = new PersonalDictionary(null);
    assertTrue(d.is_empty());
    assertEquals(Arrays.asList(), d.query_word_matches("a", false, 3));
    assertEquals(Arrays.asList(), d.query_shortcuts("a", 3));
  }
}
