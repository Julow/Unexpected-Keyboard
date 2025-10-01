package juloo.keyboard2;

import android.os.Handler;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import java.util.List;

/** Keep track of the word being typed. */
public final class CurrentlyTypedWord
{
  InputConnection _ic = null;
  Handler _handler;
  Callback _callback;

  /** The currently typed word. */
  StringBuilder _w = new StringBuilder();
  /** This can be disabled if the editor doesn't support looking at the text
      before the cursor. */
  boolean _enabled = false;
  /** The current word is empty while the selection is ongoing. */
  boolean _has_selection = false;
  /** Used to avoid concurrent refreshes in [delayed_refresh()]. */
  boolean _refresh_pending = false;

  /** The estimated cursor position. Used to avoid expensive IPC calls when the
      typed word can be estimated locally with [typed]. When the cursor
      position gets out of sync, the text before the cursor is queried again to
      the editor. */
  int _cursor;

  public CurrentlyTypedWord(Handler h, Callback cb)
  {
    _handler = h;
    _callback = cb;
  }

  public String get()
  {
    return _w.toString();
  }

  public void started(EditorInfo info, InputConnection ic)
  {
    _ic = ic;
    _enabled = true;
    _has_selection = info.initialSelStart != info.initialSelEnd;
    _cursor = info.initialSelStart;
    if (!_has_selection)
      set_current_word(info.getInitialTextBeforeCursor(10, 0));
  }

  public void typed(String s)
  {
    if (!_enabled)
      return;
    _has_selection = false;
    type_chars(s);
    callback();
  }

  public void selection_updated(int oldSelStart, int newSelStart, int newSelEnd)
  {
    // Avoid the expensive [refresh_current_word] call when [typed] was called
    // before.
    boolean new_has_sel = newSelStart != newSelEnd;
    if (!_enabled || (newSelStart == _cursor && new_has_sel == _has_selection))
      return;
    _has_selection = new_has_sel;
    refresh_current_word();
    _cursor = newSelStart;
  }

  public void event_sent(int code, int meta)
  {
    if (!_enabled)
      return;
    delayed_refresh();
  }

  void callback()
  {
    _callback.currently_typed_word(_w.toString());
  }

  /** Estimate the currently typed word after [chars] has been typed. */
  void type_chars(String s)
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

  /** Refresh the current word by immediately querying the editor. */
  void refresh_current_word()
  {
    _refresh_pending = false;
    if (_has_selection)
      set_current_word("");
    else
      set_current_word(_ic.getTextBeforeCursor(10, 0));
  }

  /** Refresh the current word by immediately querying the editor. */
  void set_current_word(CharSequence text_before_cursor)
  {
    if (text_before_cursor == null)
    {
      _enabled = false;
      return;
    }
    _w.setLength(0);
    type_chars(text_before_cursor.toString());
    callback();
  }

  /** Wait some time to let the editor finishes reacting to changes and call
      [refresh_current_word]. */
  void delayed_refresh()
  {
    _refresh_pending = true;
    _handler.postDelayed(delayed_refresh_run, 50);
  }

  Runnable delayed_refresh_run = new Runnable()
  {
    public void run()
    {
      if (_refresh_pending)
        refresh_current_word();
    }
  };

  public static interface Callback
  {
    public void currently_typed_word(String word);
  }
}
