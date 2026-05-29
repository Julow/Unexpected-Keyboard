package juloo.keyboard2.suggestions;

import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import juloo.keyboard2.Config;
import juloo.keyboard2.R;
import juloo.keyboard2.dict.PersonalDictionary;
import juloo.keyboard2.dict.PersonalDictionaryActivity;

public class CandidatesView extends LinearLayout
{
  static final int NUM_CANDIDATES = 4;

  /** Candidates currently visible. Entries can be [null] when there are less
      than [NUM_CANDIDATES] suggestions.
      - Entries at indexes [0] to [2] are word suggestions.
      - Entry at index [3] is the emoji suggestion. */
  String[] _items = new String[NUM_CANDIDATES];

  /** Text views showing the candidates in [_items]. Text views visibility is
      set to [GONE] when there are less than [NUM_CANDIDATES] suggestions. */
  TextView[] _item_views = new TextView[NUM_CANDIDATES];

  /** Message when no dictionary is installed. Visible when no candidates are
      shown. Might be [null]. */
  View _status_no_dict = null;

  /** Small button shown while typing to add the current word to the personal
      dictionary. Might be [null] until inflated. */
  View _add_word_btn = null;

  /** Word currently being typed, used when the add-word button is tapped. */
  String _current_word = null;

  /** When set, tapping the add-word button will pre-fill the word field with
      this value (the original word before autocorrect replaced it). Persists
      independently of [_current_word] so it survives the word-tracking reset
      that happens when the cursor lands after a space post-undo. */
  String _pending_word = null;

  /** When set, tapping the add-word button will pre-fill the replacement field
      with this value (the word that autocorrect had previously inserted). */
  String _pending_replacement = null;

  public CandidatesView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate()
  {
    super.onFinishInflate();
    setup_item_view(0, R.id.candidates_middle);
    setup_item_view(1, R.id.candidates_right);
    setup_item_view(2, R.id.candidates_left);
    setup_item_view(3, R.id.candidates_emoji);
    setup_add_word_button();
  }

  /** Called with the word currently being typed so the add-word button knows
      what to pre-fill. Pass [null] when no word is being typed. */
  public void set_current_word(String word)
  {
    _current_word = word;
  }

  /** Called when backspace undoes an autocorrect. Highlights the add-word
      button and pre-fills both the original word and the correction. */
  public void on_autocorrect_undone(String original, String corrected)
  {
    _pending_word = original;
    _pending_replacement = corrected;
    set_add_word_highlight(true);
  }

  void set_add_word_highlight(boolean highlighted)
  {
    if (_add_word_btn == null) return;
    android.util.TypedValue tv = new android.util.TypedValue();
    int colorAttr = highlighted
      ? R.attr.colorLabelActivated
      : R.attr.colorLabel;
    getContext().getTheme().resolveAttribute(colorAttr, tv, true);
    ((TextView)_add_word_btn).setTextColor(tv.data);
  }

  void clear_add_word_highlight()
  {
    _pending_word = null;
    _pending_replacement = null;
    set_add_word_highlight(false);
  }

  public void set_candidates(Suggestions s)
  {
    int s_count = s.count;
    for (int i = 0; i < Suggestions.MAX_COUNT; i++)
      _items[i] = (i < s_count) ? s.suggestions[i] : null;
    _items[3] = s.emoji_suggestion;
    // Hide the status message when showing candidates.
    if (s_count != 0 && _status_no_dict != null)
      _status_no_dict.setVisibility(View.GONE);
    for (int i = 0; i < _item_views.length; i++)
    {
      TextView v = _item_views[i];
      if (_items[i] != null)
      {
        v.setText(_items[i]);
        v.setVisibility(View.VISIBLE);
      }
      else
      {
        v.setVisibility(View.GONE);
      }
    }
  }

  public void clear_candidates()
  {
    for (int i = 0; i < _item_views.length; i++)
    {
      _items[i] = null;
      _item_views[i].setVisibility(View.GONE);
    }
  }

  public void refresh_config(Config config)
  {
    clear_candidates();
    // The status message indicates whether the dictionaries should be
    // installed.
    boolean no_dict = config.current_dictionary == null
      && (config.personal_dictionary == null || config.personal_dictionary.get_all().isEmpty());
    if (no_dict)
      inflate_status_no_dict(config);
    else if (_status_no_dict != null)
      _status_no_dict.setVisibility(View.GONE);
    set_sizes(config);
  }

  /** Set the height of the suggestion row and the text size. */
  void set_sizes(Config config)
  {
    // Make the candidates view about as high as a keyboard row.
    float row_height = config.keyboard_rows_height_pixels * (1 - config.key_vertical_margin);
    ViewGroup.MarginLayoutParams p =
      (ViewGroup.MarginLayoutParams)getLayoutParams();
    p.height = (int)row_height;
    setLayoutParams(p);
    // Match the size of labels on the keyboard.
    float text_size = row_height * config.characterSize * config.labelTextSize;
    for (int i = 0; i < NUM_CANDIDATES; i++)
    {
      TextView v = _item_views[i];
      // Set text size and enable auto size if supported.
      if (VERSION.SDK_INT < 26)
        v.setTextSize(TypedValue.COMPLEX_UNIT_PX, text_size);
      else
        v.setAutoSizeTextTypeUniformWithConfiguration(
            (int)(text_size / 2.), (int)text_size, 1, TypedValue.COMPLEX_UNIT_PX);
    }
  }

  /** Show or hide a status view and inflate it if needed. */
  View inflate_and_show(View v, boolean show, int layout_id)
  {
    if (!show)
    {
      if (v != null)
        v.setVisibility(View.GONE);
    }
    else
    {
      if (v == null)
      {
        v = View.inflate(getContext(), layout_id, null);
        addView(v);
      }
      v.setVisibility(View.VISIBLE);
    }
    return v;
  }

  void inflate_status_no_dict(Config config)
  {
    if (_status_no_dict == null)
    {
      _status_no_dict = View.inflate(getContext(),
          R.layout.candidates_status_no_dict, null);
      // Insert before the "+" button so it stays at the right edge.
      int add_idx = (_add_word_btn != null) ? indexOfChild(_add_word_btn) : -1;
      if (add_idx >= 0)
        addView(_status_no_dict, add_idx);
      else
        addView(_status_no_dict);
    }
    Locale current_locale = (config.device_locales.default_ != null) ?
      Locale.forLanguageTag(config.device_locales.default_.lang_tag) : null;
    TextView tv = _status_no_dict.findViewById(android.R.id.text1);
    if (tv != null && current_locale != null)
      tv.setText(getResources().getString(
            R.string.candidates_status_click_to_install,
            current_locale.getDisplayName()));
    _status_no_dict.setVisibility(View.VISIBLE);
  }


  private void setup_item_view(final int item_index, int item_id)
  {
    TextView v = (TextView)findViewById(item_id);
    v.setOnClickListener(new View.OnClickListener()
        {
          @Override
          public void onClick(View _v)
          {
            String it = _items[item_index];
            if (it != null)
              Config.globalConfig().handler.suggestion_entered(it);
          }
        });
    v.setVisibility(View.GONE);
    _item_views[item_index] = v;
  }

  private void setup_add_word_button()
  {
    _add_word_btn = findViewById(R.id.candidates_add_word);
    if (_add_word_btn == null)
      return;
    _add_word_btn.setVisibility(View.VISIBLE);
    _add_word_btn.setOnClickListener(new View.OnClickListener()
        {
          @Override
          public void onClick(View _v)
          {
            Intent i = new Intent(getContext(), PersonalDictionaryActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            String word = (_pending_word != null && !_pending_word.isEmpty())
              ? _pending_word : _current_word;
            if (word != null && !word.isEmpty())
              i.putExtra(PersonalDictionaryActivity.EXTRA_WORD, word);
            if (_pending_replacement != null)
              i.putExtra(PersonalDictionaryActivity.EXTRA_REPLACEMENT, _pending_replacement);
            clear_add_word_highlight();
            getContext().startActivity(i);
          }
        });
  }

  /** Whether the candidates view should be shown for a given editor. */
  public static boolean should_show(EditorInfo info)
  {
    int variation = info.inputType & InputType.TYPE_MASK_VARIATION;
    int flags = info.inputType & InputType.TYPE_MASK_FLAGS;
    switch (info.inputType & InputType.TYPE_MASK_CLASS)
    {
      case InputType.TYPE_CLASS_TEXT:
        switch (variation)
        {
          case InputType.TYPE_TEXT_VARIATION_PASSWORD:
          case InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
          case InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD:
            return false;
          default:
            /* Editor requested that we don't show suggestions. Enable
               suggestions anyway when the flags [NO_SUGGESTIONS] and
               [AUTO_CORRECT] are present at the same time. This happens with
               Google Keep. */
            if ((flags &
                  (InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                   | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT))
                == InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
              return false;
            return true;
        }
      case InputType.TYPE_CLASS_NUMBER:
        // Beware of TYPE_NUMBER_VARIATION_PASSWORD
        return false;
      default: return false;
    }
  }
}
