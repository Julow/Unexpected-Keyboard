package juloo.keyboard2;

import android.os.Looper;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;

class KeyEventHandler implements Config.IKeyEventHandler
{
  IReceiver _recv;
  Autocapitalisation _autocap;

  public KeyEventHandler(Looper looper, IReceiver recv)
  {
    _recv = recv;
    _autocap = new Autocapitalisation(looper,
        this.new Autocapitalisation_callback());
  }

  /** Editing just started. */
  public void started(EditorInfo info)
  {
    _autocap.started(info, _recv.getCurrentInputConnection());
  }

  /** Selection has been updated. */
  public void selection_updated(int oldSelStart, int newSelStart)
  {
    _autocap.selection_updated(oldSelStart, newSelStart);
  }

  /** A key is being pressed. There will not necessarily be a corresponding
      [key_up] event. */
  public void key_down(KeyValue key, boolean isSwipe)
  {
    if (key == null)
      return;
    switch (key.getKind())
    {
      case Modifier:
        // Stop auto capitalisation when activating a system modifier
        switch (key.getModifier())
        {
          case CTRL:
          case ALT:
          case META:
            _autocap.stop();
            break;
        }
        break;
      default: break;
    }
  }

  /** A key has been released. */
  public void key_up(KeyValue key, Pointers.Modifiers mods)
  {
    if (key == null)
      return;
    switch (key.getKind())
    {
      case Char: send_text(String.valueOf(key.getChar())); break;
      case String: send_text(key.getString()); break;
      case Event: _recv.handle_event_key(key.getEvent()); break;
      case Keyevent: send_key_down_up(key.getKeyevent(), mods); break;
      case Modifier: break;
      case Editing: handle_editing_key(key.getEditing(), mods); break;
    }
  }

  // private void handleDelKey(int before, int after)
  // {
  //  CharSequence selection = getCurrentInputConnection().getSelectedText(0);

  //  if (selection != null && selection.length() > 0)
  //  getCurrentInputConnection().commitText("", 1);
  //  else
  //  getCurrentInputConnection().deleteSurroundingText(before, after);
  // }

  int sendMetaKey(int eventCode, int metaFlags, int metaState, boolean down)
  {
    int action;
    int updatedMetaState;
    if (down) { action = KeyEvent.ACTION_DOWN; updatedMetaState = metaState | metaFlags; }
    else { action = KeyEvent.ACTION_UP; updatedMetaState = metaState & ~metaFlags; }
    send_keyevent(action, eventCode, metaState);
    return updatedMetaState;
  }

  int sendMetaKeyForModifier(KeyValue.Modifier mod, int metaState, boolean down)
  {
    switch (mod)
    {
      case CTRL:
        return sendMetaKey(KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.META_CTRL_LEFT_ON | KeyEvent.META_CTRL_ON, metaState, down);
      case ALT:
        return sendMetaKey(KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.META_ALT_LEFT_ON | KeyEvent.META_ALT_ON, metaState, down);
      case SHIFT:
        return sendMetaKey(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.META_SHIFT_LEFT_ON | KeyEvent.META_SHIFT_ON, metaState, down);
      case META:
        return sendMetaKey(KeyEvent.KEYCODE_META_LEFT, KeyEvent.META_META_LEFT_ON | KeyEvent.META_META_ON, metaState, down);
      default: return metaState;
    }
  }

  /*
   * Don't set KeyEvent.FLAG_SOFT_KEYBOARD.
   */
  void send_key_down_up(int keyCode, Pointers.Modifiers mods)
  {
    int metaState = 0;
    for (int i = 0; i < mods.size(); i++)
      metaState = sendMetaKeyForModifier(mods.get(i), metaState, true);
    send_keyevent(KeyEvent.ACTION_DOWN, keyCode, metaState);
    send_keyevent(KeyEvent.ACTION_UP, keyCode, metaState);
    for (int i = mods.size() - 1; i >= 0; i--)
      metaState = sendMetaKeyForModifier(mods.get(i), metaState, false);
  }

  void send_keyevent(int eventAction, int eventCode, int meta)
  {
    InputConnection conn = _recv.getCurrentInputConnection();
    if (conn == null)
      return;
    conn.sendKeyEvent(new KeyEvent(1, 1, eventAction, eventCode, 0, meta));
    if (eventAction == KeyEvent.ACTION_UP)
      _autocap.event_sent(eventCode, meta);
  }

  void send_text(CharSequence text)
  {
    InputConnection conn = _recv.getCurrentInputConnection();
    if (conn == null)
      return;
    conn.commitText(text, 1);
    _autocap.typed(text);
  }

  /** See {!InputConnection.performContextMenuAction}. */
  void send_context_menu_action(int id)
  {
    InputConnection conn = _recv.getCurrentInputConnection();
    if (conn == null)
      return;
    conn.performContextMenuAction(id);
  }

  void handle_editing_key(KeyValue.Editing ev, Pointers.Modifiers mods)
  {
    switch (ev)
    {
      case COPY: send_context_menu_action(android.R.id.copy); break;
      case PASTE: send_context_menu_action(android.R.id.paste); break;
      case CUT: send_context_menu_action(android.R.id.cut); break;
      case SELECT_ALL: send_context_menu_action(android.R.id.selectAll); break;
      case SHARE: send_context_menu_action(android.R.id.shareText); break;
      case PASTE_PLAIN: send_context_menu_action(android.R.id.pasteAsPlainText); break;
      case UNDO: send_context_menu_action(android.R.id.undo); break;
      case REDO: send_context_menu_action(android.R.id.redo); break;
      case REPLACE: send_context_menu_action(android.R.id.replaceText); break;
      case ASSIST: send_context_menu_action(android.R.id.textAssist); break;
      case AUTOFILL: send_context_menu_action(android.R.id.autofill); break;
      case CURSOR_LEFT: move_cursor(-1, mods); break;
      case CURSOR_RIGHT: move_cursor(1, mods); break;
    }
  }

  static ExtractedTextRequest _move_cursor_req = null;

  /** Query the cursor position. The extracted text is empty. Returns [null] if
      the editor doesn't support this operation. */
  ExtractedText get_cursor_pos(InputConnection conn)
  {
    if (_move_cursor_req == null)
    {
      _move_cursor_req = new ExtractedTextRequest();
      _move_cursor_req.hintMaxChars = 0;
    }
    return conn.getExtractedText(_move_cursor_req, 0);
  }

  /** Move the cursor right or left, if possible without sending key events.
      Unlike arrow keys, the selection is not removed even if shift is not on. */
  void move_cursor(int d, Pointers.Modifiers mods)
  {
    InputConnection conn = _recv.getCurrentInputConnection();
    if (conn == null)
      return;
    ExtractedText et = get_cursor_pos(conn);
    if (et == null) // Editor doesn't support moving the cursor
    {
      move_cursor_fallback(d, mods);
      return;
    }
    int sel_start = et.selectionStart;
    int sel_end = et.selectionEnd;
    // Continue expanding the selection even if shift is not pressed
    if (sel_end != sel_start)
    {
      sel_end += d;
      if (sel_end == sel_start) // Avoid making the selection empty
        sel_end += d;
    }
    else
    {
      sel_end += d;
      // Leave 'sel_start' where it is if shift is pressed
      if (!mods.has(KeyValue.Modifier.SHIFT))
        sel_start = sel_end;
    }
    conn.setSelection(sel_start, sel_end);
  }

  /** Send arrow keys as a fallback for editors that do not support
      [getExtractedText] like Termux. */
  void move_cursor_fallback(int d, Pointers.Modifiers mods)
  {
    while (d < 0)
    {
      send_key_down_up(KeyEvent.KEYCODE_DPAD_LEFT, mods);
      d++;
    }
    while (d > 0)
    {
      send_key_down_up(KeyEvent.KEYCODE_DPAD_RIGHT, mods);
      d--;
    }
  }

  public static interface IReceiver
  {
    public void handle_event_key(KeyValue.Event ev);
    public void set_shift_state(boolean state, boolean lock);
    public InputConnection getCurrentInputConnection();
  }

  class Autocapitalisation_callback implements Autocapitalisation.Callback
  {
    @Override
    public void update_shift_state(boolean should_enable, boolean should_disable)
    {
      if (should_enable)
        _recv.set_shift_state(true, false);
      else if (should_disable)
        _recv.set_shift_state(false, false);
    }
  }
}
