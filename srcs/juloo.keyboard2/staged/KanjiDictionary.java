package juloo.keyboard2.staged;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/** The dictionary that [Kanji] uses to convert a kana reading to kanji.

    The dictionary is a gzipped UTF-8 text file, one entry per line, sorted by
    reading:

    <pre>reading &lt;TAB&gt; word &lt;TAB&gt; cost</pre>

    `reading` is hiragana and is what the keyboard types, `word` is what the
    reading is converted to, and `cost` is how unlikely the word is (a lower
    cost is a more common word). Sorting by reading lets the entries of a
    reading be found by binary search, which avoids building a map of the
    50,000 entries. */
public final class KanjiDictionary
{
  /** The whole file. Entries are the lines of this string. */
  private static String _text = "";
  /** Offset of the first character of every entry in [_text]. */
  private static int[] _starts = new int[0];

  /** Read [in] as the dictionary, replacing the previous one. Return the
      number of entries. The stream is closed. */
  public static int load(InputStream in) throws IOException
  {
    byte[] data = read_all(in);
    in.close();
    // The file is gzipped, but the build could store it uncompressed.
    if (data.length > 2 && (data[0] & 0xff) == 0x1f && (data[1] & 0xff) == 0x8b)
      data = read_all(new GZIPInputStream(new ByteArrayInputStream(data)));
    return load(new String(data, "UTF-8"));
  }

  /** Read [text] as the dictionary, replacing the previous one. Return the
      number of entries. */
  public static int load(String text)
  {
    int[] starts = entry_starts(text);
    _text = text;
    _starts = starts;
    return starts.length;
  }

  public static boolean is_loaded()
  {
    return _starts.length > 0;
  }

  public static int count()
  {
    return _starts.length;
  }

  /** Index of the first entry whose reading is exactly [reading], or [-1] when
      the reading is not in the dictionary. Every entry of a reading follows
      the first one. */
  static int find(String reading)
  {
    int lo = 0;
    int hi = _starts.length - 1;
    int found = -1;
    while (lo <= hi)
    {
      int mid = (lo + hi) >>> 1;
      int cmp = reading(mid).compareTo(reading);
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
    int start = _starts[i];
    return _text.substring(start, _text.indexOf('\t', start));
  }

  /** What entry [i] converts to. */
  static String word(int i)
  {
    int start = _text.indexOf('\t', _starts[i]) + 1;
    return _text.substring(start, _text.indexOf('\t', start));
  }

  /** How unlikely the word of entry [i] is. */
  static int cost(int i)
  {
    int start = _text.indexOf('\t', _text.indexOf('\t', _starts[i]) + 1) + 1;
    int end = _text.indexOf('\n', start);
    if (end < 0)
      end = _text.length();
    return Integer.parseInt(_text.substring(start, end));
  }

  /** The offset of the first character of every non-empty line of [text]. */
  private static int[] entry_starts(String text)
  {
    int count = 0;
    int i = 0;
    while (i < text.length())
    {
      int end = text.indexOf('\n', i);
      if (end < 0)
        end = text.length();
      if (end > i)
        count++;
      i = end + 1;
    }
    int[] starts = new int[count];
    int k = 0;
    i = 0;
    while (i < text.length())
    {
      int end = text.indexOf('\n', i);
      if (end < 0)
        end = text.length();
      if (end > i)
        starts[k++] = i;
      i = end + 1;
    }
    return starts;
  }

  private static byte[] read_all(InputStream in) throws IOException
  {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    int n;
    while ((n = in.read(buffer)) > 0)
      out.write(buffer, 0, n);
    return out.toByteArray();
  }
}
