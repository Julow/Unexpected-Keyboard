package juloo.keyboard2.suggestions;

import android.content.Context;
import android.os.Build.VERSION;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.RelativeSizeSpan;
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
import juloo.keyboard2.KeyboardData;
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

  /** The control that shows and toggles the reading of the candidates. Hidden
      for a language whose candidates carry no reading. */
  TextView _rubytext_toggle;

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
    _rubytext_toggle = (TextView)findViewById(R.id.rubytext_toggle);
    _rubytext_toggle.setOnClickListener(new View.OnClickListener()
        {
          @Override
          public void onClick(View _v)
          {
            Config c = Config.globalConfig();
            c.set_annotations(!c.annotations());
            /* Redraw in place: [Keyboard2] ignores this preference, so nothing
               reloads, and the row keeps its candidates while the readings
               appear or disappear. */
            update_rubytext_toggle();
            redraw_annotations();
          }
        });
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
    update_rubytext_toggle();
  }

  /** The one character the control shows and the reading that goes above it, in
      that order, or [null] for a language whose candidates never carry a
      reading.

      One fixed exemplar per language rather than a candidate of the current
      row. A sample of the row changes on every keystroke, so the control could
      never become a symbol the user learns; for Japanese the reading belongs
      to the word, so a sample drawn from a multi-character candidate pairs a
      character with a reading it does not have; and sampling also made the
      control disappear whenever the row happened to hold no reading, which
      loses the switch mid-typing.

      Japanese draws the character whose reading is the language's own name for
      a reading written above a character - a true pair, and it says what the
      control shows. Chinese draws the character for writing, with its rising
      second tone above it.

      The two are deliberately not the same glyph: no user ever sees both, since
      the control is per language, so parallelism between them buys nothing and
      each glyph earns its place on its own grounds.

      The Chinese one was chosen against two criteria, in this order. The control
      exists to show a reading, so the mark it carries has to be legible at the
      control's reading size - three quarters of the control's size, which is the
      annotation size - and the flat first tone is the faintest of the four
      marks, so a character that takes it demonstrates the worst case rather than
      the best: that is what ruled out the character for China. And the glyph has
      to be the same in simplified and traditional, because the build ships
      zh_CN, zh_SG and zh_TW: that is what ruled out the character for language,
      which was otherwise the most apt, as it would have needed two glyphs picked
      by subtype. This one takes the rising tone, which is unmistakable, and is
      script-invariant. Anything else, Korean included, has no reading to show. */
  static String[] rubytext_exemplar()
  {
    switch (current_script())
    {
      case "kana":
        return new String[]{ "\u97f3", "\u304a\u3093" }; /* sound, "on" */
      case "pinyin":
      case "zhuyin":
        return new String[]{ "\u6587", "\u02ca" }; /* writing, rising tone */
      default:
        return null;
    }
  }

  /** The script the layout in front of the user declares - "kana", "pinyin",
      "zhuyin", "hangul" - or "" when there is none.

      Not the app's language. [DeviceLocales.OFFERED] is a table the build pins,
      so the language reads "ja_JP" on every layout; a script comes from the
      layout file, and changing layout changes it. Nothing pins a layout file.

      Read from the layout, not from the composer, so an idle row - no
      composition, only dictionary suggestions - still knows what it is showing
      and keeps the control. */
  static String current_script()
  {
    Config config = Config.globalConfig();
    int i = config.get_current_layout();
    if (i < 0 || i >= config.layouts.size())
      return "";
    KeyboardData layout = config.layouts.get(i);
    return layout == null ? "" : layout.script;
  }

  /** Whether the layout in front of the user annotates with a tone mark. An
      empty script - the system layout - is not a language that annotates. */
  static boolean is_chinese()
  {
    String script = current_script();
    return script.equals("pinyin") || script.equals("zhuyin");
  }

  /** How much larger than its line a Chinese tone mark is drawn. The marks are
      spacing modifier letters - U+02C9, U+02CA, U+02C7 and U+02CB - whose ink
      is a thin stroke high in the line: measured in the pinyin row it was 5px
      against a main text of 30px; the Chinese session measured that the hinted ink plateaus below 2x. 2.2x puts it at 10px, measured on the device. */
  static final float MARK_SCALE = 2.2f;
  /* Measured, not extrapolated: the mark is a thin stroke and hinting quantises
     its growth away. U+02CA in this box: 13.6px em -> 2px ink, 23.1px (1.7x) -> 4px,
     27.2px (2.0x) -> 4px, 40px -> 6px. Bold thickens the stroke without raising the
     ink. 2.2x is the chosen scale; 2.0x is the floor. The rows still show what the
     capture measures. */

  /** Draws a mark larger and in a heavier stroke than its line is laid out
      with. Deliberately a drawing-only span rather than a relative size span:
      a span that changed the metrics would make every cell taller and move the
      text inside it, which is the alignment trap of 44c396a. */
  static class HeavierMark extends CharacterStyle
  {
    float _scale;

    HeavierMark(float scale)
    {
      _scale = scale;
    }

    @Override
    public void updateDrawState(TextPaint tp)
    {
      tp.setTextSize(tp.getTextSize() * _scale);
      tp.setFakeBoldText(true);
    }
  }

  /** The annotation as it is drawn. Japanese readings are kana and are read
      well enough at the annotation size; Chinese tone marks are not, so they
      are drawn bigger and bolder. */
  CharSequence shown_annotation(String annotation)
  {
    if (annotation == null || !is_chinese())
      return annotation;
    SpannableString s = new SpannableString(annotation);
    s.setSpan(new HeavierMark(MARK_SCALE), 0, annotation.length(),
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    return s;
  }

  /** Draw the rubytext control in the state of the preference. Shown for a
      language that annotates its candidates, from the moment the keyboard is
      up rather than as the candidates come and go. */
  void update_rubytext_toggle()
  {
    String[] exemplar = rubytext_exemplar();
    if (exemplar == null)
    {
      _rubytext_toggle.setVisibility(View.GONE);
      return;
    }
    _rubytext_toggle.setVisibility(View.VISIBLE);
    String text = exemplar[0];
    String reading = exemplar[1];
    if (Config.globalConfig().annotations() && reading.length() > 0)
    {
      /* One string, the reading small above the character. A Chinese tone mark
         gets a little more room than a kana reading would, and is drawn in a
         heavier stroke: it is a single thin letter, not a word. */
      boolean chinese = is_chinese();
      SpannableString s = new SpannableString(reading + "\n" + text);
      s.setSpan(new RelativeSizeSpan(chinese ? 0.6f : 0.5f), 0,
          reading.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      if (chinese)
        s.setSpan(new HeavierMark(chinese ? MARK_SCALE : 1f), 0, reading.length(),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      _rubytext_toggle.setText(s);
    }
    else
      _rubytext_toggle.setText(text);
  }

  /** Whether the annotation of a candidate cell is drawn. When the readings
      are on, the line is kept even for a candidate that has no reading so
      that the cells stay aligned; when they are off, the line goes so that
      the row does not keep a blank one. */
  int annotation_visibility()
  {
    return Config.globalConfig().annotations() ? View.VISIBLE : View.GONE;
  }

  /** Show or hide the readings of the cells that are already drawn. The cells
      are not re-created: rebuilding them would blank the row for a frame, and
      the toggle has to show the same candidates in the other state. Their text
      is unchanged, only the annotation line goes or comes back. */
  void redraw_annotations()
  {
    int vis = annotation_visibility();
    for (int i = 0; i < _row.getChildCount(); i++)
    {
      TextView annotation =
        (TextView)_row.getChildAt(i).findViewById(R.id.candidate_annotation);
      if (annotation != null)
        annotation.setVisibility(vis);
    }
    _row.requestLayout();
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
      annotation.setText(shown_annotation(candidate.annotation));
        /* The Chinese mark is a spacing modifier letter: its ink sits at the top
           of the line box, so the descent is wasted space that grows the cell past
           the bar and clips the text. Trim it for Chinese only, and put the trim
           back on every bind because the cells are recycled - kana fills its line
           and would be clipped by the same trim. */
        boolean chinese_mark = is_chinese();
        annotation.setIncludeFontPadding(!chinese_mark);
        android.view.ViewGroup.LayoutParams alp = annotation.getLayoutParams();
        alp.height = chinese_mark ? (int)(_annotation_size * 1.5f)
                                  : android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
        annotation.setLayoutParams(alp);
      annotation.setVisibility(annotation_visibility());
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
    /* The control follows the language, not the row: an empty row does not
       take the switch away while the keyboard is up. */
    if (_rubytext_toggle != null)
      update_rubytext_toggle();
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
    if (_rubytext_toggle != null)
      /* The control is one key wide and shows two lines, the reading above the
         character: a character at the full label size does not leave room for
         the reading line inside the control's box. */
      set_text_size(_rubytext_toggle, _annotation_size, false);
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
