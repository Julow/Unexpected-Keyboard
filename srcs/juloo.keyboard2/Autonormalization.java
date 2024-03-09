package juloo.keyboard2;

import android.view.inputmethod.EditorInfo;
import android.icu.text.Normalizer2;
import android.view.inputmethod.InputConnection;

/** Compose the characters specified in [should_normalize_char()] using unicode
    normalization. The composing region is used to mark text where
    normalization could happen if more characters are typed. */
public final class Autonormalization
{
  /** Composing text waiting for more combinations. */
  StringBuilder _c = new StringBuilder();
  InputConnection _ic = null;
  Normalizer2 _normalizer_cached = null;

  public Autonormalization() {}

  public void started(EditorInfo info, InputConnection ic)
  {
    _ic = ic;
    _c.setLength(0);
  }

  /** If the characters could combine with a following character, update the
      composing text and return [true]. If the typed chars can't combine, flush
      the composing text and return [false], without committing the text. */
  public boolean typed(CharSequence s)
  {
    if (_ic == null || s.length() == 0)
      return false;
    if (should_normalize_char(s.charAt(0)))
    {
      type_normalized(s);
      return true;
    }
    flush();
    return false;
  }

  /** Handle key events, might flush the composing text. Do not call [typed]. */
  public void key_up(KeyValue kv)
  {
    switch (kv.getKind())
    {
      // [typed] will be called later
      case Char:
      case String: break;
      // Do not change the composing text for these keys
      case Modifier:
      case Compose_pending: break;
      // The other keys flush the composing text
      case Event:
      case Keyevent:
      case Editing:
        flush();
        break;
    }
  }

  /** If [typed()] has been called before, [flush()] must be called before
      sending any other command to the [InputConnection]. */
  void flush()
  {
    if (_ic == null)
      return;
    _c.setLength(0);
    _ic.finishComposingText();
  }

  Normalizer2 normalizer()
  {
    if (_normalizer_cached == null)
      _normalizer_cached = Normalizer2.getNFKCInstance();
    return _normalizer_cached;
  }

  void type_normalized(CharSequence s)
  {
    Normalizer2 norm = normalizer();
    norm.normalizeSecondAndAppend(_c, s);
    /* Only keep the string of normalizable character at the end of [_c].
       Commit the rest. */
    int i = _c.length() - 1;
    while (i > 0 && should_normalize_char(_c.charAt(i)))
      i--;
    if (i > 0)
    {
      _ic.commitText(_c.subSequence(0, i), 1);
      _c.delete(0, i);
    }
    _ic.setComposingText(_c, 1);
  }

  /** Characters for which autonormalization will happen. */
  boolean should_normalize_char(char c)
  {
    return (c >= '\u1100' && c <= '\u11FF') // Hangul Jamo
      || (c >= '\u3130' && c <= '\u318F') // Hangul Compatibility Jamo
      || (c >= '\uA960' && c <= '\uA97F') // Hangul Jamo Extended-A
      || (c >= '\uD7B0' && c <= '\uD7FF') // Hangul Jamo Extended-B
      ;
  }
}
