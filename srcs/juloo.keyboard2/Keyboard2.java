package juloo.keyboard2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.os.Build.VERSION;
import android.os.IBinder;
import android.text.InputType;
import android.util.Log;
import android.util.LogPrinter;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Keyboard2 extends InputMethodService
  implements SharedPreferences.OnSharedPreferenceChangeListener
{
  static private final String TAG = "Keyboard2";

  private Keyboard2View _keyboardView;
  private KeyEventHandler _keyeventhandler;
  // If not 'null', the layout to use instead of [_currentTextLayout].
  private KeyboardData _currentSpecialLayout;
  private Current_text_layout _currentTextLayout;
  // Layout associated with the currently selected locale.
  private KeyboardData _localeTextLayout;
  private ViewGroup _emojiPane = null;

  private Config _config;

  private boolean _debug_logs = false;

  /** Layout currently visible. */
  KeyboardData current_layout()
  {
    if (_currentSpecialLayout != null)
      return _currentSpecialLayout;
    KeyboardData layout;
    if (_currentTextLayout == Current_text_layout.SECONDARY)
      layout = _config.second_layout;
    else if (_config.layout == null)
      layout = _localeTextLayout;
    else
      layout = _config.layout;
    return _config.modify_layout(layout);
  }

  void setTextLayout(Current_text_layout layout)
  {
    _currentTextLayout = layout;
    _currentSpecialLayout = null;
    _keyboardView.setKeyboard(current_layout());
  }

  @Override
  public void onCreate()
  {
    super.onCreate();
    KeyboardData.init(getResources());
    SharedPreferences prefs = DirectBootAwarePreferences.get_shared_preferences(this);
    prefs.registerOnSharedPreferenceChangeListener(this);
    _keyeventhandler = new KeyEventHandler(getMainLooper(), this.new Receiver());
    Config.initGlobalConfig(prefs, getResources(), _keyeventhandler);
    _config = Config.globalConfig();
    _keyboardView = (Keyboard2View)inflate_view(R.layout.keyboard);
    _keyboardView.reset();
    _debug_logs = getResources().getBoolean(R.bool.debug_logs);
  }

  private List<InputMethodSubtype> getEnabledSubtypes(InputMethodManager imm)
  {
    String pkg = getPackageName();
    for (InputMethodInfo imi : imm.getEnabledInputMethodList())
      if (imi.getPackageName().equals(pkg))
        return imm.getEnabledInputMethodSubtypeList(imi, true);
    return Arrays.asList();
  }

  private void refreshSubtypeLayout(InputMethodSubtype subtype)
  {
    String s = subtype.getExtraValueOf("default_layout");
    if (s != null)
      _localeTextLayout = _config.layout_of_string(getResources(), s);
    else
      _localeTextLayout = KeyboardData.load(getResources(), R.xml.qwerty);
  }

  private void extra_keys_of_subtype(Set<KeyValue> dst, InputMethodSubtype subtype)
  {
    String extra_keys = subtype.getExtraValueOf("extra_keys");
    if (extra_keys == null)
      return;
    String[] ks = extra_keys.split("\\|");
    for (int i = 0; i < ks.length; i++)
      dst.add(KeyValue.getKeyByName(ks[i]));
  }

  private void refreshAccentsOption(InputMethodManager imm, InputMethodSubtype subtype)
  {
    HashSet<KeyValue> extra_keys = new HashSet<KeyValue>();
    List<InputMethodSubtype> enabled_subtypes = getEnabledSubtypes(imm);
    switch (_config.accents)
    {
      // '3' was "all accents", now unused
      case 1: case 3:
        extra_keys_of_subtype(extra_keys, subtype);
        for (InputMethodSubtype s : enabled_subtypes)
          extra_keys_of_subtype(extra_keys, s);
        break;
      case 2:
        extra_keys_of_subtype(extra_keys, subtype);
        break;
      case 4: break;
      default: throw new IllegalArgumentException();
    }
    _config.extra_keys_subtype = extra_keys;
    if (enabled_subtypes.size() > 1)
      _config.shouldOfferSwitchingToNextInputMethod = true;
  }

  private void refreshSubtypeLegacyFallback()
  {
    // Fallback for the accents option: Only respect the "None" case
    switch (_config.accents)
    {
      case 1: case 2: case 3: _config.extra_keys_subtype = null; break;
      case 4: _config.extra_keys_subtype = new HashSet<KeyValue>(); break;
    }
  }

  private void refreshSubtypeImm()
  {
    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
    if (VERSION.SDK_INT < 28)
      _config.shouldOfferSwitchingToNextInputMethod = true;
    else
      _config.shouldOfferSwitchingToNextInputMethod = shouldOfferSwitchingToNextInputMethod();
    if (VERSION.SDK_INT < 12)
    {
      // Subtypes won't work well under API level 12 (getExtraValueOf)
      refreshSubtypeLegacyFallback();
    }
    else
    {
      InputMethodSubtype subtype = imm.getCurrentInputMethodSubtype();
      if (subtype == null)
      {
        // On some rare cases, [subtype] is null.
        refreshSubtypeLegacyFallback();
      }
      else
      {
        refreshSubtypeLayout(subtype);
        refreshAccentsOption(imm, subtype);
      }
    }
    if (_config.second_layout == null)
    {
      _config.shouldOfferSwitchingToSecond = false;
      _currentTextLayout = Current_text_layout.PRIMARY;
    }
    else
    {
      _config.shouldOfferSwitchingToSecond = true;
    }
  }

  private String actionLabel_of_imeAction(int action)
  {
    int res;
    switch (action)
    {
      case EditorInfo.IME_ACTION_NEXT: res = R.string.key_action_next; break;
      case EditorInfo.IME_ACTION_DONE: res = R.string.key_action_done; break;
      case EditorInfo.IME_ACTION_GO: res = R.string.key_action_go; break;
      case EditorInfo.IME_ACTION_PREVIOUS: res = R.string.key_action_prev; break;
      case EditorInfo.IME_ACTION_SEARCH: res = R.string.key_action_search; break;
      case EditorInfo.IME_ACTION_SEND: res = R.string.key_action_send; break;
      case EditorInfo.IME_ACTION_UNSPECIFIED:
      case EditorInfo.IME_ACTION_NONE:
      default: return null;
    }
    return getResources().getString(res);
  }

  private void refresh_action_label(EditorInfo info)
  {
    // First try to look at 'info.actionLabel', if it isn't set, look at
    // 'imeOptions'.
    if (info.actionLabel != null)
    {
      _config.actionLabel = info.actionLabel.toString();
      _keyeventhandler.actionId = info.actionId;
      _config.swapEnterActionKey = false;
    }
    else
    {
      int action = info.imeOptions & EditorInfo.IME_MASK_ACTION;
      _config.actionLabel = actionLabel_of_imeAction(action); // Might be null
      _keyeventhandler.actionId = action;
      _config.swapEnterActionKey =
        (info.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0;
    }
  }

  /** Might re-create the keyboard view. [_keyboardView.setKeyboard()] and
      [setInputView()] must be called soon after. */
  private void refresh_config()
  {
    int prev_theme = _config.theme;
    _config.refresh(getResources());
    refreshSubtypeImm();
    // Refreshing the theme config requires re-creating the views
    if (prev_theme != _config.theme)
    {
      _keyboardView = (Keyboard2View)inflate_view(R.layout.keyboard);
      _emojiPane = null;
    }
    _keyboardView.reset();
  }

  private void log_editor_info(EditorInfo info)
  {
    LogPrinter p = new LogPrinter(Log.DEBUG, TAG);
    info.dump(p, "");
    if (info.extras != null)
      Log.d(TAG, "extras: "+info.extras.toString());
    Log.d(TAG, "swapEnterActionKey: "+_config.swapEnterActionKey);
    Log.d(TAG, "actionLabel: "+_config.actionLabel);
  }

  private void refresh_special_layout(EditorInfo info)
  {
    switch (info.inputType & InputType.TYPE_MASK_CLASS)
    {
      case InputType.TYPE_CLASS_NUMBER:
      case InputType.TYPE_CLASS_PHONE:
      case InputType.TYPE_CLASS_DATETIME:
        _currentSpecialLayout = KeyboardData.load_pin_entry(getResources());
        break;
      default:
        _currentSpecialLayout = null;
        break;
    }
  }

  @Override
  public void onStartInputView(EditorInfo info, boolean restarting)
  {
    refresh_config();
    refresh_action_label(info);
    refresh_special_layout(info);
    _keyboardView.setKeyboard(current_layout());
    _keyeventhandler.started(info);
    setInputView(_keyboardView);
    if (_debug_logs)
      log_editor_info(info);
  }

  @Override
  public void setInputView(View v)
  {
    ViewParent parent = v.getParent();
    if (parent != null && parent instanceof ViewGroup)
      ((ViewGroup)parent).removeView(v);
    super.setInputView(v);
  }

  @Override
  public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype)
  {
    refreshSubtypeImm();
    _keyboardView.setKeyboard(current_layout());
  }

  @Override
  public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd)
  {
    super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
    _keyeventhandler.selection_updated(oldSelStart, newSelStart);
  }

  @Override
  public void onFinishInputView(boolean finishingInput)
  {
    super.onFinishInputView(finishingInput);
    _keyboardView.reset();
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences _prefs, String _key)
  {
    refresh_config();
    setInputView(_keyboardView);
    _keyboardView.setKeyboard(current_layout());
  }

  @Override
  public boolean onEvaluateFullscreenMode()
  {
    /* Entirely disable fullscreen mode. */
    return false;
  }

  /** Not static */
  public class Receiver implements KeyEventHandler.IReceiver
  {
    public void switchToNextInputMethod()
    {
      InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
      imm.showInputMethodPicker();
      // deprecated in version 28: imm.switchToNextInputMethod(getConnectionToken(), false);
      // added in version 28: switchToNextInputMethod(false);
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

    public void set_shift_state(boolean state, boolean lock)
    {
      _keyboardView.set_shift_state(state, lock);
    }

    public void switch_text()
    {
      _currentSpecialLayout = null;
      _keyboardView.setKeyboard(current_layout());
    }

    public void switch_layout(int layout_id)
    {
      _keyboardView.setKeyboard(KeyboardData.load(getResources(), layout_id));
    }

    public void switch_second()
    {
      if (_config.second_layout != null)
        setTextLayout(Current_text_layout.SECONDARY);
    }

    public void switch_primary()
    {
      setTextLayout(Current_text_layout.PRIMARY);
    }

    public void showKeyboardConfig()
    {
      Intent intent = new Intent(Keyboard2.this, SettingsActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(intent);
    }

    public InputConnection getCurrentInputConnection()
    {
      return Keyboard2.this.getCurrentInputConnection();
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

  private static enum Current_text_layout
  {
    PRIMARY,
    SECONDARY
  }
}
