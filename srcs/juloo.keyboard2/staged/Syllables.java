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

      [tones] holds the tone typed after every syllable, [0] when none was
      typed. A syllable that has a tone is written with the characters that
      have it only, which usually leaves a single one.

      The first candidate is the most frequent character of every syllable.
      The next ones replace the characters of the last syllables, which are the
      ones the user is the most likely to want to correct.

      Every candidate carries the tone of the syllable it corrects as its
      annotation, so that the keyboard can show it above the character.

      Returns an empty list when a syllable is unknown or has no character
      with the typed tone. */
  static List<Composer.Candidate> candidates(List<String> syllables,
      int[] tones, boolean traditional_first)
  {
    List<Composer.Candidate> out = new ArrayList<Composer.Candidate>();
    int n = syllables.size();
    if (n == 0)
      return out;
    String[] chars = new String[n];
    /* The tone of every character of [chars], as digits. */
    String[] char_tones = new String[n];
    for (int i = 0; i < n; i++)
    {
      String all = PinyinData.chars(syllables.get(i));
      String all_tones = PinyinData.tones(syllables.get(i));
      if (all == null || all.length() == 0 || all_tones == null)
        return out;
      int wanted = (tones == null || i >= tones.length) ? 0 : tones[i];
      StringBuilder cs = new StringBuilder();
      StringBuilder ts = new StringBuilder();
      for (int j = 0; j < all.length(); j++)
      {
        int tone = all_tones.charAt(j) - '0';
        if (wanted != 0 && tone != wanted)
          continue;
        cs.append(all.charAt(j));
        ts.append(all_tones.charAt(j));
      }
      if (cs.length() == 0)
        return out;
      chars[i] = cs.toString();
      char_tones[i] = ts.toString();
    }
    char[] chosen = new char[n];
    for (int i = 0; i < n; i++)
      chosen[i] = chars[i].charAt(0);
    /* The words that the syllables can be written with, most likely first. The
       characters of the last syllables are replaced first, which are the ones
       the user is the most likely to want to correct. Each word carries the
       tone of the character it replaces, which is the one to explain to the
       user. */
    List<char[]> words = new ArrayList<char[]>();
    List<Character> word_tones = new ArrayList<Character>();
    words.add(chosen.clone());
    word_tones.add(char_tones[n - 1].charAt(0));
    for (int i = n - 1; i >= 0; i--)
    {
      char saved = chosen[i];
      for (int j = 1; j < chars[i].length(); j++)
      {
        chosen[i] = chars[i].charAt(j);
        words.add(chosen.clone());
        word_tones.add(char_tones[i].charAt(j));
      }
      chosen[i] = saved;
    }
    Set<String> seen = new HashSet<String>();
    add_all(out, seen, words, word_tones, traditional_first);
    add_all(out, seen, words, word_tones, !traditional_first);
    return out;
  }

  private static void add_all(List<Composer.Candidate> out, Set<String> seen,
      List<char[]> words, List<Character> word_tones, boolean traditional)
  {
    for (int i = 0; i < words.size(); i++)
      add(out, seen, words.get(i), word_tones.get(i).charValue() - '0',
          traditional);
  }

  /** Write [chars] in the given script and add it to [out] if it is new. */
  private static void add(List<Composer.Candidate> out, Set<String> seen,
      char[] chars, int tone, boolean traditional)
  {
    StringBuilder b = new StringBuilder(chars.length);
    for (char c : chars)
      b.append(traditional ? PinyinData.traditional(c) : PinyinData.simplified(c));
    String word = b.toString();
    if (seen.add(word))
      out.add(new Composer.Candidate(word, tone_mark(tone)));
  }

  /** The mark to show above a character of the given tone. The first tone is
      shown with its own mark, ˉ, which Zhuyin does not require to be typed. */
  private static String tone_mark(int tone)
  {
    switch (tone)
    {
      case 1: return "\u02c9"; // ˉ
      case 2: return "\u02ca"; // ˊ
      case 3: return "\u02c7"; // ˇ
      case 4: return "\u02cb"; // ˋ
      case 5: return "\u02d9"; // ˙
      default: return null;
    }
  }
}
