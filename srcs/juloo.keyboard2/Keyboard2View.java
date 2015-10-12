package juloo.keyboard2;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;

public class Keyboard2View extends View
	implements View.OnTouchListener, Handler.Callback
{
	private static final float		KEY_PER_ROW = 10;

	private static final long		VIBRATE_MIN_INTERVAL = 100;

	private Keyboard2		_ime;
	private KeyboardData	_keyboard;

	private ArrayList<KeyDown>	_downKeys = new ArrayList<KeyDown>();

	private int				_flags = 0;

	private Vibrator		_vibratorService;
	private long			_lastVibration = 0;

	private Handler			_handler;
	private static int		_currentWhat = 0;

	private float			_marginTop;
	private float			_keyWidth;
	private float			_keyPadding;
	private float			_keyBgPadding;
	private float			_keyRound;

	private float			_subValueDist = 10f;
	private boolean			_vibrateEnabled = true;
	private long			_vibrateDuration = 20;
	private long			_longPressTimeout = 600;
	private long			_longPressInterval = 65;
	private float			_marginBottom;
	private float			_keyHeight;
	private float			_horizontalMargin;

	private Paint			_keyBgPaint = new Paint();
	private Paint			_keyDownBgPaint = new Paint();
	private Paint			_keyLabelPaint;
	private Paint			_keyLabelLockedPaint;
	private Paint			_keySubLabelPaint;
	private Paint			_keySubLabelRightPaint;
	private Paint			_specialKeyLabelPaint;
	private Paint			_specialKeyLabelLockedPaint;
	private Paint			_specialKeySubLabelPaint;
	private Paint			_specialKeySubLabelRightPaint;

	private static RectF	_tmpRect = new RectF();

	public Keyboard2View(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		_vibratorService = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
		_handler = new Handler(this);
		Typeface	specialKeysFont = Typeface.createFromAsset(getContext().getAssets(), "fonts/keys.ttf");
		_horizontalMargin = getResources().getDimension(R.dimen.horizontal_margin);
		_marginTop = getResources().getDimension(R.dimen.margin_top);
		_marginBottom = getResources().getDimension(R.dimen.margin_bottom);
		_keyHeight = getResources().getDimension(R.dimen.key_height);
		_keyPadding = getResources().getDimension(R.dimen.key_padding);
		_keyBgPadding = getResources().getDimension(R.dimen.key_bg_padding);
		_keyRound = getResources().getDimension(R.dimen.key_round);
		_keyBgPaint.setColor(getResources().getColor(R.color.key_bg));
		_keyDownBgPaint.setColor(getResources().getColor(R.color.key_down_bg));
		_keyLabelPaint = initLabelPaint(Paint.Align.CENTER, R.color.key_label, R.dimen.label_text_size, null);
		_keyLabelLockedPaint = initLabelPaint(Paint.Align.CENTER, R.color.key_label_locked, R.dimen.label_text_size, null);
		_keySubLabelPaint = initLabelPaint(Paint.Align.LEFT, R.color.key_sub_label, R.dimen.sublabel_text_size, null);
		_keySubLabelRightPaint = initLabelPaint(Paint.Align.RIGHT, R.color.key_sub_label, R.dimen.sublabel_text_size, null);
		_specialKeyLabelPaint = initLabelPaint(Paint.Align.CENTER, R.color.key_label, R.dimen.label_text_size, specialKeysFont);
		_specialKeyLabelLockedPaint = initLabelPaint(Paint.Align.CENTER, R.color.key_label_locked, R.dimen.label_text_size, specialKeysFont);
		_specialKeySubLabelPaint = initLabelPaint(Paint.Align.LEFT, R.color.key_sub_label, R.dimen.sublabel_text_size, specialKeysFont);
		_specialKeySubLabelRightPaint = initLabelPaint(Paint.Align.RIGHT, R.color.key_sub_label, R.dimen.sublabel_text_size, specialKeysFont);
		setOnTouchListener(this);
	}

	private Paint		initLabelPaint(Paint.Align align, int color, int size, Typeface font)
	{
		Paint				paint = new Paint(Paint.ANTI_ALIAS_FLAG);

		paint.setTextAlign(align);
		paint.setColor(getResources().getColor(color));
		paint.setTextSize(getResources().getDimension(size));
		if (font != null)
			paint.setTypeface(font);
		return (paint);
	}

	public void			reset_prefs(Keyboard2 ime)
	{
		SharedPreferences	prefs = PreferenceManager.getDefaultSharedPreferences(ime);

		_ime = ime;
		_subValueDist = prefs.getFloat("sub_value_dist", _subValueDist);
		_vibrateEnabled = prefs.getBoolean("vibrate_enabled", _vibrateEnabled);
		_vibrateDuration = prefs.getInt("vibrate_duration", (int)_vibrateDuration);
		_longPressTimeout = prefs.getInt("longpress_timeout", (int)_longPressTimeout);
		_longPressInterval = prefs.getInt("longpress_interval", (int)_longPressInterval);
		_marginBottom = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
			prefs.getInt("margin_bottom", (int)_marginBottom), getResources().getDisplayMetrics());
		_keyHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
			prefs.getInt("key_height", (int)_keyHeight), getResources().getDisplayMetrics());
		_horizontalMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
			prefs.getInt("horizontal_margin", (int)_horizontalMargin), getResources().getDisplayMetrics());
		reset();
	}

	public void			setKeyboard(KeyboardData keyboard)
	{
		_keyboard = keyboard;
		reset();
	}

	public void			reset()
	{
		_flags = 0;
		_downKeys.clear();
		requestLayout();
		invalidate();
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
		KeyDown				key = getKeyDown(pointerId);
		KeyValue			newValue;

		if (key != null)
		{
			moveX -= key.downX;
			moveY -= key.downY;
			if ((Math.abs(moveX) + Math.abs(moveY)) < _subValueDist)
				newValue = key.key.key0;
			else if (moveX < 0)
				newValue = (moveY < 0) ? key.key.key1 : key.key.key3;
			else if (moveY < 0)
				newValue = key.key.key2;
			else
				newValue = key.key.key4;
			if (newValue != null && newValue != key.value)
			{
				if (key.timeoutWhat != -1)
				{
					_handler.removeMessages(key.timeoutWhat);
					if ((newValue.getFlags() & KeyValue.FLAG_NOCHAR) == 0)
						_handler.sendEmptyMessageDelayed(key.timeoutWhat, _longPressTimeout);
				}
				key.value = newValue;
				key.flags = newValue.getFlags();
				updateFlags();
				invalidate();
				vibrate();
			}
		}
	}

	private void		onTouchDown(float touchX, float touchY, int pointerId)
	{
		float				x;
		float				y;
		float				keyW;

		y = _marginTop - _keyHeight;
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
					{
						int what = _currentWhat++;
						if (key.key0 != null && (key.key0.getFlags() & KeyValue.FLAG_NOCHAR) == 0)
							_handler.sendEmptyMessageDelayed(what, _longPressTimeout);
						_downKeys.add(new KeyDown(pointerId, key, touchX, touchY, what));
					}
					vibrate();
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
			if (k.timeoutWhat != -1)
			{
				_handler.removeMessages(k.timeoutWhat);
				k.timeoutWhat = -1;
			}
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

	private void		vibrate()
	{
		if (!_vibrateEnabled)
			return ;
		long now = System.currentTimeMillis();
		if ((now - _lastVibration) > VIBRATE_MIN_INTERVAL)
		{
			_lastVibration = now;
			try
			{
				_vibratorService.vibrate(_vibrateDuration);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean		handleMessage(Message msg)
	{
		long				now = System.currentTimeMillis();

		for (KeyDown key : _downKeys)
		{
			if (key.timeoutWhat == msg.what)
			{
				_handler.sendEmptyMessageDelayed(msg.what, _longPressInterval);
				_ime.handleKeyUp(key.value, _flags);
				vibrate();
				return (true);
			}
		}
		return (false);
	}

	@Override
	public void			onMeasure(int wSpec, int hSpec)
	{
		DisplayMetrics		dm = getContext().getResources().getDisplayMetrics();
		int					height;

		if (_keyboard.getRows() == null)
			height = 0;
		else
			height = (int)(_keyHeight * ((float)_keyboard.getRows().size())
				+ _marginTop + _marginBottom);
		setMeasuredDimension(dm.widthPixels, height);
		_keyWidth = (getWidth() - (_horizontalMargin * 2)) / KEY_PER_ROW;
	}

	@Override
	protected void		onDraw(Canvas canvas)
	{
		float				x;
		float				y;

		y = _marginTop;
		for (KeyboardData.Row row : _keyboard.getRows())
		{
			x = (KEY_PER_ROW * _keyWidth - row.getWidth(_keyWidth)) / 2f + _horizontalMargin;
			for (KeyboardData.Key k : row)
			{
				float keyW = _keyWidth * k.width;
				KeyDown keyDown = getKeyDown(k);
				_tmpRect.set(x + _keyBgPadding, y + _keyBgPadding,
					x + keyW - _keyBgPadding, y + _keyHeight - _keyBgPadding);
				if (keyDown != null)
					canvas.drawRect(_tmpRect, _keyDownBgPaint);
				else
					canvas.drawRoundRect(_tmpRect, _keyRound, _keyRound, _keyBgPaint);
				if (k.key0 != null)
					drawLabel(canvas, k.key0, keyW / 2f + x, (_keyHeight + _keyLabelPaint.getTextSize()) / 2f + y,
						(keyDown != null && (keyDown.flags & KeyValue.FLAG_LOCKED) != 0));
				float subPadding = _keyBgPadding + _keyPadding;
				if (k.key1 != null)
					drawSubLabel(canvas, k.key1, x + subPadding, y + subPadding - _keySubLabelPaint.ascent(), false);
				if (k.key3 != null)
					drawSubLabel(canvas, k.key3, x + subPadding, y + _keyHeight - subPadding - _keySubLabelPaint.descent(), false);
				if (k.key2 != null)
					drawSubLabel(canvas, k.key2, x + keyW - subPadding, y + subPadding - _keySubLabelRightPaint.ascent(), true);
				if (k.key4 != null)
					drawSubLabel(canvas, k.key4, x + keyW - subPadding, y + _keyHeight - subPadding - _keySubLabelRightPaint.descent(), true);
				x += keyW;
			}
			y += _keyHeight;
		}
	}

	private void		drawLabel(Canvas canvas, KeyValue k, float x, float y, boolean locked)
	{
		if ((k.getFlags() & KeyValue.FLAG_KEY_FONT) != 0)
			canvas.drawText(k.getSymbol(_flags), x, y, locked ? _specialKeyLabelLockedPaint : _specialKeyLabelPaint);
		else
			canvas.drawText(k.getSymbol(_flags), x, y, locked ? _keyLabelLockedPaint : _keyLabelPaint);
	}

	private void		drawSubLabel(Canvas canvas, KeyValue k, float x, float y, boolean right)
	{
		if ((k.getFlags() & KeyValue.FLAG_KEY_FONT) != 0)
			canvas.drawText(k.getSymbol(_flags), x, y, right ? _specialKeySubLabelRightPaint : _specialKeySubLabelPaint);
		else
			canvas.drawText(k.getSymbol(_flags), x, y, right ? _keySubLabelRightPaint : _keySubLabelPaint);
	}

	private class KeyDown
	{
		public int				pointerId;
		public KeyValue			value;
		public KeyboardData.Key	key;
		public float			downX;
		public float			downY;
		public int				flags;
		public int				timeoutWhat;

		public KeyDown(int pointerId, KeyboardData.Key key, float x, float y, int what)
		{
			this.pointerId = pointerId;
			value = key.key0;
			this.key = key;
			downX = x;
			downY = y;
			flags = (value == null) ? 0 : value.getFlags();
			timeoutWhat = what;
		}
	}
}
