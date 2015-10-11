package juloo.keyboard2;

import android.content.res.Configuration;
import android.content.Intent;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.os.Bundle;
import android.text.InputType;
import android.preference.PreferenceManager;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

public class Keyboard2 extends InputMethodService
	implements SharedPreferences.OnSharedPreferenceChangeListener
{
	private Keyboard2View	_inputView = null;
	private KeyboardData	_textKeyboard = null;
	private KeyboardData	_numericKeyboard = null;

	@Override
	public void				onCreate()
	{
		super.onCreate();
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
		updateConfig();
		_inputView = (Keyboard2View)getLayoutInflater().inflate(R.layout.input, null);
		_inputView.reset_prefs(this);
	}

	@Override
	public View				onCreateInputView()
	{
		ViewGroup		parent = (ViewGroup)_inputView.getParent();

		if (parent != null)
			parent.removeView(_inputView);
		return (_inputView);
	}

	@Override
	public void				onStartInputView(EditorInfo info, boolean restarting)
	{
		if ((info.inputType & InputType.TYPE_CLASS_NUMBER) != 0)
			_inputView.setKeyboard(_numericKeyboard);
		else
			_inputView.setKeyboard(_textKeyboard);
	}

	@Override
	public void				onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		updateConfig();
		_inputView.reset_prefs(this);
	}

	@Override
	public void				onAppPrivateCommand(String command, Bundle data)
	{
	}

	@Override
	public void				onConfigurationChanged(Configuration newConfig)
	{
		_inputView.reset();
	}

	private void			updateConfig()
	{
		SharedPreferences	prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String				keyboardLayout = prefs.getString("keyboard_layout", null);
		int					xmlRes = 0;

		if (keyboardLayout != null)
			xmlRes = getResources().getIdentifier(keyboardLayout, "xml", getPackageName());
		if (xmlRes == 0)
			xmlRes = R.xml.azerty;
		_textKeyboard = new KeyboardData(getResources().getXml(xmlRes));
		_numericKeyboard = new KeyboardData(getResources().getXml(R.xml.numeric));
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
		else if (key.getEventCode() == KeyValue.EVENT_SWITCH_TEXT)
			_inputView.setKeyboard(_textKeyboard);
		else if (key.getEventCode() == KeyValue.EVENT_SWITCH_NUMERIC)
			_inputView.setKeyboard(_numericKeyboard);
		else if ((flags & (KeyValue.FLAG_CTRL | KeyValue.FLAG_ALT)) != 0)
			handleMetaKeyUp(key, flags);
		else if (key.getEventCode() == KeyEvent.KEYCODE_DEL)
			handleDelKey(1, 0);
		else if (key.getEventCode() == KeyEvent.KEYCODE_FORWARD_DEL)
			handleDelKey(0, 1);
		else if (key.getChar(flags) == KeyValue.CHAR_NONE && key.getEventCode() != KeyValue.EVENT_NONE)
			handleMetaKeyUp(key, flags);
		else if (key.getChar(flags) != KeyValue.CHAR_NONE)
			sendKeyChar(key.getChar(flags));
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
