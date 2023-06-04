package juloo.keyboard2;

import android.content.Context;
import android.os.Build.VERSION;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;

public final class VibratorCompat
{
  public static void vibrate(View v, VibrationBehavior b)
  {
    switch (b)
    {
      case DISABLED:
        break;
      case SYSTEM:
        if (VERSION.SDK_INT >= 8)
          v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP,
              HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
        break;
      case STRONG:
        vibrator_vibrate(v, 60);
        break;
      case MEDIUM:
        vibrator_vibrate(v, 30);
        break;
      case LIGHT:
        vibrator_vibrate(v, 15);
        break;
    }
  }

  /** Use the older [Vibrator] when the newer API is not available or the user
      wants more control. */
  static void vibrator_vibrate(View v, int duration)
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

  public static enum VibrationBehavior
  {
    DISABLED,
    SYSTEM,
    STRONG,
    MEDIUM,
    LIGHT;

    VibrationBehavior() {}

    /** Defaults [SYSTEM] for unrecognized strings. */
    public static VibrationBehavior of_string(String s)
    {
      switch (s)
      {
        case "disabled": return DISABLED;
        case "system": return SYSTEM;
        case "strong": return STRONG;
        case "medium": return MEDIUM;
        case "light": return LIGHT;
        default: return SYSTEM;
      }
    }
  }
}
