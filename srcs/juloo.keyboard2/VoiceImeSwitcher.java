package juloo.keyboard2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.inputmethodservice.InputMethodService;
import android.os.Build.VERSION;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.ArrayAdapter;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;

class VoiceImeSwitcher
{
  static final String PREF_LAST_USED = "voice_ime_last_used";
  static final String PREF_KNOWN_IMES = "voice_ime_known";

  /** Switch to the voice ime. This might open a chooser popup. Preferences are
      used to store the last selected voice ime and to detect whether the
      chooser popup must be shown. Returns [false] if the detection failed and
      is unlikely to succeed. */
  public static boolean switch_to_voice_ime(InputMethodService ims,
      InputMethodManager imm, SharedPreferences prefs)
  {
    List<IME> imes = get_voice_ime_list(imm);
    String last_used = prefs.getString(PREF_LAST_USED, null);
    String last_known_imes = prefs.getString(PREF_KNOWN_IMES, null);
    IME last_used_ime = get_ime_by_id(imes, last_used);
    if (imes.size() == 0)
      return false;
    if (last_used == null || last_known_imes == null || last_used_ime == null
        || !last_known_imes.equals(serialize_ime_ids(imes)))
      choose_voice_ime_and_update_prefs(ims, prefs, imes);
    else
      switch_input_method(ims, last_used_ime);
    return true;
  }

  public static boolean choose_voice_ime(InputMethodService ims,
      InputMethodManager imm, SharedPreferences prefs)
  {
    List<IME> imes = get_voice_ime_list(imm);
    choose_voice_ime_and_update_prefs(ims, prefs, imes);
    return true;
  }

  /** Show the voice IME chooser popup and switch to the selected IME.
      Preferences are updated so that future calls to [switch_to_voice_ime]
      switch to the newly selected IME. */
  static void choose_voice_ime_and_update_prefs(final InputMethodService ims,
      final SharedPreferences prefs, final List<IME> imes)
  {
    List<String> ime_display_names = get_ime_display_names(ims, imes);
    ArrayAdapter layouts = new ArrayAdapter(ims, android.R.layout.simple_list_item_1, ime_display_names);
    AlertDialog dialog = new AlertDialog.Builder(ims)
      .setAdapter(layouts, new DialogInterface.OnClickListener(){
        public void onClick(DialogInterface _dialog, int which)
        {
          IME selected = imes.get(which);
          prefs.edit()
            .putString(PREF_LAST_USED, selected.get_id())
            .putString(PREF_KNOWN_IMES, serialize_ime_ids(imes))
            .commit();
          switch_input_method(ims, selected);
        }
      })
      .create();
    Utils.show_dialog_on_ime(dialog, ims.getWindow().getWindow().getDecorView().getWindowToken());
  }

  static void switch_input_method(InputMethodService ims, IME ime)
  {
    if (VERSION.SDK_INT < 28)
      ims.switchInputMethod(ime.get_id());
    else
      ims.switchInputMethod(ime.get_id(), ime.subtype);
  }

  static IME get_ime_by_id(List<IME> imes, String id)
  {
    if (id != null)
      for (IME ime : imes)
        if (ime.get_id().equals(id))
          return ime;
    return null;
  }

  static List<String> get_ime_display_names(InputMethodService ims, List<IME> imes)
  {
    List<String> names = new ArrayList<String>();
    for (IME ime : imes)
      names.add(ime.get_display_name(ims));
    return names;
  }

  static List<IME> get_voice_ime_list(InputMethodManager imm)
  {
    List<IME> imes = new ArrayList<IME>();
    for (InputMethodInfo im : imm.getEnabledInputMethodList())
      for (InputMethodSubtype imst : imm.getEnabledInputMethodSubtypeList(im, true))
        if (imst.getMode().equals("voice"))
          imes.add(new IME(im, imst));
    return imes;
  }

  /** The chooser popup is shown whether this string changes. */
  static String serialize_ime_ids(List<IME> imes)
  {
    StringBuilder b = new StringBuilder();
    for (IME ime : imes)
    {
      b.append(ime.get_id());
      b.append(',');
    }
    return b.toString();
  }

  static class IME
  {
    public final InputMethodInfo im;
    public final InputMethodSubtype subtype;

    IME(InputMethodInfo im_, InputMethodSubtype st)
    {
      im = im_;
      subtype = st;
    }

    String get_id() { return im.getId(); }

    /** Localised display name. */
    String get_display_name(Context ctx)
    {
      String subtype_name = "";
      if (VERSION.SDK_INT >= 14)
      {
        subtype_name = subtype.getDisplayName(ctx, im.getPackageName(), null).toString();
        if (!subtype_name.equals(""))
          subtype_name = " - " + subtype_name;
      }
      return im.loadLabel(ctx.getPackageManager()).toString() + subtype_name;
    }
  }
}
