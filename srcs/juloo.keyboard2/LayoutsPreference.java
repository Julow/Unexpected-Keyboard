package juloo.keyboard2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LayoutsPreference extends ListGroupPreference
{
  static final String KEY = "layouts";
  static final List<String> DEFAULT = Collections.singletonList("system");

  /** Layout names as stored in the preferences. */
  List<String> _layout_names;
  /** Text displayed for each layout in the dialog list. */
  String[] _layout_display_names;

  public LayoutsPreference(Context ctx, AttributeSet attrs)
  {
    super(ctx, attrs);
    setKey(KEY);
    Resources res = ctx.getResources();
    _layout_names = Arrays.asList(res.getStringArray(R.array.pref_layout_values));
    _layout_display_names = res.getStringArray(R.array.pref_layout_entries);
  }

  public static List<String> load_from_preferences(SharedPreferences prefs)
  {
    return load_from_preferences(KEY, prefs, DEFAULT);
  }

  @Override
  protected void onSetInitialValue(boolean restoreValue, Object defaultValue)
  {
    super.onSetInitialValue(restoreValue, defaultValue);
    if (_values.size() == 0)
      set_values(new ArrayList<String>(DEFAULT), false);
  }

  @Override
  String label_of_value(String value, int i)
  {
    int value_i = _layout_names.indexOf(value);
    String lname = value_i < 0 ? value : _layout_display_names[value_i];
    return getContext().getString(R.string.pref_layouts_item, i + 1, lname);
  }

  @Override
  AddButton on_attach_add_button(AddButton prev_btn)
  {
    if (prev_btn == null)
      return new LayoutsAddButton(getContext());
    return prev_btn;
  }

  @Override
  boolean should_allow_remove_item()
  {
    return (_values.size() > 1);
  }

  void select(final SelectionCallback callback)
  {
    ArrayAdapter layouts = new ArrayAdapter(getContext(), android.R.layout.simple_list_item_1, _layout_display_names);
    new AlertDialog.Builder(getContext())
      .setView(R.layout.custom_extra_key_add_dialog)
      .setAdapter(layouts, new DialogInterface.OnClickListener(){
        public void onClick(DialogInterface dialog, int which)
        {
          callback.select(_layout_names.get(which));
        }
      })
      .show();
  }

  class LayoutsAddButton extends AddButton
  {
    public LayoutsAddButton(Context ctx)
    {
      super(ctx);
      setLayoutResource(R.layout.pref_layouts_add_btn);
    }
  }
}
