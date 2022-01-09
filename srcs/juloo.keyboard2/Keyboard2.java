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
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.view.ContextThemeWrapper;
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
  private Keyboard2View _keyboardView;
  private int _currentTextLayout;
  private ViewGroup _emojiPane = null;

  private Config _config;

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
  public void onCreate()
  {
    super.onCreate();
    PreferenceManager.setDefaultValues(this, R.xml.settings, false);
    PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    Config.initGlobalConfig(this, new KeyEventHandler(this.new Receiver()));
    _config = Config.globalConfig();
    _keyboardView = (Keyboard2View)inflate_view(R.layout.keyboard);
    _keyboardView.reset();
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

  private int extra_keys_of_subtype(InputMethodSubtype subtype)
  {
    String extra_keys = subtype.getExtraValueOf("extra_keys");
    int flags = 0;
    if (extra_keys != null)
      for (String acc : extra_keys.split("\\|"))
        flags |= Config.extra_key_flag_of_name(acc);
    return flags;
  }

  private void refreshAccentsOption(InputMethodManager imm, InputMethodSubtype subtype)
  {
    int to_keep = 0;
    switch (_config.accents)
    {
      case 1:
        to_keep |= extra_keys_of_subtype(subtype);
        for (InputMethodSubtype s : getEnabledSubtypes(imm))
          to_keep |= extra_keys_of_subtype(s);
        break;
      case 2: to_keep |= extra_keys_of_subtype(subtype); break;
      case 3: to_keep = KeyValue.FLAGS_HIDDEN_KEYS; break;
      case 4: break;
      default: throw new IllegalArgumentException();
    }
    _config.key_flags_to_remove = ~to_keep & KeyValue.FLAGS_HIDDEN_KEYS;
  }

  private void refreshSubtypeLegacyFallback()
  {
    // Fallback for the accents option: Only respect the "None" case
    switch (_config.accents)
    {
      case 1: case 2: case 3: _config.key_flags_to_remove = 0; break;
      case 4: _config.key_flags_to_remove = KeyValue.FLAGS_HIDDEN_KEYS; break;
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

  private String actionLabel_of_imeAction(int action)
  {
    switch (action)
    {
      case EditorInfo.IME_ACTION_UNSPECIFIED:
      case EditorInfo.IME_ACTION_NEXT: return "Next";
      case EditorInfo.IME_ACTION_DONE: return "Done";
      case EditorInfo.IME_ACTION_GO: return "Go";
      case EditorInfo.IME_ACTION_PREVIOUS: return "Prev";
      case EditorInfo.IME_ACTION_SEARCH: return "Search";
      case EditorInfo.IME_ACTION_SEND: return "Send";
      case EditorInfo.IME_ACTION_NONE:
      default: return null;
    }
  }

  private void refreshEditorInfo(EditorInfo info)
  {
    // First try to look at 'info.actionLabel', if it isn't set, look at
    // 'imeOptions'.
    if (info.actionLabel != null)
    {
      _config.actionLabel = info.actionLabel.toString();
      _config.actionId = info.actionId;
      _config.swapEnterActionKey = false;
    }
    else
    {
      int action = info.imeOptions & EditorInfo.IME_MASK_ACTION;
      _config.actionLabel = actionLabel_of_imeAction(action); // Might be null
      _config.actionId = action;
      _config.swapEnterActionKey =
        (info.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0;
    }
  }

  @Override
  public void onStartInputView(EditorInfo info, boolean restarting)
  {
    // Update '_config' before calling 'KeyboardView.setKeyboard'
    refreshSubtypeImm();
    refreshEditorInfo(info);
    if ((info.inputType & InputType.TYPE_CLASS_NUMBER) != 0)
      _keyboardView.setKeyboard(getLayout(R.xml.numeric));
    else
      _keyboardView.setKeyboard(getLayout(_currentTextLayout));
    _keyboardView.reset(); // Layout might need to change due to rotation
    setInputView(_keyboardView);
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
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
  {
    int prev_theme = _config.theme;
    _config.refresh(this);
    refreshSubtypeImm();
    _keyboardView.refreshConfig(getLayout(_currentTextLayout));
    // Refreshing the theme config requires re-creating the views
    if (prev_theme != _config.theme)
    {
      _keyboardView = (Keyboard2View)inflate_view(R.layout.keyboard);
      _emojiPane = null;
    }
  }

  /** Not static */
  public class Receiver implements KeyEventHandler.IReceiver
  {
    public void switchToNextInputMethod()
    {
      InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
      imm.switchToNextInputMethod(getConnectionToken(), false);
    }

    public void setPane_emoji()
    {
      if (_emojiPane == null)
        _emojiPane = (ViewGroup)inflate_view(R.layout.emoji_pane);
      setInputView(_emojiPane);
    }

    public void setPane_normal()
    {
      setInputView(_keyboardView);
    }

    public void performAction()
    {
      InputConnection conn = getCurrentInputConnection();
      if (conn == null)
        return;
      conn.performEditorAction(_config.actionId);
    }

    public void setLayout(int res_id)
    {
      if (res_id == -1)
        res_id = _currentTextLayout;
      _keyboardView.setKeyboard(getLayout(res_id));
    }

    public void sendKeyEvent(int eventCode, int meta)
    {
      InputConnection conn = getCurrentInputConnection();
      if (conn == null)
        return;
      KeyEvent event = new KeyEvent(1, 1, KeyEvent.ACTION_DOWN, eventCode, 0, meta);
      conn.sendKeyEvent(event);
      conn.sendKeyEvent(KeyEvent.changeAction(event, KeyEvent.ACTION_UP));
    }

    public void showKeyboardConfig()
    {
      Intent intent = new Intent(Keyboard2.this, SettingsActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(intent);
    }

    public void commitText(String text)
    {
      getCurrentInputConnection().commitText(text, 1);
    }

    public void commitChar(char c)
    {
      sendKeyChar(c);
    }
  }

  private IBinder getConnectionToken()
  {
    return getWindow().getWindow().getAttributes().token;
  }

  private View inflate_view(int layout)
  {
    return View.inflate(new ContextThemeWrapper(this, _config.theme), layout, null);
  }
}
