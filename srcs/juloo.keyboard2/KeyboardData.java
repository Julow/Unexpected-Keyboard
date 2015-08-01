package juloo.keyboard2;

import android.content.res.XmlResourceParser;
import java.util.ArrayList;

class KeyboardData
{
	private ArrayList<Row>	_rows;

	public KeyboardData(XmlResourceParser parser)
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
						rows.add(new Row(parser));
					else
						throw new Exception("Unknow keyboard tag: " + tag);
				}
			}
			_rows = rows;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public ArrayList<Row>	getRows()
	{
		return (_rows);
	}

	public class Row extends ArrayList<Key>
	{
		private float		_keysWidth;

		public Row(XmlResourceParser parser) throws Exception
		{
			super();

			int status;
			_keysWidth = 0;
			while ((status = parser.next()) != XmlResourceParser.END_TAG)
			{
				if (status == XmlResourceParser.START_TAG)
				{
					String tag = parser.getName();
					if (tag.equals("key"))
					{
						Key k = new Key(parser);
						_keysWidth += k.width;
						add(k);
					}
					else
						throw new Exception("Unknow row tag: " + tag);
				}
			}
		}

		public float		getWidth(float keyWidth)
		{
			return (keyWidth * _keysWidth);
		}
	}

	public class Key
	{
		/*
		** 1   2
		**   0
		** 3   4
		*/
		public KeyValue		key0;
		public KeyValue		key1;
		public KeyValue		key2;
		public KeyValue		key3;
		public KeyValue		key4;

		public float		width;

		public Key(XmlResourceParser parser) throws Exception
		{
			key0 = KeyValue.getKeyByName(parser.getAttributeValue(null, "key0"));
			key1 = KeyValue.getKeyByName(parser.getAttributeValue(null, "key1"));
			key2 = KeyValue.getKeyByName(parser.getAttributeValue(null, "key2"));
			key3 = KeyValue.getKeyByName(parser.getAttributeValue(null, "key3"));
			key4 = KeyValue.getKeyByName(parser.getAttributeValue(null, "key4"));
			try
			{
				width = parser.getAttributeFloatValue(null, "width", 1f);
			}
			catch (Exception e)
			{
				width = 1f;
			}
			while (parser.next() != XmlResourceParser.END_TAG)
				continue ;
		}
	}
}
