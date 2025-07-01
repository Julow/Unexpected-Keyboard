package juloo.keyboard2;

import android.app.AlertDialog;
import android.content.res.Resources;
import android.graphics.Insets;
import android.os.Build.VERSION;
import android.os.IBinder;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

public final class Utils
{
  /** Turn the first letter of a string uppercase. */
  public static String capitalize_string(String s)
  {
    if (s.length() < 1)
      return s;
    // Make sure not to cut a code point in half
    int i = s.offsetByCodePoints(0, 1);
    return s.substring(0, i).toUpperCase(Locale.getDefault()) + s.substring(i);
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

  public static String read_all_utf8(InputStream inp) throws Exception
  {
    InputStreamReader reader = new InputStreamReader(inp, "UTF-8");
    StringBuilder out = new StringBuilder();
    int buff_length = 8000;
    char[] buff = new char[buff_length];
    int l;
    while ((l = reader.read(buff, 0, buff_length)) != -1)
      out.append(buff, 0, l);
    return out.toString();
  }
}
