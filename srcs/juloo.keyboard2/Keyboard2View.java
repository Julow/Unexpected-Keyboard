package juloo.keyboard2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import java.util.Vector;

public class Keyboard2View extends View
	implements View.OnTouchListener
{
	private static final float	KEY_PER_ROW = 10;

	private Keyboard2		_ime;
	private KeyboardData	_keyboard;

	private Vector<KeyDown>	_downKeys;

	private float			_verticalMargin;
	private float			_horizontalMargin;
	private float			_keyWidth;
	private float			_keyHeight;
	private float			_keyPadding;
	private float			_keyBgPadding;
	private float			_keyRound;

	private Paint			_keyBgPaint;
	private Paint			_keyDownBgPaint;
	private Paint			_keyLabelPaint;
	private Paint			_keySubLabelPaint;

	public Keyboard2View(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		_downKeys = new Vector<KeyDown>();
		_verticalMargin = getResources().getDimension(R.dimen.vertical_margin);
		_horizontalMargin = getResources().getDimension(R.dimen.horizontal_margin);
		_keyHeight = getResources().getDimension(R.dimen.key_height);
		_keyPadding = getResources().getDimension(R.dimen.key_padding);
		_keyBgPadding = getResources().getDimension(R.dimen.key_bg_padding);
		_keyRound = getResources().getDimension(R.dimen.key_round);
		_keyWidth = (dm.widthPixels - (_horizontalMargin * 2)) / KEY_PER_ROW;
		_keyBgPaint = new Paint();
		_keyBgPaint.setColor(getResources().getColor(R.color.key_bg));
		_keyDownBgPaint = new Paint();
		_keyDownBgPaint.setColor(getResources().getColor(R.color.key_down_bg));
		_keyLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		_keyLabelPaint.setColor(getResources().getColor(R.color.key_label));
		_keyLabelPaint.setTextSize(getResources().getDimension(R.dimen.label_text_size));
		_keyLabelPaint.setTextAlign(Paint.Align.CENTER);
		_keySubLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
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

	private boolean		isKeyDown(KeyboardData.Key key)
	{
		for (KeyDown k : _downKeys)
		{
			if (k.key == key)
				return (true);
		}
		return (false);
	}

	private void		onTouchMove(float moveX, float moveY, int pointerId)
	{
		KeyDown				k = getKeyDown(pointerId);

		if (k != null)
		{
			k.updateDown(moveX, moveY);
		}
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
					_downKeys.add(new KeyDown(pointerId, key, touchX, touchY));
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
			if (k.value != null)
				_ime.handleKeyUp(k.value);
			_downKeys.remove(k);
			invalidate();
			return ;
		}
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

		y = _verticalMargin;
		for (KeyboardData.Row row : _keyboard.getRows())
		{
			x = (KEY_PER_ROW * _keyWidth - row.getWidth(_keyWidth)) / 2 + _horizontalMargin;
			for (KeyboardData.Key k : row)
			{
				float keyW = _keyWidth * k.width;
				if (isKeyDown(k))
					canvas.drawRect(x + _keyBgPadding, y + _keyBgPadding,
						x + keyW - _keyBgPadding, y + _keyHeight - _keyBgPadding, _keyDownBgPaint);
				else
					canvas.drawRoundRect(new RectF(x + _keyBgPadding, y + _keyBgPadding,
						x + keyW - _keyBgPadding, y + _keyHeight - _keyBgPadding), _keyRound, _keyRound, _keyBgPaint);
				if (k.key0 != null)
					canvas.drawText(k.key0.getSymbol(), keyW / 2 + x,
						(_keyHeight + _keyLabelPaint.getTextSize()) / 2 + y, _keyLabelPaint);
				float textOffsetY = _keySubLabelPaint.getTextSize() / 2;
				float subPadding = _keyPadding + _keyBgPadding;
				if (k.key1 != null)
					canvas.drawText(k.key1.getSymbol(), x + subPadding,
						y + subPadding + textOffsetY, _keySubLabelPaint);
				if (k.key2 != null)
					canvas.drawText(k.key2.getSymbol(), x + keyW - subPadding,
						y + subPadding + textOffsetY, _keySubLabelPaint);
				textOffsetY /= 2; // lol
				if (k.key3 != null)
					canvas.drawText(k.key3.getSymbol(), x + subPadding,
						y + _keyHeight - subPadding + textOffsetY, _keySubLabelPaint);
				if (k.key4 != null)
					canvas.drawText(k.key4.getSymbol(), x + keyW - subPadding,
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

		public KeyDown(int pointerId, KeyboardData.Key key, float x, float y)
		{
			this.pointerId = pointerId;
			value = key.key0;
			this.key = key;
			downX = x;
			downY = y;
		}

		public boolean			updateDown(float x, float y)
		{
			KeyValue		newValue = getDownValue(x - downX, y - downY);

			if (newValue != null && newValue != value)
			{
				value = newValue;
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
