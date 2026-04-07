package juloo.keyboard2.suggestions;

import java.util.Arrays;
import java.util.List;
import juloo.cdict.Cdict;
import juloo.keyboard2.dict.Dictionaries;
import juloo.keyboard2.Config;
import juloo.keyboard2.ComposeKey;
import juloo.keyboard2.ComposeKeyData;

/** Keep track of the word being typed and provide suggestions for
    [CandidatesView]. */
public final class Suggestions
{
  Callback _callback;
  Config _config;

  /** Current suggestions. The best suggestion is at index [0]. */
  public String[] suggestions = new String[MAX_COUNT];
  /** Number of suggestions at the beginning of the [suggestions] array that
      are not [null]. */
  public int count = 0;
  public String emoji_suggestion = null;
  /** Number of suggestions in [suggestions]. */
  public static final int MAX_COUNT = 3;

  public Suggestions(Callback c, Config conf)
  {
    _callback = c;
    _config = conf;
  }

  public void currently_typed_word(String word)
  {
    if (word.length() < 2 || _config.current_dictionary == null)
      clear();
    else
      query_suggestions(word);
    set_suggestions();
  }

  void clear()
  {
    count = 0;
    emoji_suggestion = null;
  }

  int query_suggestions(String word)
  {
    Cdict dict = _config.current_dictionary;
    boolean first_char_upper = Character.isUpperCase(word.charAt(0));
    word = apply_substitutions(word);
    Cdict.Result r = dict.find(word);
    int i = 0;
    if (r.found)
      suggestions[i++] = dict.word(r.index);
    int[] suffixes = dict.suffixes(r, MAX_COUNT);
    // Disable distance search for small words
    int[] dist = (word.length() < 3 || i + 1 >= MAX_COUNT) ? NO_RESULTS :
      dict.distance(word, 1, MAX_COUNT);
    for (int j = 0; j < MAX_COUNT && i < MAX_COUNT; j++)
    {
      if (suffixes.length > j)
        suggestions[i++] = dict.word(suffixes[j]);
      if (dist.length > j && i < MAX_COUNT)
        suggestions[i++] = dict.word(dist[j]);
    }
    if (first_char_upper)
      capitalize_results();
    emoji_suggestion = query_emoji(word); // word with substitutions applied
    count = i;
    return i;
  }

  void capitalize_results()
  {
    for (int i = 0; i < count; i++)
      suggestions[i] = suggestions[i].substring(0, 1).toUpperCase()
        + suggestions[i].substring(1);
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

  void set_suggestions()
  {
    _callback.set_suggestions(this);
  }

  static final int[] NO_RESULTS = new int[0];

  public static interface Callback
  {
    public void set_suggestions(Suggestions suggestions);
  }
}
