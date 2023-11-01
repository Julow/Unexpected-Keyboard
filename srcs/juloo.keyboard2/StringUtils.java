package juloo.keyboard2;

final class Utils
{
  /** Turn the first letter of a string uppercase. */
  public static String capitalize_string(String s)
  {
    // Make sure not to cut a code point in half
    int i = s.offsetByCodePoints(0, 1);
    return s.substring(0, i).toUpperCase() + s.substring(i);
  }
}
