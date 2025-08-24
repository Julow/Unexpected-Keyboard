package juloo.keyboard2;

import java.util.List;
import java.util.Arrays;

/** Keep track of the word being typed and provide suggestions for
    [CandidatesView]. */
public final class Suggestions
{
  Callback _callback;

  public Suggestions(Callback c)
  {
    _callback = c;
  }

  public void currently_typed_word(String word)
  {
    if (word.equals(""))
    {
      _callback.set_suggestions(NO_SUGGESTIONS);
    }
    else
    {
      // TODO
      _callback.set_suggestions(Arrays.asList(word));
    }
  }

  static final List<String> NO_SUGGESTIONS = Arrays.asList();

  public static interface Callback
  {
    public void set_suggestions(List<String> suggestions);
  }
}
