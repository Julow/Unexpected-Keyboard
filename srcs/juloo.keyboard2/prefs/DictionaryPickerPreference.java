package juloo.keyboard2.prefs;

import android.app.AlertDialog;
import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import juloo.keyboard2.R;
import juloo.keyboard2.dict.Dictionaries;
import juloo.keyboard2.dict.SupportedDictionaries;

/** A [ListPreference] whose entries are built dynamically from the installed
    dictionaries. Shows "Auto" plus each installed dictionary by display name. */
public class DictionaryPickerPreference extends ListPreference
{
  public DictionaryPickerPreference(Context ctx, AttributeSet attrs)
  {
    super(ctx, attrs);
  }

  @Override
  protected void onPrepareDialogBuilder(AlertDialog.Builder builder)
  {
    refresh_entries();
    super.onPrepareDialogBuilder(builder);
  }

  void refresh_entries()
  {
    Context ctx = getContext();
    Set<String> installed = Dictionaries.instance(ctx).get_installed();
    SupportedDictionaries supported = new SupportedDictionaries(ctx.getResources());

    List<CharSequence> entries = new ArrayList<>();
    List<CharSequence> values = new ArrayList<>();

    entries.add(ctx.getString(R.string.pref_selected_dictionary_auto));
    values.add("auto");

    for (int i = 0; i < supported.length(); i++)
    {
      String name = supported.dict_name(i);
      if (installed.contains(name))
      {
        entries.add(supported.display_name(i));
        values.add(name);
      }
    }

    setEntries(entries.toArray(new CharSequence[0]));
    setEntryValues(values.toArray(new CharSequence[0]));
  }
}
