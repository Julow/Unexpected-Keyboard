package juloo.keyboard2;

import android.content.Context;
import android.content.res.Configuration;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.inputmethodservice.InputMethodService;
import android.os.Bundle;
import android.text.InputType;
import android.preference.PreferenceManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Keyboard2 extends InputMethodService
	implements SharedPreferences.OnSharedPreferenceChangeListener
{
	private Keyboard2View	_keyboardView;
  private int _currentTextLayout;
	private ViewGroup		_emojiPane = null;
	private Typeface		_specialKeyFont = null;

	private Config			_config;

  private Map<Integer, KeyboardData> _layoutCache = new HashMap<Integer, KeyboardData>();

  private static final int DEFAULT_LAYOUT = R.xml.qwerty;
  private static final Map<String, Integer> LAYOUTS = new HashMap<String, Integer>();

  private static void add_layout(String lang, int resId)
  {
    LAYOUTS.put(new Locale(lang).getLanguage(), resId);
  }

  static
  {
    add_layout("fr", R.xml.azerty);
  }

  private KeyboardData getLayout(int resId)
  {
    KeyboardData l = _layoutCache.get(resId);
    if (l == null)
    {
      l = KeyboardData.parse(getResources().getXml(resId));
      _layoutCache.put(resId, l);
    }
    return l;
  }

	@Override
	public void				onCreate()
	{
		super.onCreate();
		_specialKeyFont = Typeface.createFromAsset(getAssets(), "fonts/keys.ttf");
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
		_config = new Config(this);
		_keyboardView = (Keyboard2View)getLayoutInflater().inflate(R.layout.keyboard, null);
		_keyboardView.reset();
	}

	public Config			getConfig()
	{
		return (_config);
	}

	public Typeface			getSpecialKeyFont()
	{
		return (_specialKeyFont);
	}

  private void refreshSubtype(InputMethodSubtype subtype)
  {
    Integer l = LAYOUTS.get(subtype.getLanguageTag());
    if (l == null)
      l = DEFAULT_LAYOUT;
    _currentTextLayout = l;
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
    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
    _config.shouldOfferSwitchingToNextInputMethod = imm.shouldOfferSwitchingToNextInputMethod(getCurrentInputBinding().getConnectionToken());
    refreshSubtype(imm.getCurrentInputMethodSubtype());
		if ((info.inputType & InputType.TYPE_CLASS_NUMBER) != 0)
      _keyboardView.setKeyboard(getLayout(R.xml.numeric));
    else
      _keyboardView.setKeyboard(getLayout(_currentTextLayout));
	}

  @Override
  public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype)
  {
    refreshSubtype(subtype);
    _keyboardView.setKeyboard(getLayout(_currentTextLayout));
  }

	@Override
	public void				onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		_config.refresh();
		_keyboardView.reset();
	}

	@Override
	public void				onConfigurationChanged(Configuration newConfig)
	{
		_keyboardView.reset();
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
			_keyboardView.setKeyboard(getLayout(_currentTextLayout));
		else if (eventCode == KeyValue.EVENT_SWITCH_NUMERIC)
			_keyboardView.setKeyboard(getLayout(R.xml.numeric));
		else if (eventCode == KeyValue.EVENT_SWITCH_EMOJI)
		{
			if (_emojiPane == null)
				_emojiPane = (ViewGroup)getLayoutInflater().inflate(R.layout.emoji_pane, null);
			setInputView(_emojiPane);
		}
		else if (eventCode == KeyValue.EVENT_SWITCH_BACK_EMOJI)
			setInputView(_keyboardView);
		else if (eventCode == KeyValue.EVENT_CHANGE_METHOD)
		{
			InputMethodManager	imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);

			imm.switchToNextInputMethod(getCurrentInputBinding().getConnectionToken(), false);
		}
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
		else
			sendKeyChar(keyChar);
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
