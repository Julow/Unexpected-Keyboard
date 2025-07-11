package juloo.keyboard2;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;

public class Theme
{
  // Key colors
  public final int colorKey;
  public final int colorKeyActivated;

  // Label colors
  public final int lockedColor;
  public final int activatedColor;
  public final int labelColor;
  public final int subLabelColor;
  public final int secondaryLabelColor;
  public final int greyedLabelColor;

  // Key borders
  public final float keyBorderRadius;
  public final float keyBorderWidth;
  public final float keyBorderWidthActivated;
  public final int keyBorderColorLeft;
  public final int keyBorderColorTop;
  public final int keyBorderColorRight;
  public final int keyBorderColorBottom;

  public final int colorNavBar;
  public final boolean isLightNavBar;

  public Theme(Context context, AttributeSet attrs)
  {
    getKeyFont(context); // _key_font will be accessed
    TypedArray s = context.getTheme().obtainStyledAttributes(attrs, R.styleable.keyboard, 0, 0);
    colorKey = s.getColor(R.styleable.keyboard_colorKey, 0);
    colorKeyActivated = s.getColor(R.styleable.keyboard_colorKeyActivated, 0);
    // colorKeyboard = s.getColor(R.styleable.keyboard_colorKeyboard, 0);
    colorNavBar = s.getColor(R.styleable.keyboard_navigationBarColor, 0);
    isLightNavBar = s.getBoolean(R.styleable.keyboard_windowLightNavigationBar, false);
    labelColor = s.getColor(R.styleable.keyboard_colorLabel, 0);
    activatedColor = s.getColor(R.styleable.keyboard_colorLabelActivated, 0);
    lockedColor = s.getColor(R.styleable.keyboard_colorLabelLocked, 0);
    subLabelColor = s.getColor(R.styleable.keyboard_colorSubLabel, 0);
    secondaryLabelColor = adjustLight(labelColor,
        s.getFloat(R.styleable.keyboard_secondaryDimming, 0.25f));
    greyedLabelColor = adjustLight(labelColor,
        s.getFloat(R.styleable.keyboard_greyedDimming, 0.5f));
    keyBorderRadius = s.getDimension(R.styleable.keyboard_keyBorderRadius, 0);
    keyBorderWidth = s.getDimension(R.styleable.keyboard_keyBorderWidth, 0);
    keyBorderWidthActivated = s.getDimension(R.styleable.keyboard_keyBorderWidthActivated, 0);
    keyBorderColorLeft = s.getColor(R.styleable.keyboard_keyBorderColorLeft, colorKey);
    keyBorderColorTop = s.getColor(R.styleable.keyboard_keyBorderColorTop, colorKey);
    keyBorderColorRight = s.getColor(R.styleable.keyboard_keyBorderColorRight, colorKey);
    keyBorderColorBottom = s.getColor(R.styleable.keyboard_keyBorderColorBottom, colorKey);
    s.recycle();
  }

  /** Interpolate the 'value' component toward its opposite by 'alpha'. */
  int adjustLight(int color, float alpha)
  {
    float[] hsv = new float[3];
    Color.colorToHSV(color, hsv);
    float v = hsv[2];
    hsv[2] = alpha - (2 * alpha - 1) * v;
    return Color.HSVToColor(hsv);
  }

  Paint initIndicationPaint(Paint.Align align, Typeface font)
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

  public static final class Computed
  {
    public final float vertical_margin;
    public final float horizontal_margin;
    public final float margin_top;
    public final float margin_left;
    public final float row_height;
    public final Paint indication_paint;

    public final Key key;
    public final Key key_activated;

    public Computed(Theme theme, Config config, float keyWidth, KeyboardData layout)
    {
      // Rows height is proportional to the keyboard height, meaning it doesn't
      // change for layouts with more or less rows. 3.95 is the usual height of
      // a layout in KeyboardData unit. The keyboard will be higher if the
      // layout has more rows and smaller if it has less because rows stay the
      // same height.
      row_height = Math.min(
          config.screenHeightPixels * config.keyboardHeightPercent / 100 / 3.95f,
          config.screenHeightPixels / layout.keysHeight);
      vertical_margin = config.key_vertical_margin * row_height;
      horizontal_margin = config.key_horizontal_margin * keyWidth;
      // Add half of the key margin on the left and on the top as it's also
      // added on the right and on the bottom of every keys.
      margin_top = config.marginTop + vertical_margin / 2;
      margin_left = horizontal_margin / 2;
      key = new Key(theme, config, keyWidth, false);
      key_activated = new Key(theme, config, keyWidth, true);
      indication_paint = init_label_paint(config, null);
      indication_paint.setColor(theme.subLabelColor);
    }

    public static final class Key
    {
      public final Paint bg_paint = new Paint();
      public final Paint border_left_paint;
      public final Paint border_top_paint;
      public final Paint border_right_paint;
      public final Paint border_bottom_paint;
      public final float border_width;
      public final float border_radius;
      final Paint _label_paint;
      final Paint _special_label_paint;
      final Paint _sublabel_paint;
      final Paint _special_sublabel_paint;
      final int _label_alpha_bits;

      public Key(Theme theme, Config config, float keyWidth, boolean activated)
      {
        bg_paint.setColor(activated ? theme.colorKeyActivated : theme.colorKey);
        if (config.borderConfig)
        {
          border_radius = config.customBorderRadius * keyWidth;
          border_width = config.customBorderLineWidth;
        }
        else
        {
          border_radius = theme.keyBorderRadius;
          border_width = activated ? theme.keyBorderWidthActivated : theme.keyBorderWidth;
        }
        bg_paint.setAlpha(activated ? config.keyActivatedOpacity : config.keyOpacity);
        border_left_paint = init_border_paint(config, border_width, theme.keyBorderColorLeft);
        border_top_paint = init_border_paint(config, border_width, theme.keyBorderColorTop);
        border_right_paint = init_border_paint(config, border_width, theme.keyBorderColorRight);
        border_bottom_paint = init_border_paint(config, border_width, theme.keyBorderColorBottom);
        _label_paint = init_label_paint(config, null);
        _special_label_paint = init_label_paint(config, _key_font);
        _sublabel_paint = init_label_paint(config, null);
        _special_sublabel_paint = init_label_paint(config, _key_font);
        _label_alpha_bits = (config.labelBrightness & 0xFF) << 24;
      }

      public Paint label_paint(boolean special_font, int color, float text_size)
      {
        Paint p = special_font ? _special_label_paint : _label_paint;
        p.setColor((color & 0x00FFFFFF) | _label_alpha_bits);
        p.setTextSize(text_size);
        return p;
      }

      public Paint sublabel_paint(boolean special_font, int color, float text_size, Paint.Align align)
      {
        Paint p = special_font ? _special_sublabel_paint : _sublabel_paint;
        p.setColor((color & 0x00FFFFFF) | _label_alpha_bits);
        p.setTextSize(text_size);
        p.setTextAlign(align);
        return p;
      }
    }

    static Paint init_border_paint(Config config, float border_width, int color)
    {
      Paint p = new Paint();
      p.setAlpha(config.keyOpacity);
      p.setStyle(Paint.Style.STROKE);
      p.setStrokeWidth(border_width);
      p.setColor(color);
      return p;
    }

    static Paint init_label_paint(Config config, Typeface font)
    {
      Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
      p.setTextAlign(Paint.Align.CENTER);
      if (font != null)
        p.setTypeface(font);
      return p;
    }
  }
}
