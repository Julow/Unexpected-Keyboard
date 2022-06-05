package juloo.keyboard2;

import android.view.KeyEvent;

class KeyEventHandler implements Config.IKeyEventHandler
{
  private IReceiver _recv;

  public KeyEventHandler(IReceiver recv)
  {
    _recv = recv;
  }

  public void handleKeyUp(KeyValue key, Pointers.Modifiers mods)
  {
    if (key == null)
      return;
    int event;
    switch (key.getKind())
    {
      case Char:
        // Send an event if some modifiers are active.
        event = key.getCharEvent();
        if (shouldSendEvents(mods) && event != KeyValue.EVENT_NONE)
          handleKeyUpWithModifier(event, mods);
        _recv.commitChar(key.getChar());
        break;
      case String:
        _recv.commitText(key.getString());
        break;
      case Event:
        event = key.getEvent();
        switch (event)
        {
          case KeyValue.EVENT_CONFIG: _recv.showKeyboardConfig(); break;
          case KeyValue.EVENT_SWITCH_TEXT: _recv.switchMain(); break;
          case KeyValue.EVENT_SWITCH_NUMERIC: _recv.switchNumeric(); break;
          case KeyValue.EVENT_SWITCH_EMOJI: _recv.setPane_emoji(); break;
          case KeyValue.EVENT_SWITCH_BACK_EMOJI: _recv.setPane_normal(); break;
          case KeyValue.EVENT_CHANGE_METHOD: _recv.switchToNextInputMethod(); break;
          case KeyValue.EVENT_ACTION: _recv.performAction(); break;
          case KeyValue.EVENT_SWITCH_PROGRAMMING: _recv.switchProgramming(); break;
          default:
            handleKeyUpWithModifier(event, mods);
            break;
        }
        break;
      case Modifier:
        break;
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

  private int sendMetaKeyForModifier(int mod, int metaState, boolean down)
  {
    switch (mod)
    {
      case KeyValue.MOD_CTRL:
        return sendMetaKey(KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.META_CTRL_LEFT_ON | KeyEvent.META_CTRL_ON, metaState, down);
      case KeyValue.MOD_ALT:
        return sendMetaKey(KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.META_ALT_LEFT_ON | KeyEvent.META_ALT_ON, metaState, down);
      case KeyValue.MOD_SHIFT:
        return sendMetaKey(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.META_SHIFT_LEFT_ON | KeyEvent.META_SHIFT_ON, metaState, down);
      case KeyValue.MOD_META:
        return sendMetaKey(KeyEvent.KEYCODE_META_LEFT, KeyEvent.META_META_LEFT_ON | KeyEvent.META_META_ON, metaState, down);
      default: return metaState;
    }
  }

  /*
   * Don't set KeyEvent.FLAG_SOFT_KEYBOARD.
   */
	private void handleKeyUpWithModifier(int keyCode, Pointers.Modifiers mods)
	{
		if (keyCode == KeyValue.EVENT_NONE)
			return ;
    int metaState = 0;
    for (int i = 0; i < mods.size(); i++)
      metaState = sendMetaKeyForModifier(mods.get(i), metaState, true);
		_recv.sendKeyEvent(KeyEvent.ACTION_DOWN, keyCode, metaState);
    _recv.sendKeyEvent(KeyEvent.ACTION_UP, keyCode, metaState);
    for (int i = mods.size() - 1; i >= 0; i--)
      metaState = sendMetaKeyForModifier(mods.get(i), metaState, false);
	}

  /** Whether to send up and down events (true) or commit the text (false). */
  private boolean shouldSendEvents(Pointers.Modifiers mods)
  {
    // Check for modifiers
    for (int i = 0; i < mods.size(); i++)
    {
      switch (mods.get(i))
      {
        case KeyValue.MOD_CTRL:
        case KeyValue.MOD_ALT:
        case KeyValue.MOD_META: return true;
        default: break;
      }
    }
    return false;
  }

  public static interface IReceiver
  {
    public void switchToNextInputMethod();
    public void setPane_emoji();
    public void setPane_normal();
    public void showKeyboardConfig();
    public void performAction();

    public void switchMain();
    public void switchNumeric();
    public void switchProgramming();

    public void sendKeyEvent(int eventAction, int eventCode, int meta);

    public void commitText(String text);
    public void commitChar(char c);
  }
}
