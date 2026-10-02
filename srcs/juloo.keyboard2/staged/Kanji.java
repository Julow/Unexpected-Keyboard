package juloo.keyboard2.staged;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Convert a kana reading to kanji, using [KanjiDictionary].

    The whole reading is converted at once: every way of cutting it into
    dictionary words is considered and the one with the lowest total cost wins.
    That is what turns にほんご into 日本語 rather than に + ほん + ご, and
    わたしは into 私は rather than わたし + は.

    Each word has a cost and the cheapest cut of the reading is the most likely
    one. A kana that no word starts with is passed through with a high cost, so
    that a reading can always be converted and a half-typed word does not
    disappear.

    The candidates are the cheapest cut, then cuts that differ by a single word,
    then the reading itself, so that the user can always keep the kana. */
final class Kanji
{
  /** Longer than the longest reading of the dictionary (22 kana), so that no
      word is out of reach. See [srcs/japanese/build_dict.py]. */
  private static final int MAX_READING = 24;
  /** Cost of a kana that is left as it is. Higher than any dictionary cost of
      the entries that are kept, so that real words win. */
  private static final int UNKNOWN_COST = 12000;
  /** Cost added per word. Mozc's word costs are meant to be added to the cost
      of going from one word to the next, which the open source dictionary does
      not ship; a constant plays that role and stops the reading from being cut
      into as many words as possible. にほんご is 日本語 and not 日本 + ご. */
  private static final int JOIN_COST = 10000;
  /** Added to a word that is written with the kana of its own reading, so that
      the kanji can win over the plain kana: わたし gives 私. Not added to a
      single kana, which is nearly always a particle (の, は, に) and is written
      as kana. */
  private static final int KANA_COST = 300;
  /** Added to a word that is written in katakana. The open source dictionary
      holds katakana spellings of common words (イイ, コレ) and katakana names
      (ニノ) that would otherwise win over the kana or the kanji. A katakana
      word that is the only reading of its entry, such as コンピュータ, is still
      converted. */
  private static final int KATAKANA_COST = 7000;
  /** How many other words of a same reading are tried as alternatives. */
  private static final int ALTERNATIVES = 4;

  /** The kanji that the kana [reading] can be converted to, the most likely
      first, and the reading itself last. Returns an empty list when there is
      nothing to convert, which is the case of katakana, of a reading that
      contains something else than kana, and of an empty reading. */
  static List<String> candidates(String reading, int count)
  {
    List<String> out = new ArrayList<String>();
    int n = reading.length();
    if (n == 0 || !is_kana(reading) || !KanjiDictionary.is_loaded())
      return out;
    // Cheapest cut of the reading up to every position.
    int[] cost = new int[n + 1];
    int[] from = new int[n + 1];
    int[] entry = new int[n + 1];
    for (int i = 0; i <= n; i++)
    {
      cost[i] = Integer.MAX_VALUE;
      from[i] = -1;
      entry[i] = -1;
    }
    cost[0] = 0;
    for (int i = 0; i < n; i++)
    {
      if (cost[i] == Integer.MAX_VALUE)
        continue;
      int max = Math.min(MAX_READING, n - i);
      for (int len = 1; len <= max; len++)
      {
        String r = reading.substring(i, i + len);
        for (int e = KanjiDictionary.find(r);
            e >= 0 && e < KanjiDictionary.count(); e++)
        {
          if (!KanjiDictionary.reading(e).equals(r))
            break;
          relax(cost, from, entry, i + len, i, e, cost[i]
              + KanjiDictionary.cost(e)
              + penalty(r, KanjiDictionary.word(e)) + JOIN_COST);
        }
      }
      relax(cost, from, entry, i + 1, i, -1,
          cost[i] + UNKNOWN_COST + JOIN_COST);
    }
    if (cost[n] == Integer.MAX_VALUE)
      return out;
    List<int[]> path = new ArrayList<int[]>();
    for (int p = n; p > 0; p = from[p])
      path.add(new int[]{from[p], p, entry[p]});
    Collections.reverse(path);

    String best = render(reading, path, -1, -1);
    out.add(best);

    // Replace one word of the winning cut by another word of the same reading.
    List<String> alternatives = new ArrayList<String>();
    List<int[]> ranked = new ArrayList<int[]>();
    for (int s = 0; s < path.size(); s++)
    {
      int[] segment = path.get(s);
      String r = reading.substring(segment[0], segment[1]);
      int segment_cost = (segment[2] < 0) ? UNKNOWN_COST
        : KanjiDictionary.cost(segment[2]);
      int seen = 0;
      for (int e = KanjiDictionary.find(r);
          e >= 0 && e < KanjiDictionary.count() && seen < ALTERNATIVES; e++)
      {
        if (!KanjiDictionary.reading(e).equals(r))
          break;
        if (e == segment[2])
          continue;
        seen++;
        String candidate = render(reading, path, s, e);
        if (out.contains(candidate) || alternatives.contains(candidate))
          continue;
        alternatives.add(candidate);
        ranked.add(new int[]{cost[n] - segment_cost + KanjiDictionary.cost(e),
          alternatives.size() - 1});
      }
    }
    Collections.sort(ranked, new Comparator<int[]>()
        {
          public int compare(int[] a, int[] b) { return a[0] - b[0]; }
        });
    for (int i = 0; i < ranked.size() && out.size() + 1 < count; i++)
      out.add(alternatives.get(ranked.get(i)[1]));

    if (!out.contains(reading))
      out.add(reading);
    return out;
  }

  /** The path, with the word of segment [replace] replaced by entry [entry]
      when both are not negative. */
  private static String render(String reading, List<int[]> path, int replace,
      int entry)
  {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < path.size(); i++)
    {
      int[] segment = path.get(i);
      int e = (i == replace) ? entry : segment[2];
      b.append((e < 0) ? reading.substring(segment[0], segment[1])
          : KanjiDictionary.word(e));
    }
    return b.toString();
  }

  /** See [KANA_COST] and [KATAKANA_COST]. */
  private static int penalty(String reading, String word)
  {
    if (word.equals(reading))
      return (reading.length() < 2) ? 0 : KANA_COST;
    return is_katakana(word) ? KATAKANA_COST : 0;
  }

  private static boolean is_katakana(String word)
  {
    for (int i = 0; i < word.length(); i++)
    {
      char c = word.charAt(i);
      if ((c < 0x30A1 || c > 0x30F6) && c != '\u30FC')
        return false;
    }
    return word.length() > 0;
  }

  /** Keep the cheapest way to reach [at]. */
  private static void relax(int[] cost, int[] from, int[] entry, int at,
      int from_i, int entry_i, int c)
  {
    if (c < cost[at])
    {
      cost[at] = c;
      from[at] = from_i;
      entry[at] = entry_i;
    }
  }

  /** Whether the reading is made of the kana that the dictionary uses. */
  private static boolean is_kana(String reading)
  {
    for (int i = 0; i < reading.length(); i++)
    {
      char c = reading.charAt(i);
      if ((c < 0x3041 || c > 0x3096) && c != '\u30fc')
        return false;
    }
    return true;
  }
}
