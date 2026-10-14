package juloo.keyboard2.staged;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Build the candidate words of a composition made of Chinese syllables. */
final class Syllables
{
  /** The words that the [syllables] can be written with, the most likely
      first. Both scripts are offered: [traditional_first] selects the one that
      comes first, the other one is appended after all of its candidates.

      The first candidate is the most frequent character of every syllable.
      The next ones replace the characters of the last syllables, which are the
      ones the user is the most likely to want to correct.

      Returns an empty list when a syllable is unknown. */
  static List<String> candidates(List<String> syllables,
      boolean traditional_first)
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
    /* The words that the syllables can be written with, most likely first. The
       characters of the last syllables are replaced first, which are the ones
       the user is the most likely to want to correct. */
    List<char[]> words = new ArrayList<char[]>();
    words.add(chosen.clone());
    for (int i = n - 1; i >= 0; i--)
    {
      char saved = chosen[i];
      for (int j = 1; j < chars[i].length(); j++)
      {
        chosen[i] = chars[i].charAt(j);
        words.add(chosen.clone());
      }
      chosen[i] = saved;
    }
    Set<String> seen = new HashSet<String>();
    add_all(out, seen, words, traditional_first);
    add_all(out, seen, words, !traditional_first);
    return out;
  }

  private static void add_all(List<String> out, Set<String> seen,
      List<char[]> words, boolean traditional)
  {
    for (char[] word : words)
      add(out, seen, word, traditional);
  }

  /** Write [chars] in the given script and add it to [out] if it is new. */
  private static void add(List<String> out, Set<String> seen, char[] chars,
      boolean traditional)
  {
    StringBuilder b = new StringBuilder(chars.length);
    for (char c : chars)
      b.append(traditional ? PinyinData.traditional(c) : PinyinData.simplified(c));
    String word = b.toString();
    if (seen.add(word))
      out.add(word);
  }
}
