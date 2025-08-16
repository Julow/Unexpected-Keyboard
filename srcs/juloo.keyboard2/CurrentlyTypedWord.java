package juloo.keyboard2;

import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import java.util.List;

/** Keep track of the word being typed. */
public final class CurrentlyTypedWord
{
  InputConnection _ic = null;
  Callback _callback;

  StringBuilder _w = new StringBuilder();
  boolean _enabled = false;

  /** The estimated cursor position. Used to avoid expensive IPC calls when the
      typed word can be estimated locally with [typed]. When the cursor
      position gets out of sync, the text before the cursor is queried again to
      the editor. */
  int _cursor;

  int refresh_count = 0;

  public CurrentlyTypedWord(Callback cb)
  {
    _callback = cb;
  }

  public void started(EditorInfo info, InputConnection ic)
  {
    _ic = ic;
    refresh_current_word(info.getInitialTextBeforeCursor(10, 0),
        info.initialSelStart != info.initialSelEnd);
    _cursor = info.initialSelStart;
  }

  public void typed(String s)
  {
    if (!_enabled)
      return;
    type_chars(s);
    callback();
  }

  public void selection_updated(int oldSelStart, int newSelStart, int newSelEnd)
  {
    // Avoid the expensive [refresh_current_word] call when [typed] was called
    // before.
    if (!_enabled || newSelStart == _cursor)
      return;
    refresh_current_word(_ic.getTextBeforeCursor(10, 0),
        newSelStart != newSelEnd);
    _cursor = newSelStart;
  }

  private void callback()
  {
    _callback.currently_typed_word(_w.toString() + refresh_count);
  }

  /** Estimate the currently typed word after [chars] has been typed. */
  private void type_chars(String s)
  {
    int len = s.length();
    for (int i = 0; i < len;)
    {
      int c = s.codePointAt(i);
      if (Character.isLetter(c))
        _w.appendCodePoint(c);
      else
        _w.setLength(0);
      _cursor++;
      i += Character.charCount(c);
    }
  }

  /** Set [_enabled]. */
  private void refresh_current_word(CharSequence text_before_cursor, boolean has_selection)
  {
    _w.setLength(0);
    if (_ic == null || text_before_cursor == null)
    {
      _enabled = false;
      return;
    }
    _enabled = true;
    if (has_selection)
      return;
    refresh_count++;
    type_chars(text_before_cursor.toString());
    callback();
  }

  public static interface Callback
  {
    public void currently_typed_word(String word);
  }
}
