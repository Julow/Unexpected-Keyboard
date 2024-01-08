package juloo.keyboard2;

import android.content.Context;
import android.os.Build.VERSION;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;

public final class VibratorCompat
{
  public static void vibrate(View v, Config config)
  {
    if (config.vibrate_custom)
    {
      if (config.vibrate_duration > 0)
        vibrator_vibrate(v, config.vibrate_duration);
    }
    else
    {
      if (VERSION.SDK_INT >= 8)
        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
    }
  }

  /** Use the older [Vibrator] when the newer API is not available or the user
      wants more control. */
  static void vibrator_vibrate(View v, long duration)
  {
    try
    {
      get_vibrator(v).vibrate(duration);
    }
    catch (Exception e) {}
  }

  static Vibrator vibrator_service = null;

  static Vibrator get_vibrator(View v)
  {
    if (vibrator_service == null)
    {
      vibrator_service =
        (Vibrator)v.getContext().getSystemService(Context.VIBRATOR_SERVICE);
    }
    return vibrator_service;
  }
}
