package juloo.keyboard2.staged;

/** The dictionary that [Kanji] uses to convert a kana reading to kanji.

    The dictionary is a UTF-8 text file, one entry per line, sorted by reading:

    <pre>reading &lt;TAB&gt; word &lt;TAB&gt; cost</pre>

    `reading` is hiragana and is what the keyboard types, `word` is what the
    reading is converted to, and `cost` is how unlikely the word is (a lower
    cost is a more common word). Sorting by reading lets the entries of a
    reading be found by binary search.

    The three fields of every entry are parsed once, in [load], rather than
    derived from the text at every lookup. They are read in the inner loop of
    the conversion, which runs on every keystroke: a `substring` and an
    `Integer.parseInt` per binary-search probe were most of what a keystroke
    cost. */
public final class KanjiDictionary
{
  /** The reading of every entry, in file order. */
  private static String[] _readings = new String[0];
  /** What the word of every entry converts to. */
  private static String[] _words = new String[0];
  /** How unlikely the word of every entry is. */
  private static int[] _costs = new int[0];
  /** Number of entries in the arrays above. */
  private static int _count = 0;

  /** Read [text] as the dictionary, replacing the previous one. Return the
      number of entries. A line that is not three tab-separated fields is
      skipped. */
  public static int load(String text)
  {
    String[] lines = text.split("\n");
    String[] readings = new String[lines.length];
    String[] words = new String[lines.length];
    int[] costs = new int[lines.length];
    int n = 0;
    for (int i = 0; i < lines.length; i++)
    {
      String line = lines[i];
      int first = line.indexOf('\t');
      if (first <= 0)
        continue;
      int second = line.indexOf('\t', first + 1);
      if (second < 0 || second + 1 >= line.length())
        continue;
      int cost;
      try
      {
        cost = Integer.parseInt(line.substring(second + 1));
      }
      catch (NumberFormatException e)
      {
        continue;
      }
      readings[n] = line.substring(0, first);
      words[n] = line.substring(first + 1, second);
      costs[n] = cost;
      n++;
    }
    _readings = readings;
    _words = words;
    _costs = costs;
    _count = n;
    return n;
  }

  static boolean is_loaded()
  {
    return _count > 0;
  }

  static int count()
  {
    return _count;
  }

  /** Index of the first entry whose reading is exactly [reading], or [-1] when
      the reading is not in the dictionary. Every entry of a reading follows
      the first one. */
  static int find(String reading)
  {
    int lo = 0;
    int hi = _count - 1;
    int found = -1;
    while (lo <= hi)
    {
      int mid = (lo + hi) >>> 1;
      int cmp = _readings[mid].compareTo(reading);
      if (cmp < 0)
        lo = mid + 1;
      else
      {
        if (cmp == 0)
          found = mid;
        hi = mid - 1;
      }
    }
    return found;
  }

  /** The reading of entry [i]. */
  static String reading(int i)
  {
    return _readings[i];
  }

  /** What entry [i] converts to. */
  static String word(int i)
  {
    return _words[i];
  }

  /** How unlikely the word of entry [i] is. */
  static int cost(int i)
  {
    return _costs[i];
  }
}
