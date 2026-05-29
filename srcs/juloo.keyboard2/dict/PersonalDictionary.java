package juloo.keyboard2.dict;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Manages user-defined words and custom autocorrections. */
public final class PersonalDictionary
{
  static final char SEPARATOR = '\0';
  static final String PREF_NAME = "personal_dictionary";
  static final String PREF_WORDS = "words";

  public static final class Entry
  {
    public final String word;        // trigger / word to recognise
    public final String replacement; // what to insert (== word if no autocorrect)

    Entry(String word, String replacement)
    {
      this.word = word;
      this.replacement = (replacement != null && !replacement.isEmpty()) ? replacement : word;
    }

    static Entry parse(String raw)
    {
      int sep = raw.indexOf(SEPARATOR);
      if (sep < 0)
        return new Entry(raw, raw);
      return new Entry(raw.substring(0, sep), raw.substring(sep + 1));
    }

    String serialize()
    {
      if (word.equals(replacement))
        return word;
      return word + SEPARATOR + replacement;
    }
  }

  static PersonalDictionary _instance = null;

  public static PersonalDictionary instance(Context ctx)
  {
    if (_instance == null)
      _instance = new PersonalDictionary(ctx.getApplicationContext());
    return _instance;
  }

  SharedPreferences _prefs;
  List<Entry> _entries;

  PersonalDictionary(Context ctx)
  {
    _prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    load();
  }

  void load()
  {
    Set<String> raw = _prefs.getStringSet(PREF_WORDS, null);
    _entries = new ArrayList<>();
    if (raw != null)
    {
      for (String s : raw)
        _entries.add(Entry.parse(s));
    }
  }

  void save()
  {
    Set<String> raw = new HashSet<>();
    for (Entry e : _entries)
      raw.add(e.serialize());
    _prefs.edit().putStringSet(PREF_WORDS, raw).apply();
  }

  public List<Entry> get_all() { return _entries; }

  /** Add (or update) an entry. [replacement] may be null/empty to mean the
      word stands on its own without autocorrect. */
  public void add(String word, String replacement)
  {
    for (int i = _entries.size() - 1; i >= 0; i--)
      if (_entries.get(i).word.equalsIgnoreCase(word))
        _entries.remove(i);
    _entries.add(new Entry(word, (replacement != null && !replacement.isEmpty()) ? replacement : word));
    save();
  }

  public void remove(String word)
  {
    for (int i = _entries.size() - 1; i >= 0; i--)
      if (_entries.get(i).word.equalsIgnoreCase(word))
        _entries.remove(i);
    save();
  }

  /** Fill [dst] starting at [offset] with up to [max_count] suggestions.
      Returns the number of entries added. */
  public int find_suggestions(String typed_word, String[] dst, int offset, int max_count)
  {
    if (_entries.isEmpty() || typed_word.length() < 1)
      return 0;
    String lower = typed_word.toLowerCase();
    int count = 0;
    // Exact word match first (supports autocorrect replacements)
    for (Entry e : _entries)
    {
      if (count >= max_count || offset + count >= dst.length)
        break;
      if (e.word.equalsIgnoreCase(typed_word))
        dst[offset + count++] = e.replacement;
    }
    // Prefix match next
    for (Entry e : _entries)
    {
      if (count >= max_count || offset + count >= dst.length)
        break;
      if (!e.word.equalsIgnoreCase(typed_word) && e.word.toLowerCase().startsWith(lower))
        dst[offset + count++] = e.replacement;
    }
    return count;
  }
}
