package android.util;

/** Config only converts DIP values in these tests. Fail on other units. */
public class TypedValue
{
  public static float applyDimension(int unit, float value, DisplayMetrics dm)
  {
    if (unit != 1)
      throw new AssertionError("Unexpected dimension unit: " + unit);
    return value * dm.density;
  }
}
