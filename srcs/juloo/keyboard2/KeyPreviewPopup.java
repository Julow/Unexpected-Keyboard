package juloo.keyboard2;

import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.TextView;

class KeyPreviewPopup extends PopupWindow
	implements Handler.Callback
{
	private final TextView	_content;
	private final View		_anchor;

	private Config			_config;

	private final Handler	_handler;

	private int				_minWidth;

	public KeyPreviewPopup(View anchor, Config config)
	{
		super(anchor.getContext());
		_config = config;
		_content = new TextView(anchor.getContext());
		/*
		** TODO: move all resources get to Config object
		*/
		_content.setTextColor(anchor.getResources().getColor(R.color.preview_text));
		_content.setTextSize(anchor.getResources().getDimension(R.dimen.preview_text));
		int padding = (int)anchor.getResources().getDimension(R.dimen.preview_padding);
		_content.setPaddingRelative(padding, padding, padding, padding);
		_content.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
		_content.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
		_anchor = anchor;
		_handler = new Handler(this);
		setMinWidth(0);
		setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
		setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		setBackgroundDrawable(anchor.getResources().getDrawable(R.drawable.preview_popup));
		setContentView(_content);
		setClippingEnabled(false);
		setTouchable(false);
	}

	@Override
	public boolean		handleMessage(Message msg)
	{
		forceDismiss();
		return (true);
	}

	public void			forceDismiss()
	{
		setMinWidth(0);
		dismiss();
	}

	public void			setPreview(KeyValue key, int flags)
	{
		StringBuilder		preview;

		if (key == null)
		{
			_handler.sendEmptyMessageDelayed(0, _config.previewDismissTimeout);
			return ;
		}
		_handler.removeMessages(0);
		preview = new StringBuilder();
		if ((flags & KeyValue.FLAG_CTRL) != 0)
			preview.append("Ctrl-");
		if ((flags & KeyValue.FLAG_ALT) != 0)
			preview.append("Alt-");
		if ((flags & KeyValue.FLAG_SHIFT) != 0 && !Character.isLetter(key.getChar(0)))
			preview.append("Shift-");
		preview.append(key.getSymbol(flags));
		_content.setText(preview.toString());
		show();
	}

	private void		setMinWidth(int minWidth)
	{
		_minWidth = minWidth;
		_content.setMinWidth(minWidth);
	}

	private void		show()
	{
		int					x;
		int					y;
		int					width;
		int					height;

		_content.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
		width = _content.getMeasuredWidth();
		height = _content.getMeasuredHeight();
		if (width > _minWidth)
			setMinWidth(width);
		x = (_anchor.getMeasuredWidth() - width) / 2;
		y = -(height + _config.previewBottomMargin);
		if (!isShowing())
			showAtLocation(_anchor, Gravity.NO_GRAVITY, x, y);
		update(x, y, width, height);
	}
}
