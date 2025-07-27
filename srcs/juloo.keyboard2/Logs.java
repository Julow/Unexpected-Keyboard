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
    _debug_logs.println("should_show_candidates_view: "+conf.should_show_candidates_view);
  }

  public static void debug_config_migration(int from_version, int to_version)
  {
    debug("Migrating config version from " + from_version + " to " + to_version);
  }

  public static void debug(String s)
  {
    if (_debug_logs != null)
      _debug_logs.println(s);
  }

  public static void exn(String msg, Exception e)
  {
    Log.e(TAG, msg, e);
  }

  public static void trace()
  {
    if (_debug_logs != null)
      _debug_logs.println(Log.getStackTraceString(new Exception()));
  }
}
