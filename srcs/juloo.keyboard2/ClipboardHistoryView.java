package juloo.keyboard2;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClipboardHistoryView extends ListView
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
      _history = _service.get_history();
    }
    setAdapter(_adapter);
  }

  /** The history entry at index [pos] is removed from the history and added to
      the list of pinned clipboards. */
  public void pin_entry(int pos)
  {
    ClipboardPinView v = (ClipboardPinView)((ViewGroup)getParent().getParent()).findViewById(R.id.clipboard_pin_view);
    String clip = _history.get(pos);
    v.add_entry(clip);
    _service.remove_history_entry(clip);
  }

  @Override
  public void on_clipboard_history_change(List<String> history)
  {
    _history = history;
    _adapter.notifyDataSetChanged();
    invalidate();
  }

  @Override
  protected void onAttachedToWindow()
  {
    super.onAttachedToWindow();
    _adapter.notifyDataSetChanged();
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
      return v;
    }
  }
}
