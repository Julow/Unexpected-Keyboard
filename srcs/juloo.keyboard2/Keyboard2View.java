package juloo.keyboard2;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;

public class Keyboard2View extends View
	implements View.OnTouchListener
{
	private static final float	KEY_PER_ROW = 10;
	private static final float	KEY_MARGIN_DPI = 2;
	private static final float	KEY_PADDING_DPI = 6;
	private static final float	KEY_HEIGHT_DPI = 40;
	private static final float	KEY_LABEL_DPI = 16;
	private static final float	KEY_SUBLABEL_DPI = 12;

	private ArrayList<Row>	_rows;

	private float			_keyWidth;
	private float			_keyHeight;
	private float			_keyMargin;
	private float			_keyPadding;

	private Paint			_keyBgPaint;
	private Paint			_keyDownBgPaint;
	private Paint			_keyLabelPaint;
	private Paint			_keySubLabelPaint;

	public Keyboard2View(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		_rows = null;
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		_keyMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, KEY_MARGIN_DPI, dm);
		_keyHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, KEY_HEIGHT_DPI, dm);
		_keyPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, KEY_PADDING_DPI, dm);
		_keyWidth = (dm.widthPixels - _keyMargin) / KEY_PER_ROW - _keyMargin;
		_keyBgPaint = new Paint();
		_keyBgPaint.setColor(getResources().getColor(R.color.key_bg));
		_keyDownBgPaint = new Paint();
		_keyDownBgPaint.setColor(getResources().getColor(R.color.key_down_bg));
		_keyLabelPaint = new Paint();
		_keyLabelPaint.setColor(getResources().getColor(R.color.key_label));
		_keyLabelPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, KEY_LABEL_DPI, dm));
		_keyLabelPaint.setTextAlign(Paint.Align.CENTER);
		_keySubLabelPaint = new Paint();
		_keySubLabelPaint.setColor(getResources().getColor(R.color.key_label));
		_keySubLabelPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, KEY_SUBLABEL_DPI, dm));
		_keySubLabelPaint.setTextAlign(Paint.Align.CENTER);
		setOnTouchListener(this);
	}

	public void			loadKeyboard(int res)
	{
		XmlResourceParser parser = getContext().getResources().getXml(res);
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
						rows.add(new Row(parser, _keyWidth, _keyMargin));
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

	@Override
	public boolean		onTouch(View v, MotionEvent event)
	{
		String log;

		log = "Action: ";
		switch (event.getAction())
		{
		case MotionEvent.ACTION_POINTER_DOWN:
			log += "NEW POINTER DOWN";
			break ;
		case MotionEvent.ACTION_POINTER_UP:
			log += "NEW POINTER UP";
			break ;
		case MotionEvent.ACTION_DOWN:
			log += "DOWN";
			break ;
		case MotionEvent.ACTION_UP:
			log += "UP";
			break ;
		case MotionEvent.ACTION_MOVE:
			log += "MOVE";
			break ;
		case MotionEvent.ACTION_CANCEL:
			log += "CANCEL";
			break ;
		default:
			log += "UNKNOW";
			break ;
		}
		for (int p = 0; p < event.getPointerCount(); p++)
		{
			log += ", P#";
			log += String.valueOf(p);
			log += ": ";
			log += String.valueOf(event.getX(p));
			log += "/";
			log += String.valueOf(event.getY(p));
		}
		Keyboard2.log(log);
		return (true);
	}

	private Key			getKeyAt(float at_x, float at_y)
	{
		float				x;
		float				y;

		y = _keyMargin;
		for (Row row : _rows)
		{
			x = (KEY_PER_ROW * (_keyMargin + _keyWidth) - _keyMargin - row.getWidth()) / 2 + _keyMargin;
			for (Key k : row)
			{
				float keyW = _keyWidth * k.width;
				if (at_x >= x && at_x < (x + keyW)
					&& at_y >= y && at_y < (y + _keyHeight))
					return (k);
				x += keyW + _keyMargin;
			}
			y += _keyHeight + _keyMargin;
		}
		return (null);
	}

	@Override
	public void			onMeasure(int wSpec, int hSpec)
	{
		int					height;

		if (_rows == null)
			height = 0;
		else
			height = (int)((_keyHeight + _keyMargin) * (float)_rows.size() + _keyMargin);
		setMeasuredDimension(MeasureSpec.getSize(wSpec), height);
	}

	@Override
	protected void		onDraw(Canvas canvas)
	{
		float				x;
		float				y;

		if (_rows == null)
			return ;
		y = _keyMargin;
		for (Row row : _rows)
		{
			x = (KEY_PER_ROW * (_keyMargin + _keyWidth) - _keyMargin - row.getWidth()) / 2 + _keyMargin;
			for (Key k : row)
			{
				float keyW = _keyWidth * k.width;
				if (k.down)
					canvas.drawRect(x, y, x + keyW, y + _keyHeight, _keyBgPaint);
				else
					canvas.drawRect(x, y, x + keyW, y + _keyHeight, _keyDownBgPaint);
				if (k.key0 != null)
					canvas.drawText(new char[]{k.key0.getChar()}, 0, 1,
						keyW / 2 + x, (_keyHeight + _keyLabelPaint.getTextSize()) / 2 + y, _keyLabelPaint);
				if (k.key1 != null)
					canvas.drawText(new char[]{k.key1.getChar()}, 0, 1,
						x + _keyPadding, y + _keyPadding, _keySubLabelPaint);
				if (k.key2 != null)
					canvas.drawText(new char[]{k.key2.getChar()}, 0, 1,
						x + keyW - _keyPadding, y + _keyPadding, _keySubLabelPaint);
				if (k.key3 != null)
					canvas.drawText(new char[]{k.key3.getChar()}, 0, 1,
						x + _keyPadding, y + _keyHeight - _keyPadding, _keySubLabelPaint);
				if (k.key4 != null)
					canvas.drawText(new char[]{k.key4.getChar()}, 0, 1,
						x + keyW - _keyPadding, y + _keyHeight - _keyPadding, _keySubLabelPaint);
				x += keyW + _keyMargin;
			}
			y += _keyHeight + _keyMargin;
		}
	}

	private class Row extends ArrayList<Key>
	{
		private float		_width;

		public Row(XmlResourceParser parser, float keyWidth, float keyMargin) throws Exception
		{
			super();

			int status;
			_width = 0;
			while ((status = parser.next()) != XmlResourceParser.END_TAG)
			{
				if (status == XmlResourceParser.START_TAG)
				{
					String tag = parser.getName();
					if (tag.equals("key"))
					{
						Key k = new Key(parser);
						if (_width != 0f)
							_width += keyMargin;
						_width += keyWidth * k.width;
						add(k);
					}
					else
						throw new Exception("Unknow row tag: " + tag);
				}
			}
		}

		public float		getWidth()
		{
			return (_width);
		}
	}

	private class Key
	{
		public KeyValue		key0;
		public KeyValue		key1;
		public KeyValue		key2;
		public KeyValue		key3;
		public KeyValue		key4;

		public float		width;

		public boolean		down;

		public Key(XmlResourceParser parser) throws Exception
		{
			down = false;
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
