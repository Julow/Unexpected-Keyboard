package juloo.keyboard2;

import org.junit.Test;
import static org.junit.Assert.*;

public class CurrentlyTypedWordTest
{
  public CurrentlyTypedWordTest() {}

  static CurrentlyTypedWord typed(String... chunks)
  {
    CurrentlyTypedWord w = new CurrentlyTypedWord(null,
        new CurrentlyTypedWord.Callback() {
          public void currently_typed_word(String word) {}
        });
    w._enabled = true;
    for (String c : chunks)
      w.typed(c);
    return w;
  }

  static void check(String word, String prev, CurrentlyTypedWord w)
  {
    assertEquals(word, w.get());
    assertEquals(prev, w.previous_word());
  }

  @Test
  public void previous_word()
  {
    check("bon", null, typed("bon"));
    check("di", "bon", typed("bon di"));
    check("di", "bon", typed("b", "o", "n", " ", "d", "i"));
    check("", "bon", typed("bon "));
    check("ho", "fer", typed("fer-ho"));
    check("molt", "és", typed("Això és molt"));
    // Two spaces don't lose the previous word.
    check("dia", "bon", typed("bon  dia"));
    // Punctuation starts a new phrase.
    check("però", null, typed("Sí, però"));
    check("Bon", null, typed("Això. Bon"));
    check("", null, typed("Això."));
    check("x", null, typed("a\nx"));
  }

  @Test
  public void editor_text()
  {
    CurrentlyTypedWord w = typed();
    w.set_current_word("bon di");
    check("di", "bon", w);
    // Truncated text: the first word might be incomplete and is not used.
    String pad = "x".repeat(CurrentlyTypedWord.TEXT_BEFORE_CURSOR_LENGTH);
    w.set_current_word(pad + "ció és");
    check("és", null, w);
    w.set_current_word(pad + "ció és molt");
    check("molt", "és", w);
    w.set_current_word((CharSequence)null);
    check("", null, w);
  }
}
