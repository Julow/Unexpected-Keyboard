package juloo.keyboard2;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import java.util.Iterator;
import juloo.keyboard2.suggestions.Suggestions;

public final class KeyEventHandler
  implements Config.IKeyEventHandler,
             ClipboardHistoryService.ClipboardPasteCallback,
             CurrentlyTypedWord.Callback
{
  IReceiver _recv;
  Autocapitalisation _autocap;
  Suggestions _suggestions;
  CurrentlyTypedWord _typedword;
  /** State of the system modifiers. It is updated whether a modifier is down
      or up and a corresponding key event is sent. */
  Pointers.Modifiers _mods;
  /** Consistent with [_mods]. This is a mutable state rather than computed
      from [_mods] to ensure that the meta state is correct while up and down
      events are sent for the modifier keys. */
  int _meta_state = 0;
  /** Whether to force sending arrow keys to move the cursor when
      [setSelection] could be used instead. */
  boolean _move_cursor_force_fallback = false;
  /** Whether the space bar automatically enters the best suggestion. */
  boolean _space_bar_auto_complete = false;
  /** Remember the action that was handled. This is used by autocorrect. */
  LastAction _last_action = null;
  LastAction _next_last_action = null;
  long _last_space_bar_time = 0;
  /** Current Hangul sequential input state. -1 = component missing. */
  int _hangul_initial = -1;
  int _hangul_medial = -1;
  int _hangul_final = 0;

  public KeyEventHandler(IReceiver recv, Config config)
  {
    _recv = recv;
    Handler handler = recv.getHandler();
    _autocap = new Autocapitalisation(handler,
        this.new Autocapitalisation_callback());
    _mods = Pointers.Modifiers.EMPTY;
    _suggestions = new Suggestions(recv, config);
    _typedword = new CurrentlyTypedWord(handler, this);
  }

  /** Editing just started. */
  public void started(Config conf)
  {
    reset_hangul();
    InputConnection ic = _recv.getCurrentInputConnection();
    _autocap.started(conf, ic);
    _typedword.started(conf, ic);
    _suggestions.started();
    _move_cursor_force_fallback =
      conf.editor_config.should_move_cursor_force_fallback;
    _space_bar_auto_complete = conf.space_bar_auto_complete;
    _last_action = null;
    _last_space_bar_time = 0;
  }

  static final char[] HANGUL_INITIALS =
  {
    'ㄱ','ㄲ','ㄴ','ㄷ','ㄸ','ㄹ','ㅁ','ㅂ','ㅃ','ㅅ','ㅆ','ㅇ','ㅈ','ㅉ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ'
  };
  static final char[] HANGUL_MEDIALS =
  {
    'ㅏ','ㅐ','ㅑ','ㅒ','ㅓ','ㅔ','ㅕ','ㅖ','ㅗ','ㅘ','ㅙ','ㅚ',
    'ㅛ','ㅜ','ㅝ','ㅞ','ㅟ','ㅠ','ㅡ','ㅢ','ㅣ'
  };
  static final char[] HANGUL_FINALS =
  {
    0,'ㄱ','ㄲ','ㄳ','ㄴ','ㄵ','ㄶ','ㄷ','ㄹ','ㄺ','ㄻ','ㄼ','ㄽ',
    'ㄾ','ㄿ','ㅀ','ㅁ','ㅂ','ㅄ','ㅅ','ㅆ','ㅇ','ㅈ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ'
  };

  static char make_hangul_syllable(int initial, int medial, int fin)
  {
    return (char)(0xAC00 + initial * 588 + medial * 28 + fin);
  }

  static int combine_medial(int a, int b)
  {
    // ㅗ: 8, ㅜ: 13, ㅡ: 18
    if (a == 8 && b == 0) return 9;   // ㅗ+ㅏ = ㅘ
    if (a == 8 && b == 1) return 10;  // ㅗ+ㅐ = ㅙ
    if (a == 8 && b == 20) return 11; // ㅗ+ㅣ = ㅚ
    if (a == 13 && b == 4) return 14; // ㅜ+ㅓ = ㅝ
    if (a == 13 && b == 5) return 15; // ㅜ+ㅔ = ㅞ
    if (a == 13 && b == 20) return 16; // ㅜ+ㅣ = ㅟ
    if (a == 18 && b == 20) return 19; // ㅡ+ㅣ = ㅢ
    return -1;
  }

  static int combine_final(int a, int b)
  {
    // simple final indices
    if (a == 1 && b == 19) return 3;   // ㄱ+ㅅ = ㄳ
    if (a == 4 && b == 22) return 5;   // ㄴ+ㅈ = ㄵ
    if (a == 4 && b == 27) return 6;   // ㄴ+ㅎ = ㄶ
    if (a == 8 && b == 1) return 9;    // ㄹ+ㄱ = ㄺ
    if (a == 8 && b == 16) return 10;  // ㄹ+ㅁ = ㄻ
    if (a == 8 && b == 17) return 11;  // ㄹ+ㅂ = ㄼ
    if (a == 8 && b == 19) return 12;  // ㄹ+ㅅ = ㄽ
    if (a == 8 && b == 25) return 13;  // ㄹ+ㅌ = ㄾ
    if (a == 8 && b == 26) return 14;  // ㄹ+ㅍ = ㄿ
    if (a == 8 && b == 27) return 15;  // ㄹ+ㅎ = ㅀ
    if (a == 17 && b == 19) return 18; // ㅂ+ㅅ = ㅄ
    return 0;
  }

  static int combine_double_initial(int initial)
  {
    switch (initial)
    {
      case 0: return 1;   // ㄱ+ㄱ = ㄲ
      case 3: return 4;   // ㄷ+ㄷ = ㄸ
      case 7: return 8;   // ㅂ+ㅂ = ㅃ
      case 9: return 10;  // ㅅ+ㅅ = ㅆ
      case 12: return 13; // ㅈ+ㅈ = ㅉ
      default: return -1;
    }
  }

  static int combine_double_final(int fin)
  {
    switch (fin)
    {
      case 1: return 2;   // ㄱ+ㄱ = ㄲ
      case 19: return 20; // ㅅ+ㅅ = ㅆ
      default: return 0;
    }
  }

  static int split_compound_final_first(int fin)
  {
    switch (fin)
    {
      case 3: return 1;   // ㄳ→ㄱ
      case 5: return 4;   // ㄵ→ㄴ
      case 6: return 4;   // ㄶ→ㄴ
      case 9: return 8;   // ㄺ→ㄹ
      case 10: return 8;  // ㄻ→ㄹ
      case 11: return 8;  // ㄼ→ㄹ
      case 12: return 8;  // ㄽ→ㄹ
      case 13: return 8;  // ㄾ→ㄹ
      case 14: return 8;  // ㄿ→ㄹ
      case 15: return 8;  // ㅀ→ㄹ
      case 18: return 17; // ㅄ→ㅂ
      default: return 0;
    }
  }

  static int split_compound_final_second_initial(int fin)
  {
    switch (fin)
    {
      case 3: return 9;   // ㄳ→ㅅ
      case 5: return 12;  // ㄵ→ㅈ
      case 6: return 18;  // ㄶ→ㅎ
      case 9: return 0;   // ㄺ→ㄱ
      case 10: return 6;  // ㄻ→ㅁ
      case 11: return 7;  // ㄼ→ㅂ
      case 12: return 9;  // ㄽ→ㅅ
      case 13: return 25; // ㄾ→ㅌ
      case 14: return 26; // ㄿ→ㅍ
      case 15: return 18; // ㅀ→ㅎ
      case 18: return 9;  // ㅄ→ㅅ
      default: return -1;
    }
  }

  static int initial_from_final(int fin)
  {
    // final_idx -> initial_idx mapping
    if (fin >= 1 && fin <= 27)
    {
      char c = HANGUL_FINALS[fin];
      for (int i = 0; i < HANGUL_INITIALS.length; i++)
        if (HANGUL_INITIALS[i] == c)
          return i;
    }
    return -1;
  }

  static int final_from_initial(int initial)
  {
    // initial_idx -> final_idx mapping
    if (initial >= 0 && initial < HANGUL_INITIALS.length)
    {
      char c = HANGUL_INITIALS[initial];
      for (int i = 1; i < HANGUL_FINALS.length; i++)
        if (HANGUL_FINALS[i] == c)
          return i;
    }
    return 0;
  }

  static int split_compound_medial_first(int medial)
  {
    switch (medial)
    {
      case 9: return 8;   // ㅘ→ㅗ
      case 10: return 8;  // ㅙ→ㅗ
      case 11: return 8;  // ㅚ→ㅗ
      case 14: return 13; // ㅝ→ㅜ
      case 15: return 13; // ㅞ→ㅜ
      case 16: return 13; // ㅟ→ㅜ
      case 19: return 18; // ㅢ→ㅡ
      default: return -1;
    }
  }

  void reset_hangul()
  {
    _hangul_initial = -1;
    _hangul_medial = -1;
    _hangul_final = 0;
  }

  boolean has_hangul_state()
  {
    return _hangul_initial >= 0 || _hangul_medial >= 0;
  }

  int hangul_initial_index(char c)
  {
    for (int i = 0; i < HANGUL_INITIALS.length; i++)
      if (HANGUL_INITIALS[i] == c)
        return i;
    return -1;
  }

  int hangul_medial_index(char c)
  {
    for (int i = 0; i < HANGUL_MEDIALS.length; i++)
      if (HANGUL_MEDIALS[i] == c)
        return i;
    return -1;
  }

  int hangul_final_index(char c)
  {
    for (int i = 1; i < HANGUL_FINALS.length; i++)
      if (HANGUL_FINALS[i] == c)
        return i;
    return -1;
  }

  int hangul_medial_from_precomposed(int precomposed)
  {
    return ((precomposed - 0xAC00) % 588) / 28;
  }

  void replace_hangul_current()
  {
    if (_hangul_initial >= 0 && _hangul_medial >= 0)
    {
      char c = make_hangul_syllable(_hangul_initial, _hangul_medial, _hangul_final);
      replace_surrounding_text(1, 0, String.valueOf(c));
    }
    else if (_hangul_initial >= 0)
    {
      replace_surrounding_text(1, 0, String.valueOf(HANGUL_INITIALS[_hangul_initial]));
    }
    else if (_hangul_medial >= 0)
    {
      replace_surrounding_text(1, 0, String.valueOf(HANGUL_MEDIALS[_hangul_medial]));
    }
  }

  void send_hangul_current()
  {
    if (_hangul_initial >= 0 && _hangul_medial >= 0)
    {
      char c = make_hangul_syllable(_hangul_initial, _hangul_medial, _hangul_final);
      send_text(String.valueOf(c));
    }
    else if (_hangul_initial >= 0)
    {
      send_text(String.valueOf(HANGUL_INITIALS[_hangul_initial]));
    }
    else if (_hangul_medial >= 0)
    {
      send_text(String.valueOf(HANGUL_MEDIALS[_hangul_medial]));
    }
  }

  void start_hangul_initial(int idx)
  {
    _hangul_initial = idx;
    _hangul_medial = -1;
    _hangul_final = 0;
    send_text(String.valueOf(HANGUL_INITIALS[idx]));
  }

  void start_hangul_medial(int idx)
  {
    _hangul_initial = -1;
    _hangul_medial = idx;
    _hangul_final = 0;
    send_text(String.valueOf(HANGUL_MEDIALS[idx]));
  }

  boolean handle_hangul_char(char c)
  {
    int medial = hangul_medial_index(c);
    if (medial >= 0)
    {
      handle_hangul_medial(medial);
      return true;
    }
    int initial = hangul_initial_index(c);
    if (initial >= 0)
    {
      handle_hangul_initial(c);
      return true;
    }
    int fin = hangul_final_index(c);
    if (fin >= 0)
    {
      handle_hangul_initial(c);
      return true;
    }
    return false;
  }

  void handle_hangul_initial(char c)
  {
    int initial = hangul_initial_index(c);
    int fin = hangul_final_index(c);

    if (_hangul_initial >= 0 && _hangul_medial < 0)
    {
      int combined = (_hangul_initial == initial) ? combine_double_initial(initial) : -1;
      if (combined >= 0)
      {
        _hangul_initial = combined;
        replace_hangul_current();
        return;
      }
    }

    if (_hangul_initial >= 0 && _hangul_medial >= 0)
    {
      // We have initial + medial, try to add final
      if (_hangul_final == 0 && fin > 0)
      {
        _hangul_final = fin;
        replace_hangul_current();
        return;
      }
      // Try compound final
      if (_hangul_final > 0)
      {
        int double_final = (_hangul_final == fin) ? combine_double_final(fin) : 0;
        if (double_final > 0)
        {
          _hangul_final = double_final;
          replace_hangul_current();
          return;
        }
        int compound = combine_final(_hangul_final, fin);
        if (compound > 0)
        {
          _hangul_final = compound;
          replace_hangul_current();
          return;
        }
      }
    }

    if (initial >= 0)
      start_hangul_initial(initial);
    else
      reset_hangul();
  }

  void handle_hangul_medial(int medial)
  {
    if (medial < 0)
      return;

    if (_hangul_initial >= 0 && _hangul_medial >= 0)
    {
      // We have a full syllable. Try medial combination first.
      int combined = combine_medial(_hangul_medial, medial);
      if (combined >= 0)
      {
        _hangul_medial = combined;
        _hangul_final = 0;
        replace_hangul_current();
        return;
      }

      // If we have a final, try splitting it to next syllable
      if (_hangul_final > 0)
      {
        int moved_initial;
        if (_hangul_final >= 3 && _hangul_final <= 27)
        {
          // Compound final: split to first+second
          int first = split_compound_final_first(_hangul_final);
          int second = split_compound_final_second_initial(_hangul_final);
          if (first > 0 && second >= 0)
          {
            // Split: previous syllable loses final, new syllable starts with second
            char prev = make_hangul_syllable(_hangul_initial, _hangul_medial, 0);
            char next = make_hangul_syllable(second, medial, 0);
            replace_surrounding_text(1, 0, String.valueOf(prev) + String.valueOf(next));
            _hangul_initial = second;
            _hangul_medial = medial;
            _hangul_final = 0;
            return;
          }
        }

        // Simple final → next initial
        moved_initial = initial_from_final(_hangul_final);
        if (moved_initial >= 0)
        {
          char prev = make_hangul_syllable(_hangul_initial, _hangul_medial, 0);
          char next = make_hangul_syllable(moved_initial, medial, 0);
          replace_surrounding_text(1, 0, String.valueOf(prev) + String.valueOf(next));
          _hangul_initial = moved_initial;
          _hangul_medial = medial;
          _hangul_final = 0;
          return;
        }
      }

      // No combination, start new vowel syllable
      start_hangul_medial(medial);
      return;
    }

    if (_hangul_initial >= 0 && _hangul_medial < 0)
    {
      // initial + medial = syllable
      _hangul_medial = medial;
      _hangul_final = 0;
      replace_hangul_current();
      return;
    }

    if (_hangul_medial >= 0)
    {
      // Previous medial, try combination
      int combined = combine_medial(_hangul_medial, medial);
      if (combined >= 0)
      {
        _hangul_medial = combined;
        replace_hangul_current();
        return;
      }
      start_hangul_medial(medial);
      return;
    }

    // No state, start new medial
    start_hangul_medial(medial);
  }

  boolean handle_hangul_backspace()
  {
    if (!has_hangul_state())
      return false;

    // Final exists -> remove or split compound
    if (_hangul_final > 0)
    {
      int first = split_compound_final_first(_hangul_final);
      if (first > 0)
      {
        _hangul_final = first;
        replace_hangul_current();
      }
      else
      {
        _hangul_final = 0;
        replace_hangul_current();
      }
      return true;
    }

    // No final, medial exists
    if (_hangul_medial >= 0)
    {
      int first = split_compound_medial_first(_hangul_medial);
      if (first >= 0)
      {
        _hangul_medial = first;
        replace_hangul_current();
      }
      else if (_hangul_initial >= 0)
      {
        // Remove medial, back to initial
        _hangul_medial = -1;
        replace_hangul_current();
      }
      else
      {
        // Standalone medial, delete it
        replace_surrounding_text(1, 0, "");
        reset_hangul();
      }
      return true;
    }

    // Initial only
    if (_hangul_initial >= 0)
    {
      replace_surrounding_text(1, 0, "");
      reset_hangul();
      return true;
    }

    return false;
  }

  /** Selection has been updated. */
  public void selection_updated(int oldSelStart, int newSelStart, int newSelEnd)
  {
    _autocap.selection_updated(oldSelStart, newSelStart);
    _typedword.selection_updated(oldSelStart, newSelStart, newSelEnd);
  }

  /** A key is being pressed. There will not necessarily be a corresponding
      [key_up] event. */
  @Override
  public void key_down(KeyValue key, boolean isSwipe)
  {
    if (key == null)
      return;
    // Stop auto capitalisation when pressing some keys
    switch (key.getKind())
    {
      case Modifier:
        switch (key.getModifier())
        {
          case CTRL:
          case ALT:
          case META:
            _autocap.stop();
            break;
        }
        break;
      case Compose_pending:
        _autocap.stop();
        break;
      case Slider:
        // Don't wait for the next key_up and move the cursor right away. This
        // is called after the trigger distance have been travelled.
        reset_hangul();
        handle_slider(key.getSlider(), key.getSliderRepeat(), true);
        break;
      default: break;
    }
  }

  /** A key has been released. */
  @Override
  public void key_up(KeyValue key, Pointers.Modifiers mods)
  {
    if (key == null)
      return;
    _next_last_action = LastAction.OTHER;
    Pointers.Modifiers old_mods = _mods;
    update_meta_state(mods);
    switch (key.getKind())
    {
      case Char:
        if (!handle_hangul_char(key.getChar()))
        {
          reset_hangul();
          send_text(String.valueOf(key.getChar()));
        }
        break;
      case Hangul_initial:
        handle_hangul_initial(key.getString().charAt(0));
        break;
      case Hangul_medial:
        handle_hangul_medial(hangul_medial_from_precomposed(
              key.getHangulPrecomposed()));
        break;
      case String:
        if (key.getString().length() == 1 && handle_hangul_char(key.getString().charAt(0)))
        {
          // Handled by Hangul automaton
        }
        else
        {
          reset_hangul();
          send_text(key.getString());
        }
        break;
      case Event:
        reset_hangul();
        _recv.handle_event_key(key.getEvent());
        break;
      case Keyevent:
        reset_hangul();
        send_key_down_up(key.getKeyevent());
        break;
      case Modifier: break;
      case Editing:
        if (key.getEditing() != KeyValue.Editing.BACKSPACE)
          reset_hangul();
        handle_editing_key(key.getEditing());
        break;
      case Compose_pending:
        reset_hangul();
        _recv.set_compose_pending(true);
        break;
      case Slider:
        reset_hangul();
        handle_slider(key.getSlider(), key.getSliderRepeat(), false);
        break;
      case Macro:
        reset_hangul();
        evaluate_macro(key.getMacro());
        break;
    }
    update_meta_state(old_mods);
    _last_action = _next_last_action;
  }

  @Override
  public void mods_changed(Pointers.Modifiers mods)
  {
    update_meta_state(mods);
  }

  @Override
  public void suggestion_entered(String text)
  {
    String old = _typedword.get();
    int cur_rel = _typedword.cursor_relative();
    replace_surrounding_text(old.length() + cur_rel, -cur_rel, text + " ");
    last_replaced_word = old;
    last_replacement_word_len = text.length() + 1;
    _next_last_action = LastAction.SUGGESTION_ENTERED;
  }

  @Override
  public void paste_from_clipboard_pane(String content)
  {
    send_text(content);
  }

  @Override
  public void currently_typed_word(String word)
  {
    _suggestions.currently_typed_word(word);
  }

  public void ime_subtype_changed()
  {
    // Refresh the suggestions immediately after dictionary changed.
    _suggestions.currently_typed_word(_typedword.get());
  }

  /** Update [_mods] to be consistent with the [mods], sending key events if
      needed. */
  void update_meta_state(Pointers.Modifiers mods)
  {
    // Released modifiers
    Iterator<KeyValue> it = _mods.diff(mods);
    while (it.hasNext())
      sendMetaKeyForModifier(it.next(), false);
    // Activated modifiers
    it = mods.diff(_mods);
    while (it.hasNext())
      sendMetaKeyForModifier(it.next(), true);
    _mods = mods;
  }

  // private void handleDelKey(int before, int after)
  // {
  //  CharSequence selection = getCurrentInputConnection().getSelectedText(0);

  //  if (selection != null && selection.length() > 0)
  //  getCurrentInputConnection().commitText("", 1);
  //  else
  //  getCurrentInputConnection().deleteSurroundingText(before, after);
  // }

  void sendMetaKey(int eventCode, int meta_flags, boolean down)
  {
    if (down)
    {
      _meta_state = _meta_state | meta_flags;
      send_keyevent(KeyEvent.ACTION_DOWN, eventCode, _meta_state);
    }
    else
    {
      send_keyevent(KeyEvent.ACTION_UP, eventCode, _meta_state);
      _meta_state = _meta_state & ~meta_flags;
    }
  }

  void sendMetaKeyForModifier(KeyValue kv, boolean down)
  {
    switch (kv.getKind())
    {
      case Modifier:
        switch (kv.getModifier())
        {
          case CTRL:
            sendMetaKey(KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.META_CTRL_LEFT_ON | KeyEvent.META_CTRL_ON, down);
            break;
          case ALT:
            sendMetaKey(KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.META_ALT_LEFT_ON | KeyEvent.META_ALT_ON, down);
            break;
          case SHIFT:
            sendMetaKey(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.META_SHIFT_LEFT_ON | KeyEvent.META_SHIFT_ON, down);
            break;
          case META:
            sendMetaKey(KeyEvent.KEYCODE_META_LEFT, KeyEvent.META_META_LEFT_ON | KeyEvent.META_META_ON, down);
            break;
          default:
            break;
        }
        break;
    }
  }

  void send_key_down_up(int keyCode)
  {
    send_key_down_up(keyCode, _meta_state);
  }

  /** Ignores currently pressed system modifiers. */
  void send_key_down_up(int keyCode, int metaState)
  {
    send_keyevent(KeyEvent.ACTION_DOWN, keyCode, metaState);
    send_keyevent(KeyEvent.ACTION_UP, keyCode, metaState);
  }

  void send_keyevent(int eventAction, int eventCode, int metaState)
  {
    InputConnection conn = _recv.getCurrentInputConnection();
    if (conn == null)
      return;
    conn.sendKeyEvent(new KeyEvent(1, 1, eventAction, eventCode, 0,
          metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
          KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
    if (eventAction == KeyEvent.ACTION_UP)
    {
      _autocap.event_sent(eventCode, metaState);
      _typedword.event_sent(eventCode, metaState);
    }
  }

  void send_text(String text)
  {
    InputConnection conn = _recv.getCurrentInputConnection();
    if (conn == null)
      return;
    _autocap.typed(text);
    _typedword.typed(text);
    conn.commitText(text, 1);
  }

  void replace_surrounding_text(int remove_before, int remove_after,
      String new_text)
  {
    InputConnection conn = _recv.getCurrentInputConnection();
    if (conn == null)
      return;
    conn.beginBatchEdit();
    conn.deleteSurroundingText(remove_before, remove_after);
    conn.commitText(new_text, 1);
    conn.endBatchEdit();
  }

  /** See {!InputConnection.performContextMenuAction}. */
  void send_context_menu_action(int id)
  {
    InputConnection conn = _recv.getCurrentInputConnection();
    if (conn == null)
      return;
    conn.performContextMenuAction(id);
  }

  @SuppressLint("InlinedApi")
  void handle_editing_key(KeyValue.Editing ev)
  {
    switch (ev)
    {
      case COPY: if(_typedword.is_selection_not_empty()) send_context_menu_action(android.R.id.copy); break;
      case PASTE: send_context_menu_action(android.R.id.paste); break;
      case CUT: if(_typedword.is_selection_not_empty()) send_context_menu_action(android.R.id.cut); break;
      case SELECT_ALL: send_context_menu_action(android.R.id.selectAll); break;
      case SHARE: send_context_menu_action(android.R.id.shareText); break;
      case PASTE_PLAIN: send_context_menu_action(android.R.id.pasteAsPlainText); break;
      case UNDO: send_context_menu_action(android.R.id.undo); break;
      case REDO: send_context_menu_action(android.R.id.redo); break;
      case REPLACE: send_context_menu_action(android.R.id.replaceText); break;
      case ASSIST: send_context_menu_action(android.R.id.textAssist); break;
      case AUTOFILL: send_context_menu_action(android.R.id.autofill); break;
      case DELETE_WORD: send_key_down_up(KeyEvent.KEYCODE_DEL, KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON); break;
      case FORWARD_DELETE_WORD: send_key_down_up(KeyEvent.KEYCODE_FORWARD_DEL, KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON); break;
      case SELECTION_CANCEL: cancel_selection(); break;
      case SPACE_BAR: handle_space_bar(); break;
      case BACKSPACE: handle_backspace(); break;
    }
  }

  static ExtractedTextRequest _move_cursor_req = null;

  /** Query the cursor position. The extracted text is empty. Returns [null] if
      the editor doesn't support this operation. */
  ExtractedText get_cursor_pos(InputConnection conn)
  {
    if (_move_cursor_req == null)
    {
      _move_cursor_req = new ExtractedTextRequest();
      _move_cursor_req.hintMaxChars = 0;
    }
    return conn.getExtractedText(_move_cursor_req, 0);
  }

  /** [r] might be negative, in which case the direction is reversed. */
  void handle_slider(KeyValue.Slider s, int r, boolean key_down)
  {
    switch (s)
    {
      case Cursor_left: move_cursor(-r); break;
      case Cursor_right: move_cursor(r); break;
      case Cursor_up: move_cursor_vertical(-r); break;
      case Cursor_down: move_cursor_vertical(r); break;
      case Selection_cursor_left: move_cursor_sel(r, true, key_down); break;
      case Selection_cursor_right: move_cursor_sel(r, false, key_down); break;
    }
  }

  /** Move the cursor right or left, if possible without sending key events.
      Unlike arrow keys, the selection is not removed even if shift is not on.
      Falls back to sending arrow keys events if the editor do not support
      moving the cursor or a modifier other than shift is pressed. */
  void move_cursor(int d)
  {
    InputConnection conn = _recv.getCurrentInputConnection();
    if (conn == null)
      return;
    ExtractedText et = get_cursor_pos(conn);
    if (et != null && can_set_selection(conn))
    {
      int sel_start = et.selectionStart;
      int sel_end = et.selectionEnd;
      // Continue expanding the selection even if shift is not pressed
      if (sel_end != sel_start)
      {
        sel_end += d;
        if (sel_end == sel_start) // Avoid making the selection empty
          sel_end += d;
      }
      else
      {
        sel_end += d;
        // Leave 'sel_start' where it is if shift is pressed
        if ((_meta_state & KeyEvent.META_SHIFT_ON) == 0)
          sel_start = sel_end;
      }
      if (conn.setSelection(sel_start, sel_end))
        return; // Fallback to sending key events if [setSelection] failed
    }
    move_cursor_fallback(d);
  }

  /** Move one of the two side of a selection. If [sel_left] is true, the left
      position is moved, otherwise the right position is moved. */
  void move_cursor_sel(int d, boolean sel_left, boolean key_down)
  {
    InputConnection conn = _recv.getCurrentInputConnection();
    if (conn == null)
      return;
    ExtractedText et = get_cursor_pos(conn);
    if (et != null && can_set_selection(conn))
    {
      int sel_start = et.selectionStart;
      int sel_end = et.selectionEnd;
      // Reorder the selection when the slider has just been pressed. The
      // selection might have been reversed if one end crossed the other end
      // with a previous slider.
      if (key_down && sel_start > sel_end)
      {
        sel_start = et.selectionEnd;
        sel_end = et.selectionStart;
      }
      do
      {
        if (sel_left)
          sel_start += d;
        else
          sel_end += d;
        // Move the cursor twice if moving it once would make the selection
        // empty and stop selection mode.
      } while (sel_start == sel_end);
      if (conn.setSelection(sel_start, sel_end))
        return; // Fallback to sending key events if [setSelection] failed
    }
    move_cursor_fallback(d);
  }

  /** Returns whether the selection can be set using [conn.setSelection()].
      This can happen on Termux or when system modifiers are activated for
      example. */
  boolean can_set_selection(InputConnection conn)
  {
    final int system_mods =
      KeyEvent.META_CTRL_ON | KeyEvent.META_ALT_ON | KeyEvent.META_META_ON;
    return !_move_cursor_force_fallback && (_meta_state & system_mods) == 0;
  }

  void move_cursor_fallback(int d)
  {
    if (d < 0)
      send_key_down_up_repeat(KeyEvent.KEYCODE_DPAD_LEFT, -d);
    else
      send_key_down_up_repeat(KeyEvent.KEYCODE_DPAD_RIGHT, d);
  }

  /** Move the cursor up and down. This sends UP and DOWN key events that might
      make the focus exit the text box. */
  void move_cursor_vertical(int d)
  {
    if (d < 0)
      send_key_down_up_repeat(KeyEvent.KEYCODE_DPAD_UP, -d);
    else
      send_key_down_up_repeat(KeyEvent.KEYCODE_DPAD_DOWN, d);
  }

  void evaluate_macro(KeyValue[] keys)
  {
    if (keys.length == 0)
      return;
    // Ignore modifiers that are activated at the time the macro is evaluated
    mods_changed(Pointers.Modifiers.EMPTY);
    evaluate_macro_loop(keys, 0, Pointers.Modifiers.EMPTY, _autocap.pause());
  }

  /** Evaluate the macro asynchronously to make sure event are processed in the
      right order. */
  void evaluate_macro_loop(final KeyValue[] keys, int i, Pointers.Modifiers mods, final boolean autocap_paused)
  {
    boolean should_delay = false;
    KeyValue kv = KeyModifier.modify_no_modmap(keys[i], mods);
    if (kv != null)
    {
      if (kv.hasFlagsAny(KeyValue.FLAG_LATCH))
      {
        // Non-special latchable keys clear latched modifiers
        if (!kv.hasFlagsAny(KeyValue.FLAG_SPECIAL))
          mods = Pointers.Modifiers.EMPTY;
        mods = mods.with_extra_mod(kv);
      }
      else
      {
        key_down(kv, false);
        key_up(kv, mods);
        mods = Pointers.Modifiers.EMPTY;
      }
      should_delay = wait_after_macro_key(kv);
    }
    i++;
    if (i >= keys.length) // Stop looping
    {
      _autocap.unpause(autocap_paused);
    }
    else if (should_delay)
    {
      // Add a delay before sending the next key to avoid race conditions
      // causing keys to be handled in the wrong order. Notably, KeyEvent keys
      // handling is scheduled differently than the other edit functions.
      final int i_ = i;
      final Pointers.Modifiers mods_ = mods;
      _recv.getHandler().postDelayed(new Runnable() {
        public void run()
        {
          evaluate_macro_loop(keys, i_, mods_, autocap_paused);
        }
      }, 1000/30);
    }
    else
      evaluate_macro_loop(keys, i, mods, autocap_paused);
  }

  boolean wait_after_macro_key(KeyValue kv)
  {
    switch (kv.getKind())
    {
      case Keyevent:
      case Editing:
      case Event:
        return true;
      case Slider:
        return _move_cursor_force_fallback;
      default:
        return false;
    }
  }

  /** Repeat calls to [send_key_down_up]. */
  void send_key_down_up_repeat(int event_code, int repeat)
  {
    while (repeat-- > 0)
      send_key_down_up(event_code);
  }

  void cancel_selection()
  {
    InputConnection conn = _recv.getCurrentInputConnection();
    if (conn == null)
      return;
    ExtractedText et = get_cursor_pos(conn);
    if (et == null) return;
    final int curs = et.selectionStart;
    // Notify the receiver as Android's [onUpdateSelection] is not triggered.
    if (conn.setSelection(curs, curs))
      _recv.selection_state_changed(false);
  }

  /** The word that was replaced by a suggestion when the last action was to
      enter a suggestion (with the space bar or the candidates view) or [null]
      otherwise. */
  String last_replaced_word = null;
  /** Length of the text before the cursor that should be replaced by
      backspace. */
  int last_replacement_word_len = 0;

  /** Implement autocorrect when enabled in the settings. */
  void handle_space_bar()
  {
    if (_space_bar_auto_complete && _suggestions.count > 0
        && !_typedword.is_selection_not_empty()
        && _typedword.cursor_relative() == 0)
      suggestion_entered(_suggestions.suggestions[0]);
    else if (!handle_space_bar_double_tap())
    {
      send_text(" ");
      _last_space_bar_time = System.currentTimeMillis();
      _next_last_action = LastAction.SPACE_BAR;
    }
  }

  boolean handle_space_bar_double_tap()
  {
    if (_last_action != LastAction.SPACE_BAR)
      return false;
    if (System.currentTimeMillis() - _last_space_bar_time > ViewConfiguration.getDoubleTapTimeout())
      return false;
    if (_typedword.is_selection_not_empty() || _typedword.cursor_relative() != 0)
      return false;
    InputConnection conn = _recv.getCurrentInputConnection();
    if (conn == null)
      return false;
    CharSequence before = conn.getTextBeforeCursor(2, 0);
    if (before == null || before.length() < 2 || before.charAt(before.length() - 1) != ' ')
      return false;
    char previous = before.charAt(before.length() - 2);
    if (Character.isWhitespace(previous) || ".!?。！？".indexOf(previous) >= 0)
      return false;
    replace_surrounding_text(1, 0, ". ");
    _autocap.typed(". ");
    _typedword.delayed_refresh();
    _last_space_bar_time = 0;
    _next_last_action = LastAction.OTHER;
    return true;
  }

  /** Undo the last autocorrect. */
  void handle_backspace()
  {
    if (handle_hangul_backspace())
      return;
    if (_last_action == LastAction.SUGGESTION_ENTERED
        && last_replaced_word != null)
    {
      replace_surrounding_text(last_replacement_word_len, 0,
          last_replaced_word + " ");
      last_replaced_word = null;
    }
    else
    {
      send_key_down_up(KeyEvent.KEYCODE_DEL);
    }
  }

  public static interface IReceiver extends Suggestions.Callback
  {
    public void handle_event_key(KeyValue.Event ev);
    public void set_shift_state(boolean state, boolean lock);
    public void set_compose_pending(boolean pending);
    public void selection_state_changed(boolean selection_is_ongoing);
    public InputConnection getCurrentInputConnection();
    public Handler getHandler();
  }

  class Autocapitalisation_callback implements Autocapitalisation.Callback
  {
    @Override
    public void update_shift_state(boolean should_enable, boolean should_disable)
    {
      if (should_enable)
        _recv.set_shift_state(true, false);
      else if (should_disable)
        _recv.set_shift_state(false, false);
    }
  }

  public static enum LastAction
  {
    SUGGESTION_ENTERED,
    SPACE_BAR,
    OTHER
  }
}
