package juloo.keyboard2.prefs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import java.util.ArrayList;
import java.util.List;
import juloo.keyboard2.*;
import org.json.JSONException;
import org.json.JSONObject;

/** Lets the user enter custom words suggested when their prefix or an
    optional shortcut is typed. Shows at the top of the "Personal dictionary"
    settings screen. */
public class PersonalDictionaryPreference
  extends ListGroupPreference<PersonalDictionary.Entry>
{
  /** This pref stores a list encoded as JSON. Items are either a plain
      string (a word with no shortcut) or an object with the keys [word] and
      [shortcut]. */
  static final String KEY = "personal_dictionary";
  static final ListGroupPreference.Serializer<PersonalDictionary.Entry>
    SERIALIZER = new EntrySerializer();

  public PersonalDictionaryPreference(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    setKey(KEY);
  }

  /** The stored entries. Never [null]. */
  public static List<PersonalDictionary.Entry> get(SharedPreferences prefs)
  {
    List<PersonalDictionary.Entry> entries =
      load_from_preferences(KEY, prefs, null, SERIALIZER);
    return (entries != null) ? entries : new ArrayList<PersonalDictionary.Entry>();
  }

  String label_of_value(PersonalDictionary.Entry value, int i)
  {
    return value.shortcut.isEmpty() ? value.word
      : value.shortcut + " \u2192 " + value.word;
  }

  @Override
  void select(final SelectionCallback<PersonalDictionary.Entry> callback,
      PersonalDictionary.Entry old_value)
  {
    View content =
      View.inflate(getContext(), R.layout.dialog_personal_dict_entry, null);
    final EditText word_input =
      (EditText)content.findViewById(R.id.personal_dict_word);
    final EditText shortcut_input =
      (EditText)content.findViewById(R.id.personal_dict_shortcut);
    if (old_value != null)
    {
      word_input.setText(old_value.word);
      shortcut_input.setText(old_value.shortcut);
    }
    final AlertDialog dialog = new AlertDialog.Builder(getContext())
      .setView(content)
      .setPositiveButton(android.R.string.ok, null)
      .setNegativeButton(android.R.string.cancel, null)
      .show();
    // Override the OK button to keep the dialog open on invalid input.
    dialog.getButton(DialogInterface.BUTTON_POSITIVE)
      .setOnClickListener(new View.OnClickListener(){
        public void onClick(View v)
        {
          String w = word_input.getText().toString().trim();
          String s = shortcut_input.getText().toString().trim();
          if (w.equals(""))
          {
            word_input.setError(
                getContext().getString(R.string.pref_personal_dict_word_hint));
            return;
          }
          int invalid = invalid_shortcut_error(s);
          if (invalid != 0)
          {
            shortcut_input.setError(getContext().getString(invalid));
            return;
          }
          callback.select(new PersonalDictionary.Entry(w, s));
          dialog.dismiss();
        }
      });
  }

  /** [0] when [s] is a shortcut that can actually be typed, else the
      resource of a string explaining why it can never fire. An empty
      shortcut is valid and means the entry has no shortcut. */
  static int invalid_shortcut_error(String s)
  {
    if (s.isEmpty())
      return 0;
    if (s.length() < 2)
      return R.string.pref_personal_dict_shortcut_too_short;
    for (int i = 0; i < s.length(); i++)
    {
      if (!CurrentlyTypedWord.is_word_char(s.charAt(i)))
        return R.string.pref_personal_dict_shortcut_invalid_chars;
    }
    return 0;
  }

  @Override
  Serializer<PersonalDictionary.Entry> get_serializer() { return SERIALIZER; }

  static class EntrySerializer
    implements ListGroupPreference.Serializer<PersonalDictionary.Entry>
  {
    public PersonalDictionary.Entry load_item(Object obj) throws JSONException
    {
      if (obj instanceof String)
        return new PersonalDictionary.Entry((String)obj, "");
      JSONObject o = (JSONObject)obj;
      return new PersonalDictionary.Entry(o.getString("word"),
          o.optString("shortcut", ""));
    }

    public Object save_item(PersonalDictionary.Entry v) throws JSONException
    {
      if (v.shortcut.isEmpty())
        return v.word;
      JSONObject o = new JSONObject();
      o.put("word", v.word);
      o.put("shortcut", v.shortcut);
      return o;
    }
  }
}
