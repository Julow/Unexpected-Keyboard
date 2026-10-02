package juloo.keyboard2.staged;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Type Chinese characters with the Zhuyin (Bopomofo) input method used in
    Taiwan, for example ㄋㄧ for 你.

    Every syllable is made of an initial, a medial and a final, in this order,
    optionally followed by a tone mark. The syllables are looked up in the same
    table as [Pinyin]; tones are ignored but the tone marks are read, as they
    tell where syllables end. The traditional variant of the characters is
    offered. */
public final class Zhuyin implements Composer
{
  private static final String INITIALS = "ㄅㄆㄇㄈㄉㄊㄋㄌㄍㄎㄏㄐㄑㄒㄓㄔㄕㄖㄗㄘㄙ";
  private static final String MEDIALS = "ㄧㄨㄩ";
  private static final String FINALS = "ㄚㄛㄜㄝㄞㄟㄠㄡㄢㄣㄤㄥㄦ";
  /** The first, second, third, fourth and neutral tones. The first tone is
      left out when typing. */
  private static final String TONES = "ˉˊˇˋ˙";

  private final StringBuilder _keys = new StringBuilder();

  @Override
  public boolean type(String s)
  {
    if (s.length() == 0)
      return false;
    for (int i = 0; i < s.length(); i++)
      if (!is_symbol(s.charAt(i)))
        return false;
    _keys.append(s);
    return true;
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
    return Syllables.candidates(syllables(), true);
  }

  @Override
  public void reset()
  {
    _keys.setLength(0);
  }

  static boolean is_symbol(char c)
  {
    return INITIALS.indexOf(c) >= 0 || MEDIALS.indexOf(c) >= 0
      || FINALS.indexOf(c) >= 0 || TONES.indexOf(c) >= 0;
  }

  /** The pinyin spelling of the syllables typed so far, or an empty list when
      the symbols are not a sequence of complete syllables. */
  List<String> syllables()
  {
    String keys = _keys.toString();
    if (keys.length() == 0)
      return Collections.emptyList();
    List<String> out = new ArrayList<String>();
    int i = 0;
    while (i < keys.length())
    {
      int start = i;
      if (i < keys.length() && INITIALS.indexOf(keys.charAt(i)) >= 0)
        i++;
      if (i < keys.length() && MEDIALS.indexOf(keys.charAt(i)) >= 0)
        i++;
      if (i < keys.length() && FINALS.indexOf(keys.charAt(i)) >= 0)
        i++;
      if (i == start)
        return Collections.emptyList();
      String spelling = keys.substring(start, i);
      if (i < keys.length() && TONES.indexOf(keys.charAt(i)) >= 0)
        i++;
      String syllable = PinyinData.pinyin(spelling);
      if (syllable == null)
        return Collections.emptyList();
      out.add(syllable);
    }
    return out;
  }
}
