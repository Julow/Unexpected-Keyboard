package juloo.keyboard2.suggestions;

import java.util.Arrays;
import java.util.List;
import juloo.cdict.Cdict;
import juloo.keyboard2.dict.Dictionaries;
import juloo.keyboard2.Config;

/** Keep track of the word being typed and provide suggestions for
    [CandidatesView]. */
public final class Suggestions
{
  Callback _callback;
  Config _config;

  public Suggestions(Callback c, Config conf)
  {
    _callback = c;
    _config = conf;
  }

  public void currently_typed_word(String word)
  {
    Cdict dict = _config.current_dictionary;
    if (word.length() < 2 || dict == null)
    {
      _callback.set_suggestions(NO_SUGGESTIONS);
    }
    else
    {
      Cdict.Result r = dict.find(word);
      String[] suggestions = new String[3];
      int i = 0;
      if (r.found)
        suggestions[i++] = word;
      int[] suffixes = dict.suffixes(r, 3);
      int[] dist = dict.distance(word, 1, 3);
      for (int j = 0; j < 3 && i < 3; j++)
      {
        if (suffixes.length > j)
          suggestions[i++] = dict.word(suffixes[j]);
        if (dist.length > j && i < 3)
          suggestions[i++] = dict.word(dist[j]);
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
