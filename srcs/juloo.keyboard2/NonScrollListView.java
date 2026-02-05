package juloo.keyboard2;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.ListView;

/** A non-scrollable list view that can be embedded in a bigger ScrollView.
    Credits to Dedaniya HirenKumar in
    https://stackoverflow.com/questions/18813296/non-scrollable-listview-inside-scrollview */
public class NonScrollListView extends ListView
{
  public NonScrollListView(Context context)
  {
    super(context);
  }

  public NonScrollListView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
  }

  public NonScrollListView(Context context, AttributeSet attrs, int defStyle)
  {
    super(context, attrs, defStyle);
  }

  @Override
  public void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
  {
    int heightMeasureSpec_custom = MeasureSpec.makeMeasureSpec(
        Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
    super.onMeasure(widthMeasureSpec, heightMeasureSpec_custom);
    // Remove params modification to avoid interference
  }
}
