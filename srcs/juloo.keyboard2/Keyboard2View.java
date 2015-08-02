package juloo.keyboard2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import java.util.LinkedList;

public class Keyboard2View extends View
	implements View.OnTouchListener
{
	private static final float	KEY_PER_ROW = 10;

	private Keyboard2		_ime;
	private KeyboardData	_keyboard;

	private LinkedList<KeyDown>	_downKeys = new LinkedList<KeyDown>();

	private int				_flags = 0;

	private float			_verticalMargin;
	private float			_horizontalMargin;
	private float			_keyWidth;
	private float			_keyHeight;
	private float			_keyPadding;
	private float			_keyBgPadding;
	private float			_keyRound;

	private Paint			_keyBgPaint = new Paint();
	private Paint			_keyDownBgPaint = new Paint();
	private Paint			_keyLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint			_keyLabelLockedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint			_keySubLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

	public Keyboard2View(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		_verticalMargin = getResources().getDimension(R.dimen.vertical_margin);
		_horizontalMargin = getResources().getDimension(R.dimen.horizontal_margin);
		_keyHeight = getResources().getDimension(R.dimen.key_height);
		_keyPadding = getResources().getDimension(R.dimen.key_padding);
		_keyBgPadding = getResources().getDimension(R.dimen.key_bg_padding);
		_keyRound = getResources().getDimension(R.dimen.key_round);
		_keyWidth = (dm.widthPixels - (_horizontalMargin * 2)) / KEY_PER_ROW;
		_keyBgPaint.setColor(getResources().getColor(R.color.key_bg));
		_keyDownBgPaint.setColor(getResources().getColor(R.color.key_down_bg));
		_keyLabelPaint.setColor(getResources().getColor(R.color.key_label));
		_keyLabelPaint.setTextSize(getResources().getDimension(R.dimen.label_text_size));
		_keyLabelPaint.setTextAlign(Paint.Align.CENTER);
		_keyLabelLockedPaint.setColor(getResources().getColor(R.color.key_label_locked));
		_keyLabelLockedPaint.setTextSize(getResources().getDimension(R.dimen.label_text_size));
		_keyLabelLockedPaint.setTextAlign(Paint.Align.CENTER);
		_keySubLabelPaint.setColor(getResources().getColor(R.color.key_sub_label));
		_keySubLabelPaint.setTextSize(getResources().getDimension(R.dimen.sublabel_text_size));
		_keySubLabelPaint.setTextAlign(Paint.Align.CENTER);
		setOnTouchListener(this);
	}

	public void			setKeyboard(Keyboard2 ime, KeyboardData keyboardData)
	{
		_ime = ime;
		_keyboard = keyboardData;
	}

	@Override
	public boolean		onTouch(View v, MotionEvent event)
	{
		float				x;
		float				y;
		float				keyW;
		int					p;

		switch (event.getActionMasked())
		{
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			onTouchUp(event.getPointerId(event.getActionIndex()));
			break ;
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_POINTER_DOWN:
			p = event.getActionIndex();
			onTouchDown(event.getX(p), event.getY(p), event.getPointerId(p));
			break ;
		case MotionEvent.ACTION_MOVE:
			for (p = 0; p < event.getPointerCount(); p++)
				onTouchMove(event.getX(p), event.getY(p), event.getPointerId(p));
			break ;
		default:
			return (false);
		}
		return (true);
	}

	private KeyDown		getKeyDown(int pointerId)
	{
		for (KeyDown k : _downKeys)
		{
			if (k.pointerId == pointerId)
				return (k);
		}
		return (null);
	}

	private KeyDown		getKeyDown(KeyboardData.Key key)
	{
		for (KeyDown k : _downKeys)
		{
			if (k.key == key)
				return (k);
		}
		return (null);
	}

	private void		onTouchMove(float moveX, float moveY, int pointerId)
	{
		KeyDown				k = getKeyDown(pointerId);

		if (k != null && k.updateDown(moveX, moveY))
			updateFlags();
	}

	private void		onTouchDown(float touchX, float touchY, int pointerId)
	{
		float				x;
		float				y;
		float				keyW;

		y = _verticalMargin - _keyHeight;
		for (KeyboardData.Row row : _keyboard.getRows())
		{
			y += _keyHeight;
			if (touchY < y || touchY >= (y + _keyHeight))
				continue ;
			x = (KEY_PER_ROW * _keyWidth - row.getWidth(_keyWidth)) / 2 + _horizontalMargin;
			for (KeyboardData.Key key : row)
			{
				keyW = _keyWidth * key.width;
				if (touchX >= x && touchX < (x + keyW))
				{
					KeyDown down = getKeyDown(key);
					if (down != null)
					{
						if ((down.flags & KeyValue.FLAG_LOCK) != 0)
						{
							down.flags ^= KeyValue.FLAG_LOCK;
							down.flags |= KeyValue.FLAG_LOCKED;
						}
						else if (down.pointerId == -1)
							down.pointerId = pointerId;
					}
					else
						_downKeys.add(new KeyDown(pointerId, key, touchX, touchY));
					updateFlags();
					invalidate();
					return ;
				}
				x += keyW;
			}
		}
	}

	private void		onTouchUp(int pointerId)
	{
		KeyDown				k = getKeyDown(pointerId);

		if (k != null)
		{
			if ((k.flags & KeyValue.FLAG_KEEP_ON) != 0)
			{
				k.flags ^= KeyValue.FLAG_KEEP_ON;
				k.pointerId = -1;
				return ;
			}
			for (int i = 0; i < _downKeys.size(); i++)
			{
				KeyDown downKey = _downKeys.get(i);
				if (downKey.pointerId == -1 && (downKey.flags & KeyValue.FLAG_LOCKED) == 0)
					_downKeys.remove(i--);
				else if ((downKey.flags & KeyValue.FLAG_KEEP_ON) != 0)
					downKey.flags ^= KeyValue.FLAG_KEEP_ON;
			}
			if (k.value != null && (k.flags & (KeyValue.FLAG_LOCKED | KeyValue.FLAG_NOCHAR)) == 0)
				_ime.handleKeyUp(k.value, _flags);
			_downKeys.remove(k);
			updateFlags();
			invalidate();
			return ;
		}
	}

	private void		updateFlags()
	{
		_flags = 0;
		for (KeyDown k : _downKeys)
			_flags |= k.flags;
	}

	@Override
	public void			onMeasure(int wSpec, int hSpec)
	{
		int					height;

		if (_keyboard.getRows() == null)
			height = 0;
		else
			height = (int)(_keyHeight * ((float)_keyboard.getRows().size())
				+ (_verticalMargin * 2));
		setMeasuredDimension(MeasureSpec.getSize(wSpec), height);
	}

	@Override
	protected void		onDraw(Canvas canvas)
	{
		float				x;
		float				y;
		boolean				upperCase = ((_flags & KeyValue.FLAG_SHIFT) != 0);

		y = _verticalMargin;
		for (KeyboardData.Row row : _keyboard.getRows())
		{
			x = (KEY_PER_ROW * _keyWidth - row.getWidth(_keyWidth)) / 2 + _horizontalMargin;
			for (KeyboardData.Key k : row)
			{
				float keyW = _keyWidth * k.width;
				KeyDown keyDown = getKeyDown(k);
				if (keyDown != null)
					canvas.drawRect(x + _keyBgPadding, y + _keyBgPadding,
						x + keyW - _keyBgPadding, y + _keyHeight - _keyBgPadding, _keyDownBgPaint);
				else
					canvas.drawRoundRect(new RectF(x + _keyBgPadding, y + _keyBgPadding,
						x + keyW - _keyBgPadding, y + _keyHeight - _keyBgPadding), _keyRound, _keyRound, _keyBgPaint);
				if (k.key0 != null)
					canvas.drawText(k.key0.getSymbol(upperCase), keyW / 2 + x,
						(_keyHeight + _keyLabelPaint.getTextSize()) / 2 + y,
						(keyDown != null && (keyDown.flags & KeyValue.FLAG_LOCKED) != 0)
							? _keyLabelLockedPaint : _keyLabelPaint);
				float textOffsetY = _keySubLabelPaint.getTextSize() / 2;
				float subPadding = _keyPadding + _keyBgPadding;
				if (k.key1 != null)
					canvas.drawText(k.key1.getSymbol(upperCase), x + subPadding,
						y + subPadding + textOffsetY, _keySubLabelPaint);
				if (k.key2 != null)
					canvas.drawText(k.key2.getSymbol(upperCase), x + keyW - subPadding,
						y + subPadding + textOffsetY, _keySubLabelPaint);
				textOffsetY /= 2; // lol
				if (k.key3 != null)
					canvas.drawText(k.key3.getSymbol(upperCase), x + subPadding,
						y + _keyHeight - subPadding + textOffsetY, _keySubLabelPaint);
				if (k.key4 != null)
					canvas.drawText(k.key4.getSymbol(upperCase), x + keyW - subPadding,
						y + _keyHeight - subPadding + textOffsetY, _keySubLabelPaint);
				x += keyW;
			}
			y += _keyHeight;
		}
	}

	private class KeyDown
	{
		private static final float	SUB_VALUE_DIST = 6f;

		public int				pointerId;
		public KeyValue			value;
		public KeyboardData.Key	key;
		public float			downX;
		public float			downY;
		public int				flags;

		public KeyDown(int pointerId, KeyboardData.Key key, float x, float y)
		{
			this.pointerId = pointerId;
			value = key.key0;
			this.key = key;
			downX = x;
			downY = y;
			flags = (value == null) ? 0 : value.getFlags();
		}

		public boolean			updateDown(float x, float y)
		{
			KeyValue		newValue = getDownValue(x - downX, y - downY);

			if (newValue != null && newValue != value)
			{
				value = newValue;
				flags = newValue.getFlags();
				return (true);
			}
			return (false);
		}

		private KeyValue		getDownValue(float x, float y)
		{
			if ((Math.abs(x) + Math.abs(y)) < SUB_VALUE_DIST)
				return (key.key0);
			if (x < 0)
			{
				if (y < 0)
					return (key.key1);
				return (key.key3);
			}
			else if (y < 0)
				return (key.key2);
			return (key.key4);
		}
	}
}
