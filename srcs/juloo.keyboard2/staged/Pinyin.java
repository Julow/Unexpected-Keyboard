package juloo.keyboard2.staged;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Type Chinese characters by writing Hanyu Pinyin on a latin layout, for
    example `ni` for 你 and `nihao` for 你好.

    The keys typed so far are held as composing text and translated to
    characters that are offered as candidates. Tone marks are not typed: the
    table is ordered by frequency and tones are ignored, so `ma` offers 吗 妈 马
    …. The traditional variant of the characters is not offered, this input
    method is used with Simplified Chinese; see [Zhuyin] for the traditional
    characters. */
public final class Pinyin implements Composer
{
  /** The longest pinyin syllable, "zhuang", "chuang" and "shuang". */
  private static final int MAX_SYLLABLE = 6;

  private final StringBuilder _keys = new StringBuilder();

  @Override
  public boolean type(String s)
  {
    if (s.length() == 0)
      return false;
    for (int i = 0; i < s.length(); i++)
    {
      char c = s.charAt(i);
      /* Auto capitalisation can turn the first letter of a sentence into an
         upper case letter, pinyin itself is always lower case. */
      if (!is_letter(c))
        return false;
    }
    for (int i = 0; i < s.length(); i++)
      _keys.append(Character.toLowerCase(s.charAt(i)));
    return true;
  }

  static boolean is_letter(char c)
  {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
  }

  @Override
  public boolean backspace()
  {
    int len = _keys.length();
    if (len == 0)
      return false;
    _keys.setLength(len - 1);
    return true;
  }

  @Override
  public String text()
  {
    return _keys.toString();
  }

  @Override
  public List<String> candidates()
  {
    return Syllables.candidates(syllables(), false);
  }

  @Override
  public void reset()
  {
    _keys.setLength(0);
  }

  /** The syllables spelled by the keys typed so far, or an empty list when the
      keys are not a sequence of complete syllables. */
  List<String> syllables()
  {
    String keys = _keys.toString();
    if (keys.length() == 0)
      return Collections.emptyList();
    List<String> out = new ArrayList<String>();
    int i = 0;
    while (i < keys.length())
    {
      int len = longest_syllable(keys, i);
      if (len == 0)
        return Collections.emptyList();
      out.add(keys.substring(i, i + len));
      i += len;
    }
    return out;
  }

  /** The length of the longest syllable starting at [i], or [0]. */
  private static int longest_syllable(String keys, int i)
  {
    int max = Math.min(MAX_SYLLABLE, keys.length() - i);
    for (int len = max; len > 0; len--)
      if (PinyinData.chars(keys.substring(i, i + len)) != null)
        return len;
    return 0;
  }
}
