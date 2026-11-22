package juloo.keyboard2.dict;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import juloo.cdict.Cdict;
import juloo.keyboard2.Logs;
import juloo.keyboard2.R;
import juloo.keyboard2.Utils;

public class DictionariesActivity extends Activity
{
  /** Request code of the file picker started by the import button. */
  static final int IMPORT_FILE_REQUEST = 1;

  Dictionaries _dictionaries;
  DictionaryListView _list_installed;
  DictionaryListView _list_locales;
  DictionaryListView _list_all;

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    _dictionaries = Dictionaries.instance(this);
    setContentView(R.layout.dictionaries_activity);
    _list_installed =
      (DictionaryListView)findViewById(R.id.dictionaries_list_installed);
    _list_locales =
      (DictionaryListView)findViewById(R.id.dictionaries_list_locales);
    _list_all = (DictionaryListView)findViewById(R.id.dictionaries_list_all);
    findViewById(R.id.dictionaries_import_button).setOnClickListener(
        new View.OnClickListener()
        {
          public void onClick(View v) { open_file_picker(); }
        });
    refresh_lists();
  }

  void toggle_installed(String dict_name)
  {
    if (_dictionaries.get_installed().contains(dict_name))
      _dictionaries.uninstall(dict_name);
    else
      download_in_browser(dict_name);
    refresh_lists();
  }

  void refresh_lists()
  {
    Set<String> installed = _dictionaries.get_installed();
    _list_installed.refresh(installed);
    _list_locales.refresh(installed);
    _list_all.refresh(installed);
  }

  /** Open the dictionary file in a browser. */
  void download_in_browser(String dict_name)
  {
    try
    {
      startActivity(new Intent(Intent.ACTION_VIEW,
            url_of_dictionary(dict_name)));
    }
    catch (Exception e)
    {
      post_toast(getString(R.string.dictionaries_download_failed));
    }
  }

  static final String DICT_REPO_URL =
    "https://raw.githubusercontent.com/Julow/Unexpected-Keyboard-dictionaries/refs/heads/main";

  static Uri url_of_dictionary(String dict_name)
  {
    int format_version = Cdict.format_version();
    return Uri.parse(DICT_REPO_URL + "/v" + format_version + "/" + dict_name
        + ".dict");
  }

  @Override
  protected void onActivityResult(int request, int result, Intent data)
  {
    super.onActivityResult(request, result, data);
    if (request == IMPORT_FILE_REQUEST && result == RESULT_OK
        && data != null && data.getData() != null)
      import_file(data.getData());
  }

  /** Accept any file. */
  void open_file_picker()
  {
    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    i.addCategory(Intent.CATEGORY_OPENABLE);
    i.setType("*/*");
    startActivityForResult(i, IMPORT_FILE_REQUEST);
  }

  void import_file(Uri uri)
  {
    (new Thread()
     {
       public void run()
       {
         try
         {
           install_dictionary_from_file(uri);
           post_toast(getString(R.string.dictionaries_import_success));
         }
         catch (Exception e)
         {
           Logs.exn("Dictionary import", e);
           post_toast(getString(R.string.dictionaries_import_failed)
               + " (" + e.getMessage() + ")");
         }
         runOnUiThread(new Runnable()
             {
               public void run() { refresh_lists(); }
             });
       }
     }).start();
  }

  /** Install the dictionary file [uri]. Only accept known dictionaries.
      Unknown files are rejected as they can crash libcdict. */
  void install_dictionary_from_file(Uri uri) throws Exception
  {
    SupportedDictionaries ds = SupportedDictionaries.get(getResources());
    // Dictionaries are always compressed.
    byte[] data = Utils.read_all_bytes(
        new GZIPInputStream(getContentResolver().openInputStream(uri)));
    String dict_name = ds.find_by_checksum(data);
    if (dict_name == null)
      throw new Exception("Unknown dictionary hash");
    Cdict.of_bytes(data); // Check that the dictionary can load.
    _dictionaries.install(dict_name, data);
  }

  /** Show a message to the user. Can be called from any thread. */
  void post_toast(String msg)
  {
    runOnUiThread(new Runnable()
        {
          public void run()
          {
            Toast.makeText(DictionariesActivity.this, msg,
                Toast.LENGTH_SHORT).show();
          }
        });
  }
}
