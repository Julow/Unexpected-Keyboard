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

  /** The suggestion displayed at the center of the candidates view and entered
      by the space bar. */
  public String best_suggestion = null;

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
      set_suggestions(NO_SUGGESTIONS);
    }
    else
    {
      String[] dst = new String[3];
      query_suggestions(dict, word, dst, 3);
      set_suggestions(Arrays.asList(dst));
    }
  }

  int query_suggestions(Cdict dict, String word, String[] dst, int max_count)
  {
    Cdict.Result r = dict.find(word);
    int i = 0;
    if (r.found)
      dst[i++] = word;
    boolean first_char_upper = Character.isUpperCase(word.charAt(0));
    // Do the dictionary query in lower case and re-apply the upper case after
    if (first_char_upper)
    {
      r = dict.find(word.toLowerCase());
      if (r.found)
        dst[i++] = word;
    }
    int[] suffixes = dict.suffixes(r, max_count);
    // Disable distance search for small words
    int[] dist = (word.length() < 3 || i + 1 >= max_count) ? NO_RESULTS :
      dict.distance(word, 1, max_count);
    for (int j = 0; j < max_count && i < max_count; j++)
    {
      if (suffixes.length > j)
        dst[i++] = dict.word(suffixes[j]);
      if (dist.length > j && i < max_count)
        dst[i++] = dict.word(dist[j]);
    }
    if (first_char_upper)
      capitalize_results(dst);
    return i;
  }

  void capitalize_results(String[] rs)
  {
    for (int i = 0; i < rs.length; i++)
      if (rs[i] != null)
        rs[i] = rs[i].substring(0, 1).toUpperCase() + rs[i].substring(1);
  }

  void set_suggestions(List<String> ws)
  {
    _callback.set_suggestions(ws);
    best_suggestion = (ws.size() > 0) ? ws.get(0) : null;
  }

  static final List<String> NO_SUGGESTIONS = Arrays.asList();
  static final int[] NO_RESULTS = new int[0];

  public static interface Callback
  {
    public void set_suggestions(List<String> suggestions);
  }
}
