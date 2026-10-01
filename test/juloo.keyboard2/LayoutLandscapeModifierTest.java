package juloo.keyboard2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Geometry of split layouts. Rows are built programmatically so that no
    Android resources are needed. */
public class LayoutLandscapeModifierTest
{
  static final float EPS = 1e-4f;

  public LayoutLandscapeModifierTest()
  {
    // Same as [res/xml/split_middle_column.xml].
    LayoutModifier.split_middle_column = row_of(
        key("complete_emoji", 1f),
        key("complete_first", 3f),
        key("complete_second", 3f),
        key("complete_third", 3f));
  }

  /** Regression test: the number row is split with [row_index = -1], which
      used to index the middle column at -1 and crash the keyboard. */
  @Test
  public void number_row_is_split_without_middle_key()
  {
    KeyboardData.Row r = row_of_widths(1, 1, 1, 1, 1, 1, 1, 1, 1, 1);
    KeyboardData.Row s = LayoutLandscapeModifier.transform_number_row(r);
    assertEquals(10, s.keys.size());
    assertEquals(10f + LayoutLandscapeModifier.ADDED_WIDTH, s.keysWidth, EPS);
    assertEquals(LayoutLandscapeModifier.ADDED_WIDTH, s.keys.get(5).shift, EPS);
  }

  @Test
  public void regular_row_gets_middle_key()
  {
    KeyboardData.Row r = row_of_widths(1, 1, 1, 1, 1, 1, 1, 1, 1, 1);
    // Bottom row: first key of the middle column, which is 1 unit wide.
    KeyboardData.Row s = LayoutLandscapeModifier.split_row(r, 0);
    assertEquals(11, s.keys.size());
    assertEquals(15f, s.keysWidth, EPS);
    KeyboardData.Key mid = s.keys.get(5);
    KeyboardData.Key right = s.keys.get(6);
    assertEquals(1f, mid.width, EPS);
    assertEquals(2f, mid.shift, EPS);
    assertEquals(2f, right.shift, EPS);
    assertEquals(KeyboardData.Key.Role.Suggestion, mid.role);
  }

  @Test
  public void rows_above_the_middle_column_have_no_middle_key()
  {
    KeyboardData.Row r = row_of_widths(1, 1, 1, 1, 1, 1, 1, 1, 1, 1);
    KeyboardData.Row s = LayoutLandscapeModifier.split_row(r, 4);
    assertEquals(10, s.keys.size());
    assertEquals(15f, s.keysWidth, EPS);
  }

  @Test
  public void key_on_the_middle_is_duplicated()
  {
    // The 2 units wide key covers the middle of the row.
    KeyboardData.Row r = row_of_widths(1, 1, 1, 1, 2, 1, 1, 1, 1);
    assertEquals(10f, r.keysWidth, EPS);
    // Without middle key.
    KeyboardData.Row s = LayoutLandscapeModifier.split_row(r, -1);
    assertEquals(10, s.keys.size());
    assertEquals(15f, s.keysWidth, EPS);
    assertEquals(1.5f, s.keys.get(4).width, EPS);
    assertEquals(1.5f, s.keys.get(5).width, EPS);
    // With a 3 units wide middle key.
    KeyboardData.Row t = LayoutLandscapeModifier.split_row(r, 1);
    assertEquals(11, t.keys.size());
    assertEquals(15f, t.keysWidth, EPS);
    assertEquals(3f, t.keys.get(5).width, EPS);
  }

  @Test
  public void single_key_row_is_unchanged()
  {
    KeyboardData.Row r = row_of_widths(4);
    KeyboardData.Row s = LayoutLandscapeModifier.split_row(r, 0);
    assertEquals(1, s.keys.size());
    assertEquals(4f, s.keysWidth, EPS);
  }

  @Test
  public void whole_layout_is_split_from_the_bottom()
  {
    List<KeyboardData.Row> rows = new ArrayList<KeyboardData.Row>();
    rows.add(row_of_widths(1, 1, 1, 1, 1, 1, 1, 1, 1, 1));
    rows.add(row_of_widths(1, 1, 1, 1, 1, 1, 1, 1, 1, 1));
    KeyboardData kw = new KeyboardData(rows, 10f, null, null, null, null,
        true, false, true);
    KeyboardData split = LayoutLandscapeModifier.transform_to_landscape(kw);
    assertEquals(15f, split.keysWidth, EPS);
    // The bottom row (index 0 from the bottom) gets the emoji key, the row
    // above gets the first word suggestion.
    assertEquals(1f, split.rows.get(1).keys.get(5).width, EPS);
    assertEquals(3f, split.rows.get(0).keys.get(5).width, EPS);
    assertEquals(kw.keysHeight, split.keysHeight, EPS);
  }

  static KeyboardData.Key key(String name, float width)
  {
    KeyValue[] ks = new KeyValue[9];
    ks[0] = KeyValue.getKeyByName(name);
    return new KeyboardData.Key(ks, null, 0, width, 0f, null,
        KeyboardData.Key.Role.Suggestion);
  }

  static KeyboardData.Key key(float width)
  {
    return new KeyboardData.Key(new KeyValue[9], null, 0, width, 0f, null,
        KeyboardData.Key.Role.Normal);
  }

  static KeyboardData.Row row_of(KeyboardData.Key... keys)
  {
    return new KeyboardData.Row(new ArrayList<KeyboardData.Key>(Arrays.asList(keys)), 1f, 0f);
  }

  static KeyboardData.Row row_of_widths(float... widths)
  {
    ArrayList<KeyboardData.Key> keys = new ArrayList<KeyboardData.Key>();
    for (float w : widths)
      keys.add(key(w));
    return new KeyboardData.Row(keys, 1f, 0f);
  }
}
