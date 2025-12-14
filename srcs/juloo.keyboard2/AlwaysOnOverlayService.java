package juloo.keyboard2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public final class AlwaysOnOverlayService extends Service
{
  public static final String ACTION_START = "juloo.keyboard2.action.ALWAYS_ON_OVERLAY_START";
  public static final String ACTION_STOP  = "juloo.keyboard2.action.ALWAYS_ON_OVERLAY_STOP";
  public static final String PREF_ALWAYS_ON_OVERLAY = "always_on_overlay";

  private static final String CHANNEL_ID = "always_on_overlay";
  private static final int NOTIF_ID = 4242;

  private WindowManager _wm;
  private WindowManager.LayoutParams _lp;

  private android.content.SharedPreferences _prefs;

  // Bridge (default process)
  private Messenger _imeBridge = null;
  private boolean _bridgeBound = false;

  // Panes we may attach to WindowManager (track ALL to prevent leaks)
  private View _keyboardPane = null;          // R.layout.keyboard (Keyboard2View)
  private ViewGroup _clipboardPane = null;    // R.layout.clipboard_pane
  private ViewGroup _emojiPane = null;        // R.layout.emoji_pane (optional)
  private View _currentAttached = null;       // what we *think* is attached now

  private final ServiceConnection _bridgeConn = new ServiceConnection()
  {
    @Override public void onServiceConnected(ComponentName name, IBinder service)
    {
      _imeBridge = new Messenger(service);
      _bridgeBound = true;
    }
    @Override public void onServiceDisconnected(ComponentName name)
    {
      _imeBridge = null;
      _bridgeBound = false;
    }
  };

  public static void start(Context ctx)
  {
    Intent i = new Intent(ctx, AlwaysOnOverlayService.class);
    i.setAction(ACTION_START);
    if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i);
    else ctx.startService(i);
  }

  public static void stop(Context ctx)
  {
    // Hard stop is more reliable on OEM ROMs
    try { ctx.stopService(new Intent(ctx, AlwaysOnOverlayService.class)); }
    catch (Exception ignored) {}
  }

  @Override
  public void onCreate()
  {
    super.onCreate();
    _wm = (WindowManager)getSystemService(WINDOW_SERVICE);
    _prefs = DirectBootAwarePreferences.get_shared_preferences(this);
    bindImeBridge(); // best-effort
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId)
  {
    String action = (intent == null) ? null : intent.getAction();

    // Optional graceful stop path (even though Tile uses stopService)
    if (ACTION_STOP.equals(action))
    {
      _prefs.edit().putBoolean(PREF_ALWAYS_ON_OVERLAY, false).apply();
      cleanupAndStop();
      return START_NOT_STICKY;
    }

    // Do NOT force-enable pref here. Tile is source of truth.
    if (!_prefs.getBoolean(PREF_ALWAYS_ON_OVERLAY, false))
    {
      stopSelf();
      return START_NOT_STICKY;
    }

    if (!Settings.canDrawOverlays(this))
    {
      stopSelf();
      return START_NOT_STICKY;
    }

    // Re-bind every time: 2nd start may have lost binder
    bindImeBridge();

    ensureConfigInitialized();
    startForeground(NOTIF_ID, buildNotification());
    showKeyboardPane();

    return START_STICKY;
  }

  @Override
  public void onDestroy()
  {
    cleanupAndStop(); // idempotent
    super.onDestroy();
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) { return null; }

  private void cleanupAndStop()
  {
    // Remove ALL possible attached views (fix “poisoned clipboard button” leak)
    removeViewQuietly(_currentAttached);
    removeViewQuietly(_keyboardPane);
    removeViewQuietly(_clipboardPane);
    removeViewQuietly(_emojiPane);

    _currentAttached = null;

    try { stopForeground(true); } catch (Exception ignored) {}

    unbindImeBridge();
  }

  private void bindImeBridge()
  {
    if (_bridgeBound) return;
    try
    {
      bindService(new Intent(this, ImeBridgeService.class), _bridgeConn, Context.BIND_AUTO_CREATE);
    }
    catch (Exception ignored)
    {
      _bridgeBound = false;
      _imeBridge = null;
    }
  }

  private void unbindImeBridge()
  {
    if (!_bridgeBound) return;
    try { unbindService(_bridgeConn); } catch (Exception ignored) {}
    _bridgeBound = false;
    _imeBridge = null;
  }

  private void removeViewQuietly(View v)
  {
    if (v == null) return;
    try { _wm.removeViewImmediate(v); } catch (Exception ignored) {}
  }

  private void ensureLayoutParams()
  {
    if (_lp != null) return;

    final int type = (Build.VERSION.SDK_INT >= 26)
        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        : WindowManager.LayoutParams.TYPE_PHONE;

    _lp = new WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        type,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    );
    _lp.gravity = Gravity.BOTTOM;
    _lp.setTitle("AlwaysOnKeyboardOverlay");
  }

  private Context themedContext()
  {
    Config cfg = Config.globalConfig();
    return new ContextThemeWrapper(this, cfg.theme);
  }

  private void attachPane(View v)
  {
    ensureLayoutParams();

    // Defensive: if this view was leaked/left attached, try to remove first
    removeViewQuietly(v);

    // Remove current attached view (also defensive: try removing known panes)
    removeViewQuietly(_currentAttached);

    try
    {
      _wm.addView(v, _lp);
      _currentAttached = v;
      v.requestApplyInsets();
    }
    catch (Exception ignored)
    {
      // If add fails, try to keep state consistent
      _currentAttached = null;
    }
  }

  private void ensureConfigInitialized()
  {
    if (Config.globalConfig() != null)
      return;

    Config.IKeyEventHandler handler = new OverlayKeyEventHandler();
    boolean unfolded = false;
    Config.initGlobalConfig(_prefs, getResources(), handler, unfolded);

    ClipboardHistoryService.on_startup(this, new ClipboardHistoryService.ClipboardPasteCallback() {
      @Override public void paste_from_clipboard_pane(String content)
      {
        sendCommitText(content);
      }
    });
  }

  private void showKeyboardPane()
  {
    if (_keyboardPane == null)
    {
      View v = View.inflate(themedContext(), R.layout.keyboard, null);
      if (v instanceof Keyboard2View)
      {
        ((Keyboard2View)v).setKeyboard(resolveTextLayoutModified(Config.globalConfig()));
      }
      _keyboardPane = v;
    }
    attachPane(_keyboardPane);
  }

  private void showClipboardPane()
  {
    if (_clipboardPane == null)
      _clipboardPane = (ViewGroup)View.inflate(themedContext(), R.layout.clipboard_pane, null);
    attachPane(_clipboardPane);
  }

  private void showEmojiPane()
  {
    if (_emojiPane == null)
      _emojiPane = (ViewGroup)View.inflate(themedContext(), R.layout.emoji_pane, null);
    attachPane(_emojiPane);
  }

  private KeyboardData resolveTextLayoutUnmodified(Config cfg)
  {
    KeyboardData layout = null;
    try
    {
      int idx = cfg.get_current_layout();
      if (cfg.layouts != null && idx >= 0 && idx < cfg.layouts.size())
        layout = cfg.layouts.get(idx);
    }
    catch (Exception ignored) {}

    if (layout == null)
      layout = KeyboardData.load(getResources(), R.xml.latn_qwerty_us);
    return layout;
  }

  private KeyboardData resolveTextLayoutModified(Config cfg)
  {
    try { return LayoutModifier.modify_layout(resolveTextLayoutUnmodified(cfg)); }
    catch (Exception ignored) { return resolveTextLayoutUnmodified(cfg); }
  }

  private void setMainKeyboardLayout(KeyboardData layout)
  {
    if (!(_keyboardPane instanceof Keyboard2View))
      return;
    ((Keyboard2View)_keyboardPane).setKeyboard(layout);
  }

  private Notification buildNotification()
  {
    ensureNotifChannel();
    Intent open = new Intent(this, SettingsActivity.class);
    open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    int piFlags = 0;
    if (Build.VERSION.SDK_INT >= 23)
      piFlags |= android.app.PendingIntent.FLAG_IMMUTABLE;

    android.app.PendingIntent pi =
        android.app.PendingIntent.getActivity(this, 0, open, piFlags);

    return new NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_qs_always_on)
        .setContentTitle("Always-on keyboard")
        .setContentText("Overlay keyboard is running")
        .setContentIntent(pi)
        .setOngoing(true)
        .build();
  }

  private void ensureNotifChannel()
  {
    if (Build.VERSION.SDK_INT < 26) return;
    NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    if (nm == null) return;
    nm.createNotificationChannel(new NotificationChannel(
        CHANNEL_ID, "Always-on keyboard", NotificationManager.IMPORTANCE_LOW));
  }

  private void sendCommitText(String text)
  {
    if (text == null || text.length() == 0) return;
    if (_imeBridge == null) { bindImeBridge(); return; }

    Bundle b = new Bundle();
    b.putString("text", text);
    Message m = Message.obtain(null, ImeBridgeService.MSG_COMMIT_TEXT);
    m.setData(b);
    try { _imeBridge.send(m); } catch (RemoteException ignored) {}
  }

  private void sendKeyDownUp(int keyCode, int metaState)
  {
    if (_imeBridge == null) { bindImeBridge(); return; }

    Bundle b = new Bundle();
    b.putInt("keyCode", keyCode);
    b.putInt("metaState", metaState);
    Message m = Message.obtain(null, ImeBridgeService.MSG_SEND_KEY_DOWN_UP);
    m.setData(b);
    try { _imeBridge.send(m); } catch (RemoteException ignored) {}
  }

  private void sendContextMenu(int id)
  {
    if (_imeBridge == null) { bindImeBridge(); return; }

    Bundle b = new Bundle();
    b.putInt("id", id);
    Message m = Message.obtain(null, ImeBridgeService.MSG_CONTEXT_MENU);
    m.setData(b);
    try { _imeBridge.send(m); } catch (RemoteException ignored) {}
  }

  private static int metaStateFromMods(Pointers.Modifiers mods)
  {
    int ms = 0;
    if (mods == null) return 0;

    if (mods.has(KeyValue.Modifier.SHIFT))
      ms |= KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON;
    if (mods.has(KeyValue.Modifier.CTRL))
      ms |= KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON;
    if (mods.has(KeyValue.Modifier.ALT))
      ms |= KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON;
    if (mods.has(KeyValue.Modifier.META))
      ms |= KeyEvent.META_META_ON | KeyEvent.META_META_LEFT_ON;
    return ms;
  }

  private static boolean hasNonShiftMeta(int metaState)
  {
    int nonShift = KeyEvent.META_CTRL_ON | KeyEvent.META_ALT_ON | KeyEvent.META_META_ON;
    return (metaState & nonShift) != 0;
  }

  private static int keyCodeForChar(char c)
  {
    if (c >= 'a' && c <= 'z') return KeyEvent.KEYCODE_A + (c - 'a');
    if (c >= 'A' && c <= 'Z') return KeyEvent.KEYCODE_A + (c - 'A');
    if (c >= '0' && c <= '9') return KeyEvent.KEYCODE_0 + (c - '0');

    switch (c)
    {
      case ' ': return KeyEvent.KEYCODE_SPACE;
      case '\n': return KeyEvent.KEYCODE_ENTER;
      case '\t': return KeyEvent.KEYCODE_TAB;
      case ',': return KeyEvent.KEYCODE_COMMA;
      case '.': return KeyEvent.KEYCODE_PERIOD;
      default: return 0;
    }
  }

  private final class OverlayKeyEventHandler implements Config.IKeyEventHandler
  {
    @Override public void key_down(KeyValue value, boolean is_swipe) {}
    @Override public void mods_changed(Pointers.Modifiers mods) {}

    @Override
    public void key_up(KeyValue value, Pointers.Modifiers mods)
    {
      if (value == null) return;
      final int meta = metaStateFromMods(mods);

      switch (value.getKind())
      {
        case Char: {
          char c = value.getChar();
          if (hasNonShiftMeta(meta))
          {
            int kc = keyCodeForChar(c);
            if (kc != 0) { sendKeyDownUp(kc, meta); return; }
          }
          sendCommitText(String.valueOf(c));
          return;
        }

        case String:
          sendCommitText(value.getString());
          return;

        case Keyevent:
          sendKeyDownUp(value.getKeyevent(), meta);
          return;

        case Editing:
          handleEditing(value.getEditing());
          return;

        case Event:
          handleEvent(value.getEvent());
          return;

        default:
          return;
      }
    }

    private void handleEvent(KeyValue.Event e)
    {
      switch (e)
      {
        case SWITCH_CLIPBOARD:
          showClipboardPane();
          break;
        case SWITCH_BACK_CLIPBOARD:
          showKeyboardPane();
          break;

        case SWITCH_EMOJI:
          showEmojiPane();
          break;
        case SWITCH_BACK_EMOJI:
          showKeyboardPane();
          break;

        case SWITCH_NUMERIC: {
          Config cfg = Config.globalConfig();
          KeyboardData base = resolveTextLayoutUnmodified(cfg);
          KeyboardData num = LayoutModifier.modify_numpad(
              KeyboardData.load(getResources(), R.xml.numeric), base);
          setMainKeyboardLayout(num);
          showKeyboardPane();
          break;
        }

        case SWITCH_TEXT:
          setMainKeyboardLayout(resolveTextLayoutModified(Config.globalConfig()));
          showKeyboardPane();
          break;

        case SWITCH_GREEKMATH: {
          Config cfg = Config.globalConfig();
          KeyboardData base = resolveTextLayoutUnmodified(cfg);
          KeyboardData gm = LayoutModifier.modify_numpad(
              KeyboardData.load(getResources(), R.xml.greekmath), base);
          setMainKeyboardLayout(gm);
          showKeyboardPane();
          break;
        }

        case CONFIG: {
          Intent i = new Intent(AlwaysOnOverlayService.this, SettingsActivity.class);
          i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          startActivity(i);
          break;
        }

        case CHANGE_METHOD_PICKER: {
          InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
          if (imm != null) imm.showInputMethodPicker();
          break;
        }

        default:
          break;
      }
    }

    private void handleEditing(KeyValue.Editing e)
    {
      switch (e)
      {
        case COPY: sendContextMenu(android.R.id.copy); break;
        case PASTE: sendContextMenu(android.R.id.paste); break;
        case CUT: sendContextMenu(android.R.id.cut); break;
        case SELECT_ALL: sendContextMenu(android.R.id.selectAll); break;
        default: break;
      }
    }
  }
}
