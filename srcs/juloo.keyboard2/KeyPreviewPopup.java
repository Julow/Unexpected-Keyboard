package juloo.keyboard2;

import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.TextView;

class KeyPreviewPopup extends PopupWindow
{
	private TextView	_content;
	private View		_anchor;

	public KeyPreviewPopup(View anchor)
	{
		super(anchor.getContext());
		_content = new TextView(anchor.getContext());
		_content.setTextColor(0xFFFFFFFF);
		_anchor = anchor;
		setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
		setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		setContentView(_content);
		setTouchable(false);
	}

	public void			setPreview(String preview)
	{
		System.out.println("popup preview: " + preview);
		if (preview == null)
			dismiss();
		else
		{
			_content.setText(preview);
			if (!isShowing())
				showAtLocation(_anchor, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, -400);
		}
	}
}
