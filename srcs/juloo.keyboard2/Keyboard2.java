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
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Keyboard2 extends InputMethodService
  implements SharedPreferences.OnSharedPreferenceChangeListener
{
  private Keyboard2View _keyboardView;
  private KeyEventHandler _keyeventhandler;
  // If not 'null', the layout to use instead of [_currentTextLayout].
  private KeyboardData _currentSpecialLayout;
  private Current_text_layout _currentTextLayout;
  // Layout associated with the currently selected locale. Not 'null'.
  private KeyboardData _localeTextLayout;
  private ViewGroup _emojiPane = null;
  public int actionId; // Action performed by the Action key.

  private Config _config;

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

  void setSpecialLayout(KeyboardData l)
  {
    _currentSpecialLayout = l;
    _keyboardView.setKeyboard(l);
  }

  KeyboardData loadLayout(int layout_id)
  {
    return KeyboardData.load(getResources(), layout_id);
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
    Logs.set_debug_logs(getResources().getBoolean(R.bool.debug_logs));
  }

  private List<InputMethodSubtype> getEnabledSubtypes(InputMethodManager imm)
  {
    String pkg = getPackageName();
    for (InputMethodInfo imi : imm.getEnabledInputMethodList())
      if (imi.getPackageName().equals(pkg))
        return imm.getEnabledInputMethodSubtypeList(imi, true);
    return Arrays.asList();
  }

  private void extra_keys_of_subtype(ExtraKeys dst, InputMethodSubtype subtype)
  {
    String extra_keys = subtype.getExtraValueOf("extra_keys");
    String script = subtype.getExtraValueOf("script");
    if (extra_keys == null)
      return;
    dst.add_keys_for_script(script, ExtraKeys.parse_extra_keys(extra_keys));
  }

  private void refreshAccentsOption(InputMethodManager imm, InputMethodSubtype subtype)
  {
    ExtraKeys extra_keys = new ExtraKeys();
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

  InputMethodManager get_imm()
  {
    return (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
  }

  private void refreshSubtypeImm()
  {
    InputMethodManager imm = get_imm();
    if (VERSION.SDK_INT < 28)
      _config.shouldOfferSwitchingToNextInputMethod = true;
    else
      _config.shouldOfferSwitchingToNextInputMethod = shouldOfferSwitchingToNextInputMethod();
    _config.shouldOfferVoiceTyping = (get_voice_typing_im(imm) != null);
    KeyboardData default_layout = null;
    _config.extra_keys_subtype = null;
    if (VERSION.SDK_INT >= 12)
    {
      InputMethodSubtype subtype = imm.getCurrentInputMethodSubtype();
      if (subtype != null)
      {
        String s = subtype.getExtraValueOf("default_layout");
        if (s != null)
          default_layout = _config.layout_of_string(getResources(), s);
        refreshAccentsOption(imm, subtype);
      }
    }
    if (default_layout == null)
      default_layout = KeyboardData.load(getResources(), R.xml.latn_qwerty_us);
    _localeTextLayout = default_layout;
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
      actionId = info.actionId;
      _config.swapEnterActionKey = false;
    }
    else
    {
      int action = info.imeOptions & EditorInfo.IME_MASK_ACTION;
      _config.actionLabel = actionLabel_of_imeAction(action); // Might be null
      actionId = action;
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

  /** Returns the id and subtype of the voice typing IM. Returns [null] if none
      is installed or if the feature is unsupported. */
  SimpleEntry<String, InputMethodSubtype> get_voice_typing_im(InputMethodManager imm)
  {
    if (VERSION.SDK_INT < 11) // Due to InputMethodSubtype
      return null;
    for (InputMethodInfo im : imm.getEnabledInputMethodList())
      for (InputMethodSubtype imst : imm.getEnabledInputMethodSubtypeList(im, true))
        // Switch to the first IM that has a subtype of this mode
        if (imst.getMode().equals("voice"))
          return new SimpleEntry(im.getId(), imst);
    return null;
  }

  private void refresh_special_layout(EditorInfo info)
  {
    switch (info.inputType & InputType.TYPE_MASK_CLASS)
    {
      case InputType.TYPE_CLASS_NUMBER:
      case InputType.TYPE_CLASS_PHONE:
      case InputType.TYPE_CLASS_DATETIME:
        _currentSpecialLayout =
          _config.modify_numpad(KeyboardData.load(getResources(), R.xml.pin));
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
    Logs.debug_startup_input_view(info, _config);
  }

  @Override
  public void setInputView(View v)
  {
    ViewParent parent = v.getParent();
    if (parent != null && parent instanceof ViewGroup)
      ((ViewGroup)parent).removeView(v);
    super.setInputView(v);
    updateSoftInputWindowLayoutParams();
  }


  @Override
  public void updateFullscreenMode() {
    super.updateFullscreenMode();
    updateSoftInputWindowLayoutParams();
  }

  private void updateSoftInputWindowLayoutParams() {
    final Window window = getWindow().getWindow();
    updateLayoutHeightOf(window, ViewGroup.LayoutParams.MATCH_PARENT);
    final View inputArea = window.findViewById(android.R.id.inputArea);

    updateLayoutHeightOf(
            (View) inputArea.getParent(),
            isFullscreenMode()
                    ? ViewGroup.LayoutParams.MATCH_PARENT
                    : ViewGroup.LayoutParams.WRAP_CONTENT);
    updateLayoutGravityOf((View) inputArea.getParent(), Gravity.BOTTOM);

  }

  private static void updateLayoutHeightOf(final Window window, final int layoutHeight) {
    final WindowManager.LayoutParams params = window.getAttributes();
    if (params != null && params.height != layoutHeight) {
      params.height = layoutHeight;
      window.setAttributes(params);
    }
  }

  private static void updateLayoutHeightOf(final View view, final int layoutHeight) {
    final ViewGroup.LayoutParams params = view.getLayoutParams();
    if (params != null && params.height != layoutHeight) {
      params.height = layoutHeight;
      view.setLayoutParams(params);
    }
  }

  private static void updateLayoutGravityOf(final View view, final int layoutGravity) {
    final ViewGroup.LayoutParams lp = view.getLayoutParams();
    if (lp instanceof LinearLayout.LayoutParams) {
      final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) lp;
      if (params.gravity != layoutGravity) {
        params.gravity = layoutGravity;
        view.setLayoutParams(params);
      }
    } else if (lp instanceof FrameLayout.LayoutParams) {
      final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) lp;
      if (params.gravity != layoutGravity) {
        params.gravity = layoutGravity;
        view.setLayoutParams(params);
      }
    }
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
    public void handle_event_key(KeyValue.Event ev)
    {
      switch (ev)
      {
        case CONFIG:
          Intent intent = new Intent(Keyboard2.this, SettingsActivity.class);
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          startActivity(intent);
          break;

        case SWITCH_TEXT:
          _currentSpecialLayout = null;
          _keyboardView.setKeyboard(current_layout());
          break;

        case SWITCH_NUMERIC:
          setSpecialLayout(_config.modify_numpad(loadLayout(R.xml.numeric)));
          break;

        case SWITCH_EMOJI:
          if (_emojiPane == null)
            _emojiPane = (ViewGroup)inflate_view(R.layout.emoji_pane);
          setInputView(_emojiPane);
          break;

        case SWITCH_BACK_EMOJI:
          setInputView(_keyboardView);
          break;

        case CHANGE_METHOD:
          get_imm().showInputMethodPicker();
          break;

        case CHANGE_METHOD_PREV:
          if (VERSION.SDK_INT < 28)
            get_imm().switchToLastInputMethod(getConnectionToken());
          else
            switchToPreviousInputMethod();
          break;

        case ACTION:
          InputConnection conn = getCurrentInputConnection();
          if (conn != null)
            conn.performEditorAction(actionId);
          break;

        case SWITCH_SECOND:
          if (_config.second_layout != null)
            setTextLayout(Current_text_layout.SECONDARY);
          break;

        case SWITCH_SECOND_BACK:
          setTextLayout(Current_text_layout.PRIMARY);
          break;

        case SWITCH_GREEKMATH:
          setSpecialLayout(_config.modify_numpad(loadLayout(R.xml.greekmath)));
          break;

        case CAPS_LOCK:
          set_shift_state(true, true);
          break;

        case SWITCH_VOICE_TYPING:
          SimpleEntry<String, InputMethodSubtype> im = get_voice_typing_im(get_imm());
          if (im == null)
            return;
          // Best-effort. Good enough for triggering Google's voice typing.
          if (VERSION.SDK_INT < 28)
            switchInputMethod(im.getKey());
          else
            switchInputMethod(im.getKey(), im.getValue());
          break;
      }
    }

    public void set_shift_state(boolean state, boolean lock)
    {
      _keyboardView.set_shift_state(state, lock);
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
