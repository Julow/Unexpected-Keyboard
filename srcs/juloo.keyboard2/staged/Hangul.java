package juloo.keyboard2.staged;

import java.util.Collections;
import java.util.List;

/** Compose Hangul syllables from the jamo typed on a 두벌식 (dubeolsik)
    layout.

    The jamo are kept decomposed in a buffer and turned into syllables each
    time the buffer changes. Keeping them decomposed is what allows a syllable
    to be rewritten by the keys that follow it: typing ㅅ ㅏ ㄹ ㅏ builds the
    buffer [ㅅㅏㄹㅏ], which is rendered as 사람, while a naive incremental
    composition would render 살 and then be unable to move the ㄹ to the next
    syllable.

    The rendering rules are the standard ones:
    - a syllable starts with a consonant, or with a silent ㅇ when it starts
      with a vowel;
    - compound vowels (ㅘ, ㅢ, …) and compound finals (ㄳ, ㄺ, …) are assembled
      from their components, which is why the compound jamo found on the layout
      are expanded when they are typed;
    - a final consonant followed by a vowel becomes the initial of the next
      syllable (사람, 갑사), splitting the final when it is compound. */
public final class Hangul implements Composer
{
  /** The jamo typed so far, as elementary jamo. */
  private final StringBuilder _jamo = new StringBuilder();

  @Override
  public boolean type(String s)
  {
    if (s.length() == 0)
      return false;
    // Refuse the key entirely when part of it is not a jamo, so that the
    // caller commits the composition instead of splitting it.
    for (int i = 0; i < s.length(); i++)
      if (expand(s.charAt(i)) == null)
        return false;
    for (int i = 0; i < s.length(); i++)
      _jamo.append(expand(s.charAt(i)));
    return true;
  }

  @Override
  public boolean backspace()
  {
    int len = _jamo.length();
    if (len == 0)
      return false;
    _jamo.setLength(len - 1);
    return true;
  }

  @Override
  public String text()
  {
    return render(_jamo);
  }

  @Override
  public List<String> candidates()
  {
    return Collections.emptyList();
  }

  @Override
  public void reset()
  {
    _jamo.setLength(0);
  }

  /** The initial consonants, in the order used by the Unicode Hangul Syllables
      block. */
  private static final String INITIALS = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ";
  /** The medial vowels, in the order used by the Unicode Hangul Syllables
      block. */
  private static final String MEDIALS = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ";
  /** The final consonants, in the order used by the Unicode Hangul Syllables
      block. The first entry is the absence of a final consonant. */
  private static final String FINALS = "\0ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ";
  /** Index of the silent consonant in [INITIALS]. */
  private static final int SILENT = 11;
  private static final char FIRST_SYLLABLE = 0xAC00;

  /** Expand the compound jamo that appear on the layout into the elementary
      jamo that [render] expects. Return [null] for anything else. */
  static String expand(char c)
  {
    switch (c)
    {
      // Compound vowels
      case 'ㅘ': return "ㅗㅏ";
      case 'ㅙ': return "ㅗㅐ";
      case 'ㅚ': return "ㅗㅣ";
      case 'ㅝ': return "ㅜㅓ";
      case 'ㅞ': return "ㅜㅔ";
      case 'ㅟ': return "ㅜㅣ";
      case 'ㅢ': return "ㅡㅣ";
      // Compound finals
      case 'ㄳ': return "ㄱㅅ";
      case 'ㄵ': return "ㄴㅈ";
      case 'ㄶ': return "ㄴㅎ";
      case 'ㄺ': return "ㄹㄱ";
      case 'ㄻ': return "ㄹㅁ";
      case 'ㄼ': return "ㄹㅂ";
      case 'ㄽ': return "ㄹㅅ";
      case 'ㄾ': return "ㄹㅌ";
      case 'ㄿ': return "ㄹㅍ";
      case 'ㅀ': return "ㄹㅎ";
      case 'ㅄ': return "ㅂㅅ";
      default:
        if (INITIALS.indexOf(c) >= 0 || MEDIALS.indexOf(c) >= 0)
          return String.valueOf(c);
        return null;
    }
  }

  static boolean is_consonant(char c)
  {
    return INITIALS.indexOf(c) >= 0 || FINALS.indexOf(c) > 0;
  }

  static boolean is_vowel(char c)
  {
    return MEDIALS.indexOf(c) >= 0;
  }

  /** Render a sequence of elementary jamo as syllables. */
  static String render(CharSequence jamo)
  {
    StringBuilder out = new StringBuilder();
    int n = jamo.length();
    int i = 0;
    /** Initial consonant taken from the final of the previous syllable. */
    int carried = -1;
    while (i < n)
    {
      int initial;
      if (carried >= 0)
      {
        initial = carried;
        carried = -1;
      }
      else if (is_consonant(jamo.charAt(i)))
      {
        initial = INITIALS.indexOf(jamo.charAt(i++));
      }
      else
      {
        initial = SILENT;
      }
      int medial = -1;
      if (i < n && is_vowel(jamo.charAt(i)))
      {
        medial = MEDIALS.indexOf(jamo.charAt(i++));
        while (i < n && is_vowel(jamo.charAt(i)))
        {
          int m = combine_medial(MEDIALS.charAt(medial), jamo.charAt(i));
          if (m < 0)
            break;
          medial = m;
          i++;
        }
      }
      if (medial < 0)
      {
        // A consonant that is not followed by a vowel is left as it is.
        out.append(INITIALS.charAt(initial));
        continue;
      }
      int fin = 0;
      while (i < n && is_consonant(jamo.charAt(i)))
      {
        char c = jamo.charAt(i);
        int f = (fin == 0) ? FINALS.indexOf(c) : combine_final(FINALS.charAt(fin), c);
        if (f <= 0)
          break;
        fin = f;
        i++;
      }
      // A final consonant followed by a vowel moves to the next syllable.
      if (fin != 0 && i < n && is_vowel(jamo.charAt(i)))
      {
        int[] split = split_final(FINALS.charAt(fin));
        fin = FINALS.indexOf(split[0]);
        carried = INITIALS.indexOf((char)split[1]);
      }
      out.append((char)(FIRST_SYLLABLE + initial * 588 + medial * 28 + fin));
    }
    return out.toString();
  }

  /** Index of the vowel made of [first] followed by [second], or [-1]. */
  static int combine_medial(char first, char second)
  {
    switch (first)
    {
      case 'ㅗ':
        switch (second)
        {
          case 'ㅏ': return 9;
          case 'ㅐ': return 10;
          case 'ㅣ': return 11;
        }
        break;
      case 'ㅜ':
        switch (second)
        {
          case 'ㅓ': return 14;
          case 'ㅔ': return 15;
          case 'ㅣ': return 16;
        }
        break;
      case 'ㅡ':
        if (second == 'ㅣ')
          return 19;
        break;
    }
    return -1;
  }

  /** Index of the final consonant made of [first] followed by [second], or
      [-1]. */
  static int combine_final(char first, char second)
  {
    switch (first)
    {
      case 'ㄱ': if (second == 'ㅅ') return 3; break;
      case 'ㄴ':
        switch (second)
        {
          case 'ㅈ': return 5;
          case 'ㅎ': return 6;
        }
        break;
      case 'ㄹ':
        switch (second)
        {
          case 'ㄱ': return 9;
          case 'ㅁ': return 10;
          case 'ㅂ': return 11;
          case 'ㅅ': return 12;
          case 'ㅌ': return 13;
          case 'ㅍ': return 14;
          case 'ㅎ': return 15;
        }
        break;
      case 'ㅂ': if (second == 'ㅅ') return 18; break;
    }
    return -1;
  }

  /** Split the compound final [fin] into the final that is kept and the
      consonant that starts the next syllable. A final that is not compound is
      entirely moved to the next syllable. */
  static int[] split_final(char fin)
  {
    switch (fin)
    {
      case 'ㄳ': return new int[]{'ㄱ', 'ㅅ'};
      case 'ㄵ': return new int[]{'ㄴ', 'ㅈ'};
      case 'ㄶ': return new int[]{'ㄴ', 'ㅎ'};
      case 'ㄺ': return new int[]{'ㄹ', 'ㄱ'};
      case 'ㄻ': return new int[]{'ㄹ', 'ㅁ'};
      case 'ㄼ': return new int[]{'ㄹ', 'ㅂ'};
      case 'ㄽ': return new int[]{'ㄹ', 'ㅅ'};
      case 'ㄾ': return new int[]{'ㄹ', 'ㅌ'};
      case 'ㄿ': return new int[]{'ㄹ', 'ㅍ'};
      case 'ㅀ': return new int[]{'ㄹ', 'ㅎ'};
      case 'ㅄ': return new int[]{'ㅂ', 'ㅅ'};
      default: return new int[]{0, fin};
    }
  }
}
