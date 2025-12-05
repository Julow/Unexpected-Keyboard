package juloo.keyboard2.dict;

import juloo.keyboard2.R;

public enum SupportedDictionaries
{
  /** Enumeration of the supported dictionaries. */

  FR("fr", R.string.dict_name_fr, 1168721);

  /** Associated information. */

  public final String locale; /** Locale that matches this dictionary. */
  public final int name_resource; /** Display name. */
  public final int size; /** Size in bytes of the dictionary file. */

  SupportedDictionaries(String l, int r, int s)
  { locale = l; name_resource = r; size = s; }

  /** Name used in preferences, URLs and file names. */
  public String internal_name() { return locale; }
}
