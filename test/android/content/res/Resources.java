package android.content.res;

import android.util.DisplayMetrics;

/** Minimal resources for Config's JVM tests. No Android resource inflation. */
public class Resources
{
  public final DisplayMetrics metrics = new DisplayMetrics();
  public final Configuration configuration = new Configuration();

  public Resources(AssetManager assets, DisplayMetrics metrics, Configuration configuration) {}

  public DisplayMetrics getDisplayMetrics() { return metrics; }
  public Configuration getConfiguration() { return configuration; }
  public float getDimension(int id) { return 0.f; }
}
