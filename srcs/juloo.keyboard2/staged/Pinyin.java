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

  private static boolean is_letter(char c)
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
  public List<Composer.Candidate> candidates()
  {
    return Syllables.candidates(syllables(), null, false);
  }

  @Override
  public void reset()
  {
    _keys.setLength(0);
  }

  /** The syllables spelled by the keys typed so far, or an empty list when the
      keys are not a sequence of complete syllables.

      The longest syllable first, stepping back to a shorter one when the
      longest does not leave a valid tail - the way [Zhuyin.parse] reads a
      Bopomofo sequence. Without the step back, "heni" reads "hen" and then
      stalls on the "i" that is left: reading only the longest syllable at each
      position failed on 1252 of the 396 squared two-syllable combinations. */
  List<String> syllables()
  {
    List<String> out = syllables(_keys.toString(), 0);
    return (out == null) ? Collections.emptyList() : out;
  }

  /** The syllables of [keys] from [i], or [null] when it has no reading. */
  private static List<String> syllables(String keys, int i)
  {
    if (i == keys.length())
      return new ArrayList<String>();
    int max = Math.min(MAX_SYLLABLE, keys.length() - i);
    for (int len = max; len > 0; len--)
    {
      if (PinyinData.chars(keys.substring(i, i + len)) == null)
        continue;
      List<String> rest = syllables(keys, i + len);
      if (rest != null)
      {
        rest.add(0, keys.substring(i, i + len));
        return rest;
      }
    }
    return null;
  }
}
