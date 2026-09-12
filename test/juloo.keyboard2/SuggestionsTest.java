package juloo.keyboard2;

import juloo.keyboard2.suggestions.Suggestions;
import org.junit.Test;
import static org.junit.Assert.*;

public class SuggestionsTest
{
  public SuggestionsTest() {}

  @Test
  public void unicode_sequences() throws Exception
  {
    assertEquals(Suggestions.apply_unicode_sequences("955"), "λ");
    assertEquals(Suggestions.apply_unicode_sequences("a955"), "aλ");
    assertEquals(Suggestions.apply_unicode_sequences("a955b"), "aλb");
    assertEquals(Suggestions.apply_unicode_sequences("a955b955c"), "aλbλc");
    assertEquals(Suggestions.apply_unicode_sequences("1000000000000000"), "1000000000000000");
    assertEquals(Suggestions.apply_unicode_sequences("a1000000000000000"), "a1000000000000000");
    assertEquals(Suggestions.apply_unicode_sequences("a1000000000000000b"), "a1000000000000000b");
  }
}
