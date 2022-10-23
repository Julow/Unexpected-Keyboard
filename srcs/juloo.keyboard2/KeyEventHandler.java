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
    switch (key.getKind())
    {
      case Char:
        _recv.commitChar(key.getChar());
        break;
      case String:
        _recv.commitText(key.getString());
        break;
      case Event:
        switch (key.getEvent())
        {
          case CONFIG: _recv.showKeyboardConfig(); break;
          case SWITCH_TEXT: _recv.switchMain(); break;
          case SWITCH_NUMERIC: _recv.switchNumeric(); break;
          case SWITCH_EMOJI: _recv.setPane_emoji(); break;
          case SWITCH_BACK_EMOJI: _recv.setPane_normal(); break;
          case CHANGE_METHOD: _recv.switchToNextInputMethod(); break;
          case ACTION: _recv.performAction(); break;
          case SWITCH_PROGRAMMING: _recv.switchProgramming(); break;
          case SWITCH_GREEKMATH: _recv.switchGreekmath(); break;
          case CAPS_LOCK: _recv.enableCapsLock(); break;
        }
        break;
      case Keyevent:
        handleKeyUpWithModifier(key.getKeyevent(), mods);
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

  private int sendMetaKeyForModifier(KeyValue.Modifier mod, int metaState, boolean down)
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
	private void handleKeyUpWithModifier(int keyCode, Pointers.Modifiers mods)
	{
    int metaState = 0;
    for (int i = 0; i < mods.size(); i++)
      metaState = sendMetaKeyForModifier(mods.get(i), metaState, true);
		_recv.sendKeyEvent(KeyEvent.ACTION_DOWN, keyCode, metaState);
    _recv.sendKeyEvent(KeyEvent.ACTION_UP, keyCode, metaState);
    for (int i = mods.size() - 1; i >= 0; i--)
      metaState = sendMetaKeyForModifier(mods.get(i), metaState, false);
	}

  public static interface IReceiver
  {
    public void switchToNextInputMethod();
    public void setPane_emoji();
    public void setPane_normal();
    public void showKeyboardConfig();
    public void performAction();
    public void enableCapsLock();

    public void switchMain();
    public void switchNumeric();
    public void switchProgramming();
    public void switchGreekmath();

    public void sendKeyEvent(int eventAction, int eventCode, int meta);

    public void commitText(String text);
    public void commitChar(char c);
  }
}
