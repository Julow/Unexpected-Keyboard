package juloo.keyboard2.suggestions;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.HorizontalScrollView;

/** The row of candidates. It tells its listener about scrolls so that the
    candidates view can add more cells when the user reaches the end. */
public class CandidatesScrollView extends HorizontalScrollView
{
  public static interface OnScroll
  {
    public void on_scroll(int scrollX, int scrollY);
  }

  OnScroll _on_scroll = null;

  public CandidatesScrollView(Context context) { super(context); }

  public CandidatesScrollView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
  }

  public void set_on_scroll(OnScroll l) { _on_scroll = l; }

  @Override
  protected void onScrollChanged(int l, int t, int oldl, int oldt)
  {
    super.onScrollChanged(l, t, oldl, oldt);
    if (_on_scroll != null)
      _on_scroll.on_scroll(l, t);
  }
}
