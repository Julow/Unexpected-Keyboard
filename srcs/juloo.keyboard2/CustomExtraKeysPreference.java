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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;

/** Allows to enter custom keys to be added to the keyboard. This shows up at
    the top of the "Add keys to the keyboard" option. */
public class CustomExtraKeysPreference extends ListGroupPreference<String>
{
  /** This pref stores a list of strings encoded as JSON. */
  static final String KEY = "custom_extra_keys";
  static final ListGroupPreference.Serializer<String> SERIALIZER =
    new ListGroupPreference.StringSerializer();

  public CustomExtraKeysPreference(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    setKey(KEY);
  }

  public static Map<KeyValue, KeyboardData.PreferredPos> get(SharedPreferences prefs)
  {
    Map<KeyValue, KeyboardData.PreferredPos> kvs =
      new HashMap<KeyValue, KeyboardData.PreferredPos>();
    List<String> key_names = load_from_preferences(KEY, prefs, null, SERIALIZER);
    if (key_names != null)
    {
      for (String key_name : key_names)
        kvs.put(KeyValue.makeStringKey(key_name), KeyboardData.PreferredPos.DEFAULT);
    }
    return kvs;
  }

  String label_of_value(String value, int i) { return value; }

  @Override
  void select(final SelectionCallback<String> callback)
  {
    new AlertDialog.Builder(getContext())
      .setView(R.layout.dialog_edit_text)
      .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
        public void onClick(DialogInterface dialog, int which)
        {
          EditText input = (EditText)((AlertDialog)dialog).findViewById(R.id.text);
          final String k = input.getText().toString();
          if (!k.equals(""))
            callback.select(k);
        }
      })
      .setNegativeButton(android.R.string.cancel, null)
      .show();
  }

  @Override
  Serializer<String> get_serializer() { return SERIALIZER; }
}
