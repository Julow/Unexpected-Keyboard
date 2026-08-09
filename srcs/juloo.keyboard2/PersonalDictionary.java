package juloo.keyboard2;

import java.util.ArrayList;
import java.util.List;

/** A user-maintained list of words suggested when their prefix is typed. An
    entry can have a shortcut attached: typing the shortcut exactly suggests
    the word. Kept separate from the compiled Cdict dictionaries, which are
    immutable. */
public final class PersonalDictionary
{
  final List<Entry> _entries;

  public PersonalDictionary(List<Entry> entries)
  {
    _entries = new ArrayList<Entry>();
    if (entries == null)
      return;
    for (Entry e : entries)
    {
      // Pre-normalize for matching: lower-cased with the same substitutions
      // that are applied to the typed text and the compiled dictionaries.
      _entries.add(new Entry(e.word, normalized(e.word),
          e.shortcut, normalized(e.shortcut)));
    }
  }

  public boolean is_empty() { return _entries.isEmpty(); }

  /** Up to [max] expansions of shortcuts exactly equal to [typed]
      (case-insensitive), returned verbatim in insertion order.
      Case-insensitive duplicates are returned once. */
  public List<String> query_shortcuts(String typed, int max)
  {
    List<String> out = new ArrayList<String>();
    if (max <= 0 || typed.length() == 0)
      return out;
    String t = normalized(typed);
    for (Entry e : _entries)
    {
      if (e.norm_shortcut.isEmpty() || !e.norm_shortcut.equals(t))
        continue;
      add_unique(out, e.word);
      if (out.size() >= max)
        return out;
    }
    return out;
  }

  /** Up to [max] words matching [typed]: words equal to [typed] first, then
      words starting with [typed], in insertion order. Matching is
      case-insensitive and uses the same substitutions as the compiled
      dictionaries; word matches are capitalized when [capitalize] is set.
      Case-insensitive duplicates are returned once. */
  public List<String> query_word_matches(String typed, boolean capitalize,
      int max)
  {
    List<String> out = new ArrayList<String>();
    if (max <= 0 || typed.length() == 0)
      return out;
    List<String> prefix = new ArrayList<String>();
    String t = normalized(typed);
    for (Entry e : _entries)
    {
      if (e.norm_word.equals(t))
        add_unique(out, capitalized(e.word, capitalize));
      else if (e.norm_word.startsWith(t))
        prefix.add(e.word);
      if (out.size() >= max)
        return out;
    }
    for (String w : prefix)
    {
      add_unique(out, capitalized(w, capitalize));
      if (out.size() >= max)
        return out;
    }
    return out;
  }

  /** [w] lower-cased and transformed with the same character substitutions
      that were applied when building the compiled dictionaries. */
  static String normalized(String w)
  {
    StringBuilder b = new StringBuilder(w.toLowerCase());
    for (int i = 0; i < b.length(); i++)
    {
      char r =
        ComposeKey.transform_char(ComposeKeyData.substitutions, b.charAt(i));
      if (r != 0) b.setCharAt(i, r);
    }
    return b.toString();
  }

  static String capitalized(String w, boolean capitalize)
  {
    if (!capitalize || w.isEmpty() || Character.isUpperCase(w.charAt(0)))
      return w;
    return w.substring(0, 1).toUpperCase() + w.substring(1);
  }

  static void add_unique(List<String> out, String w)
  {
    for (String o : out) if (o.equalsIgnoreCase(w)) return;
    out.add(w);
  }

  /** A word and its optional shortcut. [norm_word] and [norm_shortcut] are
      the normalized forms used for matching. */
  public static final class Entry
  {
    public final String word;
    /** Empty string when no shortcut is attached. */
    public final String shortcut;
    final String norm_word;
    final String norm_shortcut;

    /** An entry to be stored; normalization happens in the enclosing
        dictionary. */
    public Entry(String word, String shortcut)
    {
      this(word, null, shortcut, null);
    }

    Entry(String word, String norm_word, String shortcut, String norm_shortcut)
    {
      this.word = word;
      this.shortcut = (shortcut != null) ? shortcut : "";
      this.norm_word =
        (norm_word != null) ? norm_word : ((word != null) ? word : "");
      this.norm_shortcut =
        (norm_shortcut != null) ? norm_shortcut : this.shortcut;
    }
  }
}
