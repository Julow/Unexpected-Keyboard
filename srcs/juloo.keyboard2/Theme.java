package juloo.keyboard2;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;

public class Theme
{
  public final Paint keyBgPaint = new Paint();
  public final Paint keyDownBgPaint = new Paint();
  public final int lockedColor;
  public final int activatedColor;
  public final int labelColor;
  public final int subLabelColor;
  public final float labelTextSize;
  public final float sublabelTextSize;

  private final Paint _keyLabelPaint;
  private final Paint _specialKeyLabelPaint;
  private final Paint _keySubLabelPaint;
  private final Paint _specialKeySubLabelPaint;

  public Theme(Context context, AttributeSet attrs)
  {
    Resources res = context.getResources();
    lockedColor = res.getColor(R.color.key_label_locked);
    activatedColor = res.getColor(R.color.key_label_activated);
    labelColor = res.getColor(R.color.key_label);
    subLabelColor = res.getColor(R.color.key_sub_label);
    labelTextSize = res.getDimension(R.dimen.label_text_size);
    sublabelTextSize = res.getDimension(R.dimen.sublabel_text_size);
    keyBgPaint.setColor(res.getColor(R.color.key_bg));
    keyDownBgPaint.setColor(res.getColor(R.color.key_down_bg));
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

  public Paint subLabelPaint(boolean special_font, boolean align_right)
  {
    Paint p = special_font ? _specialKeySubLabelPaint : _keySubLabelPaint;
    p.setTextAlign(align_right ? Paint.Align.RIGHT : Paint.Align.LEFT);
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
