package juloo.keyboard2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
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
  private static final long VIBRATE_MIN_INTERVAL = 100;

  private KeyboardData _keyboard;

  private ArrayList<KeyDown> _downKeys = new ArrayList<KeyDown>();

  private int _flags = 0;

  private Vibrator _vibratorService;
  private long _lastVibration = 0;

  private Handler _handler;
  private static int _currentWhat = 0;

  private Config _config;

  private float _keyWidth;

  private Theme _theme;

  private static RectF _tmpRect = new RectF();

  public Keyboard2View(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    _vibratorService = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
    _handler = new Handler(this);
    _theme = new Theme(getContext(), attrs);
    _config = Config.globalConfig();
    refreshConfig(null);
    setOnTouchListener(this);
  }

  /* Internally calls [reset()]. */
  public void refreshConfig(KeyboardData kw)
  {
    if (kw != null)
      setKeyboard(kw); // handle layout options then calls reset().
  }

  public void setKeyboard(KeyboardData kw)
  {
    if (!_config.shouldOfferSwitchingToNextInputMethod)
      kw = kw.replaceKeys(
          new KeyboardData.ReplaceKeysByEvent(KeyValue.EVENT_CHANGE_METHOD, null));
    if (_config.key_flags_to_remove != 0)
      kw = kw.replaceKeys(
          new KeyboardData.ReplaceKeysByFlags(_config.key_flags_to_remove, null));
    // Replace the action key to show the right label.
    KeyValue action_key = null; 
    if (_config.actionLabel != null)
      action_key = new KeyValue(_config.actionLabel, _config.actionLabel,
          KeyValue.CHAR_NONE, KeyValue.EVENT_ACTION, KeyValue.FLAG_NOREPEAT);
    kw = kw.replaceKeys(
        new KeyboardData.ReplaceKeysByEvent(KeyValue.EVENT_ACTION, action_key));
    _keyboard = kw;
    reset();
  }

  public void reset()
  {
    _flags = 0;
    _downKeys.clear();
    requestLayout();
    invalidate();
  }

  @Override
  public boolean onTouch(View v, MotionEvent event)
  {
    float x;
    float y;
    float keyW;
    int p;

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

  private KeyDown getKeyDown(int pointerId)
  {
    for (KeyDown k : _downKeys)
    {
      if (k.pointerId == pointerId)
        return (k);
    }
    return (null);
  }

  private KeyDown getKeyDown(KeyboardData.Key key)
  {
    for (KeyDown k : _downKeys)
    {
      if (k.key == key)
        return (k);
    }
    return (null);
  }

  private KeyDown getKeyDown(KeyValue kv)
  {
    for (KeyDown k : _downKeys)
    {
      if (k.value == kv)
        return (k);
    }
    return (null);
  }

  private void onTouchMove(float moveX, float moveY, int pointerId)
  {
    KeyDown key = getKeyDown(pointerId);
    KeyValue newValue;

    if (key != null)
    {
      moveX -= key.downX;
      moveY -= key.downY;
      float absDist = Math.abs(moveX) + Math.abs(moveY);
      key.ptrDist = absDist;
      if (absDist < _config.swipe_dist_px)
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

  private void onTouchDown(float touchX, float touchY, int pointerId)
  {
    float y = _config.marginTop - _config.keyHeight;
    for (KeyboardData.Row row : _keyboard.rows)
    {
      y += _config.keyHeight;
      if (touchY < y || touchY >= (y + _config.keyHeight))
        continue ;
      float x = _config.horizontalMargin;
      for (KeyboardData.Key key : row.keys)
      {
        x += key.shift * _keyWidth;
        float keyW = _keyWidth * key.width;
        if (touchX >= x && touchX < (x + keyW))
        {
          int what = _currentWhat++;
          if (key.key0 != null && (key.key0.flags & KeyValue.FLAG_NOREPEAT) == 0)
            _handler.sendEmptyMessageDelayed(what, _config.longPressTimeout);
          _downKeys.add(new KeyDown(pointerId, key, touchX, touchY, what));
          handleKeyDown(key.key0);
          updateFlags();
          invalidate();
          return ;
        }
        x += keyW;
      }
    }
  }

  // Whether a key is already activated (key down but pointer up)
  private KeyDown getActivatedKey(KeyValue kv)
  {
    for (KeyDown k : _downKeys)
    {
      if (k.value == kv && k.pointerId == -1)
        return (k);
    }
    return (null);
  }

  private void onTouchUp(int pointerId)
  {
    KeyDown k = getKeyDown(pointerId);

    if (k != null)
    {
      // Stop key repeat
      if (k.timeoutWhat != -1)
      {
        _handler.removeMessages(k.timeoutWhat);
        k.timeoutWhat = -1;
      }
      KeyDown k_on = getActivatedKey(k.value);
      if (k_on != null)
      {
        _downKeys.remove(k); // Remove dupplicate
        // Same key with FLAG_LOCK is already on, do lock
        if ((k_on.flags & KeyValue.FLAG_LOCK) != 0)
        {
          k_on.flags ^= KeyValue.FLAG_LOCK; // Next time, disable it
          k_on.flags |= KeyValue.FLAG_LOCKED;
        }
        // Otherwise, toggle it
        else
        {
          _downKeys.remove(k_on);
        }
      }
      // Key stay activated
      else if ((k.flags & KeyValue.FLAG_KEEP_ON) != 0)
      {
        k.pointerId = -1; // Set pointer up
      }
      else // Regular key up
      {
        for (int i = 0; i < _downKeys.size(); i++)
        {
          KeyDown downKey = _downKeys.get(i);
          // Disable other activated keys that aren't locked
          if (downKey.pointerId == -1 && (downKey.flags & KeyValue.FLAG_LOCKED) == 0)
            _downKeys.remove(i--);
          // Other keys currently down won't stay activated
          else if ((downKey.flags & KeyValue.FLAG_KEEP_ON) != 0)
            downKey.flags ^= KeyValue.FLAG_KEEP_ON;
        }
        _downKeys.remove(k);
        handleKeyUp(k);
      }
      updateFlags();
      invalidate();
    }
  }

  private void handleKeyUp(KeyDown key)
  {
    if (key.value != null && (key.flags & (KeyValue.FLAG_LOCKED | KeyValue.FLAG_NOCHAR)) == 0)
      _config.handler.handleKeyUp(key.value, _flags);
  }

  private void handleKeyDown(KeyValue key)
  {
    if (key == null)
      return ;
    vibrate();
  }

  private void updateFlags()
  {
    _flags = 0;
    for (KeyDown k : _downKeys)
      _flags |= k.flags;
  }

  private void vibrate()
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
  public boolean handleMessage(Message msg)
  {
    for (KeyDown key : _downKeys)
    {
      if (key.timeoutWhat == msg.what)
      {
        long nextInterval = _config.longPressInterval;
        if (_config.preciseRepeat && (key.flags & KeyValue.FLAG_PRECISE_REPEAT) != 0)
        {
          // Modulate repeat interval depending on the distance of the pointer
          float accel = Math.min(4.f, Math.max(0.3f, key.ptrDist / (_config.swipe_dist_px * 15.f)));
          nextInterval = (long)((float)nextInterval / accel);
        }
        _handler.sendEmptyMessageDelayed(msg.what, nextInterval);
        _config.handler.handleKeyUp(key.value, _flags);
        return (true);
      }
    }
    return (false);
  }

  @Override
  public void onMeasure(int wSpec, int hSpec)
  {
    DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
    int width = dm.widthPixels;
    int height =
      (int)(_config.keyHeight * _keyboard.keysHeight
          + _keyboard.rows.size() * _config.keyVerticalInterval
          + _config.marginTop + _config.marginBottom);
    setMeasuredDimension(width, height);
    _keyWidth = (width - (_config.horizontalMargin * 2)) / _keyboard.keysWidth;
  }

  @Override
  protected void onDraw(Canvas canvas)
  {
    float y = _config.marginTop;
    for (KeyboardData.Row row : _keyboard.rows)
    {
      y += row.shift * _config.keyHeight;
      float x = _config.horizontalMargin;
      float keyH = row.height * _config.keyHeight;
      for (KeyboardData.Key k : row.keys)
      {
        x += k.shift * _keyWidth + _config.keyHorizontalInterval;
        float keyW = _keyWidth * k.width - _config.keyHorizontalInterval;
        KeyDown keyDown = getKeyDown(k);
        _tmpRect.set(x, y, x + keyW, y + keyH);
        canvas.drawRoundRect(_tmpRect, _theme.keyBorderRadius, _theme.keyBorderRadius,
            (keyDown != null) ? _theme.keyDownBgPaint : _theme.keyBgPaint);
        if (k.key0 != null)
          drawLabel(canvas, k.key0, keyW / 2f + x, (keyH + _theme.labelTextSize) / 2f + y, keyDown);
        float subPadding = _config.keyPadding;
        if (k.key1 != null)
          drawSubLabel(canvas, k.key1, x + subPadding, y + subPadding, false, true, keyDown);
        if (k.key3 != null)
          drawSubLabel(canvas, k.key3, x + subPadding, y + keyH - subPadding, false, false, keyDown);
        if (k.key2 != null)
          drawSubLabel(canvas, k.key2, x + keyW - subPadding, y + subPadding, true, true, keyDown);
        if (k.key4 != null)
          drawSubLabel(canvas, k.key4, x + keyW - subPadding, y + keyH - subPadding, true, false, keyDown);
        x += keyW;
      }
      y += keyH + _config.keyVerticalInterval;
    }
  }

  @Override
  public void onDetachedFromWindow()
  {
    super.onDetachedFromWindow();
  }

  private int labelColor(KeyValue k, KeyDown hasKeyDown, int defaultColor)
  {
    if (hasKeyDown != null)
    {
      KeyDown kd = getKeyDown(k);
      if (kd != null)
      {
        if ((kd.flags & KeyValue.FLAG_LOCKED) != 0)
          return _theme.lockedColor;
        if (kd.pointerId == -1)
          return _theme.activatedColor;
      }
    }
    return defaultColor;
  }

  private void drawLabel(Canvas canvas, KeyValue k, float x, float y, KeyDown keyDown)
  {
    k = KeyModifier.handleFlags(k, _flags);
    Paint p = _theme.labelPaint(((k.flags & KeyValue.FLAG_KEY_FONT) != 0));
    p.setColor(labelColor(k, keyDown, _theme.labelColor));
    p.setTextSize(_theme.labelTextSize * scaleTextSize(k));
    canvas.drawText(k.symbol, x, y, p);
  }

  private void drawSubLabel(Canvas canvas, KeyValue k, float x, float y, boolean right, boolean up, KeyDown keyDown)
  {
    k = KeyModifier.handleFlags(k, _flags);
    Paint p = _theme.subLabelPaint(((k.flags & KeyValue.FLAG_KEY_FONT) != 0), right);
    p.setColor(labelColor(k, keyDown, _theme.subLabelColor));
    p.setTextSize(_theme.sublabelTextSize * scaleTextSize(k));
    y -= up ? p.ascent() : p.descent();
    canvas.drawText(k.symbol, x, y, p);
  }

  private float scaleTextSize(KeyValue k)
  {
    return ((k.symbol.length() < 2) ? 1.f : 0.8f) * _config.characterSize;
  }

  private static class KeyDown
  {
    /* -1 if pointer is up. */
    public int pointerId;
    public KeyValue value;
    public KeyboardData.Key key;
    public float downX;
    public float downY;
    /* Manhattan distance of the pointer to the center of the key */
    public float ptrDist;
    public int flags;
    public int timeoutWhat;

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
