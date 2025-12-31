package juloo.keyboard2;

import android.content.Context;
import android.os.Build.VERSION;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import java.util.ArrayList;
import java.util.List;

public final class DeviceLocales
{
  public final List<Loc> installed;
  public final Loc default_;

  public static DeviceLocales load(Context ctx)
  {
    InputMethodManager imm =
      (InputMethodManager)ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
    List<Loc> locs = get_installed_locales(ctx.getPackageName(), imm);
    return new DeviceLocales(locs, current_locale(imm, locs));
  }

  /** Extra keys required by all the installed locales. */
  public ExtraKeys extra_keys()
  {
    List<ExtraKeys> extra_keys = new ArrayList<ExtraKeys>();
    for (Loc l : installed)
      extra_keys.add(l.extra_keys);
    return ExtraKeys.merge(extra_keys);
  }

  public static final class Loc
  {
    public final String lang_tag;
    public final String script;
    public final String default_layout; // Might be [null]
    public final ExtraKeys extra_keys;

    public Loc(InputMethodSubtype st)
    {
      lang_tag = st.getLanguageTag();
      script = st.getExtraValueOf("script");
      default_layout = st.getExtraValueOf("default_layout");
      String extra_keys_s = st.getExtraValueOf("extra_keys");
      extra_keys = (extra_keys_s != null) ?
        ExtraKeys.parse(script, extra_keys_s) : ExtraKeys.EMPTY;
    }
  }

  private DeviceLocales(List<Loc> locs, Loc def)
  { installed = locs; default_ = def; }

  private static List<Loc> get_installed_locales(String pkg, InputMethodManager imm)
  {
    List<Loc> locs = new ArrayList<Loc>();
    for (InputMethodInfo imi : imm.getEnabledInputMethodList())
      if (imi.getPackageName().equals(pkg))
      {
        for (InputMethodSubtype subtype :
            imm.getEnabledInputMethodSubtypeList(imi, true))
          locs.add(new Loc(subtype));
        break;
      }
    return locs;
  }

  private static Loc current_locale(InputMethodManager imm, List<Loc> installed)
  {
    // Android might return a random subtype, for example, the first in the
    // list alphabetically.
    InputMethodSubtype current_subtype = imm.getCurrentInputMethodSubtype();
    if (current_subtype == null)
      return null;
    if (VERSION.SDK_INT < 24)
      return new Loc(current_subtype);
    String default_lang_tag = current_subtype.getLanguageTag();
    for (Loc l : installed)
      if (l.lang_tag.equals(default_lang_tag))
        return l;
    return null;
  }
}
