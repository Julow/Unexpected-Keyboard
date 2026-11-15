package juloo.keyboard2.suggestions;

import java.util.ArrayList;
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

  /** [previous_word] is the word before [word] in the same phrase or [null].
      It is used to rank the suggestions when the dictionary has a bigrams
      file. */
  public void currently_typed_word(String word, String previous_word)
  {
    if (!_enabled)
      return;
    if (word.length() < 2 || _config.current_dictionary == null)
      clear();
    else
    {
      Bigrams bg = _config.current_bigrams;
      int context = (bg == null) ? -1 : context_index(bg, previous_word);
      if (context < 0)
        query_suggestions(word);
      else
        query_suggestions_in_context(word, bg, context);
    }
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
      capitalize_results(suggestions, i);
    emoji_suggestion = query_emoji(word); // word with substitutions applied
    count = i;
    return i;
  }

  /** Penalties added to the cost of candidates depending on how they match
      the typed word. The typed word is kept unless another word is much more
      likely after the previous word. */
  static final int PENALTY_TYPED = 0;
  static final int PENALTY_DIACRITICS = 2 * Bigrams.COST_UNIT;
  static final int PENALTY_PREFIX = 3 * Bigrams.COST_UNIT;
  static final int PENALTY_TYPO = 7 * Bigrams.COST_UNIT;

  final ArrayList<String> _cand_words = new ArrayList<String>();
  final ArrayList<Integer> _cand_costs = new ArrayList<Integer>();

  /** Like [query_suggestions] but rank the candidates by how likely they are
      after the previous word. Words that commonly follow the previous word
      are also suggested if they start like the typed word, even if they
      differ in diacritics, for example "és" for "es". */
  void query_suggestions_in_context(String typed, Bigrams bg, int context)
  {
    Cdict dict = _config.current_dictionary;
    boolean first_char_upper = Character.isUpperCase(typed.charAt(0));
    String word = apply_substitutions(typed);
    String norm = normalize(typed);
    _cand_words.clear();
    _cand_costs.clear();
    Cdict.Result r = dict.find(word);
    if (r.found)
      add_candidate(dict.word(r.index), typed, norm, bg, context);
    for (int s : dict.suffixes(r, CONTEXT_QUERY_COUNT))
      add_candidate(dict.word(s), typed, norm, bg, context);
    if (word.length() >= 3)
      for (int d : dict.distance(word, 1, CONTEXT_QUERY_COUNT))
        add_candidate(dict.word(d), typed, norm, bg, context);
    int end = bg.entries_start(context + 1);
    for (int e = bg.entries_start(context); e < end; e++)
    {
      String w = bg.word(bg.entry_word(e));
      if (normalize(w).startsWith(norm))
        add_candidate(w, typed, norm, bg, context);
    }
    // Select the best candidates. Insertion order breaks ties, which favors
    // the dictionary order.
    int i = 0;
    for (; i < MAX_COUNT && i < _cand_words.size(); i++)
    {
      int best = i;
      for (int j = i + 1; j < _cand_words.size(); j++)
        if (_cand_costs.get(j) < _cand_costs.get(best))
          best = j;
      String w = _cand_words.get(best);
      Integer c = _cand_costs.get(best);
      _cand_words.remove(best);
      _cand_costs.remove(best);
      _cand_words.add(i, w);
      _cand_costs.add(i, c);
      suggestions[i] = w;
    }
    for (int j = i; j < MAX_COUNT; j++)
      suggestions[j] = null;
    if (first_char_upper)
      capitalize_results(suggestions, i);
    emoji_suggestion = query_emoji(word);
    count = i;
  }

  /** Add a candidate unless already present. Its cost is how unlikely it is
      after the context word plus a penalty depending on how it matches the
      typed word. */
  void add_candidate(String w, String typed, String typed_norm, Bigrams bg,
      int context)
  {
    if (_cand_words.contains(w))
      return;
    String norm = normalize(w);
    int penalty;
    if (w.equalsIgnoreCase(typed))
      penalty = PENALTY_TYPED;
    else if (norm.equals(typed_norm))
      penalty = PENALTY_DIACRITICS;
    else if (norm.startsWith(typed_norm))
      penalty = PENALTY_PREFIX;
    else
      penalty = PENALTY_TYPO;
    _cand_words.add(w);
    _cand_costs.add(bg.cost(context, bg.find(w)) + penalty);
  }

  /** Index of the previous word in [bg] or [-1] if it can't be used as a
      context. */
  static int context_index(Bigrams bg, String prev)
  {
    if (prev == null || prev.length() == 0)
      return -1;
    int i = bg.find(prev);
    if (i < 0)
      i = bg.find(prev.toLowerCase());
    // Elided articles like in "l'any" are not part of the context.
    int apos = prev.indexOf('\'');
    if (i < 0 && apos >= 0 && apos + 1 < prev.length())
      return context_index(bg, prev.substring(apos + 1));
    if (i < 0 || bg.entries_start(i) == bg.entries_start(i + 1))
      return -1;
    return i;
  }

  /** Lowercase and remove diacritics, using the same substitutions as the
      dictionaries. */
  static String normalize(String w)
  {
    return apply_substitutions(w.toLowerCase());
  }

  /** Number of results queried from the dictionary when ranking in
      context. */
  static final int CONTEXT_QUERY_COUNT = 8;

  static void capitalize_results(String[] s, int count)
  {
    for (int i = 0; i < count; i++)
      s[i] = s[i].substring(0, 1).toUpperCase() + s[i].substring(1);
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
  static String apply_substitutions(String w)
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
