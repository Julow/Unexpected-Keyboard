package juloo.keyboard2.prefs;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

public class ClickablePreference extends Preference
{
  public ClickablePreference(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    setPersistent(false);
  }

  public ClickablePreference(Context context, AttributeSet attrs, int defStyleAttr)
  {
    super(context, attrs, defStyleAttr);
    setPersistent(false);
  }
}
