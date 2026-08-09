package juloo.keyboard2.suggestions;

import java.util.Arrays;
import java.util.List;
import juloo.cdict.Cdict;
import juloo.keyboard2.dict.Dictionaries;
import juloo.keyboard2.Config;
import juloo.keyboard2.ComposeKey;
import juloo.keyboard2.ComposeKeyData;
import juloo.keyboard2.PersonalDictionary;

/** Keep track of the word being typed and provide suggestions for
    [CandidatesView]. */
public final class Suggestions
{
  Callback _callback;
  Config _config;
  boolean _enabled;

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

  public void started()
  {
    _enabled = _config.editor_config.should_show_candidates_view;
    clear();
  }

  public void currently_typed_word(String word)
  {
    if (!_enabled)
      return;
    boolean has_personal =
      _config.personal_dictionary != null
      && !_config.personal_dictionary.is_empty();
    if (word.length() < 2
        || (_config.current_dictionary == null && !has_personal))
      clear();
    else
      query_suggestions(word);
    _callback.set_suggestions(this);
  }

  void clear()
  {
    count = 0;
    for (int i = 0; i < MAX_COUNT; i++)
      suggestions[i] = null;
    emoji_suggestion = null;
  }

  int query_suggestions(String word)
  {
    clear();
    int i = 0;
    boolean first_char_upper = Character.isUpperCase(word.charAt(0));
    String subst = apply_substitutions(word);
    Cdict dict = _config.current_dictionary;
    // Personal dictionary entries are matched against the raw typed word;
    // they are normalized with the same substitutions when the dictionary is
    // loaded. Shortcut expansions take the first slots; personal word
    // matches fill the slots left unused by the compiled dictionary.
    PersonalDictionary pd = _config.personal_dictionary;
    if (pd != null)
    {
      List<String> shortcuts = pd.query_shortcuts(word, MAX_COUNT);
      for (int j = 0; j < shortcuts.size() && i < MAX_COUNT; j++)
        suggestions[i++] = shortcuts.get(j);
    }
    if (dict != null && i < MAX_COUNT)
    {
      Cdict.Result r = dict.find(subst);
      String[] cdict_words = new String[MAX_COUNT];
      int c = 0;
      if (r.found)
        cdict_words[c++] = dict.word(r.index);
      int[] suffixes = dict.suffixes(r, MAX_COUNT);
      // Disable distance search for small words
      int[] dist = (subst.length() < 3 || c + 1 >= MAX_COUNT) ? NO_RESULTS :
        dict.distance(subst, 1, MAX_COUNT);
      for (int j = 0; j < MAX_COUNT && c < MAX_COUNT; j++)
      {
        if (suffixes.length > j)
          cdict_words[c++] = dict.word(suffixes[j]);
        if (dist.length > j && c < MAX_COUNT)
          cdict_words[c++] = dict.word(dist[j]);
      }
      for (int j = 0; j < c && i < MAX_COUNT; j++)
      {
        String cw = cdict_words[j];
        if (first_char_upper)
          cw = cw.substring(0, 1).toUpperCase() + cw.substring(1);
        if (!contains_ignore_case(i, cw))
          suggestions[i++] = cw;
      }
    }
    if (pd != null && i < MAX_COUNT)
    {
      List<String> word_matches =
        pd.query_word_matches(word, first_char_upper, MAX_COUNT);
      for (int j = 0; j < word_matches.size() && i < MAX_COUNT; j++)
      {
        if (!contains_ignore_case(i, word_matches.get(j)))
          suggestions[i++] = word_matches.get(j);
      }
    }
    emoji_suggestion = query_emoji(subst);
    count = i;
    return i;
  }

  boolean contains_ignore_case(int upto, String w)
  {
    for (int k = 0; k < upto; k++)
      if (suggestions[k] != null && suggestions[k].equalsIgnoreCase(w))
        return true;
    return false;
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
