package juloo.keyboard2;

import android.os.Looper;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

class KeyEventHandler implements Config.IKeyEventHandler
{
  IReceiver _recv;
  Autocapitalisation _autocap;

  public int actionId; // Action performed by the Action key.

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

  /** A key has been released. */
  public void key_up(KeyValue key, Pointers.Modifiers mods)
  {
    if (key == null)
      return;
    switch (key.getKind())
    {
      case Char: send_text(String.valueOf(key.getChar())); break;
      case String: send_text(key.getString()); break;
      case Event:
        switch (key.getEvent())
        {
          case CONFIG: _recv.showKeyboardConfig(); break;
          case SWITCH_TEXT: _recv.set_layout(Layout.Current); break;
          case SWITCH_NUMERIC: _recv.set_layout(Layout.Numeric); break;
          case SWITCH_EMOJI: _recv.setPane_emoji(); break;
          case SWITCH_BACK_EMOJI: _recv.setPane_normal(); break;
          case SEARCH_TEXT_INPUT_EMOJI: _recv.setPane_emoji_search(); break;
          case CHANGE_METHOD: _recv.switchInputMethod(); break;
          case CHANGE_METHOD_PREV: _recv.switchToPrevInputMethod(); break;
          case ACTION:
            InputConnection conn = _recv.getCurrentInputConnection();
            if (conn != null)
              conn.performEditorAction(actionId);
            break;
          case SWITCH_SECOND: _recv.set_layout(Layout.Secondary); break;
          case SWITCH_SECOND_BACK: _recv.set_layout(Layout.Primary); break;
          case SWITCH_GREEKMATH: _recv.set_layout(Layout.Greekmath); break;
          case CAPS_LOCK: _recv.set_shift_state(true, true); break;
        }
        break;
      case Keyevent:
        handleKeyUpWithModifier(key.getKeyevent(), mods);
        break;
      case Modifier:
        break;
      case Editing:
        send_context_menu_action(action_of_editing_key(key.getEditing()));
        break;
    }
  }

  static int action_of_editing_key(KeyValue.Editing e)
  {
    switch (e)
    {
      case COPY: return android.R.id.copy;
      case PASTE: return android.R.id.paste;
      case CUT: return android.R.id.cut;
      case SELECT_ALL: return android.R.id.selectAll;
      case SHARE: return android.R.id.shareText;
      case PASTE_PLAIN: return android.R.id.pasteAsPlainText;
      case UNDO: return android.R.id.undo;
      case REDO: return android.R.id.redo;
      case REPLACE: return android.R.id.replaceText;
      case ASSIST: return android.R.id.textAssist;
      case AUTOFILL: return android.R.id.autofill;
      default: return -1; // sad
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
  void handleKeyUpWithModifier(int keyCode, Pointers.Modifiers mods)
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

  public enum Layout
  {
    Current, // The primary or secondary layout
    Primary,
    Secondary,
    Numeric,
    Greekmath
  }

  public static interface IReceiver
  {
    public void switchInputMethod();
    public void switchToPrevInputMethod();
    public void setPane_emoji();
    public void setPane_emoji_search();
    public void setPane_normal();
    public void showKeyboardConfig();
    public void set_layout(Layout l);
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
