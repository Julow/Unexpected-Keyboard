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
    int[] suffixes = dict.suffixes(r, MAX_COUNT);
    /* Chinese and Japanese are written without spaces between the words, and
       the editor reports every character typed since the last space as a
       single word. As soon as one word follows another, the run is longer than
       any word of the dictionary and the lookup finds nothing; the word being
       completed is the longest tail of the run that the dictionary knows. */
    boolean shortened = false;
    if (suffixes.length == 0)
    {
      final Cdict queried = dict;
      String tail = unspaced_tail(word, new Known()
          {
            @Override
            public boolean has_prefix(String prefix)
            {
              return queried.suffixes(queried.find(prefix), 1).length > 0;
            }
          });
      if (tail != null)
      {
        shortened = true;
        word = tail;
        r = dict.find(word);
        suffixes = dict.suffixes(r, MAX_COUNT);
      }
    }
    List<Composer.Candidate> out =
      new ArrayList<Composer.Candidate>(MAX_COUNT);
    /* The word itself is not worth suggesting when the user has just typed
       it, which is the case of the tail that was looked up. */
    if (r.found && !shortened)
      out.add(candidate_for(dict.word(r.index), word));
    // Disable distance search for small words
    int[] dist = (word.length() < 3 || out.size() + 1 >= MAX_COUNT) ? NO_RESULTS :
      dict.distance(word, 1, MAX_COUNT);
    for (int j = 0; j < MAX_COUNT && out.size() < MAX_COUNT; j++)
    {
      if (suffixes.length > j)
      {
        String stored = dict.word(suffixes[j]);
        if (!word_text(stored).equals(word))
          out.add(candidate_for(stored, word));
      }
      if (dist.length > j && out.size() < MAX_COUNT)
        out.add(candidate_for(dict.word(dist[j]), word));
    }
    if (first_char_upper)
      capitalize_results(out);
    candidates = out;
    emoji_suggestion = query_emoji(word); // word with substitutions applied
    return out.size();
  }

  /** The text of a word of the dictionary, without the reading that the
      Chinese dictionary stores after a tab (see srcs/chinese/README.md). */
  static String word_text(String word)
  {
    int tab = word.indexOf('\t');
    return (tab < 0) ? word : word.substring(0, tab);
  }

  /** A candidate for a word of the dictionary. The tone drawn above it is the
      one of the first character it adds after [typed], or of its last syllable
      when it adds nothing; a word stored without a reading has none. */
  static Composer.Candidate candidate_for(String word, String typed)
  {
    String text = word_text(word);
    int tab = word.indexOf('\t');
    if (tab < 0)
      return new Composer.Candidate(text);
    String tones = word.substring(tab + 1);
    int i = 0;
    while (i < typed.length() && i < text.length()
        && typed.charAt(i) == text.charAt(i))
      i++;
    if (i >= text.length())
      i = text.length() - 1;
    String mark = (i < 0 || i >= tones.length()) ? null
      : tone_mark(tones.charAt(i));
    return (mark == null) ? new Composer.Candidate(text)
      : new Composer.Candidate(text, mark);
  }

  /** The mark of a tone digit, or [null] when it is not one. The first tone
      has its own mark, which Zhuyin leaves out when typing. */
  static String tone_mark(char digit)
  {
    switch (digit)
    {
      case '1': return "\u02c9";
      case '2': return "\u02ca";
      case '3': return "\u02c7";
      case '4': return "\u02cb";
      case '5': return "\u02d9";
      default: return null;
    }
  }

  /** Whether the dictionary has a word that starts with a prefix, which is
      what the suggestion lookup asks of it. Kept apart from cdict so that the
      shortening of a word written without spaces is testable on a plain
      JVM. */
  interface Known
  {
    public boolean has_prefix(String prefix);
  }

  /** The longest tail of [word] that the dictionary has a word for, or [null]
      when [word] does not end with a script that is written without spaces, or
      when no tail is known at all.

      The last word of a run is the longest tail that the dictionary knows:
      中国 in the run 我爱中国. */
  static String unspaced_tail(String word, Known known)
  {
    int start = word.length();
    while (start > 0 && is_unspaced(word.charAt(start - 1)))
      start--;
    for (int i = start; i < word.length(); i++)
      if (known.has_prefix(word.substring(i)))
        return word.substring(i);
    return null;
  }

  /** Whether [c] belongs to a script that is written without spaces between
      the words, Chinese and Japanese. Korean is written with spaces and the
      editor does report its words. */
  static boolean is_unspaced(char c)
  {
    return (c >= 0x3040 && c <= 0x30ff)   // Hiragana and Katakana
      || (c >= 0x3400 && c <= 0x4dbf)     // CJK Unified Ideographs Extension A
      || (c >= 0x4e00 && c <= 0x9fff)     // CJK Unified Ideographs
      || (c >= 0xf900 && c <= 0xfaff);    // CJK Compatibility Ideographs
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
