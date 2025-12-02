package juloo.keyboard2.dict;

import juloo.keyboard2.R;

public enum SupportedDictionaries
{
  /** Enumeration of the supported dictionaries. */

  FR("fr", R.string.dict_name_fr);

  /** Associated information. */

  public final String locale; /** Locale that matches this dictionary. */
  public final int name_resource; /** Display name. */

  SupportedDictionaries(String l, int r)
  { locale = l; name_resource = r; }

  /** Name used in preferences, URLs and file names. */
  public String internal_name() { return locale; }
}
