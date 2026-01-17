package juloo.keyboard2;

import android.content.res.Resources;
import android.text.InputType;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import juloo.keyboard2.CandidatesView;

public final class EditorConfig
{
  public String actionLabel = null; // Might be [null]
  /** Action performed by the Action key. Only when [actionLabel != null]. */
  public int actionId;
  public boolean swapEnterActionKey = false; // Swap the "enter" and "action" keys
  /** Whether selection mode turns on automatically when text is selected. */
  public boolean selection_mode_enabled = true;
  /** Whether the numeric layout should be shown by default. */
  public boolean numeric_layout = false;
  /** Workaround some apps which answers to [getExtractedText] but do not react
      to [setSelection] while returning [true]. */
  public boolean should_move_cursor_force_fallback = false;

  /** Autocapitalisation. */
  public int caps_mode; // Argument for [getCursorCapsMode()]
  // Whether caps state is on initially.
  public boolean caps_initially_enabled = false;
  // Whether caps state should be updated right away.
  public boolean caps_initially_updated = false;

  /** CurrentlyTypedWord. */
  public CharSequence initial_text_before_cursor = null;
  public int initial_sel_start;
  public int initial_sel_end;

  /** Suggestions. */
  // Doesn't override [_config.suggestions_enabled].
  public boolean should_show_candidates_view;

  public EditorConfig() {}

  public void refresh(EditorInfo info, Resources res)
  {
    int inputType = info.inputType & InputType.TYPE_MASK_CLASS;
    int options = info.imeOptions;
    /* Selection mode.
       Editors with [TYPE_NULL] are for example Termux and Emacs. */
    selection_mode_enabled = inputType != InputType.TYPE_NULL;
    /* Action key. Looks at [info.actionLabel] first. */
    if (info.actionLabel != null)
    {
      actionLabel = info.actionLabel.toString();
      actionId = info.actionId;
      swapEnterActionKey = false;
    }
    else
    {
      actionId = options & EditorInfo.IME_MASK_ACTION;
      actionLabel = actionLabel_of_imeAction(actionId, res);
      swapEnterActionKey = (options & EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0;
    }
    /* Numeric layout */
    switch (inputType)
    {
      case InputType.TYPE_CLASS_NUMBER:
      case InputType.TYPE_CLASS_PHONE:
      case InputType.TYPE_CLASS_DATETIME:
        numeric_layout = true;
        break;
      default:
        numeric_layout = false;
        break;
    }
    /* setSelection fallback */
    should_move_cursor_force_fallback = _should_move_cursor_force_fallback(info);
    /* Autocapitalisation */
    caps_mode = info.inputType & TextUtils.CAP_MODE_SENTENCES;
    caps_initially_enabled = (info.initialCapsMode != 0);
    caps_initially_updated = caps_should_update_state(info);
    /* CurrentlyTypedWord */
    initial_text_before_cursor = info.getInitialTextBeforeCursor(10, 0);
    initial_sel_start = info.initialSelStart;
    initial_sel_end = info.initialSelEnd;
    /* Suggestions */
    should_show_candidates_view = CandidatesView.should_show(info);
  }

  String actionLabel_of_imeAction(int action, Resources res)
  {
    int id;
    switch (action)
    {
      case EditorInfo.IME_ACTION_NEXT: id = R.string.key_action_next; break;
      case EditorInfo.IME_ACTION_DONE: id = R.string.key_action_done; break;
      case EditorInfo.IME_ACTION_GO: id = R.string.key_action_go; break;
      case EditorInfo.IME_ACTION_PREVIOUS: id = R.string.key_action_prev; break;
      case EditorInfo.IME_ACTION_SEARCH: id = R.string.key_action_search; break;
      case EditorInfo.IME_ACTION_SEND: id = R.string.key_action_send; break;
      case EditorInfo.IME_ACTION_UNSPECIFIED:
      case EditorInfo.IME_ACTION_NONE:
      default: return null;
    }
    return res.getString(id);
  }

  boolean _should_move_cursor_force_fallback(EditorInfo info)
  {
    // This catch Acode: which sets several variations at once.
    if ((info.inputType & InputType.TYPE_MASK_VARIATION &
          InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0)
      return true;
    // Godot editor: Doesn't handle setSelection() but returns true.
    return info.packageName.startsWith("org.godotengine.editor");
  }

  /** Whether the caps state should be updated when input starts. [inputType]
      is the field from the editor info object. */
  boolean caps_should_update_state(EditorInfo info)
  {
    int class_ = info.inputType & InputType.TYPE_MASK_CLASS;
    int variation = info.inputType & InputType.TYPE_MASK_VARIATION;
    if (class_ != InputType.TYPE_CLASS_TEXT)
      return false;
    switch (variation)
    {
      case InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE:
      case InputType.TYPE_TEXT_VARIATION_NORMAL:
      case InputType.TYPE_TEXT_VARIATION_PERSON_NAME:
      case InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE:
      case InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT:
      case InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT:
        return true;
      default:
        return false;
    }
  }
}
