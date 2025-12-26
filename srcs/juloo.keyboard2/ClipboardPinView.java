package juloo.keyboard2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;

public final class ClipboardPinView extends NonScrollListView
{
  /** Preference file name that store pinned clipboards. */
  static final String PERSIST_FILE_NAME = "clipboards";
  /** Preference name for pinned clipboards. */
  static final String PERSIST_PREF = "pinned";

  List<String> _entries;
  ClipboardPinEntriesAdapter _adapter;
  SharedPreferences _persist_store;

  public ClipboardPinView(Context ctx, AttributeSet attrs)
  {
    super(ctx, attrs);
    _entries = new ArrayList<String>();
    // Storage is not be available in direct-boot mode.
    _persist_store = null;
    try
    {
      _persist_store =
        ctx.getSharedPreferences("pinned_clipboards", Context.MODE_PRIVATE);
      load_from_prefs(_persist_store, _entries);
    }
    catch (Exception _e) {}
    _adapter = this.new ClipboardPinEntriesAdapter();
    setAdapter(_adapter);
  }

  /** Pin a clipboard and persist the change. */
  public void add_entry(String text)
  {
    _entries.add(text);
    _adapter.notifyDataSetChanged();
    persist();
    invalidate();
  }

  /** Remove the entry at index [pos] and persist the change. */
  public void remove_entry(int pos)
  {
    if (pos < 0 || pos >= _entries.size())
      return;
    _entries.remove(pos);
    _adapter.notifyDataSetChanged();
    persist();
    invalidate();
  }

  /** Send the specified entry to the editor. */
  public void paste_entry(int pos)
  {
    ClipboardHistoryService.paste(_entries.get(pos));
  }

  static void load_from_prefs(SharedPreferences store, List<String> dst)
  {
    String arr_s = store.getString(PERSIST_PREF, null);
    if (arr_s == null)
      return;
    try
    {
      JSONArray arr = new JSONArray(arr_s);
      for (int i = 0; i < arr.length(); i++)
        dst.add(arr.getString(i));
    }
    catch (JSONException _e) {}
  }

  void persist()
  {
    if (_persist_store == null)
      return;
    JSONArray arr = new JSONArray();
    for (int i = 0; i < _entries.size(); i++)
      arr.put(_entries.get(i));
    _persist_store.edit()
      .putString(PERSIST_PREF, arr.toString())
      .apply();
  }

  class ClipboardPinEntriesAdapter extends BaseAdapter
  {
    public ClipboardPinEntriesAdapter() {}

    @Override
    public int getCount() { return _entries.size(); }
    @Override
    public Object getItem(int pos) { return _entries.get(pos); }
    @Override
    public long getItemId(int pos) { return _entries.get(pos).hashCode(); }

    @Override
    public View getView(final int pos, View v, ViewGroup _parent)
    {
      if (v == null)
        v = View.inflate(getContext(), R.layout.clipboard_pin_entry, null);
      ((TextView)v.findViewById(R.id.clipboard_pin_text))
        .setText(_entries.get(pos));
      v.findViewById(R.id.clipboard_pin_paste).setOnClickListener(
          new View.OnClickListener()
          {
            @Override
            public void onClick(View v) { paste_entry(pos); }
          });
      v.findViewById(R.id.clipboard_pin_remove).setOnClickListener(
          new View.OnClickListener()
          {
            @Override
            public void onClick(View v)
            {
              AlertDialog d = new AlertDialog.Builder(getContext())
                .setTitle(R.string.clipboard_remove_confirm)
                .setPositiveButton(R.string.clipboard_remove_confirmed,
                    new DialogInterface.OnClickListener(){
                      public void onClick(DialogInterface _dialog, int _which)
                      {
                        remove_entry(pos);
                      }
                    })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
              Utils.show_dialog_on_ime(d, v.getWindowToken());
            }
          });
      return v;
    }
  }
}
