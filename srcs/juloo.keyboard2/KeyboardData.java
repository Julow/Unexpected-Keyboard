package juloo.keyboard2;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class KeyboardData
{
  public final List<Row> rows;
  /** Total width of the keyboard. */
  public final float keysWidth;
  /** Total height of the keyboard. */
  public final float keysHeight;
  /** Whether to add extra keys. */
  public final boolean extra_keys;

  public KeyboardData mapKeys(MapKey f)
  {
    ArrayList<Row> rows_ = new ArrayList<Row>();
    for (Row r : rows)
      rows_.add(r.mapKeys(f));
    return new KeyboardData(rows_, keysWidth, extra_keys);
  }

  /** Add keys from the given iterator into the keyboard. Extra keys are added
   * on the empty key4 corner of the second row, from right to left. If there's
   * not enough room, key3 of the second row is tried then key2 and key1 of the
   * third row. */
  public KeyboardData addExtraKeys(Iterator<KeyValue> k)
  {
    if (!extra_keys)
      return this;
    ArrayList<Row> rows = new ArrayList<Row>(this.rows);
    addExtraKeys_to_row(rows, k, 1, 4);
    addExtraKeys_to_row(rows, k, 1, 3);
    addExtraKeys_to_row(rows, k, 2, 2);
    addExtraKeys_to_row(rows, k, 2, 1);
    return new KeyboardData(rows, keysWidth, extra_keys);
  }

  private static void addExtraKeys_to_row(ArrayList<Row> rows, final Iterator<KeyValue> extra_keys, int row_i, final int d)
  {
    if (!extra_keys.hasNext())
      return;
    rows.set(row_i, rows.get(row_i).mapKeys(new MapKey(){
      public Key apply(Key k) {
        if (k.getKeyValue(d) == null && extra_keys.hasNext())
          return k.withKeyValue(d, extra_keys.next());
        else
          return k;
      }
    }));
  }

  private static Row _bottomRow = null;
  private static Map<Integer, KeyboardData> _layoutCache = new HashMap<Integer, KeyboardData>();

  public static KeyboardData load(Resources res, int id)
  {
    KeyboardData l = _layoutCache.get(id);
    if (l == null)
    {
      try
      {
        if (_bottomRow == null)
          _bottomRow = parse_bottom_row(res.getXml(R.xml.bottom_row));
        l = parse_keyboard(res.getXml(id));
        _layoutCache.put(id, l);
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
    return l;
  }

  private static KeyboardData parse_keyboard(XmlResourceParser parser) throws Exception
  {
    if (!expect_tag(parser, "keyboard"))
      throw new Exception("Empty layout file");
    boolean bottom_row = parser.getAttributeBooleanValue(null, "bottom_row", true);
    boolean extra_keys = parser.getAttributeBooleanValue(null, "extra_keys", true);
    ArrayList<Row> rows = new ArrayList<Row>();
    while (expect_tag(parser, "row"))
        rows.add(Row.parse(parser));
    float kw = compute_max_width(rows);
    if (bottom_row)
      rows.add(_bottomRow.updateWidth(kw));
    return new KeyboardData(rows, kw, extra_keys);
  }

  private static float compute_max_width(List<Row> rows)
  {
    float w = 0.f;
    for (Row r : rows)
      w = Math.max(w, r.keysWidth);
    return w;
  }

  private static Row parse_bottom_row(XmlResourceParser parser) throws Exception
  {
    if (!expect_tag(parser, "row"))
      throw new Exception("Failed to parse bottom row");
    return Row.parse(parser);
  }

  protected KeyboardData(List<Row> rows_, float kw, boolean xk)
  {
    float kh = 0.f;
    for (Row r : rows_)
      kh += r.height + r.shift;
    rows = rows_;
    keysWidth = kw;
    keysHeight = kh;
    extra_keys = xk;
  }

  public static class Row
  {
    public final List<Key> keys;
    /** Height of the row, without 'shift'. */
    public final float height;
    /** Extra empty space on the top. */
    public final float shift;
    /** Total width of the row. */
    private final float keysWidth;

    protected Row(List<Key> keys_, float h, float s)
    {
      float kw = 0.f;
      for (Key k : keys_) kw += k.width + k.shift;
      keys = keys_;
      height = h;
      shift = s;
      keysWidth = kw;
    }

    public static Row parse(XmlResourceParser parser) throws Exception
    {
      ArrayList<Key> keys = new ArrayList<Key>();
      int status;
      float h = parser.getAttributeFloatValue(null, "height", 1f);
      float shift = parser.getAttributeFloatValue(null, "shift", 0f);
      while (expect_tag(parser, "key"))
        keys.add(Key.parse(parser));
      return new Row(keys, h, shift);
    }

    public Row mapKeys(MapKey f)
    {
      ArrayList<Key> keys_ = new ArrayList<Key>();
      for (Key k : keys)
        keys_.add(f.apply(k));
      return new Row(keys_, height, shift);
    }

    /** Change the width of every keys so that the row is 's' units wide. */
    public Row updateWidth(float newWidth)
    {
      final float s = newWidth / keysWidth;
      return mapKeys(new MapKey(){
        public Key apply(Key k) { return k.scaleWidth(s); }
      });
    }
  }

  public static class Key
  {
    /*
     ** 1   2
     **   0
     ** 3   4
     */
    public final KeyValue key0;
    public final KeyValue key1;
    public final KeyValue key2;
    public final KeyValue key3;
    public final KeyValue key4;

    /** Key width in relative unit. */
    public final float width;
    /** Extra empty space on the left of the key. */
    public final float shift;
    /** Put keys 1 to 4 on the edges instead of the corners. */
    public final boolean edgekeys;

    protected Key(KeyValue k0, KeyValue k1, KeyValue k2, KeyValue k3, KeyValue k4, float w, float s, boolean e)
    {
      key0 = k0;
      key1 = k1;
      key2 = k2;
      key3 = k3;
      key4 = k4;
      width = w;
      shift = s;
      edgekeys = e;
    }

    public static Key parse(XmlResourceParser parser) throws Exception
    {
      KeyValue k0 = KeyValue.getKeyByName(parser.getAttributeValue(null, "key0"));
      KeyValue k1 = KeyValue.getKeyByName(parser.getAttributeValue(null, "key1"));
      KeyValue k2 = KeyValue.getKeyByName(parser.getAttributeValue(null, "key2"));
      KeyValue k3 = KeyValue.getKeyByName(parser.getAttributeValue(null, "key3"));
      KeyValue k4 = KeyValue.getKeyByName(parser.getAttributeValue(null, "key4"));
      float width = parser.getAttributeFloatValue(null, "width", 1f);
      float shift = parser.getAttributeFloatValue(null, "shift", 0.f);
      boolean edgekeys = parser.getAttributeBooleanValue(null, "edgekeys", false);
      while (parser.next() != XmlResourceParser.END_TAG)
        continue ;
      return new Key(k0, k1, k2, k3, k4, width, shift, edgekeys);
    }

    /** New key with the width multiplied by 's'. */
    public Key scaleWidth(float s)
    {
      return new Key(key0, key1, key2, key3, key4, width * s, shift, edgekeys);
    }

    public KeyValue getKeyValue(int i)
    {
      switch (i)
      {
        case 0: return key0;
        case 1: return key1;
        case 2: return key2;
        case 3: return key3;
        case 4: return key4;
        default: return null;
      }
    }

    public Key withKeyValue(int i, KeyValue kv)
    {
      KeyValue k0 = key0, k1 = key1, k2 = key2, k3 = key3, k4 = key4;
      switch (i)
      {
        case 0: k0 = kv; break;
        case 1: k1 = kv; break;
        case 2: k2 = kv; break;
        case 3: k3 = kv; break;
        case 4: k4 = kv; break;
      }
      return new Key(k0, k1, k2, k3, k4, width, shift, edgekeys);
    }

    /**
     * See Pointers.onTouchMove() for the represented direction.
     */
    public KeyValue getAtDirection(int direction)
    {
      if (edgekeys)
      {
        // \ 1 /
        //  \ /
        // 3 0 2
        //  / \
        // / 4 \
        switch (direction)
        {
          case 2: case 3: return key1;
          case 4: case 5: return key2;
          case 6: case 7: return key4;
          case 8: case 1: return key3;
        }
      }
      else
      {
        // 1 | 2
        //   |
        // --0--
        //   |
        // 3 | 4
        switch (direction)
        {
          case 1: case 2: return key1;
          case 3: case 4: return key2;
          case 5: case 6: return key4;
          case 7: case 8: return key3;
        }
      }
      return null;
    }
  }

  // Not using Function<KeyValue, KeyValue> to keep compatibility with Android 6.
  public static abstract interface MapKey {
    public Key apply(Key k);
  }

  public static abstract class MapKeyValues implements MapKey {
    abstract public KeyValue apply(KeyValue kv);

    public Key apply(Key k)
    {
      return new Key(apply(k.key0), apply(k.key1), apply(k.key2),
          apply(k.key3), apply(k.key4), k.width, k.shift, k.edgekeys);
    }
  }

  /** Parsing utils */

  /** Returns [false] on [END_DOCUMENT] or [END_TAG], [true] otherwise. */
  private static boolean expect_tag(XmlResourceParser parser, String name) throws Exception
  {
    int status;
    do
    {
      status = parser.next();
      if (status == XmlResourceParser.END_DOCUMENT || status == XmlResourceParser.END_TAG)
        return false;
    }
    while (status != XmlResourceParser.START_TAG);
    if (!parser.getName().equals(name))
      throw new Exception("Unknow tag: " + parser.getName());
    return true;
  }
}
