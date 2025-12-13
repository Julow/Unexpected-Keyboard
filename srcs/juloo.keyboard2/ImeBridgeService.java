package juloo.keyboard2;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;

/**
 * Runs in the default app process. Receives commands from the overlay process
 * and applies them to the current InputConnection owned by Keyboard2 (IME).
 */
public final class ImeBridgeService extends Service implements Handler.Callback
{
  public static final int MSG_COMMIT_TEXT = 1;
  public static final int MSG_SEND_KEY_DOWN_UP = 2;
  public static final int MSG_CONTEXT_MENU = 3;

  private final Messenger _messenger =
      new Messenger(new Handler(Looper.getMainLooper(), this));

  @Override
  public IBinder onBind(Intent intent)
  {
    return _messenger.getBinder();
  }

  @Override
  public boolean handleMessage(Message msg)
  {
    Keyboard2 ime = Keyboard2.peekInstance();
    if (ime == null)
      return true;

    InputConnection conn = ime.getCurrentInputConnection();
    if (conn == null)
      return true;

    Bundle b = msg.getData();
    if (b == null) b = Bundle.EMPTY;

    switch (msg.what)
    {
      case MSG_COMMIT_TEXT: {
        String text = b.getString("text", "");
        if (!text.equals(""))
          conn.commitText(text, 1);
        break;
      }

      case MSG_SEND_KEY_DOWN_UP: {
        int keyCode = b.getInt("keyCode", 0);
        int metaState = b.getInt("metaState", 0);
        if (keyCode != 0)
          sendKeyDownUp(conn, keyCode, metaState);
        break;
      }

      case MSG_CONTEXT_MENU: {
        int id = b.getInt("id", 0);
        if (id != 0)
          conn.performContextMenuAction(id);
        break;
      }
    }
    return true;
  }

  private static void sendKeyDownUp(InputConnection conn, int keyCode, int metaState)
  {
    conn.sendKeyEvent(new KeyEvent(1, 1, KeyEvent.ACTION_DOWN, keyCode, 0,
        metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
        KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));

    conn.sendKeyEvent(new KeyEvent(1, 1, KeyEvent.ACTION_UP, keyCode, 0,
        metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
        KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
  }
}
