package juloo.keyboard2;

import java.util.Arrays;
import java.util.List;
import juloo.cdict.Cdict;

/** Keep track of the word being typed and provide suggestions for
    [CandidatesView]. */
public final class Suggestions
{
  Callback _callback;
  Cdict _dict;

  public Suggestions(Callback c)
  {
    _callback = c;
    _dict = Dictionary.main_fr;
  }

  public void currently_typed_word(String word)
  {
    if (word.length() < 2)
    {
      _callback.set_suggestions(NO_SUGGESTIONS);
    }
    else
    {
      Cdict.Result r = _dict.find(word);
      String[] suggestions = new String[3];
      int i = 0;
      if (r.found)
        suggestions[i++] = word;
      int[] suffixes = _dict.suffixes(r, 3);
      int[] dist = _dict.distance(word, 1, 3);
      for (int j = 0; j < 3 && i < 3; j++)
      {
        if (suffixes.length > j)
          suggestions[i++] = _dict.word(suffixes[j]);
        if (dist.length > j && i < 3)
          suggestions[i++] = _dict.word(dist[j]);
      }
      _callback.set_suggestions(Arrays.asList(suggestions));
    }
  }

  static final List<String> NO_SUGGESTIONS = Arrays.asList();

  public static interface Callback
  {
    public void set_suggestions(List<String> suggestions);
  }
}
