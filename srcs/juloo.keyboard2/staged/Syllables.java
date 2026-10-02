package juloo.keyboard2.staged;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Build the candidate words of a composition made of Chinese syllables. */
final class Syllables
{
  /** The number of candidates shown by [juloo.keyboard2.suggestions.CandidatesView]. */
  static final int MAX_CANDIDATES = 3;

  /** The words that the [syllables] can be written with, the most likely
      first. [traditional] selects the traditional variant of every character.

      The first candidate is the most frequent character of every syllable.
      The next ones replace the characters of the last syllables, which are the
      ones the user is the most likely to want to correct.

      Returns an empty list when a syllable is unknown. */
  static List<String> candidates(List<String> syllables, boolean traditional)
  {
    List<String> out = new ArrayList<String>();
    int n = syllables.size();
    if (n == 0)
      return out;
    String[] chars = new String[n];
    for (int i = 0; i < n; i++)
    {
      chars[i] = PinyinData.chars(syllables.get(i));
      if (chars[i] == null || chars[i].length() == 0)
        return out;
    }
    char[] chosen = new char[n];
    for (int i = 0; i < n; i++)
      chosen[i] = chars[i].charAt(0);
    Set<String> seen = new HashSet<String>();
    add(out, seen, chosen, traditional);
    for (int i = n - 1; i >= 0 && out.size() < MAX_CANDIDATES; i--)
    {
      char saved = chosen[i];
      for (int j = 1; j < chars[i].length() && out.size() < MAX_CANDIDATES; j++)
      {
        chosen[i] = chars[i].charAt(j);
        add(out, seen, chosen, traditional);
      }
      chosen[i] = saved;
    }
    return out;
  }

  private static void add(List<String> out, Set<String> seen, char[] chars,
      boolean traditional)
  {
    StringBuilder b = new StringBuilder(chars.length);
    for (char c : chars)
      b.append(traditional ? PinyinData.traditional(c) : c);
    String word = b.toString();
    if (seen.add(word))
      out.add(word);
  }
}
