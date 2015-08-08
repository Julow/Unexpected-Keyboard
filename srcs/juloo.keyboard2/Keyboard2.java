package juloo.keyboard2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

public class Keyboard2 extends InputMethodService
	implements SharedPreferences.OnSharedPreferenceChangeListener
{
	private Keyboard2View	_inputView = null;

	@Override
	public void				onCreate()
	{
		super.onCreate();
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
		_inputView = (Keyboard2View)getLayoutInflater().inflate(R.layout.input, null);
		_inputView.reset_prefs(this);
	}

	@Override
	public View				onCreateInputView()
	{
		ViewGroup		parent = (ViewGroup)_inputView.getParent();

		if (parent != null)
			parent.removeView(_inputView);
		_inputView.reset();
		return (_inputView);
	}

	@Override
	public void				onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		_inputView.reset_prefs(this);
	}

	public void				handleKeyUp(KeyValue key, int flags)
	{
		if (getCurrentInputConnection() == null)
			return ;
		if (key.getEventCode() == KeyValue.EVENT_CONFIG)
		{
			Intent intent = new Intent(this, SettingsActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
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
