package juloo.keyboard2;

import android.text.InputType;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.KeyEvent;

final class Autocapitalisation
{
  private boolean _enabled = false;
  private boolean _beginning_of_sentence = false;
  /** Used to avoid enabling shift after an arrow key is pressed. */
  private boolean _skip_next_selection_update = false;
  /** Keep track of the cursor to differentiate 'selection_updated' events
      corresponding to typing from cursor movement. */
  private int _cursor = 0;

  public boolean should_enable_shift()
  {
    return _enabled && _beginning_of_sentence;
  }

  /** Returns [true] if shift should be on initially. The input connection
      isn't stored. */
  public void started(EditorInfo info, InputConnection ic)
  {
    if ((info.inputType & InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) == 0)
    {
      _enabled = false;
      return;
    }
    _enabled = true;
    _beginning_of_sentence = ((info.initialCapsMode & TextUtils.CAP_MODE_SENTENCES) != 0);
    _cursor = 0; // Just a guess
    scan_text_before_cursor(10, ic);
  }

  public void typed(CharSequence c)
  {
    for (int i = 0; i < c.length(); i++)
      typed(c.charAt(i));
  }

  public void typed(char c)
  {
    _cursor++;
    if (is_beginning_of_sentence(c))
      _beginning_of_sentence = true;
    else if (!ignore_at_beginning_of_sentence(c))
      _beginning_of_sentence = false;
  }

  public void event_sent(int code)
  {
    switch (code)
    {
      // Disable temporarily after a keyboard cursor movement
      case KeyEvent.KEYCODE_DPAD_UP:
      case KeyEvent.KEYCODE_DPAD_RIGHT:
      case KeyEvent.KEYCODE_DPAD_DOWN:
      case KeyEvent.KEYCODE_DPAD_LEFT:
      case KeyEvent.KEYCODE_PAGE_UP:
      case KeyEvent.KEYCODE_PAGE_DOWN:
      case KeyEvent.KEYCODE_MOVE_HOME:
      case KeyEvent.KEYCODE_MOVE_END:
        _skip_next_selection_update = true;
        _beginning_of_sentence = false;
        break;
    }
  }

  /** Returns [true] if shift might be disabled. */
  public boolean selection_updated(int old_cursor, int new_cursor, InputConnection ic)
  {
    if (_skip_next_selection_update)
    {
      _cursor = new_cursor;
      _skip_next_selection_update = false;
      return false;
    }
    if (new_cursor == _cursor)
      return false;
    // Text has been inserted or cursor moved forward
    if (old_cursor == _cursor && new_cursor > old_cursor)
    {
      scan_text_before_cursor(Math.min(new_cursor - old_cursor, 10), ic);
      return true;
    }
    else
    {
      // Cursor has moved backward or text deleted
      _beginning_of_sentence = false;
      scan_text_before_cursor(10, ic);
      _cursor = new_cursor;
      return true;
    }
  }

  /** Updates [_cursor]. */
  private void scan_text_before_cursor(int range, InputConnection ic)
  {
    if (!_enabled) // Don't query characters if disabled
      return;
    CharSequence text_before = ic.getTextBeforeCursor(range, 0);
    if (text_before == null)
    {
      _beginning_of_sentence = false;
    }
    else
    {
      _beginning_of_sentence = true;
      typed(text_before);
    }
  }

  private boolean ignore_at_beginning_of_sentence(char c)
  {
    switch (c)
    {
      case ' ':
      case '"':
      case '\'':
      case '(':
      case '«':
        return true;
      default:
        return false;
    }
  }

  private boolean is_beginning_of_sentence(char c)
  {
    switch (c)
    {
      case '.':
      case ';':
      case '\n':
      case '!':
      case '?':
      case '¿':
      case '¡':
        return true;
      default:
        return false;
    }
  }
}
