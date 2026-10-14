package juloo.keyboard2.suggestions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import juloo.cdict.Cdict;
import juloo.keyboard2.dict.Dictionaries;
import juloo.keyboard2.Config;
import juloo.keyboard2.ComposeKey;
import juloo.keyboard2.ComposeKeyData;
import juloo.keyboard2.staged.Composer;

/** Keep track of the word being typed and provide suggestions for
    [CandidatesView]. */
public final class Suggestions
{
  Callback _callback;
  Config _config;
  boolean _enabled;

  /** Current candidates. The best one is at index [0]. Composition candidates
      are not bounded, this can be longer than [MAX_COUNT]. */
  public List<Composer.Candidate> candidates = Collections.emptyList();
  public String emoji_suggestion = null;
  /** Number of suggestions the dictionary offers. */
  public static final int MAX_COUNT = 3;

  public Suggestions(Callback c, Config conf)
  {
    _callback = c;
    _config = conf;
  }

  public void started()
  {
    _enabled = _config.editor_config.should_show_candidates_view;
    clear();
  }

  public void currently_typed_word(String word)
  {
    if (!_enabled)
      return;
    if (word.length() < 2 || _config.current_dictionary == null)
      clear();
    else
      query_suggestions(word);
    _callback.set_suggestions(this);
  }

  void clear()
  {
    candidates = Collections.emptyList();
    emoji_suggestion = null;
  }

  /** Show the candidates of the composing input method instead of the
      dictionary suggestions. The candidates replace the whole composition,
      which is why they are not capitalized like word suggestions are. */
  public void composition_candidates(List<Composer.Candidate> candidates)
  {
    this.candidates = candidates;
    emoji_suggestion = null;
    _callback.set_suggestions(this);
  }

  int query_suggestions(String word)
  {
    Cdict dict = _config.current_dictionary;
    boolean first_char_upper = Character.isUpperCase(word.charAt(0));
    word = apply_substitutions(word);
    Cdict.Result r = dict.find(word);
    List<Composer.Candidate> out =
      new ArrayList<Composer.Candidate>(MAX_COUNT);
    if (r.found)
      out.add(new Composer.Candidate(dict.word(r.index)));
    int[] suffixes = dict.suffixes(r, MAX_COUNT);
    // Disable distance search for small words
    int[] dist = (word.length() < 3 || out.size() + 1 >= MAX_COUNT) ? NO_RESULTS :
      dict.distance(word, 1, MAX_COUNT);
    for (int j = 0; j < MAX_COUNT && out.size() < MAX_COUNT; j++)
    {
      if (suffixes.length > j)
        out.add(new Composer.Candidate(dict.word(suffixes[j])));
      if (dist.length > j && out.size() < MAX_COUNT)
        out.add(new Composer.Candidate(dict.word(dist[j])));
    }
    if (first_char_upper)
      capitalize_results(out);
    candidates = out;
    emoji_suggestion = query_emoji(word); // word with substitutions applied
    return out.size();
  }

  static void capitalize_results(List<Composer.Candidate> s)
  {
    for (int i = 0; i < s.size(); i++)
    {
      Composer.Candidate c = s.get(i);
      s.set(i, new Composer.Candidate(
            c.text.substring(0, 1).toUpperCase() + c.text.substring(1),
            c.annotation));
    }
  }

  String query_emoji(String word)
  {
    Cdict dict = _config.emoji_dictionary;
    // Disable emoji suggestion for short words
    if (dict == null || word.length() < 3)
      return null;
    Cdict.Result r = dict.find(word);
    if (r.found)
      return dict.word(r.index);
    int[] s = dict.suffixes(r, 1);
    if (s.length > 0)
      return dict.word(s[0]);
    return null;
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

  static final int[] NO_RESULTS = new int[0];

  public static interface Callback
  {
    public void set_suggestions(Suggestions suggestions);
  }
}
