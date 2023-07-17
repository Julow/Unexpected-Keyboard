package juloo.keyboard2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;

/** Allows to enter custom keys to be added to the keyboard. This shows up at
    the top of the "Add keys to the keyboard" option. */
public class CustomExtraKeysPreference extends PreferenceCategory
{
  /** This pref stores a list of strings encoded as JSON. */
  static String KEY = "custom_extra_keys";

  boolean _attached = false;
  /** Mutable. This is the list of the key strings, not the key names. */
  List<String> _keys;

  public CustomExtraKeysPreference(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    setKey(KEY);
    setOrderingAsAdded(true);
    _keys = new ArrayList<String>();
  }

  public static List<KeyValue> get(SharedPreferences prefs)
  {
    List<KeyValue> kvs = new ArrayList<KeyValue>();
    String inp = prefs.getString(KEY, null);
    if (inp != null)
      for (String key_name : load_from_string(inp))
        kvs.add(KeyValue.makeStringKey(key_name));
    return kvs;
  }

  @Override
  protected void onSetInitialValue(boolean restoreValue, Object defaultValue)
  {
    if (restoreValue)
    {
      String persisted = getPersistedString(null);
      if (persisted != null)
        set_keys(load_from_string(persisted), false);
    }
    else if (defaultValue != null)
      set_keys(load_from_string((String)defaultValue), false);
  }

  @Override
  protected void onAttachedToActivity()
  {
    super.onAttachedToActivity();
    if (_attached)
      return;
    _attached = true;
    reattach();
  }

  void reattach()
  {
    removeAll();
    for (String k : _keys)
      addPreference(this.new CustomExtraKey(getContext(), k));
    addPreference(this.new AddButton(getContext()));
  }

  void set_keys(List<String> v, boolean persist)
  {
    _keys = v;
    reattach();
    if (persist)
      persistString(save_to_string(_keys));
  }

  void add_key(String k)
  {
    _keys.add(k);
    set_keys(_keys, true);
  }

  void remove_key(String k)
  {
    _keys.remove(k);
    set_keys(_keys, true);
  }

  static String save_to_string(List<String> keys)
  {
    return (new JSONArray(keys)).toString();
  }

  static List<String> load_from_string(String inp)
  {
    List<String> keys = new ArrayList<String>();
    try
    {
      JSONArray arr = new JSONArray(inp);
      for (int i = 0; i < arr.length(); i++)
        keys.add(arr.getString(i));
    }
    catch (JSONException e) {}
    return keys;
  }

  /** A preference with no key that is only intended to be rendered. */
  final class CustomExtraKey extends Preference implements View.OnClickListener
  {
    String _key;

    public CustomExtraKey(Context ctx, String key)
    {
      super(ctx);
      _key = key;
      setTitle(key);
      setPersistent(false);
      setWidgetLayoutResource(R.layout.custom_extra_key_widget);
    }

    /** Remove-button listener. */
    @Override
    public void onClick(View _v)
    {
      CustomExtraKeysPreference.this.remove_key(_key);
    }

    @Override
    protected View onCreateView(ViewGroup parent)
    {
      View v = super.onCreateView(parent);
      v.findViewById(R.id.btn_custom_extra_key_remove).setOnClickListener(this);
      return v;
    }
  }

  final class AddButton extends Preference
  {
    public AddButton(Context ctx)
    {
      super(ctx);
      setPersistent(false);
      setLayoutResource(R.layout.custom_extra_key_add);
    }

    @Override
    protected void onClick()
    {
      new AlertDialog.Builder(getContext())
        .setView(R.layout.custom_extra_key_add_dialog)
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
          public void onClick(DialogInterface dialog, int which)
          {
            EditText input = (EditText)((AlertDialog)dialog).findViewById(R.id.key_name);
            String k = input.getText().toString();
            if (!k.equals(""))
              CustomExtraKeysPreference.this.add_key(k);
          }
        })
        .setNegativeButton(android.R.string.cancel, null)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .show();
    }
  }
}
