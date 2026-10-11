package juloo.keyboard2;

import android.content.Context;
import android.os.Build.VERSION;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.view.HapticFeedbackConstants;
import android.view.View;

public final class VibratorCompat
{
  public static enum Mode
  {
    TAP,
    SLIDING,
    NONE
  };

  public static void vibrate(View v, Config config, Mode mode)
  {
    switch (mode)
    {
      case TAP:
        if (config.vibrate_custom)
          vibrator_vibrate(v, config.vibrate_duration);
        else
          v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP,
              HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
        break;
      case SLIDING:
        if (config.vibrate_custom)
          vibrator_vibrate(v, config.vibrate_duration / 2);
        else if (VERSION.SDK_INT >= 29)
          get_vibrator(v).vibrate(get_effect_tick());
        else
          vibrator_vibrate(v, 10);
        break;
      case NONE:
        break;
    }
  }

  /** Use the older [Vibrator] when the newer API is not available or the user
      wants more control. */
  static void vibrator_vibrate(View v, long duration)
  {
    if (duration <= 0)
      return;
    try
    {
      get_vibrator(v).vibrate(duration);
    }
    catch (Exception e) {}
  }

  static VibrationEffect _effect_tick = null;

  static VibrationEffect get_effect_tick()
  {
    if (_effect_tick == null)
      _effect_tick = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK);
    return _effect_tick;
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
