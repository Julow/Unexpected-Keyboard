package juloo.keyboard2.suggestions;

import android.content.Context;
import android.os.Build.VERSION;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import juloo.keyboard2.Config;
import juloo.keyboard2.KeyValue;
import juloo.keyboard2.Pointers;
import juloo.keyboard2.R;
import juloo.keyboard2.dict.SupportedDictionaries;
import juloo.keyboard2.staged.Composer;

public class CandidatesView extends LinearLayout
  implements CandidatesScrollView.OnScroll
{
  /** Number of cells added at a time. The next ones are added when the row is
      scrolled near its end, so that a long list of candidates is shown without
      creating every view. */
  static final int CELLS_PER_PAGE = 4;

  /** The candidates to show. The emoji suggestion, when there is one, is the
      last entry and has no annotation. */
  List<Composer.Candidate> _candidates = Collections.emptyList();
  /** Number of entries of [_candidates] that have a cell. */
  int _shown = 0;

  /** The width available to the row, or 0 before the first layout. */
  int _width = 0;

  CandidatesScrollView _scroll;
  LinearLayout _row;

  /** Message when no dictionary is installed. Visible when no candidates are
      shown. Might be [null]. */
  View _status_no_dict = null;

  View _dictionary_switch_button;
  boolean should_show_dictionary_switch = false;

  TextView _lang_name_view;

  /** Label size of a candidate and of its annotation, or 0 before
      [set_sizes]. */
  float _text_size = 0;
  float _annotation_size = 0;

  public CandidatesView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate()
  {
    super.onFinishInflate();
    _scroll = (CandidatesScrollView)findViewById(R.id.candidates_scroll);
    _row = (LinearLayout)findViewById(R.id.candidates_row);
    _scroll.set_on_scroll(this);
    setup_dictionary_switch_button();
    _lang_name_view = (TextView)findViewById(R.id.candidates_lang_name);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh)
  {
    super.onSizeChanged(w, h, oldw, oldh);
    if (w != oldw)
    {
      _width = w;
      layout_cells();
    }
  }

  /** Spread the candidates across the row: every cell takes an equal share of
      the width, never narrower than [candidates_min_width]. Past that the cells
      keep their minimum width and the row is scrolled instead. Enough cells are
      added for the row to reach the end of the screen, since the others are
      only reachable by scrolling. */
  void layout_cells()
  {
    int n = _candidates.size();
    if (n == 0 || _width == 0)
      return;
    int avail = _width - getPaddingEnd();
    int gap = getResources().getDimensionPixelSize(R.dimen.candidates_gap);
    int min = getResources().getDimensionPixelSize(R.dimen.candidates_min_width);
    /* The pitch is the share of one of the cells that fit, not of every
       candidate: a list longer than the row would otherwise take the minimum
       width for every cell and leave the row short of its end. */
    int slot = Math.max(min, avail / Math.min(n, Math.max(1, avail / min)));
    /* Every candidate keeps a cell; the ones past the end of the row are
       reached by scrolling. */
    int needed = n;
    if (needed > _shown)
      add_cells(needed - _shown);
    int width = Math.max(0, slot - gap);
    for (int i = 0; i < _row.getChildCount(); i++)
    {
      View cell = _row.getChildAt(i);
      ViewGroup.LayoutParams lp = cell.getLayoutParams();
      if (lp.width != width)
      {
        lp.width = width;
        cell.setLayoutParams(lp);
      }
    }
  }

  public void set_candidates(Suggestions s)
  {
    List<Composer.Candidate> cs = new ArrayList<Composer.Candidate>();
    cs.addAll(s.candidates);
    if (s.emoji_suggestion != null)
      cs.add(new Composer.Candidate(s.emoji_suggestion));
    _candidates = cs;
    _row.removeAllViews();
    _shown = 0;
    add_cells(CELLS_PER_PAGE);
    layout_cells();
    _scroll.scrollTo(0, 0);
    // Hide the status message when showing candidates.
    if (cs.size() != 0 && _status_no_dict != null)
      _status_no_dict.setVisibility(View.GONE);
    int dict_vis =
      (should_show_dictionary_switch && cs.size() == 0) ? View.VISIBLE : View.GONE;
    _dictionary_switch_button.setVisibility(dict_vis);
    _lang_name_view.setVisibility(dict_vis);
  }

  /** Add up to [count] more candidate cells. */
  void add_cells(int count)
  {
    int end = Math.min(_shown + count, _candidates.size());
    if (end <= _shown)
      return;
    LayoutInflater inflater = LayoutInflater.from(getContext());
    while (_shown < end)
    {
      final Composer.Candidate candidate = _candidates.get(_shown);
      View cell = inflater.inflate(R.layout.candidates_item, _row, false);
      TextView text = (TextView)cell.findViewById(R.id.candidate_text);
      TextView annotation = (TextView)cell.findViewById(R.id.candidate_annotation);
      text.setText(candidate.text);
      /* The annotation always keeps its line, even when there is none: the
         cells are centred on their own height, so a cell that hides the line
         would draw its text at a different height than its neighbours. */
      annotation.setText(candidate.annotation);
      set_text_size(text, _text_size, true);
      set_text_size(annotation, _annotation_size, false);
      cell.setOnClickListener(new View.OnClickListener()
          {
            @Override
            public void onClick(View _v)
            {
              Config.globalConfig().handler.suggestion_entered(candidate.text);
            }
          });
      _row.addView(cell);
      _shown++;
    }
  }

  /** Called when the row is scrolled: add the next cells near the end. */
  @Override
  public void on_scroll(int scrollX, int scrollY)
  {
    if (_shown < _candidates.size()
        && scrollX + _scroll.getWidth() + _scroll.getWidth() / 2
           >= _row.getWidth())
    {
      add_cells(CELLS_PER_PAGE);
      layout_cells();
    }
  }

  void clear_candidates()
  {
    _candidates = Collections.emptyList();
    _shown = 0;
    _row.removeAllViews();
  }

  public void refresh_config(Config config)
  {
    clear_candidates();
    // The status message indicates whether the dictionaries should be
    // installed.
    if (config.current_dictionary == null)
      inflate_status_no_dict(config);
    else if (_status_no_dict != null)
      _status_no_dict.setVisibility(View.GONE);
    should_show_dictionary_switch = config.should_show_dictionary_switch;
    set_sizes(config);
    SupportedDictionaries sd = SupportedDictionaries.get(getResources());
    if (config.current_dictionary_name == null)
      _lang_name_view.setText(R.string.dictionary_switcher_title);
    else
      _lang_name_view.setText(
          sd.get_display_name(config.current_dictionary_name));
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
    _text_size = row_height * config.characterSize * config.labelTextSize;
    _annotation_size = _text_size * 0.4f;
    for (int i = 0; i < _row.getChildCount(); i++)
    {
      View cell = _row.getChildAt(i);
      set_text_size((TextView)cell.findViewById(R.id.candidate_text),
          _text_size, true);
      set_text_size((TextView)cell.findViewById(R.id.candidate_annotation),
          _annotation_size, false);
    }
  }

  /** Set the text size of a candidate or of its annotation. Auto sizing is
      only used for the candidate itself. */
  void set_text_size(TextView v, float size, boolean autosize)
  {
    if (size <= 0)
      return;
    if (VERSION.SDK_INT < 26 || !autosize)
      v.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    else
      v.setAutoSizeTextTypeUniformWithConfiguration(
          (int)(size / 2.), (int)size, 1, TypedValue.COMPLEX_UNIT_PX);
  }

  void inflate_status_no_dict(Config config)
  {
    if (_status_no_dict == null)
    {
      _status_no_dict = View.inflate(getContext(),
          R.layout.candidates_status_no_dict, null);
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

  void setup_dictionary_switch_button()
  {
    _dictionary_switch_button = findViewById(R.id.dictionary_switch);
    _dictionary_switch_button.setOnClickListener(new View.OnClickListener()
        {
          @Override
          public void onClick(View _v)
          {
            Config.globalConfig().handler.key_up(
                KeyValue.getKeyByName("change_dictionary"),
                Pointers.Modifiers.EMPTY);
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
