package juloo.keyboard2;

import android.content.res.Configuration;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
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
	private Keyboard2View	_keyboardView;
	private KeyboardData	_textKeyboard = null;
	private KeyboardData	_numericKeyboard = null;
	private ViewGroup		_emojiPane = null;
	private Typeface		_specialKeyFont = null;

	@Override
	public void				onCreate()
	{
		super.onCreate();
		_specialKeyFont = Typeface.createFromAsset(getAssets(), "fonts/keys.ttf");
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
		updateConfig();
		_keyboardView = (Keyboard2View)getLayoutInflater().inflate(R.layout.keyboard, null);
		_keyboardView.reset_prefs();
	}

	public Typeface			getSpecialKeyFont()
	{
		return (_specialKeyFont);
	}

	@Override
	public View				onCreateInputView()
	{
		ViewGroup		parent = (ViewGroup)_keyboardView.getParent();

		if (parent != null)
			parent.removeView(_keyboardView);
		return (_keyboardView);
	}

	@Override
	public void				onStartInputView(EditorInfo info, boolean restarting)
	{
		if ((info.inputType & InputType.TYPE_CLASS_NUMBER) != 0)
			_keyboardView.setKeyboard(_numericKeyboard);
		else
			_keyboardView.setKeyboard(_textKeyboard);
	}

	@Override
	public void				onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		updateConfig();
		_keyboardView.reset_prefs();
	}

	@Override
	public void				onAppPrivateCommand(String command, Bundle data)
	{
	}

	@Override
	public void				onConfigurationChanged(Configuration newConfig)
	{
		_keyboardView.reset();
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
		_emojiPane = null;
	}

	public void				handleKeyUp(KeyValue key, int flags)
	{
		int				eventCode = key.getEventCode();
		char			keyChar = key.getChar(flags);

		if (getCurrentInputConnection() == null)
			return ;
		if (eventCode == KeyValue.EVENT_CONFIG)
		{
			Intent intent = new Intent(this, SettingsActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}
		else if (eventCode == KeyValue.EVENT_SWITCH_TEXT)
			_keyboardView.setKeyboard(_textKeyboard);
		else if (eventCode == KeyValue.EVENT_SWITCH_NUMERIC)
			_keyboardView.setKeyboard(_numericKeyboard);
		else if (eventCode == KeyValue.EVENT_SWITCH_EMOJI)
			setInputView(getEmojiPane());
		else if (eventCode == KeyValue.EVENT_SWITCH_BACK_EMOJI)
			setInputView(_keyboardView);
		else if ((flags & (KeyValue.FLAG_CTRL | KeyValue.FLAG_ALT)) != 0)
			handleMetaKeyUp(key, flags);
		// else if (eventCode == KeyEvent.KEYCODE_DEL)
		// 	handleDelKey(1, 0);
		// else if (eventCode == KeyEvent.KEYCODE_FORWARD_DEL)
		// 	handleDelKey(0, 1);
		else if (keyChar == KeyValue.CHAR_NONE)
		{
			if (eventCode != KeyValue.EVENT_NONE)
				handleMetaKeyUp(key, flags);
			else
				getCurrentInputConnection().commitText(key.getSymbol(flags), 1);
		}
		else if (keyChar != KeyValue.CHAR_NONE)
			sendKeyChar(keyChar);
	}

	private ViewGroup		getEmojiPane()
	{
		if (_emojiPane == null)
			_emojiPane = (ViewGroup)getLayoutInflater().inflate(R.layout.emoji_pane, null);
		return (_emojiPane);
	}

	// private void			handleDelKey(int before, int after)
	// {
	// 	CharSequence	selection = getCurrentInputConnection().getSelectedText(0);

	// 	if (selection != null && selection.length() > 0)
	// 		getCurrentInputConnection().commitText("", 1);
	// 	else
	// 		getCurrentInputConnection().deleteSurroundingText(before, after);
	// }

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
