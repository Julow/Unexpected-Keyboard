package juloo.keyboard2;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build.VERSION;
import java.util.ArrayList;
import java.util.List;

public final class ClipboardHistoryService
{
  /** Start the service on startup and start listening to clipboard changes. */
  public static void on_startup(Context ctx)
  {
    get_service(ctx);
  }

  /** Start the service if it hasn't been started before. Returns [null] if the
      feature is unsupported. */
  public static ClipboardHistoryService get_service(Context ctx)
  {
    if (VERSION.SDK_INT <= 11)
      return null;
    if (_service == null)
      _service = new ClipboardHistoryService(ctx);
    return _service;
  }

  /** The maximum size limits the amount of user data stored in memory but also
      gives a sense to the user that the history is not persisted and can be
      forgotten as soon as the app stops. */
  public static final int MAX_HISTORY_SIZE = 3;

  static ClipboardHistoryService _service = null;

  ClipboardManager _cm;
  List<String> _history;
  OnClipboardHistoryChange _listener = null;

  ClipboardHistoryService(Context ctx)
  {
    _history = new ArrayList<String>();
    _cm = (ClipboardManager)ctx.getSystemService(Context.CLIPBOARD_SERVICE);
    _cm.addPrimaryClipChangedListener(this.new SystemListener());
  }

  public List<String> get_history() { return _history; }

  /** Add clipboard entries to the history, skipping consecutive duplicates and
      empty strings. */
  public void add_clip(String clip)
  {
    int size = _history.size();
    if (clip.equals("") || (size > 0 && _history.get(size - 1).equals(clip)))
      return;
    if (size >= MAX_HISTORY_SIZE)
      _history.remove(0);
    _history.add(clip);
    if (_listener != null)
      _listener.on_clipboard_history_change(_history);
  }

  public void set_on_clipboard_history_change(OnClipboardHistoryChange l) { _listener = l; }

  public static interface OnClipboardHistoryChange
  {
    public void on_clipboard_history_change(List<String> history);
  }

  final class SystemListener implements ClipboardManager.OnPrimaryClipChangedListener
  {
    public SystemListener() {}

    @Override
    public void onPrimaryClipChanged()
    {
      ClipData clip = _cm.getPrimaryClip();
      if (clip == null)
        return;
      int count = clip.getItemCount();
      for (int i = 0; i < count; i++)
        add_clip(clip.getItemAt(i).getText().toString());
    }
  }
}
