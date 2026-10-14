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
    offered.

    Only the combinations that form a syllable of the language are accepted: a
    key that cannot follow the ones typed so far is refused, like a key that
    is not a jamo is refused by [Hangul]. The keys that are still the beginning
    of a syllable are accepted, as they are what the user types while a
    syllable is incomplete. */
public final class Zhuyin implements Composer
{
  private static final String INITIALS = "ㄅㄆㄇㄈㄉㄊㄋㄌㄍㄎㄏㄐㄑㄒㄓㄔㄕㄖㄗㄘㄙ";
  private static final String MEDIALS = "ㄧㄨㄩ";
  private static final String FINALS = "ㄚㄛㄜㄝㄞㄟㄠㄡㄢㄣㄤㄥㄦ";
  /** The first, second, third, fourth and neutral tones. The first tone is
      left out when typing. */
  private static final String TONES = "ˉˊˇˋ˙";
  /** A spelling is at most an initial, a medial and a final. */
  private static final int MAX_SPELLING = 3;

  private final StringBuilder _keys = new StringBuilder();

  @Override
  public boolean type(String s)
  {
    // Refuse the key entirely when it cannot be part of a syllable, so that
    // the caller commits the composition instead of keeping one that could
    // never be a Chinese word.
    if (!extends_composition(_keys, s))
      return false;
    _keys.append(s);
    return true;
  }

  /** Whether the key that writes [s] can extend the composition. This is the
      check that [type] applies without modifying anything, so that the
      keyboard can grey out the keys that cannot continue the composition.
      Every key is accepted while nothing is composed: there is nothing to
      continue, and the key starts a new composition or is typed as usual. */
  public boolean accepts(String s)
  {
    return (_keys.length() == 0) || extends_composition(_keys, s);
  }

  /** Whether appending [s] to [keys] gives a valid composition. */
  private static boolean extends_composition(CharSequence keys, String s)
  {
    if (s.length() == 0)
      return false;
    for (int i = 0; i < s.length(); i++)
      if (!is_symbol(s.charAt(i)))
        return false;
    StringBuilder candidate = new StringBuilder(keys);
    candidate.append(s);
    return parse(candidate, 0, true) != null;
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

  /** Whether [s] has the shape of a spelling, that is an initial, a medial and
      a final, each optional and in this order. Whether the spelling is a
      syllable of the language is decided by the table. */
  static boolean is_spelling(CharSequence s)
  {
    int i = 0;
    if (i < s.length() && INITIALS.indexOf(s.charAt(i)) >= 0)
      i++;
    if (i < s.length() && MEDIALS.indexOf(s.charAt(i)) >= 0)
      i++;
    if (i < s.length() && FINALS.indexOf(s.charAt(i)) >= 0)
      i++;
    return i == s.length() && i > 0;
  }

  /** The pinyin syllables that [keys] is made of, starting at [i], or [null]
      when it is not a valid composition.

      The spellings are read from the longest to the shortest, so that ㄒㄧㄚ
      is read as "xia" and not as "xi" followed by "a". A shorter spelling is
      tried when the longest one does not leave a valid composition, which is
      what reads ㄅㄨㄞ as "bu" followed by "ai".

      [partial] accepts a last, incomplete spelling as long as it is the
      beginning of one, which is what the keys look like while a syllable is
      being typed. */
  private static List<String> parse(CharSequence keys, int i, boolean partial)
  {
    if (i == keys.length())
      return new ArrayList<String>();
    int max = Math.min(MAX_SPELLING, keys.length() - i);
    for (int len = max; len > 0; len--)
    {
      CharSequence spelling = keys.subSequence(i, i + len);
      if (!is_spelling(spelling))
        continue;
      String syllable = PinyinData.pinyin(spelling.toString());
      if (syllable != null)
      {
        int next = i + len;
        if (next < keys.length() && TONES.indexOf(keys.charAt(next)) >= 0)
          next++;
        List<String> rest = parse(keys, next, partial);
        if (rest != null)
        {
          rest.add(0, syllable);
          return rest;
        }
      }
      else if (partial && i + len == keys.length()
          && PinyinData.is_spelling_prefix(spelling.toString()))
        return new ArrayList<String>();
    }
    return null;
  }

  /** The pinyin spelling of the syllables typed so far, or an empty list when
      the keys are not a sequence of complete syllables. */
  List<String> syllables()
  {
    List<String> syllables = parse(_keys, 0, false);
    return (syllables == null) ? Collections.<String>emptyList() : syllables;
  }
}
