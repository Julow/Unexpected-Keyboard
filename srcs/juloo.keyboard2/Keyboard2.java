package juloo.keyboard2;

import android.content.Context;
import android.content.res.Configuration;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.inputmethodservice.InputMethodService;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.IBinder;
import android.text.InputType;
import android.preference.PreferenceManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import java.util.HashMap;
import java.util.List;
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

  private List<InputMethodSubtype> getEnabledSubtypes(InputMethodManager imm)
  {
    String pkg = getPackageName();
    for (InputMethodInfo imi : imm.getEnabledInputMethodList())
      if (imi.getPackageName().equals(pkg))
        return imm.getEnabledInputMethodSubtypeList(imi, true);
    return null;
  }

  private void refreshSubtypeLayout(InputMethodSubtype subtype)
  {
    int l = _config.layout;;
    if (l == -1)
    {
      String s = subtype.getExtraValueOf("default_layout");
      if (s != null)
        l = Config.layoutId_of_string(s);
    }
    _currentTextLayout = l;
  }

  private int accents_of_subtype(InputMethodSubtype subtype)
  {
    String accents_option = subtype.getExtraValueOf("accents");
    int flags = 0;
    if (accents_option != null)
      for (String acc : accents_option.split("\\|"))
        flags |= Config.accentFlag_of_name(acc);
    return flags;
  }

  private void refreshAccentsOption(InputMethodManager imm, InputMethodSubtype subtype)
  {
    final int DONT_REMOVE = KeyValue.FLAG_ACCENT_SUPERSCRIPT | KeyValue.FLAG_ACCENT_SUBSCRIPT;
    int to_keep = DONT_REMOVE;
    switch (_config.accents)
    {
      case 1:
        to_keep |= accents_of_subtype(subtype);
        for (InputMethodSubtype s : getEnabledSubtypes(imm))
          to_keep |= accents_of_subtype(s);
        break;
      case 2: to_keep |= accents_of_subtype(subtype); break;
      case 3: to_keep = KeyValue.FLAGS_ACCENTS; break;
      case 4: break;
      default: throw new IllegalArgumentException();
    }
    _config.accent_flags_to_remove = ~to_keep & KeyValue.FLAGS_ACCENTS;
  }

  private void refreshSubtypeLegacyFallback()
  {
    // Fallback for the accents option: Only respect the "None" case
    switch (_config.accents)
    {
      case 1: case 2: case 3: _config.accent_flags_to_remove = 0; break;
      case 4: _config.accent_flags_to_remove = KeyValue.FLAGS_ACCENTS; break;
    }
    // Fallback for the layout option: Use qwerty in the "system settings" case
    _currentTextLayout = (_config.layout == -1) ? R.xml.qwerty : _config.layout;
  }

  private void refreshSubtypeImm()
  {
    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
    _config.shouldOfferSwitchingToNextInputMethod = imm.shouldOfferSwitchingToNextInputMethod(getConnectionToken());
    if (VERSION.SDK_INT < 12)
    {
      // Subtypes won't work well under API level 12 (getExtraValueOf)
      refreshSubtypeLegacyFallback();
    }
    else
    {
      InputMethodSubtype subtype = imm.getCurrentInputMethodSubtype();
      refreshSubtypeLayout(subtype);
      refreshAccentsOption(imm, subtype);
    }
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
    refreshSubtypeImm();
		if ((info.inputType & InputType.TYPE_CLASS_NUMBER) != 0)
      _keyboardView.setKeyboard(getLayout(R.xml.numeric));
    else
      _keyboardView.setKeyboard(getLayout(_currentTextLayout));
    _keyboardView.reset(); // Layout might need to change due to rotation
	}

  @Override
  public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype)
  {
    refreshSubtypeImm();
    _keyboardView.setKeyboard(getLayout(_currentTextLayout));
  }

  @Override
  public void onFinishInputView(boolean finishingInput)
  {
    super.onFinishInputView(finishingInput);
    _keyboardView.reset();
  }

	@Override
	public void				onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		_config.refresh();
    refreshSubtypeImm();
		_keyboardView.refreshConfig(_config, getLayout(_currentTextLayout));
	}

	@Override
	public void				onConfigurationChanged(Configuration newConfig)
	{
		_keyboardView.reset();
	}

	public void				handleKeyUp(KeyValue key, int flags)
	{
		if (getCurrentInputConnection() == null)
			return ;
    key = KeyModifier.handleFlags(key, flags);
		if (key.eventCode == KeyValue.EVENT_CONFIG)
		{
			Intent intent = new Intent(this, SettingsActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}
		else if (key.eventCode == KeyValue.EVENT_SWITCH_TEXT)
			_keyboardView.setKeyboard(getLayout(_currentTextLayout));
		else if (key.eventCode == KeyValue.EVENT_SWITCH_NUMERIC)
			_keyboardView.setKeyboard(getLayout(R.xml.numeric));
		else if (key.eventCode == KeyValue.EVENT_SWITCH_EMOJI)
		{
			if (_emojiPane == null)
				_emojiPane = (ViewGroup)getLayoutInflater().inflate(R.layout.emoji_pane, null);
			setInputView(_emojiPane);
		}
		else if (key.eventCode == KeyValue.EVENT_SWITCH_BACK_EMOJI)
			setInputView(_keyboardView);
		else if (key.eventCode == KeyValue.EVENT_CHANGE_METHOD)
		{
			InputMethodManager	imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
			imm.switchToNextInputMethod(getConnectionToken(), false);
		}
		else if ((flags & (KeyValue.FLAG_CTRL | KeyValue.FLAG_ALT)) != 0)
			handleMetaKeyUp(key, flags);
		// else if (eventCode == KeyEvent.KEYCODE_DEL)
		// 	handleDelKey(1, 0);
		// else if (eventCode == KeyEvent.KEYCODE_FORWARD_DEL)
		// 	handleDelKey(0, 1);
		else if (key.char_ == KeyValue.CHAR_NONE)
		{
			if (key.eventCode != KeyValue.EVENT_NONE)
				handleMetaKeyUp(key, flags);
			else
				getCurrentInputConnection().commitText(key.symbol, 1);
		}
		else
			sendKeyChar(key.char_);
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

		if (key.eventCode == KeyValue.EVENT_NONE)
			return ;
		if ((flags & KeyValue.FLAG_CTRL) != 0)
			metaState |= KeyEvent.META_CTRL_LEFT_ON | KeyEvent.META_CTRL_ON;
		if ((flags & KeyValue.FLAG_ALT) != 0)
			metaState |= KeyEvent.META_ALT_LEFT_ON | KeyEvent.META_ALT_ON;
		if ((flags & KeyValue.FLAG_SHIFT) != 0)
			metaState |= KeyEvent.META_SHIFT_LEFT_ON | KeyEvent.META_SHIFT_ON;
		event = new KeyEvent(1, 1, KeyEvent.ACTION_DOWN, key.eventCode, 0, metaState);
		getCurrentInputConnection().sendKeyEvent(event);
		getCurrentInputConnection().sendKeyEvent(KeyEvent.changeAction(event, KeyEvent.ACTION_UP));
	}

  private IBinder getConnectionToken()
  {
    return getWindow().getWindow().getAttributes().token;
  }
}
