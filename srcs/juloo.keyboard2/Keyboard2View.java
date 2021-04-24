package juloo.keyboard2;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupWindow;
import java.util.ArrayList;

public class Keyboard2View extends View
	implements View.OnTouchListener, Handler.Callback
{
	private static final long		VIBRATE_MIN_INTERVAL = 100;

	private KeyboardData		_keyboard;

	private ArrayList<KeyDown>	_downKeys = new ArrayList<KeyDown>();

	private int					_flags = 0;

	private Vibrator			_vibratorService;
	private long				_lastVibration = 0;

	private Handler				_handler;
	private static int			_currentWhat = 0;

	private Config				_config;

	private float				_keyWidth;

	private Paint _keyBgPaint = new Paint();
	private Paint _keyDownBgPaint = new Paint();
	private Paint _keyLabelPaint;
	private Paint _keySubLabelPaint;
	private Paint _specialKeyLabelPaint;
	private Paint _specialKeySubLabelPaint;
  private int _lockedColor;
  private int _labelColor;
  private int _subLabelColor;
  private float _labelTextSize;
  private float _sublabelTextSize;

	private static RectF		_tmpRect = new RectF();

	public Keyboard2View(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		_vibratorService = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
		_handler = new Handler(this);
    refreshConfig(((Keyboard2)context).getConfig());
		setOnTouchListener(this);
	}

  public void refreshConfig(Config config)
  {
    Resources res = getResources();
		_config = config;
    _lockedColor = res.getColor(R.color.key_label_locked);
    _labelColor = res.getColor(R.color.key_label);
    _subLabelColor = res.getColor(R.color.key_sub_label);
    _labelTextSize = res.getDimension(R.dimen.label_text_size) * config.characterSize;
    _sublabelTextSize = res.getDimension(R.dimen.sublabel_text_size) * config.characterSize;
		_keyBgPaint.setColor(res.getColor(R.color.key_bg));
		_keyDownBgPaint.setColor(res.getColor(R.color.key_down_bg));
		_keyLabelPaint = initLabelPaint(Paint.Align.CENTER, null);
		_keySubLabelPaint = initLabelPaint(Paint.Align.LEFT, null);
		Typeface	specialKeysFont = ((Keyboard2)getContext()).getSpecialKeyFont();
		_specialKeyLabelPaint = initLabelPaint(Paint.Align.CENTER, specialKeysFont);
		_specialKeySubLabelPaint = initLabelPaint(Paint.Align.LEFT, specialKeysFont);
  }

	private Paint		initLabelPaint(Paint.Align align, Typeface font)
	{
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setTextAlign(align);
    if (font != null)
      paint.setTypeface(font);
		return (paint);
	}

	public void			setKeyboard(KeyboardData kw)
	{
    if (!_config.shouldOfferSwitchingToNextInputMethod)
      kw = kw.removeKeys(new KeyboardData.RemoveKeysByEvent(KeyValue.EVENT_CHANGE_METHOD));
    if (_config.disableAccentKeys)
      kw = kw.removeKeys(new KeyboardData.RemoveKeysByFlags(KeyValue.FLAGS_ACCENTS));
    _keyboard = kw;
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
      float absDist = Math.abs(moveX) + Math.abs(moveY);
      key.ptrDist = absDist;
			if (absDist < _config.subValueDist)
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
					if ((newValue.flags & KeyValue.FLAG_NOREPEAT) == 0)
						_handler.sendEmptyMessageDelayed(key.timeoutWhat, _config.longPressTimeout);
				}
				key.value = newValue;
				key.flags = newValue.flags;
				updateFlags();
				invalidate();
				handleKeyDown(newValue);
			}
		}
	}

	private void		onTouchDown(float touchX, float touchY, int pointerId)
	{
		float y = _config.marginTop - _config.keyHeight;
		for (KeyboardData.Row row : _keyboard.getRows())
		{
			y += _config.keyHeight;
			if (touchY < y || touchY >= (y + _config.keyHeight))
				continue ;
      float x = _config.horizontalMargin;
			for (KeyboardData.Key key : row.getKeys())
			{
        x += key.shift * _keyWidth;
				float keyW = _keyWidth * key.width;
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
						if (key.key0 != null && (key.key0.flags & KeyValue.FLAG_NOREPEAT) == 0)
							_handler.sendEmptyMessageDelayed(what, _config.longPressTimeout);
						_downKeys.add(new KeyDown(pointerId, key, touchX, touchY, what));
					}
					handleKeyDown(key.key0);
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
			_downKeys.remove(k);
			handleKeyUp(k);
			updateFlags();
			invalidate();
			return ;
		}
	}

	private void		handleKeyUp(KeyDown key)
	{
		if (key.value != null && (key.flags & (KeyValue.FLAG_LOCKED | KeyValue.FLAG_NOCHAR)) == 0)
			((Keyboard2)getContext()).handleKeyUp(key.value, _flags);
	}

	private void		handleKeyDown(KeyValue key)
	{
		if (key == null)
			return ;
		vibrate();
	}

	private void		updateFlags()
	{
		_flags = 0;
		for (KeyDown k : _downKeys)
			_flags |= k.flags;
	}

	private void		vibrate()
	{
		if (!_config.vibrateEnabled)
			return ;
		long now = System.currentTimeMillis();
		if ((now - _lastVibration) > VIBRATE_MIN_INTERVAL)
		{
			_lastVibration = now;
			try
			{
				_vibratorService.vibrate(_config.vibrateDuration);
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
		for (KeyDown key : _downKeys)
		{
			if (key.timeoutWhat == msg.what)
			{
        long nextInterval = _config.longPressInterval;
        if (_config.preciseRepeat && (key.flags & KeyValue.FLAG_PRECISE_REPEAT) != 0)
        {
          // Modulate repeat interval depending on the distance of the pointer
          float accel = Math.min(4.f, Math.max(0.3f, key.ptrDist / (_config.subValueDist * 15.f)));
          nextInterval = (long)((float)nextInterval / accel);
        }
				_handler.sendEmptyMessageDelayed(msg.what, nextInterval);
				((Keyboard2)getContext()).handleKeyUp(key.value, _flags);
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
			height = (int)(_config.keyHeight * ((float)_keyboard.getRows().size())
				+ _config.marginTop + _config.marginBottom);
		setMeasuredDimension(dm.widthPixels, height);
		_keyWidth = (getWidth() - (_config.horizontalMargin * 2)) / _keyboard.getKeysWidth();
	}

	@Override
	protected void		onDraw(Canvas canvas)
	{
		float y = _config.marginTop;
		for (KeyboardData.Row row : _keyboard.getRows())
		{
      float x = _config.horizontalMargin;
			for (KeyboardData.Key k : row.getKeys())
			{
        x += k.shift * _keyWidth;
				float keyW = _keyWidth * k.width;
				KeyDown keyDown = getKeyDown(k);
				_tmpRect.set(x + _config.keyBgPadding, y + _config.keyBgPadding,
					x + keyW - _config.keyBgPadding, y + _config.keyHeight - _config.keyBgPadding);
				if (keyDown != null)
					canvas.drawRect(_tmpRect, _keyDownBgPaint);
				else
					canvas.drawRoundRect(_tmpRect, _config.keyRound, _config.keyRound, _keyBgPaint);
				if (k.key0 != null)
					drawLabel(canvas, k.key0, keyW / 2f + x, (_config.keyHeight + _labelTextSize) / 2f + y,
						(keyDown != null && (keyDown.flags & KeyValue.FLAG_LOCKED) != 0));
				float subPadding = _config.keyBgPadding + _config.keyPadding;
				if (k.key1 != null)
					drawSubLabel(canvas, k.key1, x + subPadding, y + subPadding - _keySubLabelPaint.ascent(), false);
				if (k.key3 != null)
					drawSubLabel(canvas, k.key3, x + subPadding, y + _config.keyHeight - subPadding - _keySubLabelPaint.descent(), false);
				if (k.key2 != null)
					drawSubLabel(canvas, k.key2, x + keyW - subPadding, y + subPadding - _keySubLabelPaint.ascent(), true);
				if (k.key4 != null)
					drawSubLabel(canvas, k.key4, x + keyW - subPadding, y + _config.keyHeight - subPadding - _keySubLabelPaint.descent(), true);
				x += keyW;
			}
			y += _config.keyHeight;
		}
	}

	@Override
	public void			onDetachedFromWindow()
	{
		super.onDetachedFromWindow();
	}

	private void		drawLabel(Canvas canvas, KeyValue k, float x, float y, boolean locked)
	{
    Paint p = ((k.flags & KeyValue.FLAG_KEY_FONT) != 0) ? _specialKeyLabelPaint : _keyLabelPaint;
    p.setColor(locked ? _lockedColor : _labelColor);
    p.setTextSize(_labelTextSize * scaleTextSize(k));
    k = KeyModifier.handleFlags(k, _flags);
    canvas.drawText(k.symbol, x, y, p);
	}

	private void		drawSubLabel(Canvas canvas, KeyValue k, float x, float y, boolean right)
	{
    Paint p = ((k.flags & KeyValue.FLAG_KEY_FONT) != 0) ? _specialKeySubLabelPaint : _keySubLabelPaint;
    p.setColor(_subLabelColor);
    p.setTextAlign(right ? Paint.Align.RIGHT : Paint.Align.LEFT);
    p.setTextSize(_sublabelTextSize * scaleTextSize(k));
    k = KeyModifier.handleFlags(k, _flags);
    canvas.drawText(k.symbol, x, y, p);
	}

  private float scaleTextSize(KeyValue k)
  {
    return (k.symbol.length() < 2) ? 1.f : 0.8f;
  }

	private static class KeyDown
	{
		public int				pointerId;
		public KeyValue			value;
		public KeyboardData.Key	key;
		public float			downX;
		public float			downY;
    /* Manhattan distance of the pointer to the center of the key */
    public float ptrDist;
		public int				flags;
		public int				timeoutWhat;

		public KeyDown(int pointerId, KeyboardData.Key key, float x, float y, int what)
		{
			this.pointerId = pointerId;
			value = key.key0;
			this.key = key;
			downX = x;
			downY = y;
      ptrDist = 0.f;
			flags = (value == null) ? 0 : value.flags;
			timeoutWhat = what;
		}
	}
}
