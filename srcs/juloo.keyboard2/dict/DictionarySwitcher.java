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
      .setAdapter(adapter, new DialogInterface.OnClickListener(){
        public void onClick(DialogInterface _d, int which)
        {
          _callback.on_switch_dictionary(dict_names.get(which));
        }
      })
      .create();
    Utils.show_dialog_on_ime(dialog,
        _ims.getWindow().getWindow().getDecorView().getWindowToken());
  }

  /** Called with the dictionary name chosen by the user. */
  public interface Callback
  {
    public void on_switch_dictionary(String dict_name);
  }
}
