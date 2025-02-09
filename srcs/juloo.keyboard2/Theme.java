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
  public final int colorKeyAction;
  public final int colorKeySpaceBar;

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
  public final float keyBorderWidthAction;
  public final float keyBorderWidthSpaceBar;
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
    colorKeyAction = s.getColor(R.styleable.keyboard_colorKeyAction, colorKey);
    colorKeySpaceBar = s.getColor(R.styleable.keyboard_colorKeySpaceBar, colorKey);
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
    keyBorderWidthAction = s.getDimension(R.styleable.keyboard_keyBorderWidthAction, 0);
    keyBorderWidthSpaceBar = s.getDimension(R.styleable.keyboard_keyBorderWidthSpaceBar, 0);
    keyBorderColorLeft = s.getColor(R.styleable.keyboard_keyBorderColorLeft, colorKey);
    keyBorderColorTop = s.getColor(R.styleable.keyboard_keyBorderColorTop, colorKey);
    keyBorderColorRight = s.getColor(R.styleable.keyboard_keyBorderColorRight, colorKey);
    keyBorderColorBottom = s.getColor(R.styleable.keyboard_keyBorderColorBottom, colorKey);
    s.recycle();
  }

  /** Interpolate the 'value' component toward its opposite by 'alpha'. */
  static int adjustLight(int color, float alpha)
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
    public final Paint indication_paint;

    public final Key key;
    public final Key key_activated;
    public final Key key_action;
    public final Key key_space_bar;

    public Computed(Theme theme, Config config, float keyWidth)
    {
      vertical_margin = config.key_vertical_margin * config.keyHeight;
      horizontal_margin = config.key_horizontal_margin * keyWidth;
      // Add half of the key margin on the left and on the top as it's also
      // added on the right and on the bottom of every keys.
      margin_top = config.marginTop + vertical_margin / 2;
      margin_left = horizontal_margin / 2;
      key = new Key(theme, config, keyWidth, false, KeyboardData.Key.Role.Normal);
      key_action = new Key(theme, config, keyWidth, false, KeyboardData.Key.Role.Action);
      key_space_bar = new Key(theme, config, keyWidth, false, KeyboardData.Key.Role.Space_bar);
      key_activated = new Key(theme, config, keyWidth, true, KeyboardData.Key.Role.Normal);
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

      public Key(Theme theme, Config config, float keyWidth, boolean activated,
          KeyboardData.Key.Role role)
      {
        border_radius = config.borderConfig ? config.customBorderRadius * keyWidth : theme.keyBorderRadius;
        int bg_color;
        if (activated)
        {
          bg_color = theme.colorKeyActivated;
          border_width = theme.keyBorderWidthActivated;
          bg_paint.setAlpha(config.keyActivatedOpacity);
        }
        else
        {
          switch (role)
          {
            case Action:
              bg_color = theme.colorKeyAction;
              border_width = theme.keyBorderWidthAction;
              break;
            case Space_bar:
              bg_color = theme.colorKeySpaceBar;
              border_width = theme.keyBorderWidthSpaceBar;
              break;
            default:
              bg_color = theme.colorKey;
              border_width = config.borderConfig ? config.customBorderLineWidth : theme.keyBorderWidth;
              break;
          }
          bg_paint.setAlpha(config.keyOpacity);
        }
        bg_paint.setColor(bg_color);
        border_left_paint = init_border_paint(config, border_width, theme.keyBorderColorLeft);
        border_top_paint = init_border_paint(config, border_width, theme.keyBorderColorTop);
        border_right_paint = init_border_paint(config, border_width, theme.keyBorderColorRight);
        border_bottom_paint = init_border_paint(config, border_width, theme.keyBorderColorBottom);
        _label_paint = init_label_paint(config, null);
        _special_label_paint = init_label_paint(config, _key_font);
        _sublabel_paint = init_label_paint(config, null);
        _special_sublabel_paint = init_label_paint(config, _key_font);
      }

      public Paint label_paint(boolean special_font, float text_size)
      {
        Paint p = special_font ? _special_label_paint : _label_paint;
        p.setTextSize(text_size);
        return p;
      }

      public Paint sublabel_paint(boolean special_font, float text_size, Paint.Align align)
      {
        Paint p = special_font ? _special_sublabel_paint : _sublabel_paint;
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
      p.setAlpha(config.labelBrightness);
      if (font != null)
        p.setTypeface(font);
      return p;
    }
  }
}
