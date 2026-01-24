package juloo.keyboard2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ColorMatrixView extends View {
    private Paint mPaint;
    private Paint mCursorPaint;
    private Shader mValShader;
    private Shader mSatShader;
    private float mHue = 0f;
    private float mSat = 1f;
    private float mVal = 1f;
    private OnColorChangedListener mListener;

    public interface OnColorChangedListener {
        void onColorChanged(float sat, float val);
    }

    public ColorMatrixView(Context context) {
        this(context, null);
    }

    public ColorMatrixView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mPaint = new Paint();
        mCursorPaint = new Paint();
        mCursorPaint.setStyle(Paint.Style.STROKE);
        mCursorPaint.setStrokeWidth(4f);
        mCursorPaint.setColor(Color.BLACK); // Will adapt to contrast
        mCursorPaint.setAntiAlias(true);
    }

    public void setOnColorChangedListener(OnColorChangedListener listener) {
        mListener = listener;
    }

    public void setHue(float hue) {
        mHue = hue;
        updateShader();
        invalidate();
    }

    public void setSatVal(float sat, float val) {
        mSat = sat;
        mVal = val;
        invalidate();
    }

    private void updateShader() {
        int color = Color.HSVToColor(new float[] { mHue, 1f, 1f });

        // Gradient from White to Pure Hue (Horizontal) - Saturation
        mSatShader = new LinearGradient(0, 0, getWidth(), 0,
                Color.WHITE, color, Shader.TileMode.CLAMP);

        // Gradient from Transparent to Black (Vertical) - Value (Lightness/Brightness)
        // Actually for HSV Value: Top is Bright (Val=1), Bottom is Dark (Val=0)
        // So we need White/Color at Top, Black at Bottom.
        // Wait, standard Sat/Val square:
        // Top-Left: White (S=0, V=1) -> Top-Right: Color (S=1, V=1)
        // Bottom: Black (V=0)

        // Let's do:
        // Compostite Shader?
        // 1. Draw LinearGradient(Left->Right) White -> Hue
        // 2. Draw LinearGradient(Top->Bottom) Transparent -> Black

        mValShader = new LinearGradient(0, 0, 0, getHeight(),
                0x00000000, 0xFF000000, Shader.TileMode.CLAMP);

        // Combining is tricky with just one paint, usually people draw twice relative
        // to each other.
        // Or ComposeShader.

        ComposeShader shader = new ComposeShader(mValShader, mSatShader, PorterDuff.Mode.DARKEN);
        // DARKEN is not quite right because we actally want to mult by blackness?
        // Let's just draw twice in onDraw instead of ComposeShader for simplicity and
        // correctness control.
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateShader();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mSatShader == null) {
            updateShader();
        }

        // 1. Draw Saturation Gradient (White -> Hue)
        mPaint.setShader(mSatShader);
        canvas.drawRect(0, 0, getWidth(), getHeight(), mPaint);

        // 2. Draw Value Gradient (Transparent -> Black)
        mPaint.setShader(mValShader);
        canvas.drawRect(0, 0, getWidth(), getHeight(), mPaint);

        // 3. Draw Cursor
        float x = mSat * getWidth();
        float y = (1f - mVal) * getHeight();

        // Contrast for cursor
        mCursorPaint.setColor(mVal < 0.5f ? Color.WHITE : Color.BLACK);
        canvas.drawCircle(x, y, 16f, mCursorPaint);
        mCursorPaint.setColor(mVal < 0.5f ? Color.BLACK : Color.WHITE);
        canvas.drawCircle(x, y, 14f, mCursorPaint); // Inner ring for visibility
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                float x = Math.max(0, Math.min(event.getX(), getWidth()));
                float y = Math.max(0, Math.min(event.getY(), getHeight()));

                mSat = x / getWidth();
                mVal = 1f - (y / getHeight());

                if (mListener != null) {
                    mListener.onColorChanged(mSat, mVal);
                }
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }
}
