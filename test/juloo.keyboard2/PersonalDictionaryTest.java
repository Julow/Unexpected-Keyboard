package juloo.keyboard2;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class PersonalDictionaryTest
{
  @Test
  public void prefix_and_exact_matching()
  {
    PersonalDictionary d = new PersonalDictionary(Arrays.asList(
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
  }

  @Test
  public void empty_dictionary()
  {
    PersonalDictionary d = new PersonalDictionary(null);
    assertTrue(d.is_empty());
    assertEquals(Arrays.asList(), d.query("a", 3));
  }
}
