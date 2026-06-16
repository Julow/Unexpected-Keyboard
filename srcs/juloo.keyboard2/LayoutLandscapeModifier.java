package juloo.keyboard2;

import java.util.ArrayList;
import java.util.List;

public final class LayoutLandscapeModifier
{
  /** Width added to the layout, in the key width unit of the layout. */
  public static final int ADDED_WIDTH = 5;

  public static KeyboardData transform_to_landscape(KeyboardData kw)
  {
    ArrayList<KeyboardData.Row> new_rows = new ArrayList<KeyboardData.Row>();
    for (KeyboardData.Row r : kw.rows)
      new_rows.add(split_row(r));
    return kw.with_rows(new_rows);
  }

  public static KeyboardData.Row transform_number_row(KeyboardData.Row r)
  {
    return split_row(r);
  }

  static KeyboardData.Row split_row(KeyboardData.Row r)
  {
    if (r.keys.size() < 2)
      return r;
    List<KeyboardData.Key> new_keys = new ArrayList<KeyboardData.Key>(r.keys);
    int i = split_at_key(r);
    KeyboardData.Key k = new_keys.get(i);
    new_keys.set(i, k.withShift(k.shift + ADDED_WIDTH));
    return r.with_keys(new_keys);
  }

  /** Index of the key that splits the layout. The row must not be empty. */
  static int split_at_key(KeyboardData.Row r)
  {
    float mid = r.keysWidth / 2;
    float off = 0f;
    int i = 0;
    int end = r.keys.size() - 1; // Exclude the last key to force a split
    for (; i < end && off < mid; i++)
    {
      KeyboardData.Key k = r.keys.get(i);
      off += k.shift + k.width;
    }
    return i;
  }
}
