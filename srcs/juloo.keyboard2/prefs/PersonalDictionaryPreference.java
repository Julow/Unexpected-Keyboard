package juloo.keyboard2.prefs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import juloo.keyboard2.*;

/** Lets the user enter custom words suggested when their prefix is typed.
    Shows at the top of the "Personal dictionary" settings screen. */
public class PersonalDictionaryPreference extends ListGroupPreference<String>
{
  /** This pref stores a list of strings encoded as JSON. */
  static final String KEY = "personal_dictionary";
  static final ListGroupPreference.Serializer<String> SERIALIZER =
    new ListGroupPreference.StringSerializer();

  public PersonalDictionaryPreference(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    setKey(KEY);
  }

  /** The stored words. Never [null]. */
  public static List<String> get(SharedPreferences prefs)
  {
    List<String> words = load_from_preferences(KEY, prefs, null, SERIALIZER);
    return (words != null) ? words : new ArrayList<String>();
  }

  String label_of_value(String value, int i) { return value; }

  @Override
  void select(final SelectionCallback<String> callback, String old_value)
  {
    View content = View.inflate(getContext(), R.layout.dialog_edit_text, null);
    ((TextView)content.findViewById(R.id.text)).setText(old_value);
    new AlertDialog.Builder(getContext())
      .setView(content)
      .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
        public void onClick(DialogInterface dialog, int which)
        {
          EditText input = (EditText)((AlertDialog)dialog).findViewById(R.id.text);
          final String w = input.getText().toString().trim();
          if (!w.equals(""))
            callback.select(w);
        }
      })
      .setNegativeButton(android.R.string.cancel, null)
      .show();
  }

  @Override
  Serializer<String> get_serializer() { return SERIALIZER; }
}
