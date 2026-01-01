package juloo.keyboard2.dict;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
    _loaded_dictionaries = new TreeMap<String, Cdict[]>();
  }

  /** Util for finding a dictionary by name. Returns [null] if not found. */
  public static Cdict find_by_name(Cdict[] dicts, String name)
  {
    for (Cdict d : dicts)
      if (d.name.equals(name))
        return d;
    return null;
  }

  /** Load an installed dictionary. Return [null] if the requested dictionary
      is not installed or the dictionary couldn't be loaded. */
  public Cdict[] load(String dict_name)
  {
    if (_loaded_dictionaries.containsKey(dict_name))
      return _loaded_dictionaries.get(dict_name);
    Cdict[] dict = load_uncached(dict_name);
    _loaded_dictionaries.put(dict_name, dict);
    return dict;
  }

  Cdict[] load_uncached(String dict_name)
  {
    if (!_installed_dictionaries.contains(dict_name))
      return null;
    try
    {
      FileInputStream inp = _context.openFileInput(dict_file_name(dict_name));
      byte[] data = Utils.read_all_bytes(inp);
      inp.close();
      return Cdict.of_bytes(data);
    }
    catch (IOException e) { return null; }
    catch (Cdict.ConstructionError e) { return null; }
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
    _loaded_dictionaries.remove(dict_name);
    save();
  }

  public void uninstall(String dict_name)
  {
    _context.deleteFile(dict_file_name(dict_name));
    _installed_dictionaries.remove(dict_name);
    _loaded_dictionaries.remove(dict_name);
    save();
  }

  /** Private */

  Context _context;
  Set<String> _installed_dictionaries;
  SharedPreferences _shared_prefs;
  Map<String, Cdict[]> _loaded_dictionaries;

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
