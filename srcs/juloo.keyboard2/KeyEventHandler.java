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
    key = KeyModifier.handleFlags(key, flags);
    switch (key.eventCode)
    {
    case KeyValue.EVENT_CONFIG: _recv.showKeyboardConfig(); return;
    case KeyValue.EVENT_SWITCH_TEXT: _recv.setLayout(-1); return;
    case KeyValue.EVENT_SWITCH_NUMERIC: _recv.setLayout(R.xml.numeric); return;
    case KeyValue.EVENT_SWITCH_EMOJI: _recv.setPane_emoji(); return;
    case KeyValue.EVENT_SWITCH_BACK_EMOJI: _recv.setPane_normal(); return;
    case KeyValue.EVENT_CHANGE_METHOD: _recv.switchToNextInputMethod(); return;
    default:
      if ((flags & (KeyValue.FLAG_CTRL | KeyValue.FLAG_ALT)) != 0)
        handleMetaKeyUp(key, flags);
      else if (key.char_ != KeyValue.CHAR_NONE)
        _recv.commitChar(key.char_);
      else if (key.eventCode != KeyValue.EVENT_NONE)
        handleMetaKeyUp(key, flags);
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

  private void handleMetaKeyUp(KeyValue key, int flags)
  {
    int meta = 0;
    if (key.eventCode == KeyValue.EVENT_NONE)
      return ;
    if ((flags & KeyValue.FLAG_CTRL) != 0)
      meta |= KeyEvent.META_CTRL_LEFT_ON | KeyEvent.META_CTRL_ON;
    if ((flags & KeyValue.FLAG_ALT) != 0)
      meta |= KeyEvent.META_ALT_LEFT_ON | KeyEvent.META_ALT_ON;
    if ((flags & KeyValue.FLAG_SHIFT) != 0)
      meta |= KeyEvent.META_SHIFT_LEFT_ON | KeyEvent.META_SHIFT_ON;
    _recv.sendKeyEvent(key.eventCode, meta);
  }

  public static interface IReceiver
  {
    public void switchToNextInputMethod();
    public void setPane_emoji();
    public void setPane_normal();
    public void showKeyboardConfig();

    /** 'res_id' is '-1' for the currently selected layout. */
    public void setLayout(int res_id);

    public void sendKeyEvent(int eventCode, int meta);

    public void commitText(String text);
    public void commitChar(char c);
  }
}
