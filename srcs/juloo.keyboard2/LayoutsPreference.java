package juloo.keyboard2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public class LayoutsPreference extends ListGroupPreference<LayoutsPreference.Layout>
{
  static final String KEY = "layouts";
  static final List<Layout> DEFAULT =
    Collections.singletonList((Layout)new SystemLayout());
  static final ListGroupPreference.Serializer<Layout> SERIALIZER =
    new Serializer();

  /** Text displayed for each layout in the dialog list. */
  String[] _layout_display_names;

  public LayoutsPreference(Context ctx, AttributeSet attrs)
  {
    super(ctx, attrs);
    setKey(KEY);
    Resources res = ctx.getResources();
    _layout_display_names = res.getStringArray(R.array.pref_layout_entries);
  }

  /** Obtained from [res/values/layouts.xml]. */
  static List<String> _unsafe_layout_ids_str = null;
  static TypedArray _unsafe_layout_ids_res = null;

  /** Layout internal names. Contains "system" and "custom". */
  public static List<String> get_layout_names(Resources res)
  {
    if (_unsafe_layout_ids_str == null)
      _unsafe_layout_ids_str = Arrays.asList(
          res.getStringArray(R.array.pref_layout_values));
    return _unsafe_layout_ids_str;
  }

  /** Layout resource id for a layout name. [-1] if not found. */
  public static int layout_id_of_name(Resources res, String name)
  {
    if (_unsafe_layout_ids_res == null)
      _unsafe_layout_ids_res = res.obtainTypedArray(R.array.layout_ids);
    int i = get_layout_names(res).indexOf(name);
    if (i >= 0)
      return _unsafe_layout_ids_res.getResourceId(i, 0);
    return -1;
  }

  /** [null] for the "system" layout. */
  public static List<KeyboardData> load_from_preferences(Resources res, SharedPreferences prefs)
  {
    List<KeyboardData> layouts = new ArrayList<KeyboardData>();
    for (Layout l : load_from_preferences(KEY, prefs, DEFAULT, SERIALIZER))
    {
      if (l instanceof NamedLayout)
        layouts.add(layout_of_string(res, ((NamedLayout)l).name));
      else if (l instanceof CustomLayout)
        layouts.add(KeyboardData.load_string(((CustomLayout)l).xml));
      else // instanceof SystemLayout
        layouts.add(null);
    }
    return layouts;
  }

  public static KeyboardData layout_of_string(Resources res, String name)
  {
    int id = layout_id_of_name(res, name);
    if (id > 0)
      return KeyboardData.load(res, id);
    // Might happen when the app is downgraded, return the system layout.
    return null;
  }

  @Override
  protected void onSetInitialValue(boolean restoreValue, Object defaultValue)
  {
    super.onSetInitialValue(restoreValue, defaultValue);
    if (_values.size() == 0)
      set_values(new ArrayList<Layout>(DEFAULT), false);
  }

  String label_of_layout(Layout l)
  {
    if (l instanceof NamedLayout)
    {
      String lname = ((NamedLayout)l).name;
      int value_i = get_layout_names(getContext().getResources()).indexOf(lname);
      return value_i < 0 ? lname : _layout_display_names[value_i];
    }
    else if (l instanceof CustomLayout)
      return getContext().getString(R.string.pref_layout_e_custom);
    else // instanceof SystemLayout
      return getContext().getString(R.string.pref_layout_e_system);
  }

  @Override
  String label_of_value(Layout value, int i)
  {
    return getContext().getString(R.string.pref_layouts_item, i + 1,
        label_of_layout(value));
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

  @Override
  ListGroupPreference.Serializer<Layout> get_serializer() { return SERIALIZER; }

  @Override
  void select(final SelectionCallback callback)
  {
    ArrayAdapter layouts = new ArrayAdapter(getContext(), android.R.layout.simple_list_item_1, _layout_display_names);
    new AlertDialog.Builder(getContext())
      .setView(R.layout.dialog_edit_text)
      .setAdapter(layouts, new DialogInterface.OnClickListener(){
        public void onClick(DialogInterface _dialog, int which)
        {
          String name = get_layout_names(getContext().getResources()).get(which);
          switch (name)
          {
            case "system":
              callback.select(new SystemLayout());
              break;
            case "custom":
              select_custom(callback);
              break;
            default:
              callback.select(new NamedLayout(name));
              break;
          }
        }
      })
      .show();
  }

  void select_custom(final SelectionCallback callback)
  {
    new AlertDialog.Builder(getContext())
      .setView(R.layout.dialog_edit_text)
      .setTitle(R.string.pref_custom_layout_title)
      .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
        public void onClick(DialogInterface dialog, int _which)
        {
          EditText input = (EditText)((AlertDialog)dialog).findViewById(R.id.text);
          callback.select(new CustomLayout(input.getText().toString()));
        }
      })
      .setNegativeButton(android.R.string.cancel, null)
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

  /** A layout selected by the user. The only implementations are
      [NamedLayout], [SystemLayout] and [CustomLayout]. */
  interface Layout {}

  static final class SystemLayout implements Layout
  {
    public SystemLayout() {}
  }

  /** The name of a layout defined in [res/xml]. */
  static final class NamedLayout implements Layout
  {
    public final String name;
    public NamedLayout(String n) { name = n; }
  }

  /** The XML description of a custom layout. */
  static final class CustomLayout implements Layout
  {
    public final String xml;
    public CustomLayout(String c) { xml = c; }
  }

  /** Named layouts are serialized to strings and custom layouts to JSON
      objects with a [kind] field. */
  static class Serializer implements ListGroupPreference.Serializer<Layout>
  {
    public Layout load_item(Object obj) throws JSONException
    {
      if (obj instanceof String)
      {
        String name = (String)obj;
        if (name.equals("system"))
          return new SystemLayout();
        return new NamedLayout(name);
      }
      JSONObject obj_ = (JSONObject)obj;
      switch (obj_.getString("kind"))
      {
        case "custom": return new CustomLayout(obj_.getString("xml"));
        case "system": default: return new SystemLayout();
      }
    }

    public Object save_item(Layout v) throws JSONException
    {
      if (v instanceof NamedLayout)
        return ((NamedLayout)v).name;
      if (v instanceof CustomLayout)
        return new JSONObject().put("kind", "custom")
          .put("xml", ((CustomLayout)v).xml);
      return new JSONObject().put("kind", "system");
    }
  }
}
