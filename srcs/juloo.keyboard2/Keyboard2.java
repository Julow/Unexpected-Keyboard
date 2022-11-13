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
  private int _currentTextLayout;
  private ViewGroup _emojiPane = null;

  private Config _config;

  private boolean _debug_logs = false;

  private KeyboardData getLayout(int resId)
  {
    return KeyboardData.load(getResources(), resId);
  }

  @Override
  public void onCreate()
  {
    super.onCreate();
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
    int l = _config.layout;
    if (l == -1)
    {
      String s = subtype.getExtraValueOf("default_layout");
      if (s != null)
        l = Config.layoutId_of_string(s);
      else
        l = R.xml.qwerty;
    }
    _currentTextLayout = l;
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
    _config.shouldOfferSwitchingToSecond =
      _config.second_layout != -1 &&
      _currentTextLayout != _config.second_layout;
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

  private void refreshConfig()
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

  private int chooseLayout(EditorInfo info)
  {
    switch (info.inputType & InputType.TYPE_MASK_CLASS)
    {
      case InputType.TYPE_CLASS_NUMBER:
        return R.xml.pin;
      case InputType.TYPE_CLASS_PHONE:
      case InputType.TYPE_CLASS_DATETIME:
        return R.xml.pin;
      default:
        return _currentTextLayout;
    }
  }

  @Override
  public void onStartInputView(EditorInfo info, boolean restarting)
  {
    refreshConfig();
    refresh_action_label(info);
    _keyboardView.setKeyboard(getLayout(chooseLayout(info)));
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
    _keyboardView.setKeyboard(getLayout(_currentTextLayout));
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
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
  {
    refreshConfig();
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

    public void switch_main()
    {
      _keyboardView.setKeyboard(getLayout(_currentTextLayout));
    }

    public void switch_layout(int layout_id)
    {
      _keyboardView.setKeyboard(getLayout(layout_id));
    }

    public void switch_second()
    {
      if (_config.second_layout == -1)
        return;
      KeyboardData layout =
        getLayout(_config.second_layout).mapKeys(new KeyboardData.MapKeyValues() {
          public KeyValue apply(KeyValue key, boolean localized)
          {
            if (key.getKind() == KeyValue.Kind.Event
                && key.getEvent() == KeyValue.Event.SWITCH_SECOND)
              return KeyValue.getKeyByName("switch_second_back");
            return key;
          }
        });
      _keyboardView.setKeyboard(layout);
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
}
