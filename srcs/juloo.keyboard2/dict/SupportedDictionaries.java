package juloo.keyboard2.dict;

import android.content.res.Resources;
import android.util.Base64;
import java.security.MessageDigest;
import java.util.Arrays;
import juloo.keyboard2.R;

/** Access arrays in [dictionaries.xml]. */
public class SupportedDictionaries
{
  public String[] locales;
  public String[] names;

  Resources _res;
  // Computed on demand
  int[] _sizes = null;
  String[] _sha256 = null;

  SupportedDictionaries(Resources res)
  {
    locales = res.getStringArray(R.array.dictionaries_locale);
    names = res.getStringArray(R.array.dictionaries_name);
    _res = res;
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
  public int size(int i) { return get_sizes()[i]; }

  public String get_display_name(String dict_name)
  {
    int i = find(dict_name);
    return (i >= 0) ? names[i] : dict_name;
  }

  /** Return the name of the dictionary if it is known, [null] otherwise. */
  public String find_by_checksum(byte[] data) throws Exception
  {
    String hash = Base64.encodeToString(
        MessageDigest.getInstance("SHA-256").digest(data), Base64.NO_WRAP);
    String[] hashes = get_sha256s();
    for (int i = 0; i < hashes.length; i++)
      if (hashes[i].equals(hash))
        return locales[i];
    return null;
  }

  int[] get_sizes()
  {
    if (_sizes == null)
      _sizes = _res.getIntArray(R.array.dictionaries_size);
    return _sizes;
  }

  String[] get_sha256s()
  {
    if (_sha256 == null)
      _sha256 = _res.getStringArray(R.array.dictionaries_sha256);
    return _sha256;
  }
}
