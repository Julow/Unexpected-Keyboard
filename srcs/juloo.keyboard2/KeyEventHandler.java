package juloo.keyboard2;

import android.annotation.SuppressLint;
import android.os.Looper;
import android.os.Handler;
import android.text.InputType;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import java.util.Iterator;

public final class KeyEventHandler
  implements Config.IKeyEventHandler,
             ClipboardHistoryService.ClipboardPasteCallback
{
  IReceiver _recv;
  Autocapitalisation _autocap;
  /** State of the system modifiers. It is updated whether a modifier is down
      or up and a corresponding key event is sent. */
  Pointers.Modifiers _mods;
  /** Consistent with [_mods]. This is a mutable state rather than computed
      from [_mods] to ensure that the meta state is correct while up and down
      events are sent for the modifier keys. */
  int _meta_state = 0;
  /** Whether to force sending arrow keys to move the cursor when
      [setSelection] could be used instead. */
  boolean _move_cursor_force_fallback = false;

  public KeyEventHandler(IReceiver recv)
  {
    _recv = recv;
    _autocap = new Autocapitalisation(recv.getHandler(),
        this.new Autocapitalisation_callback());
    _mods = Pointers.Modifiers.EMPTY;
  }

  /** Editing just started. */
  public void started(EditorInfo info)
  {
    _autocap.started(info, _recv.getCurrentInputConnection());
    // Workaround a bug in Acode, which answers to [getExtractedText] but do
    // not react to [setSelection] while returning [true].
    // Note: Using & to workaround a bug in Acode, which sets several
    // variations at once.
    _move_cursor_force_fallback = (info.inputType & InputType.TYPE_MASK_VARIATION &
      InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0;
  }

  /** Selection has been updated. */
  public void selection_updated(int oldSelStart, int newSelStart)
  {
    _autocap.selection_updated(oldSelStart, newSelStart);
  }

  /** A key is being pressed. There will not necessarily be a corresponding
      [key_up] event. */
  @Override
  public void key_down(KeyValue key, boolean isSwipe)
  {
    if (key == null)
      return;
    // Stop auto capitalisation when pressing some keys
    switch (key.getKind())
    {
      case Modifier:
        switch (key.getModifier())
        {
          case CTRL:
          case ALT:
          case META:
            _autocap.stop();
            break;
        }
        break;
      case Compose_pending:
        _autocap.stop();
        break;
      default: break;
    }
  }

  /** A key has been released. */
  @Override
  public void key_up(KeyValue key, Pointers.Modifiers mods)
  {
    if (key == null)
      return;
    Pointers.Modifiers old_mods = _mods;
    update_meta_state(mods);
    switch (key.getKind())
    {
      case Char: send_text(String.valueOf(key.getChar())); break;
      case String: send_text(key.getString()); break;
      case Event: _recv.handle_event_key(key.getEvent()); break;
      case Keyevent: send_key_down_up(key.getKeyevent()); break;
      case Modifier: break;
      case Editing: handle_editing_key(key.getEditing()); break;
      case Compose_pending: _recv.set_compose_pending(true); break;
      case Slider: handle_slider(key.getSlider(), key.getSliderRepeat()); break;
      case Macro: evaluate_macro(key.getMacro()); break;
    }
    update_meta_state(old_mods);
  }

  @Override
  public void mods_changed(Pointers.Modifiers mods)
  {
    update_meta_state(mods);
  }

  @Override
  public void paste_from_clipboard_pane(String content)
  {
    send_text(content);
  }

  /** Update [_mods] to be consistent with the [mods], sending key events if
      needed. */
  void update_meta_state(Pointers.Modifiers mods)
  {
    // Released modifiers
    Iterator<KeyValue> it = _mods.diff(mods);
    while (it.hasNext())
      sendMetaKeyForModifier(it.next(), false);
    // Activated modifiers
    it = mods.diff(_mods);
    while (it.hasNext())
      sendMetaKeyForModifier(it.next(), true);
    _mods = mods;
  }

  // private void handleDelKey(int before, int after)
  // {
  //  CharSequence selection = getCurrentInputConnection().getSelectedText(0);

  //  if (selection != null && selection.length() > 0)
  //  getCurrentInputConnection().commitText("", 1);
  //  else
  //  getCurrentInputConnection().deleteSurroundingText(before, after);
  // }

  void sendMetaKey(int eventCode, int meta_flags, boolean down)
  {
    if (down)
    {
      _meta_state = _meta_state | meta_flags;
      send_keyevent(KeyEvent.ACTION_DOWN, eventCode, _meta_state);
    }
    else
    {
      send_keyevent(KeyEvent.ACTION_UP, eventCode, _meta_state);
      _meta_state = _meta_state & ~meta_flags;
    }
  }

  void sendMetaKeyForModifier(KeyValue kv, boolean down)
  {
    switch (kv.getKind())
    {
      case Modifier:
        switch (kv.getModifier())
        {
          case CTRL:
            sendMetaKey(KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.META_CTRL_LEFT_ON | KeyEvent.META_CTRL_ON, down);
            break;
          case ALT:
            sendMetaKey(KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.META_ALT_LEFT_ON | KeyEvent.META_ALT_ON, down);
            break;
          case SHIFT:
            sendMetaKey(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.META_SHIFT_LEFT_ON | KeyEvent.META_SHIFT_ON, down);
            break;
          case META:
            sendMetaKey(KeyEvent.KEYCODE_META_LEFT, KeyEvent.META_META_LEFT_ON | KeyEvent.META_META_ON, down);
            break;
          default:
            break;
        }
        break;
    }
  }

  void send_key_down_up(int keyCode)
  {
    send_key_down_up(keyCode, _meta_state);
  }

  /** Ignores currently pressed system modifiers. */
  void send_key_down_up(int keyCode, int metaState)
  {
    send_keyevent(KeyEvent.ACTION_DOWN, keyCode, metaState);
    send_keyevent(KeyEvent.ACTION_UP, keyCode, metaState);
  }

  void send_keyevent(int eventAction, int eventCode, int metaState)
  {
    InputConnection conn = _recv.getCurrentInputConnection();
    if (conn == null)
      return;
    conn.sendKeyEvent(new KeyEvent(1, 1, eventAction, eventCode, 0,
          metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
          KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
    if (eventAction == KeyEvent.ACTION_UP)
      _autocap.event_sent(eventCode, metaState);
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

  @SuppressLint("InlinedApi")
  void handle_editing_key(KeyValue.Editing ev)
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
      case DELETE_WORD: send_key_down_up(KeyEvent.KEYCODE_DEL, KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON); break;
      case FORWARD_DELETE_WORD: send_key_down_up(KeyEvent.KEYCODE_FORWARD_DEL, KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON); break;
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

  /** [repeatition] might be negative, in which case the direction is reversed. */
  void handle_slider(KeyValue.Slider s, int repeatition)
  {
    switch (s)
    {
      case Cursor_left: move_cursor(-repeatition); break;
      case Cursor_right: move_cursor(repeatition); break;
      case Cursor_up: move_cursor_vertical(-repeatition); break;
      case Cursor_down: move_cursor_vertical(repeatition); break;
    }
  }

  /** Move the cursor right or left, if possible without sending key events.
      Unlike arrow keys, the selection is not removed even if shift is not on.
      Falls back to sending arrow keys events if the editor do not support
      moving the cursor or a modifier other than shift is pressed. */
  void move_cursor(int d)
  {
    InputConnection conn = _recv.getCurrentInputConnection();
    if (conn == null)
      return;
    ExtractedText et = get_cursor_pos(conn);
    int system_mods =
      KeyEvent.META_CTRL_ON | KeyEvent.META_ALT_ON | KeyEvent.META_META_ON;
    // Fallback to sending key events if system modifiers are activated or
    // ExtractedText is not supported, for example on Termux.
    if (!_move_cursor_force_fallback && et != null
        && (_meta_state & system_mods) == 0)
    {
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
        if ((_meta_state & KeyEvent.META_SHIFT_ON) == 0)
          sel_start = sel_end;
      }
      if (conn.setSelection(sel_start, sel_end))
        return; // [setSelection] succeeded, don't fallback to key events
    }
    if (d < 0)
      send_key_down_up_repeat(KeyEvent.KEYCODE_DPAD_LEFT, -d);
    else
      send_key_down_up_repeat(KeyEvent.KEYCODE_DPAD_RIGHT, d);
  }

  /** Move the cursor up and down. This sends UP and DOWN key events that might
      make the focus exit the text box. */
  void move_cursor_vertical(int d)
  {
    if (d < 0)
      send_key_down_up_repeat(KeyEvent.KEYCODE_DPAD_UP, -d);
    else
      send_key_down_up_repeat(KeyEvent.KEYCODE_DPAD_DOWN, d);
  }

  void evaluate_macro(KeyValue[] keys)
  {
    final Pointers.Modifiers empty = Pointers.Modifiers.EMPTY;
    // Ignore modifiers that are activated at the time the macro is evaluated
    mods_changed(empty);
    Pointers.Modifiers mods = empty;
    final boolean autocap_paused = _autocap.pause();
    for (KeyValue kv : keys)
    {
      kv = KeyModifier.modify(kv, mods);
      if (kv == null)
        continue;
      if (kv.hasFlagsAny(KeyValue.FLAG_LATCH))
      {
        // Non-special latchable keys clear latched modifiers
        if (!kv.hasFlagsAny(KeyValue.FLAG_SPECIAL))
          mods = empty;
        mods = mods.with_extra_mod(kv);
      }
      else
      {
        key_down(kv, false);
        key_up(kv, mods);
        mods = empty;
      }
    }
    _autocap.unpause(autocap_paused);
  }

  /** Repeat calls to [send_key_down_up]. */
  void send_key_down_up_repeat(int event_code, int repeat)
  {
    while (repeat-- > 0)
      send_key_down_up(event_code);
  }

  public static interface IReceiver
  {
    public void handle_event_key(KeyValue.Event ev);
    public void set_shift_state(boolean state, boolean lock);
    public void set_compose_pending(boolean pending);
    public InputConnection getCurrentInputConnection();
    public Handler getHandler();
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
