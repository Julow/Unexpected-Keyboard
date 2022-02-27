package juloo.keyboard2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

public class Keyboard2View extends View
  implements View.OnTouchListener, Pointers.IPointerEventHandler
{
  private static final long VIBRATE_MIN_INTERVAL = 100;

  private KeyboardData _keyboard;

  private Pointers _pointers;

  private int _flags = 0;

  private Vibrator _vibratorService;
  private long _lastVibration = 0;

  private static int _currentWhat = 0;

  private Config _config;

  private float _keyWidth;

  private Theme _theme;

  private static RectF _tmpRect = new RectF();

  enum Vertical
  {
    TOP,
    CENTER,
    BOTTOM
  }

  public Keyboard2View(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    _vibratorService = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
    _theme = new Theme(getContext(), attrs);
    _config = Config.globalConfig();
    _pointers = new Pointers(this, _config);
    setOnTouchListener(this);
    reset();
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
    {
      action_key = new KeyValue(_config.actionLabel, _config.actionLabel,
          KeyValue.CHAR_NONE, KeyValue.EVENT_ACTION, KeyValue.FLAG_NOREPEAT);
    }
    if (_config.swapEnterActionKey && action_key != null)
      kw = kw.replaceKeys(
          new KeyboardData.ReplaceKeysByEvent2(KeyEvent.KEYCODE_ENTER,
            action_key, KeyValue.EVENT_ACTION,
            KeyValue.getKeyByName("enter")));
    else
      kw = kw.replaceKeys(
          new KeyboardData.ReplaceKeysByEvent(KeyValue.EVENT_ACTION, action_key));
    _keyboard = kw;
    reset();
  }

  public void reset()
  {
    _flags = 0;
    _pointers.clear();
    requestLayout();
    invalidate();
  }

  public void onPointerDown(KeyValue k)
  {
    updateFlags();
    invalidate();
    if (k != null)
      vibrate();
  }

  public void onPointerSwipe(KeyValue k)
  {
    updateFlags();
    invalidate();
    if (k != null)
      vibrate();
  }

  public void onPointerUp(KeyValue k)
  {
    if (k != null && (k.flags & KeyValue.FLAG_NOCHAR) == 0)
      _config.handler.handleKeyUp(k, _flags);
    updateFlags();
    invalidate();
  }

  public void onPointerHold(KeyValue k)
  {
    if (k != null)
      _config.handler.handleKeyUp(k, _flags);
  }

  public void onPointerFlagsChanged()
  {
    updateFlags();
    invalidate();
  }

  private void updateFlags()
  {
    _flags = _pointers.getFlags();
  }

  @Override
  public boolean onTouch(View v, MotionEvent event)
  {
    int p;
    switch (event.getActionMasked())
    {
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_POINTER_UP:
        _pointers.onTouchUp(event.getPointerId(event.getActionIndex()));
        break ;
      case MotionEvent.ACTION_DOWN:
      case MotionEvent.ACTION_POINTER_DOWN:
        p = event.getActionIndex();
        float tx = event.getX(p);
        float ty = event.getY(p);
        KeyboardData.Key key = getKeyAtPosition(tx, ty);
        if (key != null)
          _pointers.onTouchDown(tx, ty, event.getPointerId(p), key);
        break ;
      case MotionEvent.ACTION_MOVE:
        for (p = 0; p < event.getPointerCount(); p++)
          _pointers.onTouchMove(event.getX(p), event.getY(p), event.getPointerId(p));
        break ;
      default:
        return (false);
    }
    return (true);
  }

  private KeyboardData.Row getRowAtPosition(float ty)
  {
    float y = _config.marginTop;
    if (ty < y)
      return null;
    for (KeyboardData.Row row : _keyboard.rows)
    {
      y += (row.shift + row.height) * _config.keyHeight;
      if (ty < y)
        return row;
    }
    return null;
  }

  private KeyboardData.Key getKeyAtPosition(float tx, float ty)
  {
    KeyboardData.Row row = getRowAtPosition(ty);
    float x = _config.horizontalMargin;
    if (row == null || tx < x)
      return null;
    for (KeyboardData.Key key : row.keys)
    {
      x += (key.shift + key.width) * _keyWidth;
      if (tx < x)
        return key;
    }
    return null;
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
  public void onMeasure(int wSpec, int hSpec)
  {
    DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
    int width = dm.widthPixels;
    int height =
      (int)(_config.keyHeight * _keyboard.keysHeight
          + _keyboard.rows.size()
          + _config.marginTop + _config.marginBottom);
    setMeasuredDimension(width, height);
    _keyWidth = (width - (_config.horizontalMargin * 2)) / _keyboard.keysWidth;
  }

  @Override
  protected void onDraw(Canvas canvas)
  {
    float y = _config.marginTop + _config.keyVerticalInterval / 2;
    for (KeyboardData.Row row : _keyboard.rows)
    {
      y += row.shift * _config.keyHeight;
      float x = _config.horizontalMargin + _config.keyHorizontalInterval / 2;
      float keyH = row.height * _config.keyHeight - _config.keyVerticalInterval;
      for (KeyboardData.Key k : row.keys)
      {
        x += k.shift * _keyWidth;
        float keyW = _keyWidth * k.width - _config.keyHorizontalInterval;
        boolean isKeyDown = _pointers.isKeyDown(k);
        _tmpRect.set(x, y, x + keyW, y + keyH);
        canvas.drawRoundRect(_tmpRect, _theme.keyBorderRadius, _theme.keyBorderRadius,
            isKeyDown ? _theme.keyDownBgPaint : _theme.keyBgPaint);
        if (k.key0 != null)
          drawLabel(canvas, k.key0, keyW / 2f + x, (keyH + scaleTextSize(k.key0, _config.labelTextSize)) / 2f + y, isKeyDown);
        float subPadding = _config.keyPadding;
        if (k.edgekeys)
        {
          if (k.key1 != null) // top key
            drawSubLabel(canvas, k.key1, x + keyW / 2f, y + subPadding, Paint.Align.CENTER, Vertical.TOP, isKeyDown);
          if (k.key3 != null) // left key
            drawSubLabel(canvas, k.key3, x + subPadding, y + keyH / 2f, Paint.Align.LEFT, Vertical.CENTER, isKeyDown);
          if (k.key2 != null) // right key
            drawSubLabel(canvas, k.key2, x + keyW - subPadding, y + keyH / 2f, Paint.Align.RIGHT, Vertical.CENTER, isKeyDown);
          if (k.key4 != null) // bottom key
            drawSubLabel(canvas, k.key4, x + keyW / 2f, y + keyH - subPadding, Paint.Align.CENTER, Vertical.BOTTOM, isKeyDown);
        }
        else
        {
          if (k.key1 != null) // top left key
            drawSubLabel(canvas, k.key1, x + subPadding, y + subPadding, Paint.Align.LEFT, Vertical.TOP, isKeyDown);
          if (k.key3 != null) // bottom left key
            drawSubLabel(canvas, k.key3, x + subPadding, y + keyH - subPadding, Paint.Align.LEFT, Vertical.BOTTOM, isKeyDown);
          if (k.key2 != null) // top right key
            drawSubLabel(canvas, k.key2, x + keyW - subPadding, y + subPadding, Paint.Align.RIGHT, Vertical.TOP, isKeyDown);
          if (k.key4 != null) // bottom right key
            drawSubLabel(canvas, k.key4, x + keyW - subPadding, y + keyH - subPadding, Paint.Align.RIGHT, Vertical.BOTTOM, isKeyDown);
        }
        x += _keyWidth * k.width;
      }
      y += row.height * _config.keyHeight;
    }
  }

  @Override
  public void onDetachedFromWindow()
  {
    super.onDetachedFromWindow();
  }

  private int labelColor(KeyValue k, boolean isKeyDown, int defaultColor)
  {
    if (isKeyDown && (k.flags & KeyValue.FLAG_LATCH) != 0)
    {
      int flags = _pointers.getKeyFlags(k);
      if (flags != -1)
      {
        if ((flags & KeyValue.FLAG_LOCKED) != 0)
          return _theme.lockedColor;
        if ((flags & KeyValue.FLAG_LATCH) == 0)
          return _theme.activatedColor;
      }
    }
    return defaultColor;
  }

  private void drawLabel(Canvas canvas, KeyValue k, float x, float y, boolean isKeyDown)
  {
    k = KeyModifier.handleFlags(k, _flags);
    Paint p = _theme.labelPaint(((k.flags & KeyValue.FLAG_KEY_FONT) != 0));
    p.setColor(labelColor(k, isKeyDown, _theme.labelColor));
    p.setTextSize(scaleTextSize(k, _config.labelTextSize));
    canvas.drawText(k.symbol, x, y, p);
  }

  private void drawSubLabel(Canvas canvas, KeyValue k, float x, float y, Paint.Align a, Vertical v, boolean isKeyDown)
  {
    k = KeyModifier.handleFlags(k, _flags);
    Paint p = _theme.subLabelPaint(((k.flags & KeyValue.FLAG_KEY_FONT) != 0), a);
    p.setColor(labelColor(k, isKeyDown, _theme.subLabelColor));
    p.setTextSize(scaleTextSize(k, _config.sublabelTextSize));
    if (v == Vertical.CENTER)
      y -= (p.ascent() + p.descent()) / 2f;
    else
      y -= (v == Vertical.TOP) ? p.ascent() : p.descent();
    canvas.drawText(k.symbol, x, y, p);
  }

  private float scaleTextSize(KeyValue k, float rel_size)
  {
    float smaller_if_long = (k.symbol.length() < 2) ? 1.f : 0.75f;
    return _config.keyHeight * rel_size * smaller_if_long * _config.characterSize;
  }
}
