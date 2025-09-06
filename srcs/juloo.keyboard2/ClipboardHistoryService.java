package juloo.keyboard2;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build.VERSION;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ClipboardHistoryService
{
  /** Start the service on startup and start listening to clipboard changes. */
  public static void on_startup(Context ctx, ClipboardPasteCallback cb)
  {
    get_service(ctx);
    _paste_callback = cb;
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

  public static void set_history_enabled(boolean e)
  {
    Config.globalConfig().set_clipboard_history_enabled(e);
    if (_service == null)
      return;
    if (e)
      _service.add_current_clip();
    else
      _service.clear_history();
  }

  /** Send the given string to the editor. */
  public static void paste(String clip)
  {
    if (_paste_callback != null)
      _paste_callback.paste_from_clipboard_pane(clip);
  }

  /** The maximum size limits the amount of user data stored in memory but also
      gives a sense to the user that the history is not persisted and can be
      forgotten as soon as the app stops. */
  public static final int MAX_HISTORY_SIZE = 6;
  /** Time in ms until history entries expire. */
  public static final long HISTORY_TTL_MS = 5 * 60 * 1000;

  static ClipboardHistoryService _service = null;
  static ClipboardPasteCallback _paste_callback = null;

  ClipboardManager _cm;
  List<HistoryEntry> _history;
  OnClipboardHistoryChange _listener = null;

  ClipboardHistoryService(Context ctx)
  {
    _history = new ArrayList<HistoryEntry>();
    _cm = (ClipboardManager)ctx.getSystemService(Context.CLIPBOARD_SERVICE);
    _cm.addPrimaryClipChangedListener(this.new SystemListener());
  }

  public List<String> clear_expired_and_get_history()
  {
    long now_ms = System.currentTimeMillis();
    List<String> dst = new ArrayList<String>();
    Iterator<HistoryEntry> it = _history.iterator();
    while (it.hasNext())
    {
      HistoryEntry ent = it.next();
      if (ent.expiry_timestamp <= now_ms)
        it.remove();
      else
        dst.add(ent.content);
    }
    return dst;
  }

  /** This will call [on_clipboard_history_change]. */
  public void remove_history_entry(String clip)
  {
    int last_pos = _history.size() - 1;
    for (int pos = last_pos; pos >= 0; pos--)
    {
      if (!_history.get(pos).content.equals(clip))
        continue;
      // Removing the current clipboard, clear the system clipboard.
      if (pos == last_pos)
      {
        if (VERSION.SDK_INT >= 28)
          _cm.clearPrimaryClip();
        else
          _cm.setText("");
      }
      _history.remove(pos);
      if (_listener != null)
        _listener.on_clipboard_history_change();
    }
  }

  /** Add clipboard entries to the history, skipping consecutive duplicates and
      empty strings. */
  public void add_clip(String clip)
  {
    if (!Config.globalConfig().clipboard_history_enabled)
      return;
    int size = _history.size();
    if (clip.equals("") || (size > 0 && _history.get(size - 1).content.equals(clip)))
      return;
    if (size >= MAX_HISTORY_SIZE)
      _history.remove(0);
    _history.add(new HistoryEntry(clip));
    if (_listener != null)
      _listener.on_clipboard_history_change();
  }

  public void clear_history()
  {
    _history.clear();
    if (_listener != null)
      _listener.on_clipboard_history_change();
  }

  public void set_on_clipboard_history_change(OnClipboardHistoryChange l) { _listener = l; }

  public static interface OnClipboardHistoryChange
  {
    public void on_clipboard_history_change();
  }

  /** Add what is currently in the system clipboard into the history. */
  void add_current_clip()
  {
    ClipData clip = _cm.getPrimaryClip();
    if (clip == null)
      return;
    int count = clip.getItemCount();
    for (int i = 0; i < count; i++)
    {
      CharSequence text = clip.getItemAt(i).getText();
      if (text != null)
        add_clip(text.toString());
    }
  }

  final class SystemListener implements ClipboardManager.OnPrimaryClipChangedListener
  {
    public SystemListener() {}

    @Override
    public void onPrimaryClipChanged()
    {
      add_current_clip();
    }
  }

  static final class HistoryEntry
  {
    public final String content;

    /** Time at which the entry expires. */
    public final long expiry_timestamp;

    public HistoryEntry(String c)
    {
      content = c;
      expiry_timestamp = System.currentTimeMillis() + HISTORY_TTL_MS;
    }
  }

  public interface ClipboardPasteCallback
  {
    public void paste_from_clipboard_pane(String content);
  }
}
