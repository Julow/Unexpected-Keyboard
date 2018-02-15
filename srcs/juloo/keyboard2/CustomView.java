package juloo.keyboard2;

import android.content.Context;
import android.view.View;
import android.view.MotionEvent;
import android.graphics.Canvas;
import juloo.javacaml.Caml;
import juloo.javacaml.Value;

public class CustomView extends View
{
	Value				callbacks;

	public CustomView(Context context) { super(context); }

	static final int m_id_onTouchEvent = Caml.hashVariant("onTouchEvent");
	@Override
	public boolean		onTouchEvent(MotionEvent event)
	{
		Caml.method(callbacks, m_id_onTouchEvent);
		Caml.argObject(event);
		return Caml.callBool();
	}

	static final int m_id_onMeasure = Caml.hashVariant("onMeasure");
	@Override
	public void			onMeasure(int wSpec, int hSpec)
	{
		Caml.method(callbacks, m_id_onMeasure);
		Caml.argInt(wSpec);
		Caml.argInt(hSpec);
		Caml.callUnit();
	}

	static final int m_id_onDraw = Caml.hashVariant("onDraw");
	@Override
	protected void		onDraw(Canvas canvas)
	{
		Caml.method(callbacks, m_id_onDraw);
		Caml.argObject(canvas);
		Caml.callUnit();
	}

	static final int m_id_onDetachedFromWindow = Caml.hashVariant("onDetachedFromWindow");
	@Override
	public void			onDetachedFromWindow()
	{
		Caml.method(callbacks, m_id_onDetachedFromWindow);
		Caml.callUnit();
	}
}
