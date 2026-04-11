package juloo.keyboard2;

import android.os.Build.VERSION;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.SurroundingText;
import java.util.List;

/** Keep track of the word being typed. This also tracks whether the selection
    is empty. */
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
  /** The cursor position within the current word relative to the end of the
    word. Equal to [0] when the cursor is at the end of the word. */
  int _w_cursor;

  public CurrentlyTypedWord(Handler h, Callback cb)
  {
    _handler = h;
    _callback = cb;
  }

  public String get()
  {
    return _w.toString();
  }

  public boolean is_selection_not_empty()
  {
    return _has_selection;
  }

  /** The cursor position relative to the end of the word. */
  public int cursor_relative()
  {
    return _w_cursor;
  }

  public void started(Config conf, InputConnection ic)
  {
    _ic = ic;
    _enabled = true;
    EditorConfig e = conf.editor_config;
    _has_selection = e.initial_sel_start != e.initial_sel_end;
    _cursor = e.initial_sel_start;
    _w_cursor = 0;
    if (!_has_selection)
    {
      set_current_word(e.initial_text_before_cursor);
      _w_cursor = -append_chars(e.initial_text_after_cursor); 
    }
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
    _cursor = newSelStart;
    refresh_current_word();
  }

  public void event_sent(int code, int meta)
  {
    if (!_enabled)
      return;
    delayed_refresh();
  }

  void callback()
  {
    String w = _w.toString();
    Logs.debug("Current word: " + w);
    _callback.currently_typed_word(w);
  }

  /** Estimate the currently typed word after [chars] has been typed. */
  void type_chars(CharSequence s, int start, int end)
  {
    for (int i = start; i < end;)
    {
      int c = Character.codePointAt(s, i);
      if (Character.isLetter(c))
        _w.appendCodePoint(c);
      else
        _w.setLength(0);
      _cursor++;
      i += Character.charCount(c);
    }
  }

  void type_chars(CharSequence s)
  {
    type_chars(s, 0, s.length());
  }

  /** Append chars to the current word without moving the cursor. Return the
      number of characters that were added in the current word. */
  int append_chars(CharSequence s, int start, int end)
  {
    int i = start;
    while (i < end)
    {
      int c = Character.codePointAt(s, i);
      if (!Character.isLetter(c))
        break;
      _w.appendCodePoint(c);
      i += Character.charCount(c);
    }
    return i - start;
  }

  int append_chars(CharSequence s)
  {
    return append_chars(s, 0, s.length());
  }

  /** Refresh the current word by immediately querying the editor. */
  void refresh_current_word()
  {
    Logs.debug("Refresh current word");
    _refresh_pending = false;
    _w_cursor = 0;
    if (_has_selection)
      set_current_word("");
    else if (VERSION.SDK_INT >= 31)
      set_current_word(_ic.getSurroundingText(20, 20, 0));
    else
      set_current_word(_ic.getTextBeforeCursor(20, 0));
  }

  /** Refresh the current word by immediately querying the editor. */
  void set_current_word(CharSequence text_before_cursor)
  {
    _w.setLength(0);
    if (text_before_cursor == null)
      return;
    int saved_cursor = _cursor;
    type_chars(text_before_cursor.toString());
    _cursor = saved_cursor;
    callback();
  }

  /** Like above but take the text after the cursor into account. */
  void set_current_word(SurroundingText st)
  {
    _w.setLength(0);
    if (st == null)
      return;
    int saved_cursor = _cursor;
    int st_sel = st.getSelectionStart();
    CharSequence st_text = st.getText();
    type_chars(st_text, 0, st_sel);
    _w_cursor = -append_chars(st_text, st_sel, st_text.length());
    _cursor = saved_cursor;
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
