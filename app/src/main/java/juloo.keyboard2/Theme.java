package juloo.keyboard2;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;

public class Theme
{
  public final Paint keyBgPaint = new Paint();
  public final Paint keyDownBgPaint = new Paint();
  public final Paint keyBorderPaint = new Paint();
  public final int lockedColor;
  public final int activatedColor;
  public final int labelColor;
  public final int subLabelColor;
  public final int secondaryLabelColor;

  public final float keyBorderRadius;
  public final float keyBorderWidth;
  public final float keyBorderWidthActivated;
  public final int keyBorderColorLeft;
  public final int keyBorderColorTop;
  public final int keyBorderColorRight;
  public final int keyBorderColorBottom;

  public final int colorNavBar;
  public final boolean isLightNavBar;

  private final Paint _keyLabelPaint;
  private final Paint _specialKeyLabelPaint;
  private final Paint _keySubLabelPaint;
  private final Paint _specialKeySubLabelPaint;
  private final Paint _indicationPaint;

  public Theme(Context context, AttributeSet attrs)
  {
    TypedArray s = context.getTheme().obtainStyledAttributes(attrs, R.styleable.keyboard, 0, 0);
    int colorKey = s.getColor(R.styleable.keyboard_colorKey, 0);
    keyBgPaint.setColor(colorKey);
    keyDownBgPaint.setColor(s.getColor(R.styleable.keyboard_colorKeyActivated, 0));
    // colorKeyboard = s.getColor(R.styleable.keyboard_colorKeyboard, 0);
    colorNavBar = s.getColor(R.styleable.keyboard_navigationBarColor, 0);
    isLightNavBar = s.getBoolean(R.styleable.keyboard_windowLightNavigationBar, false);
    labelColor = s.getColor(R.styleable.keyboard_colorLabel, 0);
    activatedColor = s.getColor(R.styleable.keyboard_colorLabelActivated, 0);
    lockedColor = s.getColor(R.styleable.keyboard_colorLabelLocked, 0);
    subLabelColor = s.getColor(R.styleable.keyboard_colorSubLabel, 0);
    float secondaryLightOffset = s.getFloat(R.styleable.keyboard_secondaryLightOffset, 1.f);
    secondaryLabelColor = adjustLight(labelColor, secondaryLightOffset);
    keyBorderRadius = s.getDimension(R.styleable.keyboard_keyBorderRadius, 0);
    keyBorderWidth = s.getDimension(R.styleable.keyboard_keyBorderWidth, 0);
    keyBorderWidthActivated = s.getDimension(R.styleable.keyboard_keyBorderWidthActivated, 0);
    keyBorderPaint.setStyle(Paint.Style.STROKE);
    keyBorderColorLeft = s.getColor(R.styleable.keyboard_keyBorderColorLeft, colorKey);
    keyBorderColorTop = s.getColor(R.styleable.keyboard_keyBorderColorTop, colorKey);
    keyBorderColorRight = s.getColor(R.styleable.keyboard_keyBorderColorRight, colorKey);
    keyBorderColorBottom = s.getColor(R.styleable.keyboard_keyBorderColorBottom, colorKey);
    s.recycle();
    _keyLabelPaint = initLabelPaint(Paint.Align.CENTER, null);
    _keySubLabelPaint = initLabelPaint(Paint.Align.LEFT, null);
    Typeface specialKeyFont = getKeyFont(context);
    _specialKeyLabelPaint = initLabelPaint(Paint.Align.CENTER, specialKeyFont);
    _specialKeySubLabelPaint = initLabelPaint(Paint.Align.LEFT, specialKeyFont);
    _indicationPaint = initLabelPaint(Paint.Align.CENTER, null);
  }

  public Paint labelPaint(boolean special_font)
  {
    Paint p = special_font ? _specialKeyLabelPaint : _keyLabelPaint;
    return p;
  }

  public Paint subLabelPaint(boolean special_font, Paint.Align align)
  {
    Paint p = special_font ? _specialKeySubLabelPaint : _keySubLabelPaint;
    p.setTextAlign(align);
    return p;
  }

  public Paint indicationPaint()
  {
    return _indicationPaint;
  }

  int adjustLight(int color, float offset)
  {
    float[] hsv = new float[3];
    Color.colorToHSV(color, hsv);
    hsv[2] += offset;
    return Color.HSVToColor(hsv);
  }

  Paint initLabelPaint(Paint.Align align, Typeface font)
  {
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setTextAlign(align);
    if (font != null)
      paint.setTypeface(font);
    return (paint);
  }

  static Typeface _key_font = null;

  static public Typeface getKeyFont(Context context)
  {
    if (_key_font == null)
      _key_font = Typeface.createFromAsset(context.getAssets(), "special_font.ttf");
    return _key_font;
  }
}
