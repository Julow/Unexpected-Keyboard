package juloo.keyboard2;

import android.app.AlertDialog;
import android.os.IBinder;
import android.view.Window;
import android.view.WindowManager;

class Utils
{
  /** Turn the first letter of a string uppercase. */
  public static String capitalize_string(String s)
  {
    // Make sure not to cut a code point in half
    int i = s.offsetByCodePoints(0, 1);
    return s.substring(0, i).toUpperCase() + s.substring(i);
  }

  /** Like [dialog.show()] but properly configure layout params when called
      from an IME. [token] is the input view's [getWindowToken()]. */
  public static void show_dialog_on_ime(AlertDialog dialog, IBinder token)
  {
    Window win = dialog.getWindow();
    WindowManager.LayoutParams lp = win.getAttributes();
    lp.token = token;
    lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
    win.setAttributes(lp);
    win.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    dialog.show();
  }
}
