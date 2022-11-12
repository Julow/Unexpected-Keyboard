package juloo.keyboard2;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class LayoutListPreference extends ListPreference
{
  public LayoutListPreference(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LayoutListPreference);
    String defaultString = a.getString(R.styleable.LayoutListPreference_defaultString);
    a.recycle();
    Resources res = context.getResources();
    String[] entries = res.getStringArray(R.array.pref_layout_entries);
    entries[0] = defaultString;
    setEntries(entries);
    setEntryValues(res.getStringArray(R.array.pref_layout_values));
    setSummary("%s");
    setDefaultValue("none");
  }
}
