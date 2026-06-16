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
    // Split the row at the key that overlaps that mid region. If the mid
    // region is entirely covered by one key, it is duplicated.
    float mid_start = r.keysWidth / 2 - 0.25f;
    float mid_end = mid_start + 0.5f;
    float off = 0f;
    int i = 0;
    int end = r.keys.size() - 1; // Exclude the last key to force a split
    for (; i < end; i++)
    {
      KeyboardData.Key k = r.keys.get(i);
      off += k.shift + k.width;
      if (off > mid_start)
      {
        if (off > mid_end)
          return split_at_index(r, i, true);
        return split_at_index(r, i + 1, false);
      }
    }
    return split_at_index(r, i, false);
  }

  /** Insert [ADDED_WIDTH] empty space before the key at index [i]. If
      [duplicate] is [true], the key at index [i] is duplicated on each side of
      the added space. */
  static KeyboardData.Row split_at_index(KeyboardData.Row r, int i,
      boolean duplicate)
  {
    List<KeyboardData.Key> new_keys = new ArrayList<KeyboardData.Key>(r.keys);
    KeyboardData.Key k = new_keys.get(i);
    if (duplicate)
    {
      // Reduce the size of the duplicated keys if they would add more than 1
      // to the width.
      float k_width = (k.width + 1.f) / 2;
      new_keys.add(i + 1, k.withWidthAndShift(k_width, ADDED_WIDTH - 1));
      new_keys.set(i, k.withWidth(k_width));
    }
    else
      new_keys.set(i, k.withShift(k.shift + ADDED_WIDTH));
    return r.with_keys(new_keys);
  }
}
