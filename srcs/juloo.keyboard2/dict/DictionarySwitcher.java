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
  Config _config;
  Dictionaries _dicts;
  SupportedDictionaries _sd;
  Runnable _callback;

  public DictionarySwitcher(InputMethodService ims, Config config,
      Dictionaries dicts, Runnable callback)
  {
    _ims = ims;
    _config = config;
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
          switch_(dict_names.get(which));
        }
      })
      .create();
    Utils.show_dialog_on_ime(dialog,
        _ims.getWindow().getWindow().getDecorView().getWindowToken());
  }

  void switch_(String dict_name)
  {
    _dicts.set_current_dictionary(_config, dict_name);
    _callback.run();
  }
}
