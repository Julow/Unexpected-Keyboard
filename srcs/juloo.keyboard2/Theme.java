package juloo.keyboard2;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Color;
import android.util.AttributeSet;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class Theme
{
  public final Paint keyBgPaint = new Paint();
  public final Paint keyDownBgPaint = new Paint();
  public final int lockedColor;
  public final int activatedColor;
  public final int labelColor;
  public final int subLabelColor;

  public final float keyBorderRadius;

  private final Paint _keyLabelPaint;
  private final Paint _specialKeyLabelPaint;
  private final Paint _keySubLabelPaint;
  private final Paint _specialKeySubLabelPaint;
  
  public static class KeyBorder
  {
    public final boolean draw;
    public final Paint paint;
    public final float width;
    public final float offset;
    public final boolean outside;
    
    public static enum Position
    {
      TOP, BOTTOM, LEFT, RIGHT
    }
    
    
    private static int getWidthId(Position pos)
    {
      switch (pos)
      {
        case TOP:            return R.styleable.keyboard_keyBorderTopWidth;
        case BOTTOM:         return R.styleable.keyboard_keyBorderBottomWidth;
        case LEFT:           return R.styleable.keyboard_keyBorderLeftWidth;
        case RIGHT: default: return R.styleable.keyboard_keyBorderRightWidth;
      }
    }
    
    private static int getColorId(Position pos)
    {
      switch (pos)
      {
        case TOP:            return R.styleable.keyboard_keyBorderTopColor;
        case BOTTOM:         return R.styleable.keyboard_keyBorderBottomColor;
        case LEFT:           return R.styleable.keyboard_keyBorderLeftColor;
        case RIGHT: default: return R.styleable.keyboard_keyBorderRightColor;
      }
    }
    
    private static int getOffsetId(Position pos)
    {
      switch (pos)
      {
        case TOP:            return R.styleable.keyboard_keyBorderTopOffset;
        case BOTTOM:         return R.styleable.keyboard_keyBorderBottomOffset;
        case LEFT:           return R.styleable.keyboard_keyBorderLeftOffset;
        case RIGHT: default: return R.styleable.keyboard_keyBorderRightOffset;
      }
    }
    
    private static String getKey(Position pos, String key)
    {
      switch (pos)
      {
        case TOP:            return "key_border_top_" + key;
        case BOTTOM:         return "key_border_bottom_" + key;
        case LEFT:           return "key_border_left_" + key;
        case RIGHT: default: return "key_border_right_" + key;
      }
    }
    public KeyBorder(SharedPreferences prefs, DisplayMetrics dm, TypedArray s, Position pos, boolean fromSettings)
    {
      int c = Integer.MAX_VALUE;
      float w = Float.MAX_VALUE;
      float o = Float.MAX_VALUE;
      
      if (fromSettings)
      {
        try { c = Color.parseColor(prefs.getString(getKey(pos, "color"), "")); } catch (Exception e) {}
        
        w = prefs.getFloat(getKey(pos, "width"), Float.MAX_VALUE);
        if (w != Float.MAX_VALUE)
          w = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, w, dm);
        
        o = prefs.getFloat(getKey(pos, "offset"), Float.MAX_VALUE);
        if (o != Float.MAX_VALUE)
          o = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, o, dm);
      }
      
      if (c == Integer.MAX_VALUE)
        c = s.getColor(getColorId(pos), 0);
        
      if (w == Float.MAX_VALUE)
        w = s.getDimension(getWidthId(pos), 0);
      
      if (o == Float.MAX_VALUE)
        o = s.getDimension(getOffsetId(pos), 0);
      
      offset = o;
      width = Math.abs(w);

      draw = width > 0.f;
      outside = w >= 0.f;

      paint = new Paint(Paint.ANTI_ALIAS_FLAG);
      paint.setColor(c);
      paint.setStrokeWidth(width);
      paint.setStyle(Paint.Style.STROKE);
    }
    
  }
  
  public final boolean drawKeyBorder;
  
  public final KeyBorder keyBorderTop;
  public final KeyBorder keyBorderBottom;
  public final KeyBorder keyBorderLeft;
  public final KeyBorder keyBorderRight;

  public Theme(Keyboard2View view, Context context, AttributeSet attrs)
  {
    TypedArray s = context.getTheme().obtainStyledAttributes(attrs, R.styleable.keyboard, 0, 0);

    int colorKey = Integer.MAX_VALUE;
    int colorKeyActive = Integer.MAX_VALUE;
    int _labelColor = Integer.MAX_VALUE;
    int _activatedColor = Integer.MAX_VALUE;
    int _lockedColor = Integer.MAX_VALUE;
    int _subLabelColor = Integer.MAX_VALUE;
    float _keyBorderRadius = -1f;
    
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    Resources res = context.getResources();
    DisplayMetrics dm = res.getDisplayMetrics();
    boolean fromSettings = "custom".equals(prefs.getString("theme", ""));
    
    if (fromSettings)
    {
      
      int color;
      try { color = Color.parseColor(prefs.getString("keyboard_bg", "")); } catch (Exception e) { color = s.getColor(R.styleable.keyboard_colorKeyboard, 0); }
      view.setBackgroundColor(color);

      try
      {
        colorKey = Color.parseColor(prefs.getString("key_bg", ""));
        colorKeyActive = Color.parseColor(prefs.getString("active_key_bg", ""));
        _labelColor = Color.parseColor(prefs.getString("label", ""));
        _activatedColor = Color.parseColor(prefs.getString("active_label", ""));
        _lockedColor = Color.parseColor(prefs.getString("locked_label", ""));
        _subLabelColor = Color.parseColor(prefs.getString("sublabel", ""));
      }
      catch (Exception e)
      { }

      _keyBorderRadius = (float)prefs.getInt("key_border_radius", -1);
      if (_keyBorderRadius > -1f)
        _keyBorderRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, _keyBorderRadius, dm);
      
    }
    
    keyBgPaint.setColor(colorKey == Integer.MAX_VALUE ? s.getColor(R.styleable.keyboard_colorKey, 0):colorKey);
    keyDownBgPaint.setColor(colorKeyActive == Integer.MAX_VALUE ? s.getColor(R.styleable.keyboard_colorKeyActivated, 0):colorKeyActive);
    labelColor = _labelColor == Integer.MAX_VALUE ? s.getColor(R.styleable.keyboard_colorLabel, 0):_labelColor;
    activatedColor = _activatedColor == Integer.MAX_VALUE ? s.getColor(R.styleable.keyboard_colorLabelActivated, 0):_activatedColor;
    lockedColor = _lockedColor == Integer.MAX_VALUE ? s.getColor(R.styleable.keyboard_colorLabelLocked, 0):_lockedColor;
    subLabelColor = _subLabelColor == Integer.MAX_VALUE ? s.getColor(R.styleable.keyboard_colorSubLabel, 0):_subLabelColor;
    keyBorderRadius = _keyBorderRadius <= -1f ? s.getDimension(R.styleable.keyboard_keyBorderRadius, 0):_keyBorderRadius;
      
    keyBorderTop    = new KeyBorder(prefs, dm, s, KeyBorder.Position.TOP,    fromSettings);
    keyBorderBottom = new KeyBorder(prefs, dm, s, KeyBorder.Position.BOTTOM, fromSettings);
    keyBorderLeft   = new KeyBorder(prefs, dm, s, KeyBorder.Position.LEFT,   fromSettings);
    keyBorderRight  = new KeyBorder(prefs, dm, s, KeyBorder.Position.RIGHT,  fromSettings);
    drawKeyBorder = keyBorderTop.draw || keyBorderBottom.draw || keyBorderLeft.draw || keyBorderRight.draw;
    
    s.recycle();
    
    _keyLabelPaint = initLabelPaint(Paint.Align.CENTER, null);
    _keySubLabelPaint = initLabelPaint(Paint.Align.LEFT, null);
    Typeface specialKeyFont = getSpecialKeyFont(context);
    _specialKeyLabelPaint = initLabelPaint(Paint.Align.CENTER, specialKeyFont);
    _specialKeySubLabelPaint = initLabelPaint(Paint.Align.LEFT, specialKeyFont);
    
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

  private Paint initLabelPaint(Paint.Align align, Typeface font)
  {
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setTextAlign(align);
    if (font != null)
      paint.setTypeface(font);
    return (paint);
  }
  
  private static Typeface _specialKeyFont = null;

  static public Typeface getSpecialKeyFont(Context context)
  {
    if (_specialKeyFont == null)
    {
      _specialKeyFont = Typeface.createFromAsset(context.getAssets(), "fonts/keys.ttf");
    }
    return _specialKeyFont;
  }
}
