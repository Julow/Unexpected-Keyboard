package juloo.keyboard2;

import java.util.ArrayList;
import java.util.List;

/** A user-maintained list of words suggested when their prefix is typed. Kept
    separate from the compiled Cdict dictionaries, which are immutable. */
public final class PersonalDictionary
{
  final List<String> _words;

  public PersonalDictionary(List<String> words)
  {
    _words = (words != null) ? words : new ArrayList<String>();
  }

  public boolean is_empty() { return _words.isEmpty(); }

  /** Up to [max] words matching [typed] as a case-insensitive prefix. Exact
      (case-insensitive) matches first, then prefix matches in insertion order.
      Stored casing is preserved; case-insensitive duplicates returned once. */
  public List<String> query(String typed, int max)
  {
    List<String> exact = new ArrayList<String>();
    List<String> prefix = new ArrayList<String>();
    String t = typed.toLowerCase();
    for (String w : _words)
    {
      String lw = w.toLowerCase();
      if (lw.equals(t)) exact.add(w);
      else if (lw.startsWith(t)) prefix.add(w);
    }
    List<String> out = new ArrayList<String>();
    for (String w : exact) { add_unique(out, w); if (out.size() >= max) return out; }
    for (String w : prefix) { add_unique(out, w); if (out.size() >= max) return out; }
    return out;
  }

  static void add_unique(List<String> out, String w)
  {
    for (String o : out) if (o.equalsIgnoreCase(w)) return;
    out.add(w);
  }
}
