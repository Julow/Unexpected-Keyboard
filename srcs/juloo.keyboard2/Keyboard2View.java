package juloo.keyboard2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

public class Keyboard2View extends View
	implements View.OnTouchListener
{
	private static final float	KEY_PER_ROW = 10;
	private static final float	KEY_MARGIN_DPI = 2;
	private static final float	KEY_PADDING_DPI = 5;
	private static final float	KEY_HEIGHT_DPI = 40;
	private static final float	KEY_LABEL_DPI = 16;
	private static final float	KEY_SUBLABEL_DPI = 10;

	private Keyboard2		_ime;
	private KeyboardData	_keyboard;

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
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		_keyMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, KEY_MARGIN_DPI, dm);
		_keyHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, KEY_HEIGHT_DPI, dm);
		_keyPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, KEY_PADDING_DPI, dm);
		_keyWidth = (dm.widthPixels - _keyMargin) / KEY_PER_ROW - _keyMargin;
		_keyBgPaint = new Paint();
		_keyBgPaint.setColor(getResources().getColor(R.color.key_bg));
		_keyDownBgPaint = new Paint();
		_keyDownBgPaint.setColor(getResources().getColor(R.color.key_down_bg));
		_keyLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		_keyLabelPaint.setColor(getResources().getColor(R.color.key_label));
		_keyLabelPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, KEY_LABEL_DPI, dm));
		_keyLabelPaint.setTextAlign(Paint.Align.CENTER);
		_keySubLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		_keySubLabelPaint.setColor(getResources().getColor(R.color.key_sub_label));
		_keySubLabelPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, KEY_SUBLABEL_DPI, dm));
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

	private void		onTouchMove(float moveX, float moveY, int pointerId)
	{
		for (KeyboardData.Row row : _keyboard.getRows())
		{
			for (KeyboardData.Key k : row)
			{
				if (k.downPointer == pointerId)
				{
					KeyValue v = k.getDownValue(moveX, moveY);
					if (v != k.downValue)
					{
						k.downValue = v;
						Keyboard2.log("Key down " + v.getName());
					}
				}
			}
		}
	}

	private void		onTouchDown(float touchX, float touchY, int pointerId)
	{
		float				x;
		float				y;
		float				keyW;

		y = -_keyHeight;
		for (KeyboardData.Row row : _keyboard.getRows())
		{
			y += _keyHeight + _keyMargin;
			if (touchY < y || touchY >= (y + _keyHeight))
				continue ;
			x = (KEY_PER_ROW * (_keyMargin + _keyWidth) - _keyMargin - row.getWidth(_keyWidth, _keyMargin)) / 2 + _keyMargin;
			for (KeyboardData.Key k : row)
			{
				keyW = _keyWidth * k.width;
				if (touchX >= x && touchX < (x + keyW) && k.downPointer == -1)
				{
					if (k.key0 != null)
						Keyboard2.log("Key down " + k.key0.getName());
					k.downPointer = pointerId;
					k.downValue = k.key0;
					k.downX = touchX;
					k.downY = touchY;
					invalidate();
					return ;
				}
				x += keyW + _keyMargin;
			}
		}
	}

	private void		onTouchUp(int pointerId)
	{
		for (KeyboardData.Row row : _keyboard.getRows())
		{
			for (KeyboardData.Key k : row)
			{
				if (k.downPointer == pointerId)
				{
					if (k.downValue != null)
						Keyboard2.log("Key up " + k.downValue.getName());
					k.downPointer = -1;
					invalidate();
				}
			}
		}
	}

	@Override
	public void			onMeasure(int wSpec, int hSpec)
	{
		int					height;

		if (_keyboard.getRows() == null)
			height = 0;
		else
			height = (int)((_keyHeight + _keyMargin) * ((float)_keyboard.getRows().size()) + _keyMargin);
		setMeasuredDimension(MeasureSpec.getSize(wSpec), height);
	}

	@Override
	protected void		onDraw(Canvas canvas)
	{
		float				x;
		float				y;

		y = _keyMargin;
		for (KeyboardData.Row row : _keyboard.getRows())
		{
			x = (KEY_PER_ROW * (_keyMargin + _keyWidth) - _keyMargin - row.getWidth(_keyWidth, _keyMargin)) / 2 + _keyMargin;
			for (KeyboardData.Key k : row)
			{
				float keyW = _keyWidth * k.width;
				if (k.downPointer != -1)
					canvas.drawRect(x, y, x + keyW, y + _keyHeight, _keyDownBgPaint);
				else
					canvas.drawRect(x, y, x + keyW, y + _keyHeight, _keyBgPaint);
				if (k.key0 != null)
					canvas.drawText(new char[]{k.key0.getChar()}, 0, 1,
						keyW / 2 + x, (_keyHeight + _keyLabelPaint.getTextSize()) / 2 + y, _keyLabelPaint);
				float textOffsetY = _keySubLabelPaint.getTextSize() / 2;
				if (k.key1 != null)
					canvas.drawText(new char[]{k.key1.getChar()}, 0, 1,
						x + _keyPadding, y + _keyPadding + textOffsetY, _keySubLabelPaint);
				if (k.key2 != null)
					canvas.drawText(new char[]{k.key2.getChar()}, 0, 1,
						x + keyW - _keyPadding, y + _keyPadding + textOffsetY, _keySubLabelPaint);
				textOffsetY /= 2; // WTF it work
				if (k.key3 != null)
					canvas.drawText(new char[]{k.key3.getChar()}, 0, 1,
						x + _keyPadding, y + _keyHeight - _keyPadding + textOffsetY, _keySubLabelPaint);
				if (k.key4 != null)
					canvas.drawText(new char[]{k.key4.getChar()}, 0, 1,
						x + keyW - _keyPadding, y + _keyHeight - _keyPadding + textOffsetY, _keySubLabelPaint);
				x += keyW + _keyMargin;
			}
			y += _keyHeight + _keyMargin;
		}
	}
}
