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

public final class KeyboardData
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
  /** Whether the bottom row should be added. */
  public final boolean bottom_row;
  /** Whether the number row is included in the layout and thus another one shouldn't be added. */
  public final boolean embedded_number_row;
  /** Whether extra keys from [method.xml] should be added to this layout. */
  public final boolean locale_extra_keys;
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
      // Use preferred direction if some preferred pos match
      if (next_to_pos != null)
      {
        for (KeyPos p : pos.positions)
          if ((p.row == -1 || p.row == next_to_pos.row)
              && (p.col == -1 || p.col == next_to_pos.col)
              && add_key_to_pos(rows, kv, next_to_pos.with_dir(p.dir)))
            return true;
        if (add_key_to_pos(rows, kv, next_to_pos.with_dir(-1)))
          return true;
      }
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
    int i_row_end = Math.min(p.row, rows.size() - 1);
    if (p.row == -1) { i_row = 0; i_row_end = rows.size() - 1; }
    for (; i_row <= i_row_end; i_row++)
    {
      Row row = rows.get(i_row);
      int i_col = p.col;
      int i_col_end = Math.min(p.col, row.keys.size() - 1);
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

  /** Insert the given row at the given indice. The row is scaled so that the
      keys already on the keyboard don't change width. */
  public KeyboardData insert_row(Row row, int i)
  {
    ArrayList<Row> rows_ = new ArrayList<Row>(this.rows);
    rows_.add(i, row.updateWidth(keysWidth));
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

  private static Map<Integer, KeyboardData> _layoutCache = new HashMap<Integer, KeyboardData>();

  public static Row load_row(Resources res, int res_id) throws Exception
  {
    return parse_row(res.getXml(res_id));
  }

  public static KeyboardData load_num_pad(Resources res) throws Exception
  {
    return parse_keyboard(res.getXml(R.xml.numpad));
  }

  /** Load a layout from a resource ID. Returns [null] on error. */
  public static KeyboardData load(Resources res, int id)
  {
    if (_layoutCache.containsKey(id))
      return _layoutCache.get(id);
    KeyboardData l = null;
    XmlResourceParser parser = null;
    try
    {
      parser = res.getXml(id);
      l = parse_keyboard(parser);
    }
    catch (Exception e)
    {
      Logs.exn("Failed to load layout id " + id, e);
    }
    if (parser != null)
      parser.close();
    _layoutCache.put(id, l);
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
    boolean bottom_row = attribute_bool(parser, "bottom_row", true);
    boolean embedded_number_row = attribute_bool(parser, "embedded_number_row", false);
    boolean locale_extra_keys = attribute_bool(parser, "locale_extra_keys", true);
    float specified_kw = attribute_float(parser, "width", 0f);
    String script = parser.getAttributeValue(null, "script");
    if (script != null && script.equals(""))
      throw error(parser, "'script' attribute cannot be empty");
    String numpad_script = parser.getAttributeValue(null, "numpad_script");
    if (numpad_script == null)
      numpad_script = script;
    else if (numpad_script.equals(""))
      throw error(parser, "'numpad_script' attribute cannot be empty");
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
          if (modmap != null)
            throw error(parser, "Multiple '<modmap>' are not allowed");
          modmap = parse_modmap(parser);
          break;
        default:
          throw error(parser, "Expecting tag <row>, got <" + parser.getName() + ">");
      }
    }
    float kw = (specified_kw != 0f) ? specified_kw : compute_max_width(rows);
    return new KeyboardData(rows, kw, modmap, script, numpad_script, name, bottom_row, embedded_number_row, locale_extra_keys);
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
      String npsc, String name_, boolean bottom_row_, boolean embedded_number_row_, boolean locale_extra_keys_)
  {
    float kh = 0.f;
    for (Row r : rows_)
      kh += r.height + r.shift;
    rows = rows_;
    modmap = mm;
    script = sc;
    numpad_script = npsc;
    name = name_;
    keysWidth = Math.max(kw, 1f);
    keysHeight = kh;
    bottom_row = bottom_row_;
    embedded_number_row = embedded_number_row_;
    locale_extra_keys = locale_extra_keys_;
  }

  /** Copies the fields of a keyboard, with rows changed. */
  protected KeyboardData(KeyboardData src, List<Row> rows)
  {
    this(rows, compute_max_width(rows), src.modmap, src.script,
        src.numpad_script, src.name, src.bottom_row, src.embedded_number_row, src.locale_extra_keys);
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
      height = Math.max(h, 0.5f);
      shift = Math.max(s, 0f);
      keysWidth = kw;
    }

    public static Row parse(XmlPullParser parser) throws Exception
    {
      ArrayList<Key> keys = new ArrayList<Key>();
      int status;
      float h = attribute_float(parser, "height", 1f);
      float shift = attribute_float(parser, "shift", 0f);
      float scale = attribute_float(parser, "scale", 0f);
      while (expect_tag(parser, "key"))
        keys.add(Key.parse(parser));
      Row row = new Row(keys, h, shift);
      if (scale > 0f)
        row = row.updateWidth(scale);
      return row;
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
    /** Key accessed by the anti-clockwise circle gesture. */
    public final KeyValue anticircle;
    /** Pack flags for every key values. Flags are: [F_LOC]. */
    private final int keysflags;
    /** Key width in relative unit. */
    public final float width;
    /** Extra empty space on the left of the key. */
    public final float shift;
    /** String printed on the keys. It has no other effect. */
    public final String indication;

    /** Whether a key was declared with the 'loc' prefix. */
    public static final int F_LOC = 1;
    public static final int ALL_FLAGS = F_LOC;

    protected Key(KeyValue[] ks, KeyValue antic, int f, float w, float s, String i)
    {
      keys = ks;
      anticircle = antic;
      keysflags = f;
      width = Math.max(w, 0f);
      shift = Math.max(s, 0f);
      indication = i;
    }

    static final Key EMPTY = new Key(new KeyValue[9], null, 0, 1.f, 1.f, null);

    /** Read a key value attribute that have a synonym. Having both synonyms
        present at the same time is an error.
        Returns [null] if the attributes are not present. */
    static String get_key_attr(XmlPullParser parser, String syn1, String syn2)
        throws Exception
    {
      String name1 = parser.getAttributeValue(null, syn1);
      String name2 = parser.getAttributeValue(null, syn2);
      if (name1 != null && name2 != null)
        throw error(parser,
            "'"+syn1+"' and '"+syn2+"' are synonyms and cannot be passed at the same time.");
      return (name1 == null) ? name2 : name1;
    }

    /** Parse the key description [key_attr] and write into [ks] at [index].
        Returns flags that can be aggregated into the value for [keysflags].
        [key_attr] can be [null] for convenience. */
    static int parse_key_attr(XmlPullParser parser, String key_val, KeyValue[] ks,
        int index)
        throws Exception
    {
      if (key_val == null)
        return 0;
      int flags = 0;
      String name_loc = stripPrefix(key_val, "loc ");
      if (name_loc != null)
      {
        flags |= F_LOC;
        key_val = name_loc;
      }
      ks[index] = KeyValue.getKeyByName(key_val);
      return (flags << index);
    }

    static KeyValue parse_nonloc_key_attr(XmlPullParser parser, String attr_name) throws Exception
    {
      String name = parser.getAttributeValue(null, attr_name);
      if (name == null)
        return null;
      return KeyValue.getKeyByName(name);
    }

    static String stripPrefix(String s, String prefix)
    {
      return s.startsWith(prefix) ? s.substring(prefix.length()) : null;
    }

    public static Key parse(XmlPullParser parser) throws Exception
    {
      KeyValue[] ks = new KeyValue[9];
      int keysflags = 0;
      keysflags |= parse_key_attr(parser, get_key_attr(parser, "key0", "c"), ks, 0);
      /* Swipe gestures (key1-key8 diagram above), with compass-point synonyms. */
      keysflags |= parse_key_attr(parser, get_key_attr(parser, "key1", "nw"), ks, 1);
      keysflags |= parse_key_attr(parser, get_key_attr(parser, "key2", "ne"), ks, 2);
      keysflags |= parse_key_attr(parser, get_key_attr(parser, "key3", "sw"), ks, 3);
      keysflags |= parse_key_attr(parser, get_key_attr(parser, "key4", "se"), ks, 4);
      keysflags |= parse_key_attr(parser, get_key_attr(parser, "key5", "w"), ks, 5);
      keysflags |= parse_key_attr(parser, get_key_attr(parser, "key6", "e"), ks, 6);
      keysflags |= parse_key_attr(parser, get_key_attr(parser, "key7", "n"), ks, 7);
      keysflags |= parse_key_attr(parser, get_key_attr(parser, "key8", "s"), ks, 8);
      /* Other key attributes */
      KeyValue anticircle = parse_nonloc_key_attr(parser, "anticircle");
      float width = attribute_float(parser, "width", 1f);
      float shift = attribute_float(parser, "shift", 0.f);
      String indication = parser.getAttributeValue(null, "indication");
      while (parser.next() != XmlPullParser.END_TAG)
        continue;
      return new Key(ks, anticircle, keysflags, width, shift, indication);
    }

    /** Whether key at [index] as [flag]. */
    public boolean keyHasFlag(int index, int flag)
    {
      return (keysflags & (flag << index)) != 0;
    }

    /** New key with the width multiplied by 's'. */
    public Key scaleWidth(float s)
    {
      return new Key(keys, anticircle, keysflags, width * s, shift, indication);
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
      return new Key(ks, anticircle, flags, width, shift, indication);
    }

    public Key withShift(float s)
    {
      return new Key(keys, anticircle, keysflags, width, s, indication);
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
      return new Key(ks, k.anticircle, k.keysflags, k.width, k.shift, k.indication);
    }
  }

  public static Modmap parse_modmap(XmlPullParser parser) throws Exception
  {
    Modmap mm = new Modmap();
    while (next_tag(parser))
    {
      Modmap.M m;
      switch (parser.getName())
      {
        case "shift": m = Modmap.M.Shift; break;
        case "fn": m = Modmap.M.Fn; break;
        case "ctrl": m = Modmap.M.Ctrl; break;
        default:
          throw error(parser, "Expecting tag <shift> or <fn>, got <" +
              parser.getName() + ">");
      }
      parse_modmap_mapping(parser, mm, m);
    }
    return mm;
  }

  private static void parse_modmap_mapping(XmlPullParser parser, Modmap mm,
      Modmap.M m) throws Exception
  {
    KeyValue a = KeyValue.getKeyByName(parser.getAttributeValue(null, "a"));
    KeyValue b = KeyValue.getKeyByName(parser.getAttributeValue(null, "b"));
    while (parser.next() != XmlPullParser.END_TAG)
      continue;
    mm.add(m, a, b);
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
    /** Default position for extra keys. */
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
    public PreferredPos(KeyValue next_to_) { next_to = next_to_; }
    public PreferredPos(KeyPos[] pos) { positions = pos; }
    public PreferredPos(KeyValue next_to_, KeyPos[] pos) { next_to = next_to_; positions = pos; }

    public PreferredPos(PreferredPos src)
    {
      next_to = src.next_to;
      positions = src.positions;
    }

    static final KeyPos[] ANYWHERE_POSITIONS =
      new KeyPos[]{ new KeyPos(-1, -1, -1) };

    static
    {
      DEFAULT = new PreferredPos(new KeyPos[]{
          new KeyPos(1, -1, 4),
          new KeyPos(1, -1, 3),
          new KeyPos(2, -1, 2),
          new KeyPos(2, -1, 1)
        });
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
