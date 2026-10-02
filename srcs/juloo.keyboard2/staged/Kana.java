package juloo.keyboard2.staged;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Compose Japanese kana from the keys of a kana layout and from romaji typed
    on a latin layout.

    Kana are mostly typed one character per key on a かな layout, but the
    dakuten ゛ and the handakuten ゜ rewrite the character that comes before
    them: typing か then ゛ must replace か with が. Romaji is worse, as a single
    kana takes several letters and a letter can be ambiguous: the "n" of "na"
    is the n of な, while a lone "n" is ん, and "kk" is っ followed by another
    consonant.

    The composition is therefore kept in two parts:
    - [_kana] holds the kana that are composed, which are the ones that do not
      depend on the keys that follow;
    - [_romaji] holds the letters that are still waiting for the keys that
      could complete them. They are shown to the user as they were typed, so
      that typing "k" shows "k" and typing "a" after it shows か.

    [text] is the concatenation of both, which is always what the user typed,
    read back as kana as early as it is unambiguous.

    The "n" is the one letter that is both a complete key (ん) and the start of
    longer keys (な, にゃ, …). A lone "n" is composed as ん right away, so that
    it is never committed as a latin letter, and [_n_pending] remembers that
    this ん can still be replaced when a vowel or a "y" follows it. */
public final class Kana implements Composer
{
  /** The kana that are composed. */
  private final StringBuilder _kana = new StringBuilder();
  /** The romaji letters that wait for the keys that could complete them. It is
      always either empty or a prefix of at least one key of [ROMAJI]. */
  private final StringBuilder _romaji = new StringBuilder();
  /** Whether the last character of [_kana] is the ん of a lone "n", which is
      still the beginning of "na", "nya", … and can be replaced. */
  private boolean _n_pending = false;

  @Override
  public boolean type(String s)
  {
    if (s.length() == 0)
      return false;
    if (all_kana(s))
    {
      // A kana key ends the romaji sequence that was being typed: a pending
      // "n" becomes ん before the kana is appended.
      resolve_pending();
      _kana.append(s);
      return true;
    }
    if (all_marks(s))
    {
      resolve_pending();
      for (int i = 0; i < s.length(); i++)
        apply_mark(s.charAt(i));
      return true;
    }
    if (all_romaji(s))
    {
      for (int i = 0; i < s.length(); i++)
        type_romaji(Character.toLowerCase(s.charAt(i)));
      return true;
    }
    // Refuse the key entirely, so that the caller commits the composition
    // instead of splitting it.
    return false;
  }

  @Override
  public boolean backspace()
  {
    if (_romaji.length() > 0)
    {
      _romaji.setLength(_romaji.length() - 1);
      return true;
    }
    if (_kana.length() > 0)
    {
      _kana.setLength(_kana.length() - 1);
      // The ん of a lone "n" is the last character of [_kana].
      _n_pending = false;
      return true;
    }
    return false;
  }

  @Override
  public String text()
  {
    return _kana.toString() + _romaji.toString();
  }

  @Override
  public List<String> candidates()
  {
    /* The kana typed so far are offered as kanji. A pending romaji sequence is
       not converted: the user is still spelling it. */
    if (_romaji.length() > 0)
      return Collections.emptyList();
    return Kanji.candidates(_kana.toString(), Syllables.MAX_CANDIDATES);
  }

  @Override
  public void reset()
  {
    _kana.setLength(0);
    _romaji.setLength(0);
    _n_pending = false;
  }

  /** Return [true] when every character of [s] is a kana. */
  static boolean all_kana(String s)
  {
    for (int i = 0; i < s.length(); i++)
      if (!is_kana(s.charAt(i)))
        return false;
    return true;
  }

  /** Hiragana U+3041..U+3096, katakana U+30A1..U+30FA, ー and the four
      iteration marks. */
  static boolean is_kana(char c)
  {
    return (c >= 0x3041 && c <= 0x3096)
      || (c >= 0x30A1 && c <= 0x30FA)
      || c == 0x30FC // ー
      || c == 0x309D || c == 0x309E // ゝ ゞ
      || c == 0x30FD || c == 0x30FE; // ヽ ヾ
  }

  static boolean is_mark(char c)
  {
    return c == 0x309B || c == 0x309C; // ゛ ゜
  }

  /** Return [true] when every character of [s] is a mark. */
  static boolean all_marks(String s)
  {
    for (int i = 0; i < s.length(); i++)
      if (!is_mark(s.charAt(i)))
        return false;
    return true;
  }

  /** Return [true] when [s] only contains letters, '-' or an apostrophe that
      follows the 'n' it disambiguates. */
  boolean all_romaji(String s)
  {
    // Extend the pending letters to know whether an apostrophe follows 'n'.
    StringBuilder pending = new StringBuilder(_n_pending ? "n" : _romaji.toString());
    for (int i = 0; i < s.length(); i++)
    {
      char c = Character.toLowerCase(s.charAt(i));
      if (c == '\'')
      {
        if (pending.length() == 0 || pending.charAt(pending.length() - 1) != 'n')
          return false;
      }
      else if (c != '-' && (c < 'a' || c > 'z'))
        return false;
      pending.append(c);
    }
    return true;
  }

  /** Add the romaji letter [c] to the pending sequence and convert it as far
      as it is unambiguous. */
  private void type_romaji(char c)
  {
    if (_n_pending)
    {
      // "nn" and "n'" are ん, which is already composed.
      if (c == 'n' || c == '\'')
      {
        _n_pending = false;
        return;
      }
      _n_pending = false;
      // A vowel or a "y" turns the composed ん back into the "n" of "na",
      // "nya", …
      if (is_vowel(c) || c == 'y')
      {
        _kana.setLength(_kana.length() - 1);
        _romaji.append('n');
      }
    }
    _romaji.append(c);
    convert_romaji();
  }

  private static boolean is_vowel(char c)
  {
    return c == 'a' || c == 'i' || c == 'u' || c == 'e' || c == 'o';
  }

  /** Apply the dakuten or the handakuten [mark] to the last character of the
      composition, or append the mark when it does not combine. */
  private void apply_mark(char mark)
  {
    int len = _kana.length();
    if (len == 0)
    {
      _kana.append(mark);
      return;
    }
    char combined = combine_mark(_kana.charAt(len - 1), mark);
    if (combined == 0)
      _kana.append(mark);
    else
      _kana.setCharAt(len - 1, combined);
  }

  /** The kana that a dakuten voices, followed by those that a handakuten
      turns into a p kana, and the kana they are turned into. The two strings
      are read in parallel. */
  private static final String MARK_BASE =
    "うかきくけこさしすせそたちつてとはひふへほゝ"
    + "ワヰヱヲウカキクケコサシスセソタチツテトハヒフヘホヽ"
    + "はひふへほハヒフヘホ";
  private static final String MARK_COMBINED =
    "ゔがぎぐげござじずぜぞだぢづでどばびぶべぼゞ"
    + "ヷヸヹヺヴガギグゲゴザジズゼゾダヂヅデドバビブベボヾ"
    + "ぱぴぷぺぽパピプペポ";

  /** Index of the first kana that a handakuten applies to in [MARK_BASE]. The
      kana before it are the ones that take the dakuten. */
  private static final int HANDAKUTEN_START =
      "うかきくけこさしすせそたちつてとはひふへほゝ"
      .length()
      + "ワヰヱヲウカキクケコサシスセソタチツテトハヒフヘホヽ".length();

  /** The kana made of [base] and [mark], or [0] when they do not combine. */
  static char combine_mark(char base, char mark)
  {
    int i;
    if (mark == 0x309B)
      i = MARK_BASE.indexOf(base);
    else if (mark == 0x309C)
      i = MARK_BASE.indexOf(base, HANDAKUTEN_START);
    else
      return 0;
    if (i < 0 || (mark == 0x309B && i >= HANDAKUTEN_START))
      return 0;
    return MARK_COMBINED.charAt(i);
  }

  /** Convert the pending romaji as far as it is unambiguous. A pending
      sequence that is a strict prefix of a longer key is kept, and a sequence
      that cannot be converted anymore is written as it was typed. */
  private void convert_romaji()
  {
    convert_pending(true);
  }

  /** Convert everything that is pending, including the sequences that could
      still be completed. Used when a key that is not a romaji letter ends the
      sequence, so that a pending "n" becomes ん. */
  private void resolve_pending()
  {
    // The sequence is over, the ん of a lone "n" cannot be replaced anymore.
    _n_pending = false;
    convert_pending(false);
  }

  /** Convert the pending letters. When [incremental] is [true], a sequence
      that is a strict prefix of a longer key is left pending and a lone "n"
      stays revisable, as more keys can still follow it. */
  private void convert_pending(boolean incremental)
  {
    while (_romaji.length() > 0)
    {
      String pending = _romaji.toString();
      if (incremental && pending.equals("n"))
      {
        // A lone "n" is ん, but it can still be the beginning of "na", "nya",
        // … so it is kept revisable instead of waiting as a latin letter.
        _kana.append('ん');
        _romaji.setLength(0);
        _n_pending = true;
        return;
      }
      if (incremental && is_prefix(pending))
        return;
      String kana = ROMAJI.get(pending);
      if (kana != null)
      {
        _kana.append(kana);
        _romaji.setLength(0);
        return;
      }
      if (is_doubled(pending))
      {
        // A doubled consonant other than "nn" is a small っ followed by the
        // rest, which is interpreted again: "kko" is っこ.
        _kana.append('っ');
        _romaji.deleteCharAt(0);
        continue;
      }
      int len = longest_key(pending);
      if (len > 0)
      {
        _kana.append(ROMAJI.get(pending.substring(0, len)));
        _romaji.delete(0, len);
        continue;
      }
      // Nothing can be converted anymore, the letters are kept as they were
      // typed.
      _kana.append(pending);
      _romaji.setLength(0);
    }
  }

  /** Return [true] when [s] is the beginning of a key that is longer than
      [s], in which case it is too early to convert it. */
  private static boolean is_prefix(String s)
  {
    for (String key : ROMAJI.keySet())
      if (key.length() > s.length() && key.startsWith(s))
        return true;
    return false;
  }

  /** Return [true] when [s] starts with a doubled consonant that is not
      "nn", which is a small っ. */
  private static boolean is_doubled(String s)
  {
    return s.length() >= 2 && s.charAt(0) == s.charAt(1)
      && CONSONANTS.indexOf(s.charAt(0)) >= 0;
  }

  /** The length of the longest key that starts [s], or [0]. */
  private static int longest_key(String s)
  {
    for (int len = s.length(); len > 0; len--)
      if (ROMAJI.containsKey(s.substring(0, len)))
        return len;
    return 0;
  }

  /** The letters that can be doubled into a small っ. The "n" is not one of
      them, "nn" is ん. */
  private static final String CONSONANTS = "bcdfghjklmpqrstvwxyz";

  /** The romaji keys and the kana they produce. */
  private static final Map<String, String> ROMAJI = romaji_table();

  private static Map<String, String> romaji_table()
  {
    Map<String, String> m = new HashMap<String, String>();
    // Vowels
    m.put("a", "あ");
    m.put("i", "い");
    m.put("u", "う");
    m.put("e", "え");
    m.put("o", "お");
    // か行, が行
    m.put("ka", "か");
    m.put("ki", "き");
    m.put("ku", "く");
    m.put("ke", "け");
    m.put("ko", "こ");
    m.put("ga", "が");
    m.put("gi", "ぎ");
    m.put("gu", "ぐ");
    m.put("ge", "げ");
    m.put("go", "ご");
    // さ行, ざ行
    m.put("sa", "さ");
    m.put("shi", "し");
    m.put("su", "す");
    m.put("se", "せ");
    m.put("so", "そ");
    m.put("za", "ざ");
    m.put("ji", "じ");
    m.put("zu", "ず");
    m.put("ze", "ぜ");
    m.put("zo", "ぞ");
    // た行, だ行
    m.put("ta", "た");
    m.put("chi", "ち");
    m.put("tsu", "つ");
    m.put("te", "て");
    m.put("to", "と");
    m.put("da", "だ");
    m.put("di", "ぢ");
    m.put("du", "づ");
    m.put("de", "で");
    m.put("do", "ど");
    // な行
    m.put("na", "な");
    m.put("ni", "に");
    m.put("nu", "ぬ");
    m.put("ne", "ね");
    m.put("no", "の");
    // は行, ば行, ぱ行
    m.put("ha", "は");
    m.put("hi", "ひ");
    m.put("fu", "ふ");
    m.put("he", "へ");
    m.put("ho", "ほ");
    m.put("ba", "ば");
    m.put("bi", "び");
    m.put("bu", "ぶ");
    m.put("be", "べ");
    m.put("bo", "ぼ");
    m.put("pa", "ぱ");
    m.put("pi", "ぴ");
    m.put("pu", "ぷ");
    m.put("pe", "ぺ");
    m.put("po", "ぽ");
    // ま行, や行, ら行, わ行
    m.put("ma", "ま");
    m.put("mi", "み");
    m.put("mu", "む");
    m.put("me", "め");
    m.put("mo", "も");
    m.put("ya", "や");
    m.put("yu", "ゆ");
    m.put("yo", "よ");
    m.put("ra", "ら");
    m.put("ri", "り");
    m.put("ru", "る");
    m.put("re", "れ");
    m.put("ro", "ろ");
    m.put("wa", "わ");
    m.put("wo", "を");
    // ん and the keys that disambiguate it
    m.put("n", "ん");
    m.put("nn", "ん");
    m.put("n'", "ん");
    // The digraphs of the yōon
    m.put("kya", "きゃ");
    m.put("kyu", "きゅ");
    m.put("kyo", "きょ");
    m.put("gya", "ぎゃ");
    m.put("gyu", "ぎゅ");
    m.put("gyo", "ぎょ");
    m.put("sha", "しゃ");
    m.put("shu", "しゅ");
    m.put("sho", "しょ");
    m.put("ja", "じゃ");
    m.put("ju", "じゅ");
    m.put("jo", "じょ");
    m.put("cha", "ちゃ");
    m.put("chu", "ちゅ");
    m.put("cho", "ちょ");
    m.put("nya", "にゃ");
    m.put("nyu", "にゅ");
    m.put("nyo", "にょ");
    m.put("hya", "ひゃ");
    m.put("hyu", "ひゅ");
    m.put("hyo", "ひょ");
    m.put("bya", "びゃ");
    m.put("byu", "びゅ");
    m.put("byo", "びょ");
    m.put("pya", "ぴゃ");
    m.put("pyu", "ぴゅ");
    m.put("pyo", "ぴょ");
    m.put("mya", "みゃ");
    m.put("myu", "みゅ");
    m.put("myo", "みょ");
    m.put("rya", "りゃ");
    m.put("ryu", "りゅ");
    m.put("ryo", "りょ");
    m.put("zya", "じゃ");
    m.put("zyu", "じゅ");
    m.put("zyo", "じょ");
    m.put("dya", "ぢゃ");
    m.put("dyu", "ぢゅ");
    m.put("dyo", "ぢょ");
    // The small kana, with the l and x prefixes
    m.put("la", "ぁ");
    m.put("li", "ぃ");
    m.put("lu", "ぅ");
    m.put("le", "ぇ");
    m.put("lo", "ぉ");
    m.put("xa", "ぁ");
    m.put("xi", "ぃ");
    m.put("xu", "ぅ");
    m.put("xe", "ぇ");
    m.put("xo", "ぉ");
    m.put("ltu", "っ");
    m.put("xtu", "っ");
    m.put("ltsu", "っ");
    m.put("xtsu", "っ");
    m.put("lya", "ゃ");
    m.put("lyu", "ゅ");
    m.put("lyo", "ょ");
    m.put("xya", "ゃ");
    m.put("xyu", "ゅ");
    m.put("xyo", "ょ");
    m.put("lwa", "ゎ");
    m.put("xwa", "ゎ");
    // The prolonged sound mark
    m.put("-", "ー");
    return m;
  }
}
