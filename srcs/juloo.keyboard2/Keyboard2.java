package juloo.keyboard2;

import android.inputmethodservice.InputMethodService;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

public class Keyboard2 extends InputMethodService
{
	private KeyboardData	_keyboardData; // TODO: settings
	private Keyboard2View	_inputView = null;

	@Override
	public void				onCreate()
	{
		super.onCreate();
		setKeyboard(R.xml.azerty);
	}

	@Override
	public View				onCreateInputView()
	{
		_inputView = (Keyboard2View)getLayoutInflater().inflate(R.layout.input, null);
		_inputView.setKeyboard(this, _keyboardData);
		return (_inputView);
	}

	private void			setKeyboard(int res)
	{
		_keyboardData = new KeyboardData(getResources().getXml(res));
		if (_inputView != null)
			_inputView.setKeyboard(this, _keyboardData);
	}

	public void				handleKeyUp(KeyValue key, int flags)
	{
		if (getCurrentInputConnection() == null)
			return ;
		if (key.getEventCode() == KeyValue.EVENT_CONFIG)
		{
			// TODO improve this shit
			final CharSequence layouts[] = new CharSequence[]{"Azerty", "Qwerty"};
			final int layout_res[] = new int[]{R.xml.azerty, R.xml.qwerty};
			final Keyboard2 self = this;
			android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
				.setTitle("Change keyboard layout")
				.setItems(layouts, new android.content.DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(android.content.DialogInterface dialog, int which)
					{
						self.setKeyboard(layout_res[which]);
					}
				})
				.create();
			android.view.WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
				lp.token = _inputView.getWindowToken();
				lp.type = android.view.WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
				dialog.getWindow().setAttributes(lp);
				dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
			dialog.show();
		}
		else if ((flags & (KeyValue.FLAG_CTRL | KeyValue.FLAG_ALT)) != 0)
			handleMetaKeyUp(key, flags);
		else if (key.getEventCode() == KeyEvent.KEYCODE_DEL)
			handleDelKey(1, 0);
		else if (key.getEventCode() == KeyEvent.KEYCODE_FORWARD_DEL)
			handleDelKey(0, 1);
		else if (key.getChar(false) == KeyValue.CHAR_NONE && key.getEventCode() != KeyValue.EVENT_NONE)
			handleMetaKeyUp(key, flags);
		else if (key.getChar(false) != KeyValue.CHAR_NONE)
			sendKeyChar(key.getChar((flags & KeyValue.FLAG_SHIFT) != 0));
	}

	private void			handleDelKey(int before, int after)
	{
		CharSequence	selection = getCurrentInputConnection().getSelectedText(0);

		if (selection != null && selection.length() > 0)
			getCurrentInputConnection().commitText("", 1);
		else
			getCurrentInputConnection().deleteSurroundingText(before, after);
	}

	private void			handleMetaKeyUp(KeyValue key, int flags)
	{
		int			metaState = 0;
		KeyEvent	event;

		if (key.getEventCode() == KeyValue.EVENT_NONE)
			return ;
		if ((flags & KeyValue.FLAG_CTRL) != 0)
			metaState |= KeyEvent.META_CTRL_LEFT_ON | KeyEvent.META_CTRL_ON;
		if ((flags & KeyValue.FLAG_ALT) != 0)
			metaState |= KeyEvent.META_ALT_LEFT_ON | KeyEvent.META_ALT_ON;
		if ((flags & KeyValue.FLAG_SHIFT) != 0)
			metaState |= KeyEvent.META_SHIFT_LEFT_ON | KeyEvent.META_SHIFT_ON;
		event = new KeyEvent(1, 1, KeyEvent.ACTION_DOWN, key.getEventCode(), 1, metaState);
		getCurrentInputConnection().sendKeyEvent(event);
		getCurrentInputConnection().sendKeyEvent(KeyEvent.changeAction(event, KeyEvent.ACTION_UP));
	}
}
