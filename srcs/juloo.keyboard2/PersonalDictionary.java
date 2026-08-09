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
    _entries = (entries != null) ? entries : new ArrayList<Entry>();
  }

  public boolean is_empty() { return _entries.isEmpty(); }

  /** Up to [max] words matching [typed]. Ranked: words whose shortcut is
      exactly [typed] (case-insensitive), then words equal to [typed], then
      words starting with [typed], in insertion order. Word matches are
      capitalized when [typed] starts with an upper-case letter; shortcut
      expansions are always returned verbatim. Case-insensitive duplicates
      are returned once. */
  public List<String> query(String typed, int max)
  {
    List<String> out = new ArrayList<String>();
    if (max <= 0 || typed.length() == 0)
      return out;
    List<String> shortcut = new ArrayList<String>();
    List<String> exact = new ArrayList<String>();
    List<String> prefix = new ArrayList<String>();
    String t = typed.toLowerCase();
    boolean capitalize = Character.isUpperCase(typed.charAt(0));
    for (Entry e : _entries)
    {
      if (!e.shortcut.isEmpty() && e.shortcut.equalsIgnoreCase(typed))
      {
        shortcut.add(e.word);
        continue;
      }
      String lw = e.word.toLowerCase();
      if (lw.equals(t)) exact.add(capitalized(e.word, capitalize));
      else if (lw.startsWith(t)) prefix.add(capitalized(e.word, capitalize));
    }
    for (String w : shortcut) { add_unique(out, w); if (out.size() >= max) return out; }
    for (String w : exact) { add_unique(out, w); if (out.size() >= max) return out; }
    for (String w : prefix) { add_unique(out, w); if (out.size() >= max) return out; }
    return out;
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

  /** A word and its optional shortcut. */
  public static final class Entry
  {
    public final String word;
    /** Empty string when no shortcut is attached. */
    public final String shortcut;

    public Entry(String word, String shortcut)
    {
      this.word = word;
      this.shortcut = (shortcut != null) ? shortcut : "";
    }
  }
}
