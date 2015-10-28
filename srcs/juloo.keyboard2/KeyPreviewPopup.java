package juloo.keyboard2;

import android.view.Gravity;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.TextView;

class KeyPreviewPopup extends PopupWindow
{
	private TextView	_content;
	private View		_anchor;
	private int			_bottomMargin;

	public KeyPreviewPopup(View anchor)
	{
		super(anchor.getContext());
		_content = new TextView(anchor.getContext());
		_content.setTextColor(anchor.getResources().getColor(R.color.preview_text));
		_content.setTextSize(anchor.getResources().getDimension(R.dimen.preview_text));
		int padding = (int)anchor.getResources().getDimension(R.dimen.preview_padding);
		_content.setPaddingRelative(padding, padding, padding, padding);
		_content.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
		_content.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
		_anchor = anchor;
		_bottomMargin = (int)anchor.getResources().getDimension(R.dimen.preview_margin);
		setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
		setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		setBackgroundDrawable(anchor.getResources().getDrawable(R.drawable.preview_popup));
		setContentView(_content);
		setClippingEnabled(false);
		setTouchable(false);
	}

	public void			setPreview(String preview)
	{
		if (preview == null)
		{
			System.out.println("popup preview dismiss");
			dismiss();
		}
		else
		{
			System.out.println("popup preview: " + preview);
			_content.setText(preview);
			if (!isShowing())
				show();
		}
	}

	private void		show()
	{
		_content.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
		showAtLocation(_anchor, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0,
			-(_content.getMeasuredHeight() + _bottomMargin));
	}
}
