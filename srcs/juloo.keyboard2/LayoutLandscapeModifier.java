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
    // Bottom row as index 0. Used by [add_middle_column] below.
    int row_index = kw.rows.size() - 1;
    for (KeyboardData.Row r : kw.rows)
      new_rows.add(split_row(r, row_index--));
    return kw.with_rows(new_rows);
  }

  public static KeyboardData.Row transform_number_row(KeyboardData.Row r)
  {
    return split_row(r, -1);
  }

  static KeyboardData.Row split_row(KeyboardData.Row r, int row_index)
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
    for (; true; i++)
    {
      KeyboardData.Key k = r.keys.get(i);
      off += k.shift + k.width;
      if (off > mid_start || i == end)
      {
        if (off > mid_end)
          return duplicate_at_index(r, i, off, row_index);
        return split_at_index(r, i + 1, row_index);
      }
    }
  }

  /** Insert [ADDED_WIDTH] empty space before the key at index [i]. */
  static KeyboardData.Row split_at_index(KeyboardData.Row r, int i,
      int row_index)
  {
    List<KeyboardData.Key> new_keys = new ArrayList<KeyboardData.Key>(r.keys);
    KeyboardData.Key k = new_keys.get(i);
    new_keys.set(i, k.withShift(k.shift + ADDED_WIDTH));
    add_middle_key(new_keys, i, row_index);
    return r.with_keys(new_keys);
  }

  /** Duplicate the key at index [i] and insert [ADDED_WIDTH] empty space in
      between. */
  static KeyboardData.Row duplicate_at_index(KeyboardData.Row r, int i,
      float off, int row_index)
  {
    List<KeyboardData.Key> new_keys = new ArrayList<KeyboardData.Key>(r.keys);
    KeyboardData.Key k = new_keys.get(i);
    // Reduce the size of the duplicated keys if they would add more than 1 to
    // the width.
    float k_width = (k.width + 1.f) / 2;
    // Adjust the size on each sides when the key is not totally centered.
    float mid_d = Math.min(off - (r.keysWidth + k.width) / 2, k_width - 1);
    new_keys.add(i + 1, k.withWidthAndShift(k_width + mid_d, ADDED_WIDTH - 1));
    new_keys.set(i, k.withWidth(k_width - mid_d));
    add_middle_key(new_keys, i + 1, row_index);
    return r.with_keys(new_keys);
  }

  /** Add the middle column defined in [split_middle_column.xml]. */
  static void add_middle_key(List<KeyboardData.Key> new_keys, int i,
      int row_index)
  {
    List<KeyboardData.Key> mid_keys = LayoutModifier.split_middle_column.keys;
    if (row_index >= mid_keys.size())
      return;
    KeyboardData.Key mid_key = mid_keys.get(row_index);
    KeyboardData.Key right_key = new_keys.get(i);
    float shift = (right_key.shift - mid_key.width) / 2.f;
    new_keys.set(i, right_key.withShift(shift));
    new_keys.add(i, mid_key.withShift(shift));
  }

}
