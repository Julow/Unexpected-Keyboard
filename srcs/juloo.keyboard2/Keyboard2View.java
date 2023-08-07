package juloo.keyboard2;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.inputmethodservice.InputMethodService;
import android.os.Build.VERSION;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import java.util.Arrays;

public class Keyboard2View extends View
  implements View.OnTouchListener, Pointers.IPointerEventHandler
{
  private KeyboardData _keyboard;
  private KeyValue _shift_kv;
  private KeyboardData.Key _shift_key;

  private Pointers _pointers;

  private Pointers.Modifiers _mods;

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
    _keyboard = kw;
    _shift_kv = KeyValue.getKeyByName("shift");
    _shift_key = _keyboard.findKeyWithValue(_shift_kv);
    if (_shift_key == null)
    {
      _shift_kv = _shift_kv.withFlags(_shift_kv.getFlags() | KeyValue.FLAG_LOCK);
      _shift_key = _keyboard.findKeyWithValue(_shift_kv);
    }
    reset();
  }

  public void reset()
  {
    _mods = Pointers.Modifiers.EMPTY;
    _pointers.clear();
    requestLayout();
    invalidate();
  }

  /** Called by auto-capitalisation. */
  public void set_shift_state(boolean state, boolean lock)
  {
    if (_keyboard == null || _shift_key == null)
      return;
    int flags = _pointers.getKeyFlags(_shift_key, _shift_kv);
    if (state)
    {
      if (flags != -1 && !lock)
        return; // Don't replace an existing pointer
      _pointers.add_fake_pointer(_shift_kv, _shift_key, lock);
    }
    else
    {
      if ((flags & KeyValue.FLAG_FAKE_PTR) != 0)
        return; // Don't remove locked pointers
      _pointers.remove_fake_pointer(_shift_kv, _shift_key);
    }
    invalidate();
  }

  public KeyValue modifyKey(KeyValue k, Pointers.Modifiers mods)
  {
    if (_keyboard.modmap != null)
    {
      if (mods.has(KeyValue.Modifier.SHIFT))
      {
        KeyValue km = _keyboard.modmap.shift.get(k);
        if (km != null)
          return km;
      }
    }
    return KeyModifier.modify(k, mods);
  }

  public void onPointerDown(boolean isSwipe)
  {
    invalidate();
    vibrate();
  }

  public void onPointerUp(KeyValue k, Pointers.Modifiers mods)
  {
    _config.handler.key_up(k, mods);
    invalidate();
  }

  public void onPointerHold(KeyValue k, Pointers.Modifiers mods)
  {
    _config.handler.key_up(k, mods);
  }

  public void onPointerFlagsChanged(boolean shouldVibrate)
  {
    invalidate();
    if (shouldVibrate)
      vibrate();
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
        _pointers.onTouchCancel();
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
    float x = _config.horizontal_margin;
    if (row == null || tx < x)
      return null;
    for (KeyboardData.Key key : row.keys)
    {
      float xLeft = x + key.shift * _keyWidth;
      float xRight = xLeft + key.width * _keyWidth;
      if (tx < xLeft)
        return null;
      if (tx < xRight)
        return key;
      x = xRight;
    }
    return null;
  }

  private void vibrate()
  {
    VibratorCompat.vibrate(this, _config.vibration_behavior);
  }

  @Override
  public void onMeasure(int wSpec, int hSpec)
  {
    DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
    int width = dm.widthPixels;
    int height =
      (int)(_config.keyHeight * _keyboard.keysHeight
          + _config.marginTop + _config.margin_bottom);
    setMeasuredDimension(width, height);
    _keyWidth = (width - (_config.horizontal_margin * 2)) / _keyboard.keysWidth;
  }

  @Override
  public void onLayout(boolean changed, int left, int top, int right, int bottom)
  {
    if (!changed)
      return;
    if (VERSION.SDK_INT >= 29)
    {
      // Disable the back-gesture on the keyboard area
      Rect keyboard_area = new Rect(
          left + (int)_config.horizontal_margin,
          top + (int)_config.marginTop,
          right - (int)_config.horizontal_margin,
          bottom - (int)_config.margin_bottom);
      setSystemGestureExclusionRects(Arrays.asList(keyboard_area));
    }
  }

  /** Horizontal and vertical position of the 9 indexes. */
  static final Paint.Align[] LABEL_POSITION_H = new Paint.Align[]{
    Paint.Align.CENTER, Paint.Align.LEFT, Paint.Align.RIGHT, Paint.Align.LEFT,
    Paint.Align.RIGHT, Paint.Align.LEFT, Paint.Align.RIGHT,
    Paint.Align.CENTER, Paint.Align.CENTER
  };

  static final Vertical[] LABEL_POSITION_V = new Vertical[]{
    Vertical.CENTER, Vertical.TOP, Vertical.TOP, Vertical.BOTTOM,
    Vertical.BOTTOM, Vertical.CENTER, Vertical.CENTER, Vertical.TOP,
    Vertical.BOTTOM
  };

  @Override
  protected void onDraw(Canvas canvas)
  {
    updateFlags();
    // Set keyboard background opacity
    getBackground().setAlpha(_config.keyboardOpacity);
    // Set keys opacity
    _theme.keyBgPaint.setAlpha(_config.keyOpacity);
    _theme.keyDownBgPaint.setAlpha(_config.keyActivatedOpacity);
    _theme.keyBorderPaint.setAlpha(_config.keyOpacity);
    float y = _config.marginTop + _config.keyVerticalInterval / 2;
    for (KeyboardData.Row row : _keyboard.rows)
    {
      y += row.shift * _config.keyHeight;
      float x = _config.horizontal_margin + _config.keyHorizontalInterval / 2;
      float keyH = row.height * _config.keyHeight - _config.keyVerticalInterval;
      for (KeyboardData.Key k : row.keys)
      {
        x += k.shift * _keyWidth;
        float keyW = _keyWidth * k.width - _config.keyHorizontalInterval;
        boolean isKeyDown = _pointers.isKeyDown(k);
        drawKeyFrame(canvas, x, y, keyW, keyH, isKeyDown);
        if (k.keys[0] != null)
          drawLabel(canvas, k.keys[0], keyW / 2f + x, y, keyH, isKeyDown);
        for (int i = 1; i < 9; i++)
        {
          if (k.keys[i] != null)
            drawSubLabel(canvas, k.keys[i], x, y, keyW, keyH, i, isKeyDown);
        }
        if (k.indication != null)
        {
          drawIndication(canvas, k.indication, keyW / 2f + x, y, keyH);
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

  /** Draw borders and background of the key. */
  void drawKeyFrame(Canvas canvas, float x, float y, float keyW, float keyH,
      boolean isKeyDown)
  {
    float r = _theme.keyBorderRadius;
    float w = isKeyDown ? _theme.keyBorderWidthActivated : _theme.keyBorderWidth;
    float w2 = _theme.keyBorderWidth / 2.f;
    _tmpRect.set(x + w2, y + w2, x + keyW - w2, y + keyH - w2);
    canvas.drawRoundRect(_tmpRect, r, r,
        isKeyDown ? _theme.keyDownBgPaint : _theme.keyBgPaint);
    if (w > 0.f)
    {
      _theme.keyBorderPaint.setStrokeWidth(w);
      float overlap = r - r * 0.85f + w; // sin(45°)
      drawBorder(canvas, x, y, x + overlap, y + keyH, _theme.keyBorderColorLeft);
      drawBorder(canvas, x, y, x + keyW, y + overlap, _theme.keyBorderColorTop);
      drawBorder(canvas, x + keyW - overlap, y, x + keyW, y + keyH, _theme.keyBorderColorRight);
      drawBorder(canvas, x, y + keyH - overlap, x + keyW, y + keyH, _theme.keyBorderColorBottom);
    }
  }

  /** Clip to draw a border at a time. This allows to call [drawRoundRect]
      several time with the same parameters but a different Paint. */
  void drawBorder(Canvas canvas, float clipl, float clipt, float clipr,
      float clipb, int color)
  {
    Paint p = _theme.keyBorderPaint;
    float r = _theme.keyBorderRadius;
    canvas.save();
    canvas.clipRect(clipl, clipt, clipr, clipb);
    p.setColor(color);
    canvas.drawRoundRect(_tmpRect, r, r, p);
    canvas.restore();
  }

  private int labelColor(KeyValue k, boolean isKeyDown, boolean sublabel)
  {
    if (isKeyDown)
    {
      int flags = _pointers.getKeyFlags(k);
      if (flags != -1)
      {
        if ((flags & KeyValue.FLAG_LOCKED) != 0)
          return _theme.lockedColor;
        return _theme.activatedColor;
      }
    }
    if (k.hasFlags(KeyValue.FLAG_SECONDARY))
      return _theme.secondaryLabelColor;
    return sublabel ? _theme.subLabelColor : _theme.labelColor;
  }

  private void drawLabel(Canvas canvas, KeyValue kv, float x, float y, float keyH, boolean isKeyDown)
  {
    kv = modifyKey(kv, _mods);
    if (kv == null)
      return;
    float textSize = scaleTextSize(kv, _config.labelTextSize, keyH);
    Paint p = _theme.labelPaint(kv.hasFlags(KeyValue.FLAG_KEY_FONT));
    p.setColor(labelColor(kv, isKeyDown, false));
    p.setAlpha(_config.labelBrightness);
    p.setTextSize(textSize);
    canvas.drawText(kv.getString(), x, (keyH - p.ascent() - p.descent()) / 2f + y, p);
  }

  private void drawSubLabel(Canvas canvas, KeyValue kv, float x, float y,
      float keyW, float keyH, int sub_index, boolean isKeyDown)
  {
    Paint.Align a = LABEL_POSITION_H[sub_index];
    Vertical v = LABEL_POSITION_V[sub_index];
    kv = modifyKey(kv, _mods);
    if (kv == null)
      return;
    float textSize = scaleTextSize(kv, _config.sublabelTextSize, keyH);
    Paint p = _theme.subLabelPaint(kv.hasFlags(KeyValue.FLAG_KEY_FONT), a);
    p.setColor(labelColor(kv, isKeyDown, true));
    p.setAlpha(_config.labelBrightness);
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
    String label = kv.getString();
    int label_len = label.length();
    // Limit the label of string keys to 3 characters
    if (label_len > 3 && kv.getKind() == KeyValue.Kind.String)
      label_len = 3;
    canvas.drawText(label, 0, label_len, x, y, p);
  }

  private void drawIndication(Canvas canvas, String indication, float x,
      float y, float keyH)
  {
    float textSize = keyH * _config.sublabelTextSize * _config.characterSize;
    Paint p = _theme.indicationPaint();
    p.setColor(_theme.subLabelColor);
    p.setTextSize(textSize);
    canvas.drawText(indication, x,
        (keyH - p.ascent() - p.descent()) * 4/5 + y, p);
  }

  private float scaleTextSize(KeyValue k, float rel_size, float keyH)
  {
    float smaller_font = k.hasFlags(KeyValue.FLAG_SMALLER_FONT) ? 0.75f : 1.f;
    return keyH * rel_size * smaller_font * _config.characterSize;
  }
}
