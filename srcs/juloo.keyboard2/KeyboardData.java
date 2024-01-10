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
  /** Might be different from [script]. Might be null. */
  public final String numpad_script;
  /** The [name] attribute. Might be null.  */
  public final String name;
  /** Position of every keys on the layout, see [getKeys()]. */
  private Map<KeyValue, KeyPos> _key_pos = null;

  public KeyboardData mapKeys(MapKey f)
  {
    ArrayList<Row> rows_ = new ArrayList<Row>();
    for (Row r : rows)
      rows_.add(r.mapKeys(f));
    return new KeyboardData(this, rows_);
  }

  /** Add keys from the given iterator into the keyboard. Preferred position is
      specified via [PreferredPos]. */
  public KeyboardData addExtraKeys(Iterator<Map.Entry<KeyValue, PreferredPos>> extra_keys)
  {
    /* Keys that couldn't be placed at their preferred position. */
    ArrayList<KeyValue> unplaced_keys = new ArrayList<KeyValue>();
    ArrayList<Row> rows = new ArrayList<Row>(this.rows);
    while (extra_keys.hasNext())
    {
      Map.Entry<KeyValue, PreferredPos> kp = extra_keys.next();
      if (!add_key_to_preferred_pos(rows, kp.getKey(), kp.getValue()))
        unplaced_keys.add(kp.getKey());
    }
    for (KeyValue kv : unplaced_keys)
      add_key_to_preferred_pos(rows, kv, PreferredPos.ANYWHERE);
    return new KeyboardData(this, rows);
  }

  /** Place a key on the keyboard according to its preferred position. Mutates
      [rows]. Returns [false] if it couldn't be placed. */
  boolean add_key_to_preferred_pos(List<Row> rows, KeyValue kv, PreferredPos pos)
  {
    if (pos.next_to != null)
    {
      KeyPos next_to_pos = getKeys().get(pos.next_to);
      if (next_to_pos != null
          && add_key_to_pos(rows, kv, next_to_pos.with_dir(-1)))
        return true;
    }
    for (KeyPos p : pos.positions)
      if (add_key_to_pos(rows, kv, p))
        return true;
    return false;
  }

  /** Place a key on the keyboard. A value of [-1] in one of the coordinate
      means that the key can be placed anywhere in that coordinate, see
      [PreferredPos]. Mutates [rows]. Returns [false] if it couldn't be placed.
      */
  boolean add_key_to_pos(List<Row> rows, KeyValue kv, KeyPos p)
  {
    int i_row = p.row;
    int i_row_end = p.row;
    if (p.row == -1) { i_row = 0; i_row_end = rows.size() - 1; }
    for (; i_row <= i_row_end; i_row++)
    {
      Row row = rows.get(i_row);
      int i_col = p.col;
      int i_col_end = p.col;
      if (p.col == -1) { i_col = 0; i_col_end = row.keys.size() - 1; }
      for (; i_col <= i_col_end; i_col++)
      {
        Key col = row.keys.get(i_col);
        int i_dir = p.dir;
        int i_dir_end = p.dir;
        if (p.dir == -1) { i_dir = 1; i_dir_end = 4; }
        for (; i_dir <= i_dir_end; i_dir++)
        {
          if (col.getKeyValue(i_dir) == null)
          {
            row.keys.set(i_col, col.withKeyValue(i_dir, kv));
            return true;
          }
        }
      }
    }
    return false;
  }

  public KeyboardData addNumPad(KeyboardData num_pad)
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
    return new KeyboardData(this, extendedRows);
  }

  public KeyboardData addNumberRow()
  {
    ArrayList<Row> rows_ = new ArrayList<Row>(this.rows);
    rows_.add(0, number_row.updateWidth(keysWidth));
    return new KeyboardData(this, rows_);
  }

  public Key findKeyWithValue(KeyValue kv)
  {
    KeyPos pos = getKeys().get(kv);
    if (pos == null || pos.row >= rows.size())
      return null;
    return rows.get(pos.row).get_key_at_pos(pos);
  }

  /** This is computed once and cached. */
  public Map<KeyValue, KeyPos> getKeys()
  {
    if (_key_pos == null)
    {
      _key_pos = new HashMap<KeyValue, KeyPos>();
      for (int r = 0; r < rows.size(); r++)
        rows.get(r).getKeys(_key_pos, r);
    }
    return _key_pos;
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
      return load_string_exn(src);
    }
    catch (Exception e)
    {
      return null;
    }
  }

  /** Like [load_string] but throws an exception on error and do not return
      [null]. */
  public static KeyboardData load_string_exn(String src) throws Exception
  {
    XmlPullParser parser = Xml.newPullParser();
    parser.setInput(new StringReader(src));
    return parse_keyboard(parser);
  }

  private static KeyboardData parse_keyboard(XmlPullParser parser) throws Exception
  {
    if (!expect_tag(parser, "keyboard"))
      throw error(parser, "Expected tag <keyboard>");
    boolean add_bottom_row = attribute_bool(parser, "bottom_row", true);
    float specified_kw = attribute_float(parser, "width", 0f);
    String script = parser.getAttributeValue(null, "script");
    String numpad_script = parser.getAttributeValue(null, "numpad_script");
    if (numpad_script == null)
      numpad_script = script;
    String name = parser.getAttributeValue(null, "name");
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
          throw error(parser, "Expecting tag <row>, got <" + parser.getName() + ">");
      }
    }
    float kw = (specified_kw != 0f) ? specified_kw : compute_max_width(rows);
    if (add_bottom_row)
      rows.add(bottom_row.updateWidth(kw));
    return new KeyboardData(rows, kw, modmap, script, numpad_script, name);
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
      throw error(parser, "Expected tag <row>");
    return Row.parse(parser);
  }

  protected KeyboardData(List<Row> rows_, float kw, Modmap mm, String sc,
      String npsc, String name_)
  {
    float kh = 0.f;
    for (Row r : rows_)
      kh += r.height + r.shift;
    rows = rows_;
    modmap = mm;
    script = sc;
    numpad_script = npsc;
    name = name_;
    keysWidth = kw;
    keysHeight = kh;
  }

  /** Copies the fields of an other keyboard, with rows changed. */
  protected KeyboardData(KeyboardData src, List<Row> rows)
  {
    this(rows, compute_max_width(rows), src.modmap, src.script,
        src.numpad_script, src.name);
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

    public Row copy()
    {
      return new Row(new ArrayList<Key>(keys), height, shift);
    }

    public void getKeys(Map<KeyValue, KeyPos> dst, int row)
    {
      for (int c = 0; c < keys.size(); c++)
        keys.get(c).getKeys(dst, row, c);
    }

    public Map<KeyValue, KeyPos> getKeys(int row)
    {
      Map<KeyValue, KeyPos> dst = new HashMap<KeyValue, KeyPos>();
      getKeys(dst, row);
      return dst;
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

    public Key get_key_at_pos(KeyPos pos)
    {
      if (pos.col >= keys.size())
        return null;
      return keys.get(pos.col);
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

    public void getKeys(Map<KeyValue, KeyPos> dst, int row, int col)
    {
      for (int i = 0; i < keys.length; i++)
        if (keys[i] != null)
          dst.put(keys[i], new KeyPos(row, col, i));
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

  /** Position of a key on the layout. */
  public final static class KeyPos
  {
    public final int row;
    public final int col;
    public final int dir;

    public KeyPos(int r, int c, int d)
    {
      row = r;
      col = c;
      dir = d;
    }

    public KeyPos with_dir(int d)
    {
      return new KeyPos(row, col, d);
    }
  }

  /** See [addExtraKeys()]. */
  public final static class PreferredPos
  {
    public static final PreferredPos DEFAULT;
    public static final PreferredPos ANYWHERE;

    /** Prefer the free position on the same keyboard key as the specified key.
        Considered before [positions]. Might be [null]. */
    public KeyValue next_to = null;

    /** Array of positions to try in order. The special value [-1] as [row],
        [col] or [dir] means that the field is unspecified. Every possible
        values are tried for unspecified fields. Unspecified fields are
        searched in this order: [dir], [col], [row]. */
    public KeyPos[] positions = ANYWHERE_POSITIONS;

    public PreferredPos() {}

    public PreferredPos(PreferredPos src)
    {
      next_to = src.next_to;
      positions = src.positions;
    }

    static final KeyPos[] ANYWHERE_POSITIONS =
      new KeyPos[]{ new KeyPos(-1, -1, -1) };

    static
    {
      DEFAULT = new PreferredPos();
      DEFAULT.positions = new KeyPos[]{
        new KeyPos(1, -1, 4),
        new KeyPos(1, -1, 3),
        new KeyPos(2, -1, 2),
        new KeyPos(2, -1, 1)
      };
      ANYWHERE = new PreferredPos();
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
      throw error(parser, "Expecting tag <" + name + ">, got <" +
          parser.getName() + ">");
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

  /** Construct a parsing error. */
  private static Exception error(XmlPullParser parser, String message)
  {
    return new Exception(message + " " + parser.getPositionDescription());
  }
}
