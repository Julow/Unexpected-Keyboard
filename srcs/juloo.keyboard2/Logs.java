package juloo.keyboard2;

import android.graphics.Insets;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LogPrinter;
import android.view.WindowInsets;
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

  public static void debug_insets(WindowInsets wi)
  {
    if (_debug_logs == null)
      return;
    Insets i = wi.getInsets(WindowInsets.Type.systemBars());
    _debug_logs.println("Insets systemBars left=" + i.left + ", right=" + i.right + ", bottom=" + i.bottom);
    i = wi.getInsets(WindowInsets.Type.displayCutout());
    _debug_logs.println("Insets displayCutout left=" + i.left + ", right=" + i.right + ", bottom=" + i.bottom);
    i = wi.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
    _debug_logs.println("Insets left=" + i.left + ", right=" + i.right + ", bottom=" + i.bottom);
  }

  public static void debug_on_measure(DisplayMetrics dm, Config conf)
  {
    if (_debug_logs == null)
      return;
    _debug_logs.println("onMeasure dm.widthPixels=" + dm.widthPixels + ", horizntal_margin=" + conf.horizontal_margin);
  }
}
