package juloo.keyboard2;

import android.view.KeyEvent;

class KeyEventHandler implements Config.IKeyEventHandler
{
  private IReceiver _recv;

  public KeyEventHandler(IReceiver recv)
  {
    _recv = recv;
  }

  public void handleKeyUp(KeyValue key, int flags)
  {
    if (key == null || (key.flags & KeyValue.FLAG_NOCHAR) != 0)
      return;
    switch (key.eventCode)
    {
    case KeyValue.EVENT_CONFIG: _recv.showKeyboardConfig(); return;
    case KeyValue.EVENT_SWITCH_TEXT: _recv.setLayout(-1); return;
    case KeyValue.EVENT_SWITCH_NUMERIC: _recv.setLayout(R.xml.numeric); return;
    case KeyValue.EVENT_SWITCH_EMOJI: _recv.setPane_emoji(); return;
    case KeyValue.EVENT_SWITCH_BACK_EMOJI: _recv.setPane_normal(); return;
    case KeyValue.EVENT_CHANGE_METHOD: _recv.switchToNextInputMethod(); return;
    case KeyValue.EVENT_ACTION: _recv.performAction(); return;
    default:
      if ((flags & (KeyValue.FLAG_CTRL | KeyValue.FLAG_ALT | KeyValue.FLAG_META)) != 0)
        handleKeyUpWithModifier(key, flags);
      else if (key.char_ != KeyValue.CHAR_NONE)
        _recv.commitChar(key.char_);
      else if (key.eventCode != KeyValue.EVENT_NONE)
        handleKeyUpWithModifier(key, flags);
      else
        _recv.commitText(key.symbol);
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

  private int sendMetaKey(int eventCode, int metaFlags, int metaState, boolean down)
  {
    int action;
    int updatedMetaState;
    if (down) { action = KeyEvent.ACTION_DOWN; updatedMetaState = metaState | metaFlags; }
    else { action = KeyEvent.ACTION_UP; updatedMetaState = metaState & ~metaFlags; }
    _recv.sendKeyEvent(action, eventCode, metaState);
    return updatedMetaState;
  }

  /* Send key events corresponding to pressed modifier keys. */
  private int sendMetaKeys(int flags, int metaState, boolean down)
  {
		if ((flags & KeyValue.FLAG_CTRL) != 0)
      metaState = sendMetaKey(KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.META_CTRL_LEFT_ON | KeyEvent.META_CTRL_ON, metaState, down);
		if ((flags & KeyValue.FLAG_ALT) != 0)
      metaState = sendMetaKey(KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.META_ALT_LEFT_ON | KeyEvent.META_ALT_ON, metaState, down);
		if ((flags & KeyValue.FLAG_SHIFT) != 0)
      metaState = sendMetaKey(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.META_SHIFT_LEFT_ON | KeyEvent.META_SHIFT_ON, metaState, down);
    if ((flags & KeyValue.FLAG_META) != 0)
      metaState = sendMetaKey(KeyEvent.KEYCODE_META_LEFT, KeyEvent.META_META_LEFT_ON | KeyEvent.META_META_ON, metaState, down);
    return metaState;
  }

  /*
   * Don't set KeyEvent.FLAG_SOFT_KEYBOARD.
   */
	private void handleKeyUpWithModifier(KeyValue key, int flags)
	{
		if (key.eventCode == KeyValue.EVENT_NONE)
			return ;
    int metaState = sendMetaKeys(flags, 0, true);
		_recv.sendKeyEvent(KeyEvent.ACTION_DOWN, key.eventCode, metaState);
    _recv.sendKeyEvent(KeyEvent.ACTION_UP, key.eventCode, metaState);
    sendMetaKeys(flags, metaState, false);
	}

  public static interface IReceiver
  {
    public void switchToNextInputMethod();
    public void setPane_emoji();
    public void setPane_normal();
    public void showKeyboardConfig();
    public void performAction();

    /** 'res_id' is '-1' for the currently selected layout. */
    public void setLayout(int res_id);

    public void sendKeyEvent(int eventAction, int eventCode, int meta);

    public void commitText(String text);
    public void commitChar(char c);
  }
}
