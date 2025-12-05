package juloo.keyboard2.dict;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import juloo.cdict.Cdict;
import juloo.keyboard2.Utils;

/** Manage and load installed dictionaries. */
public final class Dictionaries
{
  public Dictionaries(Context ctx)
  {
    _context = ctx;
    _shared_prefs = ctx.getSharedPreferences("dictionaries", Context.MODE_PRIVATE);
    Set<String> s = _shared_prefs.getStringSet(PREF_INSTALLED_DICTS, null);
    _installed_dictionaries = (s == null) ? new HashSet() : s;
  }

  /** Load an installed dictionary. Return [null] if the requested dictionary
      is not installed. Throws [IOException] if the dictionary couldn't be
      loaded. */
  public Cdict load(String dict_name)
      throws IOException, Cdict.ConstructionError
  {
    if (!_installed_dictionaries.contains(dict_name))
      return null;
    FileInputStream inp = _context.openFileInput(dict_file_name(dict_name));
    byte[] data = Utils.read_all_bytes(inp);
    inp.close();
    return Cdict.of_bytes(data);
  }

  public Set<String> get_installed() { return _installed_dictionaries; }

  public void install(String dict_name, byte[] data) throws IOException
  {
    FileOutputStream outp = _context.openFileOutput(dict_file_name(dict_name),
        Context.MODE_PRIVATE);
    outp.write(data);
    outp.close();
    set_installed(dict_name);
  }

  /** Return the absolute path used to store the dictionary with the given
      name. Return the same result whether the dictionary is installed or not. */
  public File get_install_location(String dict_name)
  {
    return _context.getFileStreamPath(dict_file_name(dict_name));
  }

  /** Declare a dictionary as installed. A dictionary file must exist at the
      path returned by [get_install_location(dict_name)]. */
  public void set_installed(String dict_name)
  {
    _installed_dictionaries.add(dict_name);
    save();
  }

  public void uninstall(String dict_name)
  {
    _context.deleteFile(dict_file_name(dict_name));
    _installed_dictionaries.remove(dict_name);
    save();
  }

  /** Private */

  Context _context;
  Set<String> _installed_dictionaries;
  SharedPreferences _shared_prefs;

  static final String PREF_INSTALLED_DICTS = "installed";

  void save()
  {
    _shared_prefs.edit()
      .putStringSet(PREF_INSTALLED_DICTS, _installed_dictionaries)
      .commit();
  }

  static String dict_file_name(String dict_name)
  {
    return dict_name + ".dict";
  }
}
