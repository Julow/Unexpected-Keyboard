package juloo.keyboard2.dict;

import android.content.res.Resources;
import android.util.Base64;
import java.util.Arrays;
import juloo.keyboard2.R;

/** Access arrays in [dictionaries.xml]. */
public class SupportedDictionaries
{
  public String[] locales;
  public String[] names;
  public int[] sizes;
  public String[] sha256;

  SupportedDictionaries(Resources res)
  {
    locales = res.getStringArray(R.array.dictionaries_locale);
    names = res.getStringArray(R.array.dictionaries_name);
    sizes = res.getIntArray(R.array.dictionaries_size);
    sha256 = res.getStringArray(R.array.dictionaries_sha256);
  }

  public static SupportedDictionaries get(Resources res)
  {
    if (_cached == null)
      _cached = new SupportedDictionaries(res);
    return _cached;
  }
  static SupportedDictionaries _cached = null;

  /** Find the index for a given dictionary name. Return [-1] if not found. */
  public int find(String dict_name)
  {
    if (dict_name == null)
      return -1;
    int i = Arrays.binarySearch(locales, dict_name);
    return (i < 0) ? -1 : i;
  }

  public int length() { return locales.length; }

  public String dict_name(int i) { return locales[i]; }
  public String display_name(int i) { return names[i]; }
  public int size(int i) { return sizes[i]; }

  public String get_display_name(String dict_name)
  {
    int i = find(dict_name);
    return (i >= 0) ? names[i] : dict_name;
  }

  /** SHA-256 hash of the dictionary in binary format. Return [null] if not
      found. */
  public byte[] get_sha256(String dict_name)
  {
    int i = find(dict_name);
    if (i < 0)
      return null;
    return Base64.decode(sha256[i], Base64.DEFAULT);
  }
}
