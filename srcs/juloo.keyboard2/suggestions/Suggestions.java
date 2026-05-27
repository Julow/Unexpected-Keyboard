package juloo.keyboard2.suggestions;

import java.util.Arrays;
import java.util.List;
import juloo.cdict.Cdict;
import juloo.keyboard2.dict.Dictionaries;
import juloo.keyboard2.dict.PersonalDictionary;
import juloo.keyboard2.Config;
import juloo.keyboard2.ComposeKey;
import juloo.keyboard2.ComposeKeyData;

/** Keep track of the word being typed and provide suggestions for
    [CandidatesView]. */
public final class Suggestions
{
  Callback _callback;
  Config _config;
  boolean _enabled;

  /** The suggestion displayed at the center of the candidates view and entered
      by the space bar. */
  public String best_suggestion = null;

  public Suggestions(Callback c, Config conf)
  {
    _callback = c;
    _config = conf;
  }

  public void started()
  {
    _enabled = _config.editor_config.should_show_candidates_view;
    best_suggestion = null;
  }

  public void currently_typed_word(String word)
  {
    _callback.set_current_word(word.isEmpty() ? null : word);
    if (!_enabled)
      return;
    Cdict dict = _config.current_dictionary;
    PersonalDictionary pdict = _config.personal_dictionary;
    boolean has_personal = pdict != null && !pdict.get_all().isEmpty();
    if (word.length() < 2 || (dict == null && !has_personal))
    {
      set_suggestions(NO_SUGGESTIONS);
      return;
    }
    String[] dst = new String[3];
    int count = 0;
    if (has_personal)
      count += pdict.find_suggestions(word, dst, count, 3 - count);
    if (dict != null && count < 3)
      count += query_suggestions(dict, word, dst, count, 3 - count);
    set_suggestions(Arrays.asList(dst));
  }

  /** Query the main dictionary for [word], writing results into [dst] starting
      at [offset]. Returns the number of suggestions added. */
  int query_suggestions(Cdict dict, String word, String[] dst, int offset, int max_count)
  {
    boolean first_char_upper = Character.isUpperCase(word.charAt(0));
    word = apply_substitutions(word);
    Cdict.Result r = dict.find(word);
    int i = 0;
    if (r.found)
      dst[offset + i++] = dict.word(r.index);
    int[] suffixes = dict.suffixes(r, max_count);
    int[] dist = (word.length() < 3 || i + 1 >= max_count) ? NO_RESULTS :
      dict.distance(word, 1, max_count);
    for (int j = 0; j < max_count && i < max_count; j++)
    {
      if (suffixes.length > j)
        dst[offset + i++] = dict.word(suffixes[j]);
      if (dist.length > j && i < max_count)
        dst[offset + i++] = dict.word(dist[j]);
    }
    if (first_char_upper)
      capitalize_results(dst, offset, i);
    return i;
  }

  /** Kept for callers that don't need offset support. */
  int query_suggestions(Cdict dict, String word, String[] dst, int max_count)
  {
    return query_suggestions(dict, word, dst, 0, max_count);
  }

  void capitalize_results(String[] rs, int offset, int count)
  {
    for (int i = offset; i < offset + count && i < rs.length; i++)
      if (rs[i] != null)
        rs[i] = rs[i].substring(0, 1).toUpperCase() + rs[i].substring(1);
  }

  void capitalize_results(String[] rs)
  {
    capitalize_results(rs, 0, rs.length);
  }

  /** Apply the same substitutions that were used when building the
      dictionaries to find word aliases. This catches missing diacritics for
      example. */
  String apply_substitutions(String w)
  {
    StringBuilder b = new StringBuilder(w);
    int len = w.length();
    for (int i = 0; i < len; i++)
    {
      char r =
        ComposeKey.transform_char(ComposeKeyData.substitutions, b.charAt(i));
      if (r != 0) b.setCharAt(i, r);
    }
    return b.toString();
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
    /** Called with the word currently being typed, or [null] when no word. */
    public void set_current_word(String word);
  }
}
