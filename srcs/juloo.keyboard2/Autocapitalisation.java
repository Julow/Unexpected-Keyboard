package juloo.keyboard2;

import android.text.InputType;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.KeyEvent;

final class Autocapitalisation
{
  private boolean _enabled = false;
  private boolean _should_enable_shift = false;

  private InputConnection _ic;
  private int _caps_mode;

  /** Keep track of the cursor to recognize cursor movements from typing. */
  private int _cursor;

  static private int SUPPORTED_CAPS_MODES =
    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES |
    InputType.TYPE_TEXT_FLAG_CAP_WORDS;

  public boolean should_enable_shift()
  {
    return _should_enable_shift;
  }

  /** Returns [true] if shift should be on initially. The input connection
      isn't stored. */
  public void started(EditorInfo info, InputConnection ic)
  {
    _ic = ic;
    _caps_mode = info.inputType & TextUtils.CAP_MODE_SENTENCES;
    if (!Config.globalConfig().autocapitalisation || _caps_mode == 0)
    {
      _enabled = false;
      return;
    }
    _enabled = true;
    _should_enable_shift = (info.initialCapsMode != 0);
  }

  public void typed(CharSequence c)
  {
    for (int i = 0; i < c.length(); i++)
      typed(c.charAt(i));
  }

  public void typed(char c)
  {
    _cursor++;
    if (is_trigger_character(c))
      update_caps_mode();
    else
      _should_enable_shift = false;
  }

  public void event_sent(int code)
  {
    switch (code)
    {
      case KeyEvent.KEYCODE_DEL:
        _cursor--;
        update_caps_mode();
        break;
    }
  }

  /** Returns [true] if shift might be disabled. */
  public boolean selection_updated(int old_cursor, int new_cursor)
  {
    if (new_cursor == _cursor) // Just typing
      return false;
    _cursor = new_cursor;
    _should_enable_shift = false;
    return true;
  }

  private void update_caps_mode()
  {
    _should_enable_shift = _enabled && (_ic.getCursorCapsMode(_caps_mode) != 0);
  }

  private boolean is_trigger_character(char c)
  {
    switch (c)
    {
      case ' ':
        return true;
      default:
        return false;
    }
  }
}
