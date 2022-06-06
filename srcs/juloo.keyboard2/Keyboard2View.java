package juloo.keyboard2;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.inputmethodservice.InputMethodService;
import android.os.Build.VERSION;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;

public class Keyboard2View extends View
  implements View.OnTouchListener, Pointers.IPointerEventHandler
{
  private static final long VIBRATE_MIN_INTERVAL = 100;

  private KeyboardData _keyboard;

  private Pointers _pointers;

  private Pointers.Modifiers _mods;

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
    _theme = new Theme(getContext(), attrs);
    _config = Config.globalConfig();
    _pointers = new Pointers(this, _config);
    refresh_navigation_bar(context);
    setOnTouchListener(this);
    reset();
  }

  private Window getParentWindow(Context context)
  {
    if (context instanceof InputMethodService)
      return ((InputMethodService)context).getWindow().getWindow();
    if (context instanceof ContextWrapper)
      return getParentWindow(((ContextWrapper)context).getBaseContext());
    return null;
  }

  public void refresh_navigation_bar(Context context)
  {
    if (VERSION.SDK_INT < 21)
      return;
    // The intermediate Window is a [Dialog].
    Window w = getParentWindow(context);
    int uiFlags = getSystemUiVisibility();
    if (_theme.isLightNavBar)
      uiFlags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
    else
      uiFlags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
    w.setNavigationBarColor(_theme.colorNavBar);
    setSystemUiVisibility(uiFlags);
  }

  public void setKeyboard(KeyboardData kw)
  {
    _keyboard = _config.modify_layout(kw);
    reset();
  }

  public void reset()
  {
    _mods = Pointers.Modifiers.EMPTY;
    _pointers.clear();
    requestLayout();
    invalidate();
  }

  public KeyValue modifyKey(KeyValue k, Pointers.Modifiers mods)
  {
    return KeyModifier.modify(k, mods);
  }

  public void onPointerDown(boolean isSwipe)
  {
    invalidate();
    vibrate();
  }

  public void onPointerUp(KeyValue k, Pointers.Modifiers mods)
  {
    _config.handler.handleKeyUp(k, mods);
    invalidate();
  }

  public void onPointerHold(KeyValue k, Pointers.Modifiers mods)
  {
    _config.handler.handleKeyUp(k, mods);
  }

  public void onPointerFlagsChanged()
  {
    invalidate();
  }

  private void updateFlags()
  {
    _mods = _pointers.getModifiers();
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
        break;
      case MotionEvent.ACTION_DOWN:
      case MotionEvent.ACTION_POINTER_DOWN:
        p = event.getActionIndex();
        float tx = event.getX(p);
        float ty = event.getY(p);
        KeyboardData.Key key = getKeyAtPosition(tx, ty);
        if (key != null)
          _pointers.onTouchDown(tx, ty, event.getPointerId(p), key);
        break;
      case MotionEvent.ACTION_MOVE:
        for (p = 0; p < event.getPointerCount(); p++)
          _pointers.onTouchMove(event.getX(p), event.getY(p), event.getPointerId(p));
        break;
      case MotionEvent.ACTION_CANCEL:
        _pointers.onTouchCancel(event.getPointerId(event.getActionIndex()));
        break;
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
    long now = System.currentTimeMillis();
    if ((now - _lastVibration) > VIBRATE_MIN_INTERVAL)
    {
      _lastVibration = now;
      if (VERSION.SDK_INT >= 5)
      {
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
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
    updateFlags();
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
        drawLabel(canvas, k.key0, keyW / 2f + x, y, keyH, isKeyDown);
        if (k.edgekeys)
        {
          drawSubLabel(canvas, k.key1, x, y, keyW, keyH, Paint.Align.CENTER, Vertical.TOP, isKeyDown);
          drawSubLabel(canvas, k.key3, x, y, keyW, keyH, Paint.Align.LEFT, Vertical.CENTER, isKeyDown);
          drawSubLabel(canvas, k.key2, x, y, keyW, keyH, Paint.Align.RIGHT, Vertical.CENTER, isKeyDown);
          drawSubLabel(canvas, k.key4, x, y, keyW, keyH, Paint.Align.CENTER, Vertical.BOTTOM, isKeyDown);
        }
        else
        {
          drawSubLabel(canvas, k.key1, x, y, keyW, keyH, Paint.Align.LEFT, Vertical.TOP, isKeyDown);
          drawSubLabel(canvas, k.key3, x, y, keyW, keyH, Paint.Align.LEFT, Vertical.BOTTOM, isKeyDown);
          drawSubLabel(canvas, k.key2, x, y, keyW, keyH, Paint.Align.RIGHT, Vertical.TOP, isKeyDown);
          drawSubLabel(canvas, k.key4, x, y, keyW, keyH, Paint.Align.RIGHT, Vertical.BOTTOM, isKeyDown);
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
    if (isKeyDown && k.hasFlags(KeyValue.FLAG_LATCH))
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

  private void drawLabel(Canvas canvas, KeyValue k, float x, float y, float keyH, boolean isKeyDown)
  {
    k = KeyModifier.modify(k, _mods);
    if (k == null)
      return;
    float textSize = scaleTextSize(k, _config.labelTextSize, keyH);
    Paint p = _theme.labelPaint(k.hasFlags(KeyValue.FLAG_KEY_FONT));
    p.setColor(labelColor(k, isKeyDown, _theme.labelColor));
    p.setTextSize(textSize);
    canvas.drawText(k.getString(), x, (keyH - p.ascent() - p.descent()) / 2f + y, p);
  }

  private void drawSubLabel(Canvas canvas, KeyValue k, float x, float y, float keyW, float keyH, Paint.Align a, Vertical v, boolean isKeyDown)
  {
    k = KeyModifier.modify(k, _mods);
    if (k == null)
      return;
    float textSize = scaleTextSize(k, _config.sublabelTextSize, keyH);
    Paint p = _theme.subLabelPaint(k.hasFlags(KeyValue.FLAG_KEY_FONT), a);
    p.setColor(labelColor(k, isKeyDown, _theme.subLabelColor));
    p.setTextSize(textSize);
    float subPadding = _config.keyPadding;
    if (v == Vertical.CENTER)
      y += (keyH - p.ascent() - p.descent()) / 2f;
    else
      y += (v == Vertical.TOP) ? subPadding - p.ascent() : keyH - subPadding - p.descent();
    if (a == Paint.Align.CENTER)
      x += keyW / 2f;
    else
      x += (a == Paint.Align.LEFT) ? subPadding : keyW - subPadding;
    canvas.drawText(k.getString(), x, y, p);
  }

  private float scaleTextSize(KeyValue k, float rel_size, float keyH)
  {
    float smaller_font = k.hasFlags(KeyValue.FLAG_SMALLER_FONT) ? 0.75f : 1.f;
    return keyH * rel_size * smaller_font * _config.characterSize;
  }
}
