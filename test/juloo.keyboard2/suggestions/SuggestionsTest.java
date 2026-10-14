package juloo.keyboard2.suggestions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import juloo.keyboard2.staged.Composer;
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

  @Test
  public void the_reading_is_stripped_from_a_word_of_the_dictionary()
  {
    assertEquals("中国", Suggestions.word_text("中国\t12"));
    // A word stored without a reading is unchanged.
    assertEquals("中国", Suggestions.word_text("中国"));
  }

  @Test
  public void the_tone_of_the_first_character_added_is_drawn()
  {
    // 中 typed, 中国 suggested: it adds 国, second tone.
    Composer.Candidate c = Suggestions.candidate_for("中国\t12", "中");
    assertEquals("中国", c.text);
    assertEquals("\u02ca", c.annotation);
    // 中国 typed, 中国共产党 suggested: it adds 共, fourth tone.
    assertEquals("\u02cb",
        Suggestions.candidate_for("中国共产党\t1243", "中国").annotation);
  }

  @Test
  public void a_word_that_adds_nothing_takes_its_last_syllable()
  {
    // 中国 itself: nothing is added, so 国 speaks for it.
    assertEquals("\u02ca",
        Suggestions.candidate_for("中国\t12", "中国").annotation);
    // Nothing typed: the first character speaks for the word.
    assertEquals("\u02c9", Suggestions.candidate_for("中国\t12", "").annotation);
  }

  @Test
  public void a_word_stored_without_a_reading_has_no_tone()
  {
    assertNull(Suggestions.candidate_for("中国", "中").annotation);
  }
}
