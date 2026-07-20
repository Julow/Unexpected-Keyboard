package juloo.keyboard2.dict;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.inputmethodservice.InputMethodService;
import android.widget.ArrayAdapter;
import java.util.ArrayList;
import java.util.List;
import juloo.keyboard2.*;

public final class DictionarySwitcher
{
  InputMethodService _ims;
  Dictionaries _dicts;
  SupportedDictionaries _sd;
  Callback _callback;

  public DictionarySwitcher(InputMethodService ims, Dictionaries dicts,
      Callback callback)
  {
    _ims = ims;
    _dicts = dicts;
    _sd = SupportedDictionaries.get(ims.getResources());
    _callback = callback;
  }

  public void choose()
  {
    final List<String> dict_names = new ArrayList<String>(_dicts.get_installed());
    List<String> labels = new ArrayList<String>();
    for (String name : dict_names)
      labels.add(_sd.get_display_name(name));
    ArrayAdapter adapter =
      new ArrayAdapter(_ims, android.R.layout.simple_list_item_1, labels);
    AlertDialog dialog = new AlertDialog.Builder(_ims)
      .setTitle(R.string.dictionary_switcher_title)
      .setAdapter(adapter, new DialogInterface.OnClickListener(){
        public void onClick(DialogInterface _d, int which)
        {
          _callback.on_switch_dictionary(dict_names.get(which));
        }
      })
      .setPositiveButton(R.string.launcher_button_dictionaries,
        new DialogInterface.OnClickListener(){
          public void onClick(DialogInterface _d, int _which)
          {
            _callback.launch_dictionaries_activity();
          }
        })
      .setNegativeButton(android.R.string.cancel, null)
      .create();
    Utils.show_dialog_on_ime(dialog, _ims);
  }

  public interface Callback
  {
    /** Called when the user switches dictionary. */
    public void on_switch_dictionary(String dict_name);
    /** Open the dictionaries activity */
    public void launch_dictionaries_activity();
  }
}
