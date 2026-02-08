package juloo.keyboard2.dict;

import android.content.res.Resources;
import java.util.Arrays;
import juloo.keyboard2.R;

/** Access arrays in [dictionaries.xml]. */
public class SupportedDictionaries
{
  public String[] locales;
  public String[] names;
  public int[] sizes;

  public SupportedDictionaries(Resources res)
  {
    locales = res.getStringArray(R.array.dictionaries_locale);
    names = res.getStringArray(R.array.dictionaries_name);
    sizes = res.getIntArray(R.array.dictionaries_size);
  }

  /** Find the index for a given dictionary name. Return [-1] if not found. */
  public int find(String dict_name)
  {
    int i = Arrays.binarySearch(locales, dict_name);
    return (i < 0) ? -1 : i;
  }

  public int length() { return locales.length; }

  public String dict_name(int i) { return locales[i]; }
  public String display_name(int i) { return names[i]; }
  public int size(int i) { return sizes[i]; }
}
