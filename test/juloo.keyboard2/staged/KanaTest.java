package juloo.keyboard2.staged;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class KanaTest
{
  private final Kana _kana = new Kana();

  /** Type every key of [keys] in order on a new composer and return the
      composition. */
  private static String compose(String... keys)
  {
    Kana kana = new Kana();
    for (String key : keys)
      assertTrue(key, kana.type(key));
    return kana.text();
  }

  @Test
  public void kana_keys_are_appended()
  {
    assertEquals("か", compose("か"));
    assertEquals("かき", compose("か", "き"));
    assertEquals("ア", compose("ア"));
    assertEquals("ー", compose("ー"));
    // The iteration marks are kana too
    assertEquals("ゝゞヽヾ", compose("ゝ", "ゞ", "ヽ", "ヾ"));
    assertEquals("きょう", compose("きょう"));
  }

  @Test
  public void dakuten_in_hiragana()
  {
    assertEquals("が", compose("か", "゛"));
    assertEquals("ば", compose("は", "゛"));
    assertEquals("ゔ", compose("う", "゛"));
    assertEquals("じ", compose("し", "゛"));
    assertEquals("ぱ", compose("は", "゜"));
    assertEquals("ぽ", compose("ほ", "゜"));
    // The mark rewrites the character before it
    assertEquals("がぎ", compose("か", "゛", "き", "゛"));
    assertEquals("ゞ", compose("ゝ", "゛"));
  }

  @Test
  public void dakuten_in_katakana()
  {
    assertEquals("バ", compose("ハ", "゛"));
    assertEquals("ポ", compose("ホ", "゜"));
    assertEquals("ヴ", compose("ウ", "゛"));
    assertEquals("ヷ", compose("ワ", "゛"));
    assertEquals("ヾ", compose("ヽ", "゛"));
    assertEquals("グ", compose("ク", "゛"));
  }

  @Test
  public void marks_that_do_not_combine_are_appended()
  {
    assertEquals("あ゛", compose("あ", "゛"));
    assertEquals("か゜", compose("か", "゜"));
    assertEquals("ん゜", compose("ん", "゜"));
    assertEquals("ー゛", compose("ー", "゛"));
    // Nothing to modify
    assertEquals("゛", compose("゛"));
  }

  @Test
  public void dakuten_applies_to_a_kana_typed_in_romaji()
  {
    assertEquals("が", compose("k", "a", "゛"));
    assertEquals("ば", compose("h", "a", "゛"));
    assertEquals("ぱ", compose("h", "a", "゜"));
  }

  @Test
  public void small_kana()
  {
    assertEquals("ぁ", compose("l", "a"));
    assertEquals("ぃ", compose("x", "i"));
    assertEquals("ぉ", compose("x", "o"));
    assertEquals("っ", compose("l", "t", "u"));
    assertEquals("っ", compose("x", "t", "u"));
    assertEquals("ゃ", compose("l", "y", "a"));
    assertEquals("ゅ", compose("x", "y", "u"));
    assertEquals("ゎ", compose("l", "w", "a"));
    assertEquals("って", compose("l", "t", "u", "t", "e"));
  }

  @Test
  public void romaji_gojuon()
  {
    assertEquals("か", compose("k", "a"));
    assertEquals("か", compose("ka"));
    assertEquals("し", compose("s", "h", "i"));
    assertEquals("つ", compose("t", "s", "u"));
    assertEquals("ち", compose("c", "h", "i"));
    assertEquals("ふ", compose("f", "u"));
    assertEquals("じ", compose("j", "i"));
    assertEquals("を", compose("w", "o"));
    assertEquals("ん", compose("n", "n"));
    assertEquals("ー", compose("-"));
    assertEquals("かー", compose("k", "a", "-"));
  }

  @Test
  public void romaji_yoon()
  {
    assertEquals("きゃ", compose("k", "y", "a"));
    assertEquals("しゃ", compose("s", "h", "a"));
    assertEquals("ちゃ", compose("c", "h", "a"));
    assertEquals("じゃ", compose("j", "a"));
    assertEquals("にゅ", compose("n", "y", "u"));
  }

  @Test
  public void romaji_sokuon()
  {
    assertEquals("っこ", compose("k", "k", "o"));
    assertEquals("った", compose("t", "t", "a"));
    assertEquals("っぱ", compose("p", "p", "a"));
    assertEquals("っさ", compose("s", "s", "a"));
    // The second consonant is still pending
    assertEquals("っk", compose("k", "k"));
  }

  @Test
  public void pending_letters_are_visible()
  {
    assertTrue(_kana.type("k"));
    assertEquals("k", _kana.text());
    assertTrue(_kana.type("a"));
    assertEquals("か", _kana.text());
    _kana.reset();
    assertTrue(_kana.type("s"));
    assertTrue(_kana.type("h"));
    assertEquals("sh", _kana.text());
    assertTrue(_kana.type("i"));
    assertEquals("し", _kana.text());
  }

  @Test
  public void romaji_n_is_ambiguous()
  {
    // A lone "n" is ん, but stays revisable as "na" is な and not んあ
    assertTrue(_kana.type("n"));
    assertEquals("ん", _kana.text());
    assertTrue(_kana.type("a"));
    assertEquals("な", _kana.text());
    assertEquals("に", compose("n", "i"));
    assertEquals("ん", compose("n", "n"));
    assertEquals("にゃ", compose("n", "y", "a"));
    // A trailing "n" is ん, it is not committed as a latin letter
    assertEquals("にほん", compose("nihon"));
    assertEquals("しんぶん", compose("shinbun"));
    // The "n" that cannot be continued is ん
    assertEquals("んk", compose("n", "k"));
    assertEquals("んか", compose("n", "k", "a"));
    assertEquals("ん", compose("n", "'"));
    // A kana key also ends the romaji sequence
    assertEquals("んあ", compose("n", "あ"));
    assertEquals("んー", compose("n", "-"));
  }

  @Test
  public void backspace_erases_the_pending_romaji()
  {
    assertTrue(_kana.type("s"));
    assertTrue(_kana.type("h"));
    assertEquals("sh", _kana.text());
    assertTrue(_kana.backspace());
    assertEquals("s", _kana.text());
    assertTrue(_kana.backspace());
    assertEquals("", _kana.text());
    assertFalse(_kana.backspace());
  }

  @Test
  public void backspace_erases_a_kana()
  {
    assertTrue(_kana.type("か"));
    assertTrue(_kana.backspace());
    assertEquals("", _kana.text());
    assertFalse(_kana.backspace());
    assertTrue(_kana.type("k"));
    assertTrue(_kana.type("a"));
    assertEquals("か", _kana.text());
    assertTrue(_kana.backspace());
    assertEquals("", _kana.text());
    // The ん of a lone "n" is erased like any other kana
    assertTrue(_kana.type("n"));
    assertEquals("ん", _kana.text());
    assertTrue(_kana.backspace());
    assertEquals("", _kana.text());
  }

  @Test
  public void other_keys_are_refused()
  {
    assertFalse(_kana.type("1"));
    assertFalse(_kana.type("。"));
    assertFalse(_kana.type(""));
    assertTrue(_kana.type("か"));
    assertFalse(_kana.type("1"));
    assertFalse(_kana.type("か1"));
    assertEquals("か", _kana.text());
    // A refused key does not resolve the pending romaji either
    assertTrue(_kana.type("k"));
    assertFalse(_kana.type("1"));
    assertFalse(_kana.type("。"));
    assertEquals("かk", _kana.text());
  }

  @Test
  public void mixing_kana_and_romaji()
  {
    assertEquals("かか", compose("か", "k", "a"));
    assertEquals("かん", compose("か", "n", "n"));
    assertEquals("あいう", compose("a", "i", "u"));
  }

  @Test
  public void candidates_and_reset()
  {
    assertTrue(_kana.candidates().isEmpty());
    assertTrue(_kana.type("か"));
    assertTrue(_kana.type("k"));
    _kana.reset();
    assertEquals("", _kana.text());
    assertTrue(_kana.candidates().isEmpty());
    assertFalse(_kana.backspace());
  }

  @Test
  public void text_is_never_null()
  {
    assertEquals("", _kana.text());
    List<String> c = _kana.candidates();
    assertNotNull(c);
  }
}
