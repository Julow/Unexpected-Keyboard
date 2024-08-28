package juloo.keyboard2;

import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.KeyEvent;

public final class Autocapitalisation
{
  boolean _enabled = false;
  boolean _should_enable_shift = false;
  boolean _should_disable_shift = false;
  boolean _should_update_caps_mode = false;

  Handler _handler;
  InputConnection _ic;
  Callback _callback;
  int _caps_mode;

  /** Keep track of the cursor to recognize cursor movements from typing. */
  int _cursor;

  static int SUPPORTED_CAPS_MODES =
    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES |
    InputType.TYPE_TEXT_FLAG_CAP_WORDS;

  public Autocapitalisation(Looper looper, Callback cb)
  {
    _handler = new Handler(looper);
    _callback = cb;
  }

  /**
   * The events are: started, typed, event sent, selection updated
   * [started] does initialisation work and must be called before any other
   * event.
   */
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
    _should_update_caps_mode = started_should_update_state(info.inputType);
    callback_now(true);
  }

  public void typed(CharSequence c)
  {
    for (int i = 0; i < c.length(); i++)
      type_one_char(c.charAt(i));
    callback(false);
  }

  public void event_sent(int code, int meta)
  {
    if (meta != 0)
    {
      _should_enable_shift = false;
      _should_update_caps_mode = false;
      return;
    }
    switch (code)
    {
      case KeyEvent.KEYCODE_DEL:
        if (_cursor > 0) _cursor--;
        _should_update_caps_mode = true;
        break;
    }
    callback(true);
  }

  public void stop()
  {
    _should_enable_shift = false;
    _should_update_caps_mode = false;
    callback_now(true);
  }

  public static interface Callback
  {
    public void update_shift_state(boolean should_enable, boolean should_disable);
  }

  /** Returns [true] if shift might be disabled. */
  public void selection_updated(int old_cursor, int new_cursor)
  {
    if (new_cursor == _cursor) // Just typing
      return;
    if (new_cursor == 0 && _ic != null)
    {
      // Detect whether the input box has been cleared
      CharSequence t = _ic.getTextAfterCursor(1, 0);
      if (t != null && t.equals(""))
        _should_update_caps_mode = true;
    }
    _cursor = new_cursor;
    _should_enable_shift = false;
    callback(true);
  }

  Runnable delayed_callback = new Runnable()
  {
    public void run()
    {
      if (_should_update_caps_mode && _ic != null)
      {
        _should_enable_shift = _enabled && (_ic.getCursorCapsMode(_caps_mode) != 0);
        _should_update_caps_mode = false;
      }
      _callback.update_shift_state(_should_enable_shift, _should_disable_shift);
    }
  };

  /** Update the shift state if [_should_update_caps_mode] is true, then call
      [_callback.update_shift_state]. This is done after a short delay to wait
      for the editor to handle the events, as this might be called before the
      corresponding event is sent. */
  void callback(boolean might_disable)
  {
    _should_disable_shift = might_disable;
    // The callback must be delayed because [getCursorCapsMode] would sometimes
    // be called before the editor finished handling the previous event.
    _handler.postDelayed(delayed_callback, 1);
  }

  /** Like [callback] but runs immediately. */
  void callback_now(boolean might_disable)
  {
    _should_disable_shift = might_disable;
    delayed_callback.run();
  }

  void type_one_char(char c)
  {
    _cursor++;
    if (is_whitespace(c))
      _should_update_caps_mode = true;
    else
      _should_enable_shift = false;
  }

  boolean is_whitespace(char c)
  {
    switch (c)
    {
      case ' ':
      case KEYCODE_ENTER:
      case KEYCODE_TAB:
        return true;
      default:
        return false;
    }
  }

  /** Whether the caps state should be updated when input starts. [inputType]
      is the field from the editor info object. */
  boolean started_should_update_state(int inputType)
  {
    int class_ = inputType & InputType.TYPE_MASK_CLASS;
    int variation = inputType & InputType.TYPE_MASK_VARIATION;
    if (class_ != InputType.TYPE_CLASS_TEXT)
      return false;
    switch (variation)
    {
      case InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE:
      case InputType.TYPE_TEXT_VARIATION_NORMAL:
      case InputType.TYPE_TEXT_VARIATION_PERSON_NAME:
      case InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE:
      case InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT:
      case InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT:
        return true;
      default:
        return false;
    }
  }
}
