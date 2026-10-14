package juloo.keyboard2.suggestions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class SuggestionsTest
{
  /** A dictionary that knows the words it is given. */
  static Suggestions.Known dictionary(final String... words)
  {
    return new Suggestions.Known()
    {
      @Override
      public boolean has_prefix(String prefix)
      {
        for (String word : words)
          if (word.startsWith(prefix))
            return true;
        return false;
      }
    };
  }

  @Test
  public void a_run_without_spaces_is_queried_with_its_last_word()
  {
    // 我爱中国 is a run of the characters typed since the last space; the
    // dictionary knows none of it, except the word being completed, 中国.
    assertEquals("中国",
        Suggestions.unspaced_tail("我爱中国", dictionary("中国")));
  }

  @Test
  public void the_longest_known_tail_is_the_word()
  {
    // The run itself starts a word, so nothing is dropped from it.
    assertEquals("中国人民",
        Suggestions.unspaced_tail("我爱中国人民", dictionary("中国人民")));
  }

  @Test
  public void a_word_written_with_separators_is_left_alone()
  {
    // A latin word is a word: it is not shortened into one of its tails.
    assertNull(Suggestions.unspaced_tail("renshi", dictionary("认识")));
  }

  @Test
  public void nothing_is_dropped_when_no_tail_is_known()
  {
    assertNull(Suggestions.unspaced_tail("我爱中国", dictionary("人民")));
  }
}
