package juloo.keyboard2.suggestions;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import juloo.keyboard2.Config;
import juloo.keyboard2.R;
import juloo.keyboard2.dict.Dictionaries;

public class CandidatesView extends LinearLayout
{
  static final int NUM_CANDIDATES = 3;

  Config _config;

  /** Candidates currently visible. Entries can be [null] when there are less
      than [NUM_CANDIDATES] suggestions. */
  String[] _items = new String[NUM_CANDIDATES];

  /** Text views showing the candidates in [_items]. Text views visibility is
      set to [GONE] when there are less than [NUM_CANDIDATES] suggestions. */
  TextView[] _item_views = new TextView[NUM_CANDIDATES];

  /** Optional view showing a message to the user. Visible when no candidates
      are shown. Might be [null]. */
  View _status_no_dict = null; // Dictionary not installed

  public CandidatesView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    _config = Config.globalConfig();
  }

  @Override
  protected void onFinishInflate()
  {
    super.onFinishInflate();
    setup_item_view(0, R.id.candidates_middle);
    setup_item_view(1, R.id.candidates_right);
    setup_item_view(2, R.id.candidates_left);
  }

  public void set_candidates(List<String> suggestions)
  {
    int s_count = suggestions.size();
    for (int i = 0; i < _item_views.length; i++)
    {
      TextView v = _item_views[i];
      if (i < s_count)
      {
        String it = suggestions.get(i);
        _items[i] = it;
        v.setText(it);
        v.setVisibility(View.VISIBLE);
      }
      else
      {
        _items[i] = null;
        v.setVisibility(View.GONE);
      }
    }
  }

  /** Refresh the status messages after a configuration refresh. The status
      message indicates whether the dictionaries should be installed. */
  public void refresh_status()
  {
    set_candidates(Suggestions.NO_SUGGESTIONS);
    _status_no_dict = inflate_and_show(_status_no_dict,
        (_config.current_dictionary == null),
        R.layout.candidates_status_no_dict);
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
              _config.handler.suggestion_entered(it);
          }
        });
    v.setVisibility(View.GONE);
    _item_views[item_index] = v;
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
            if ((flags & InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0)
              return false; // Editor requested that we don't show suggestions
            return true;
        }
      case InputType.TYPE_CLASS_NUMBER:
        // Beware of TYPE_NUMBER_VARIATION_PASSWORD
        return false;
      default: return false;
    }
  }
}
