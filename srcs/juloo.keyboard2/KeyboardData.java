package juloo.keyboard2;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.Xml;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.xmlpull.v1.XmlPullParser;

class KeyboardData
{
  public final List<Row> rows;
  /** Total width of the keyboard. */
  public final float keysWidth;
  /** Total height of the keyboard. */
  public final float keysHeight;
  /** Might be null. */
  public final Modmap modmap;
  /** Might be null. */
  public final String script;

  public KeyboardData mapKeys(MapKey f)
  {
    ArrayList<Row> rows_ = new ArrayList<Row>();
    for (Row r : rows)
      rows_.add(r.mapKeys(f));
    return new KeyboardData(rows_, keysWidth, modmap, script);
  }

  /** Add keys from the given iterator into the keyboard. Extra keys are added
   * on the empty key4 corner of the second row, from right to left. If there's
   * not enough room, key3 of the second row is tried then key2 and key1 of the
   * third row. */
  public KeyboardData addExtraKeys(Iterator<KeyValue> k)
  {
    ArrayList<Row> rows = new ArrayList<Row>(this.rows);
    addExtraKeys_to_row(rows, k, 1, 4);
    addExtraKeys_to_row(rows, k, 1, 3);
    addExtraKeys_to_row(rows, k, 2, 2);
    addExtraKeys_to_row(rows, k, 2, 1);
    if (k.hasNext())
    {
      for (int r = 0; r < rows.size(); r++)
        for (int c = 1; c <= 4; c++)
          addExtraKeys_to_row(rows, k, r, c);
    }
    return new KeyboardData(rows, keysWidth, modmap, script);
  }

  public KeyboardData addNumPad()
  {
    ArrayList<Row> extendedRows = new ArrayList<Row>();
    Iterator<Row> iterNumPadRows = num_pad.rows.iterator();
    for (Row row : rows)
    {
      ArrayList<KeyboardData.Key> keys = new ArrayList<Key>(row.keys);
      if (iterNumPadRows.hasNext())
      {
        Row numPadRow = iterNumPadRows.next();
        List<Key> nps = numPadRow.keys;
        if (nps.size() > 0) {
          float firstNumPadShift = 0.5f + keysWidth - row.keysWidth;
          keys.add(nps.get(0).withShift(firstNumPadShift));
          for (int i = 1; i < nps.size(); i++)
            keys.add(nps.get(i));
        }
      }
      extendedRows.add(new Row(keys, row.height, row.shift));
    }
    return new
      KeyboardData(extendedRows, compute_max_width(extendedRows), modmap, script);
  }

  public KeyboardData addNumberRow()
  {
    ArrayList<Row> rows_ = new ArrayList<Row>(this.rows);
    rows_.add(0, number_row.updateWidth(keysWidth));
    return new KeyboardData(rows_, keysWidth, modmap, script);
  }

  public Key findKeyWithValue(KeyValue kv)
  {
    for (Row r : rows)
    {
      Key k = r.findKeyWithValue(kv);
      if (k != null)
        return k;
    }
    return null;
  }

  public void getKeys(Set<KeyValue> dst)
  {
    for (Row r : rows)
      r.getKeys(dst);
  }

  private static void addExtraKeys_to_row(ArrayList<Row> rows, final Iterator<KeyValue> extra_keys, int row_i, final int d)
  {
    if (!extra_keys.hasNext() || row_i >= rows.size())
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

  public static Row bottom_row;
  public static Row number_row;
  public static KeyboardData num_pad;
  private static Map<Integer, KeyboardData> _layoutCache = new HashMap<Integer, KeyboardData>();

  public static void init(Resources res)
  {
    try
    {
      bottom_row = parse_row(res.getXml(R.xml.bottom_row));
      number_row = parse_row(res.getXml(R.xml.number_row));
      num_pad = parse_keyboard(res.getXml(R.xml.numpad));
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  /** Load a layout from a resource ID. Returns [null] on error. */
  public static KeyboardData load(Resources res, int id)
  {
    KeyboardData l = _layoutCache.get(id);
    if (l == null)
    {
      try
      {
        XmlResourceParser parser = res.getXml(id);
        l = parse_keyboard(parser);
        parser.close();
        _layoutCache.put(id, l);
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
    return l;
  }

  /** Load a layout from a string. Returns [null] on error. */
  public static KeyboardData load_string(String src)
  {
    try
    {
      XmlPullParser parser = Xml.newPullParser();
      parser.setInput(new StringReader(src));
      return parse_keyboard(parser);
    }
    catch (Exception e)
    {
      return null;
    }
  }

  private static KeyboardData parse_keyboard(XmlPullParser parser) throws Exception
  {
    if (!expect_tag(parser, "keyboard"))
      throw new Exception("Empty layout file");
    boolean add_bottom_row = attribute_bool(parser, "bottom_row", true);
    float specified_kw = attribute_float(parser, "width", 0f);
    String script = parser.getAttributeValue(null, "script");
    ArrayList<Row> rows = new ArrayList<Row>();
    Modmap modmap = null;
    while (next_tag(parser))
    {
      switch (parser.getName())
      {
        case "row":
          rows.add(Row.parse(parser));
          break;
        case "modmap":
          modmap = Modmap.parse(parser);
          break;
        default:
          throw new Exception("Unknown tag: " + parser.getName());
      }
    }
    float kw = (specified_kw != 0f) ? specified_kw : compute_max_width(rows);
    if (add_bottom_row)
      rows.add(bottom_row.updateWidth(kw));
    return new KeyboardData(rows, kw, modmap, script);
  }

  private static float compute_max_width(List<Row> rows)
  {
    float w = 0.f;
    for (Row r : rows)
      w = Math.max(w, r.keysWidth);
    return w;
  }

  private static Row parse_row(XmlPullParser parser) throws Exception
  {
    if (!expect_tag(parser, "row"))
      throw new Exception("Failed to parse row");
    return Row.parse(parser);
  }

  protected KeyboardData(List<Row> rows_, float kw, Modmap mm, String sc)
  {
    float kh = 0.f;
    for (Row r : rows_)
      kh += r.height + r.shift;
    rows = rows_;
    modmap = mm;
    script = sc;
    keysWidth = kw;
    keysHeight = kh;
  }

  public static class Row
  {
    public final List<Key> keys;
    /** Height of the row, without 'shift'. */
    public final float height;
    /** Extra empty space on the top. */
    public final float shift;
    /** Total width of the row. */
    public final float keysWidth;

    protected Row(List<Key> keys_, float h, float s)
    {
      float kw = 0.f;
      for (Key k : keys_) kw += k.width + k.shift;
      keys = keys_;
      height = h;
      shift = s;
      keysWidth = kw;
    }

    public static Row parse(XmlPullParser parser) throws Exception
    {
      ArrayList<Key> keys = new ArrayList<Key>();
      int status;
      float h = attribute_float(parser, "height", 1f);
      float shift = attribute_float(parser, "shift", 0f);
      while (expect_tag(parser, "key"))
        keys.add(Key.parse(parser));
      return new Row(keys, h, shift);
    }

    public void getKeys(Set<KeyValue> dst)
    {
      for (Key k : keys)
        k.getKeys(dst);
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

    public Key findKeyWithValue(KeyValue kv)
    {
      for (Key k : keys)
        if (k.hasValue(kv))
          return k;
      return null;
    }
  }

  public static class Key
  {
    /**
     *  1 7 2
     *  5 0 6
     *  3 8 4
     */
    public final KeyValue[] keys;
    /** Pack flags for every key values. Flags are: [F_LOC]. */
    private final int keysflags;
    /** Key width in relative unit. */
    public final float width;
    /** Extra empty space on the left of the key. */
    public final float shift;
    /** Keys 2 and 3 are repeated as the finger moves laterally on the key.
        Used for the left and right arrow keys on the space bar. */
    public final boolean slider;
    /** String printed on the keys. It has no other effect. */
    public final String indication;

    /** Whether a key was declared with the 'loc' prefix. */
    public static final int F_LOC = 1;
    public static final int ALL_FLAGS = F_LOC;

    protected Key(KeyValue[] ks, int f, float w, float s, boolean sl, String i)
    {
      keys = ks;
      keysflags = f;
      width = w;
      shift = s;
      slider = sl;
      indication = i;
    }

    /** Write the parsed key into [ks] at [index]. Doesn't write if the
        attribute is not present. Return flags that can be aggregated into the
        value for [keysflags]. */
    static int parse_key_attr(XmlPullParser parser, String attr, KeyValue[] ks,
        int index)
        throws Exception
    {
      String name = parser.getAttributeValue(null, attr);
      int flags = 0;
      if (name == null)
        return 0;
      String name_loc = stripPrefix(name, "loc ");
      if (name_loc != null)
      {
        flags |= F_LOC;
        name = name_loc;
      }
      ks[index] = KeyValue.getKeyByName(name);
      return (flags << index);
    }

    static String stripPrefix(String s, String prefix)
    {
      return s.startsWith(prefix) ? s.substring(prefix.length()) : null;
    }

    public static Key parse(XmlPullParser parser) throws Exception
    {
      KeyValue[] ks = new KeyValue[9];
      int keysflags = 0;
      keysflags |= parse_key_attr(parser, "key0", ks, 0);
      keysflags |= parse_key_attr(parser, "key1", ks, 1);
      keysflags |= parse_key_attr(parser, "key2", ks, 2);
      keysflags |= parse_key_attr(parser, "key3", ks, 3);
      keysflags |= parse_key_attr(parser, "key4", ks, 4);
      keysflags |= parse_key_attr(parser, "key5", ks, 5);
      keysflags |= parse_key_attr(parser, "key6", ks, 6);
      keysflags |= parse_key_attr(parser, "key7", ks, 7);
      keysflags |= parse_key_attr(parser, "key8", ks, 8);
      float width = attribute_float(parser, "width", 1f);
      float shift = attribute_float(parser, "shift", 0.f);
      boolean slider = attribute_bool(parser, "slider", false);
      String indication = parser.getAttributeValue(null, "indication");
      while (parser.next() != XmlPullParser.END_TAG)
        continue;
      return new Key(ks, keysflags, width, shift, slider, indication);
    }

    /** Whether key at [index] as [flag]. */
    public boolean keyHasFlag(int index, int flag)
    {
      return (keysflags & (flag << index)) != 0;
    }

    /** New key with the width multiplied by 's'. */
    public Key scaleWidth(float s)
    {
      return new Key(keys, keysflags, width * s, shift, slider, indication);
    }

    public void getKeys(Set<KeyValue> dst)
    {
      for (int i = 0; i < keys.length; i++)
        if (keys[i] != null)
          dst.add(keys[i]);
    }

    public KeyValue getKeyValue(int i)
    {
      return keys[i];
    }

    public Key withKeyValue(int i, KeyValue kv)
    {
      KeyValue[] ks = new KeyValue[keys.length];
      for (int j = 0; j < keys.length; j++) ks[j] = keys[j];
      ks[i] = kv;
      int flags = (keysflags & ~(ALL_FLAGS << i));
      return new Key(ks, flags, width, shift, slider, indication);
    }

    public Key withShift(float s)
    {
      return new Key(keys, keysflags, width, s, slider, indication);
    }

    public boolean hasValue(KeyValue kv)
    {
      for (int i = 0; i < keys.length; i++)
        if (keys[i] != null && keys[i].equals(kv))
          return true;
      return false;
    }
  }

  // Not using Function<KeyValue, KeyValue> to keep compatibility with Android 6.
  public static abstract interface MapKey {
    public Key apply(Key k);
  }

  public static abstract class MapKeyValues implements MapKey {
    abstract public KeyValue apply(KeyValue c, boolean localized);

    public Key apply(Key k)
    {
      KeyValue[] ks = new KeyValue[k.keys.length];
      for (int i = 0; i < ks.length; i++)
        if (k.keys[i] != null)
          ks[i] = apply(k.keys[i], k.keyHasFlag(i, Key.F_LOC));
      return new Key(ks, k.keysflags, k.width, k.shift, k.slider, k.indication);
    }
  }

  public static class Modmap
  {
    public final Map<KeyValue, KeyValue> shift;

    public Modmap(Map<KeyValue, KeyValue> s)
    {
      shift = s;
    }

    public static Modmap parse(XmlPullParser parser) throws Exception
    {
      HashMap<KeyValue, KeyValue> shift = new HashMap<KeyValue, KeyValue>();
      while (expect_tag(parser, "shift"))
        parse_mapping(parser, shift);
      return new Modmap(shift);
    }

    private static void parse_mapping(XmlPullParser parser, Map<KeyValue, KeyValue> dst) throws Exception
    {
      KeyValue a = KeyValue.getKeyByName(parser.getAttributeValue(null, "a"));
      KeyValue b = KeyValue.getKeyByName(parser.getAttributeValue(null, "b"));
      while (parser.next() != XmlPullParser.END_TAG)
        continue;
      dst.put(a, b);
    }
  }

  /** Parsing utils */

  /** Returns [false] on [END_DOCUMENT] or [END_TAG], [true] otherwise. */
  private static boolean next_tag(XmlPullParser parser) throws Exception
  {
    int status;
    do
    {
      status = parser.next();
      if (status == XmlPullParser.END_DOCUMENT || status == XmlPullParser.END_TAG)
        return false;
    }
    while (status != XmlPullParser.START_TAG);
    return true;
  }

  /** Returns [false] on [END_DOCUMENT] or [END_TAG], [true] otherwise. */
  private static boolean expect_tag(XmlPullParser parser, String name) throws Exception
  {
    if (!next_tag(parser))
      return false;
    if (!parser.getName().equals(name))
      throw new Exception("Unknown tag: " + parser.getName());
    return true;
  }

  private static boolean attribute_bool(XmlPullParser parser, String attr, boolean default_val)
  {
    String val = parser.getAttributeValue(null, attr);
    if (val == null)
      return default_val;
    return val.equals("true");
  }

  private static float attribute_float(XmlPullParser parser, String attr, float default_val)
  {
    String val = parser.getAttributeValue(null, attr);
    if (val == null)
      return default_val;
    return Float.parseFloat(val);
  }
}
