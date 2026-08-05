package juloo.keyboard2;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/** A small grabber handle used to drag the floating keyboard window. */
public class FloatingHandleView extends View
{
  private static final float HANDLE_WIDTH_DP = 32;
  private static final float HANDLE_THICKNESS_DP = 3;
  private static final float HANDLE_SPACING_DP = 6;
  private static final int HANDLE_ALPHA = 120;

  private final Paint _paint;

  public FloatingHandleView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    _paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    _paint.setStyle(Paint.Style.STROKE);
    _paint.setStrokeCap(Paint.Cap.ROUND);
    _paint.setStrokeWidth(dp(context, HANDLE_THICKNESS_DP));
    TypedArray a = context.getTheme().obtainStyledAttributes(R.styleable.keyboard);
    _paint.setColor(a.getColor(R.styleable.keyboard_colorLabel, 0));
    a.recycle();
    _paint.setAlpha(HANDLE_ALPHA);
  }

  @Override
  protected void onDraw(Canvas canvas)
  {
    float width = dp(getContext(), HANDLE_WIDTH_DP);
    float spacing = dp(getContext(), HANDLE_SPACING_DP);
    float cx = getWidth() / 2.f;
    float cy = getHeight() / 2.f;
    for (int i = -1; i <= 1; i++)
    {
      float y = cy + i * spacing;
      canvas.drawLine(cx - width / 2.f, y, cx + width / 2.f, y, _paint);
    }
  }

  private static float dp(Context context, float value)
  {
    return value * context.getResources().getDisplayMetrics().density;
  }
}
