package juloo.keyboard2;

import android.content.res.XmlResourceParser;
import java.util.ArrayList;
import java.util.List;

class KeyboardData
{
	private final List<Row> _rows;
  private final float _keysWidth;

  public KeyboardData(List<Row> rows)
  {
    float kpr = 0.f;
    for (Row r : rows) kpr = Math.max(kpr, r.keysWidth());
    _rows = rows;
    _keysWidth = kpr;
  }

	public static KeyboardData parse(XmlResourceParser parser)
	{
		ArrayList<Row> rows = new ArrayList<Row>();

		try
		{
			int status;

			while (parser.next() != XmlResourceParser.START_TAG)
				continue ;
			if (!parser.getName().equals("keyboard"))
				throw new Exception("Unknow tag: " + parser.getName());
			while ((status = parser.next()) != XmlResourceParser.END_DOCUMENT)
			{
				if (status == XmlResourceParser.START_TAG)
				{
					String tag = parser.getName();
					if (tag.equals("row"))
						rows.add(Row.parse(parser));
					else
						throw new Exception("Unknow keyboard tag: " + tag);
				}
			}
      return new KeyboardData(rows);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
    return new KeyboardData(rows);
	}

	public List<Row>	getRows() { return _rows; }

  public float getKeysWidth() { return _keysWidth; }

  public KeyboardData removeKeys(MapKeys f)
  {
		ArrayList<Row> rows = new ArrayList<Row>();
    for (Row r : _rows)
      rows.add(r.removeKeys(f));
    return new KeyboardData(rows);
  }

	public static class Row
	{
    private final List<Key> _keys;
    /* Total width of very keys. Unit is abstract. */
		private final float _keysWidth;

    public Row(List<Key> keys)
    {
      float kw = 0.f;
      for (Key k : keys) kw += k.width + k.shift;
      _keys = keys;
      _keysWidth = kw;
    }

		public static Row parse(XmlResourceParser parser) throws Exception
		{
      ArrayList<Key> keys = new ArrayList<Key>();
			int status;
			while ((status = parser.next()) != XmlResourceParser.END_TAG)
			{
				if (status == XmlResourceParser.START_TAG)
				{
					String tag = parser.getName();
					if (tag.equals("key"))
            keys.add(Key.parse(parser));
					else
						throw new Exception("Unknow row tag: " + tag);
				}
			}
      return new Row(keys);
		}

    public List<Key> getKeys() { return _keys; }

    public float keysWidth() { return _keysWidth; }

    public Row removeKeys(MapKeys f)
    {
      ArrayList<Key> keys = new ArrayList<Key>();
      for (Key k : _keys)
        keys.add(k.removeKeys(f));
      return new Row(keys);
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

    /* Key width in relative unit. */
    public final float width;
    /* Extra empty space on the left of the key. */
    public final float shift;

    public Key(KeyValue k0, KeyValue k1, KeyValue k2, KeyValue k3, KeyValue k4, float w, float s)
    {
      key0 = k0;
      key1 = k1;
      key2 = k2;
      key3 = k3;
      key4 = k4;
      width = w;
      shift = s;
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
      while (parser.next() != XmlResourceParser.END_TAG)
        continue ;
      return new Key(k0, k1, k2, k3, k4, width, shift);
    }

    public Key removeKeys(MapKeys f)
    {
      return new Key(f.map(key0), f.map(key1), f.map(key2), f.map(key3), f.map(key4), width, shift);
    }
  }

  public static abstract interface MapKeys
  {
    public abstract KeyValue map(KeyValue k);
  }

  public static class RemoveKeysByFlags implements MapKeys
  {
    private final int _flags;

    public RemoveKeysByFlags(int flags) { _flags = flags; }

    public KeyValue map(KeyValue k)
    {
      return (k == null || (k.getFlags() & _flags) != 0) ? null : k;
    }
  }

  public static class RemoveKeysByEvent implements MapKeys
  {
    private final int _eventCode;

    public RemoveKeysByEvent(int ev) { _eventCode = ev; }

    public KeyValue map(KeyValue k)
    {
      return (k == null || k.getEventCode() == _eventCode) ? null : k;
    }
  }
}
