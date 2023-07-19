package juloo.keyboard2;

import android.util.Log;
import android.util.LogPrinter;
import android.view.inputmethod.EditorInfo;
import org.json.JSONException;

public final class Logs
{
  static final String TAG = "juloo.keyboard2";

  static LogPrinter _debug_logs = null;

  public static void set_debug_logs(boolean d)
  {
    _debug_logs = d ? new LogPrinter(Log.DEBUG, TAG) : null;
  }

  public static void debug_startup_input_view(EditorInfo info, Config conf)
  {
    if (_debug_logs == null)
      return;
    info.dump(_debug_logs, "");
    if (info.extras != null)
      _debug_logs.println("extras: "+info.extras.toString());
    _debug_logs.println("swapEnterActionKey: "+conf.swapEnterActionKey);
    _debug_logs.println("actionLabel: "+conf.actionLabel);
  }

  public static void err_load_custom_extra_keys(JSONException e)
  {
    Log.e(TAG, "Failed to read custom extra keys from preferences", e);
  }
}
