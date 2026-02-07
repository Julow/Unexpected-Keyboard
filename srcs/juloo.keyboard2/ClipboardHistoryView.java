package juloo.keyboard2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClipboardHistoryView extends NonScrollListView
  implements ClipboardHistoryService.OnClipboardHistoryChange
{
  List<String> _history;
  ClipboardHistoryService _service;
  ClipboardEntriesAdapter _adapter;

  public ClipboardHistoryView(Context ctx, AttributeSet attrs)
  {
    super(ctx, attrs);
    _history = Collections.EMPTY_LIST;
    _adapter = this.new ClipboardEntriesAdapter();
    _service = ClipboardHistoryService.get_service(ctx);
    if (_service != null)
    {
      _service.set_on_clipboard_history_change(this);
      _history = _service.clear_expired_and_get_history();
    }
    setAdapter(_adapter);
  }

  /** The history entry at index [pos] is removed from the history and added to
      the list of pinned clipboards. */
  public void pin_entry(int pos)
  {
    String clip = _history.get(pos);
    android.content.Intent intent = new android.content.Intent(getContext(), SnippetMoveActivity.class);
    intent.putExtra(SnippetMoveActivity.EXTRA_CONTENT, clip);
    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
    getContext().startActivity(intent);

    // Optional: remove from history immediately? Or wait?
    // User request implies "Pinning recently copied text". Usually moving it to
    // snippets implies removing from history
    // or arguably keeping it. The original code removed it.
    // However, if the user cancels the MoveActivity, we lose the item if we remove
    // it here.
    // Better to remove it ONLY if saved. But we don't get a callback easily.
    // Let's keep it in history for safety, or assume the user will complete the
    // action.
    // Original code: _service.remove_history_entry(clip);
    // I will comment it out for safety, as we are now launching an async activity.
    // _service.remove_history_entry(clip);
  }

  /** Send the specified entry to the editor. */
  public void paste_entry(int pos)
  {
    ClipboardHistoryService.paste(_history.get(pos));
  }

  @Override
  public void on_clipboard_history_change()
  {
    update_data();
  }

  @Override
  protected void onWindowVisibilityChanged(int visibility)
  {
    if (visibility == View.VISIBLE)
      update_data();
  }

  void update_data()
  {
    _history = _service.clear_expired_and_get_history();
    _adapter.notifyDataSetChanged();
    invalidate();
  }

  class ClipboardEntriesAdapter extends BaseAdapter
  {
    public ClipboardEntriesAdapter() {}

    @Override
    public int getCount() { return _history.size(); }
    @Override
    public Object getItem(int pos) { return _history.get(pos); }
    @Override
    public long getItemId(int pos) { return _history.get(pos).hashCode(); }

    @Override
    public View getView(final int pos, View v, ViewGroup _parent)
    {
      if (v == null)
        v = View.inflate(getContext(), R.layout.clipboard_history_entry, null);
      ((TextView)v.findViewById(R.id.clipboard_entry_text))
        .setText(_history.get(pos));
      v.findViewById(R.id.clipboard_entry_addpin).setOnClickListener(
          new View.OnClickListener()
          {
            @Override
            public void onClick(View v) { pin_entry(pos); }
          });
      v.findViewById(R.id.clipboard_entry_paste).setOnClickListener(
          new View.OnClickListener()
          {
            @Override
            public void onClick(View v) { paste_entry(pos); }
          });
      // v.findViewById(R.id.clipboard_entry_removehist).setOnClickListener(
      //     new View.OnClickListener()
      //     {
      //       @Override
      //       public void onClick(View v)
      //       {
      //         AlertDialog d = new AlertDialog.Builder(getContext())
      //           .setTitle(R.string.clipboard_remove_confirm)
      //           .setPositiveButton(R.string.clipboard_remove_confirmed,
      //               new DialogInterface.OnClickListener(){
      //                 public void onClick(DialogInterface _dialog, int _which)
      //                 {
      //                   _service.remove_history_entry(_history.get(pos));
      //                 }
      //               })
      //           .setNegativeButton(android.R.string.cancel, null)
      //           .create();
      //         Utils.show_dialog_on_ime(d, v.getWindowToken());
      //       }
      //     });
      return v;
    }
  }
}
