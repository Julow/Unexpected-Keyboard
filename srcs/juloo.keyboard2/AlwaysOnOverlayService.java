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
import android.view.View;
import android.view.WindowManager;
import android.view.KeyEvent;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * Foreground service that displays a persistent bottom overlay containing a Keyboard2View.
 *
 * This version forwards key events to the active IME via ImeBridgeService so that
 * typing works in focused editors.
 */
public final class AlwaysOnOverlayService extends Service
{
  public static final String ACTION_START = "juloo.keyboard2.action.ALWAYS_ON_OVERLAY_START";
  public static final String ACTION_STOP  = "juloo.keyboard2.action.ALWAYS_ON_OVERLAY_STOP";

  public static final String PREF_ALWAYS_ON_OVERLAY = "always_on_overlay";

  private static final String CHANNEL_ID = "always_on_overlay";
  private static final int NOTIF_ID = 4242;

  private WindowManager _wm;
  private View _overlayView;

  private android.content.SharedPreferences _prefs;

  // Bridge to IME process (default app process)
  private Messenger _imeBridge = null;
  private boolean _bridgeBound = false;

  private final ServiceConnection _bridgeConn = new ServiceConnection()
  {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service)
    {
      _imeBridge = new Messenger(service);
      _bridgeBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name)
    {
      _imeBridge = null;
      _bridgeBound = false;
    }
  };

  public static void start(Context ctx)
  {
    Intent i = new Intent(ctx, AlwaysOnOverlayService.class);
    i.setAction(ACTION_START);
    if (Build.VERSION.SDK_INT >= 26)
      ctx.startForegroundService(i);
    else
      ctx.startService(i);
  }

  public static void stop(Context ctx)
  {
    Intent i = new Intent(ctx, AlwaysOnOverlayService.class);
    i.setAction(ACTION_STOP);
    ctx.startService(i);
  }

  @Override
  public void onCreate()
  {
    super.onCreate();
    _wm = (WindowManager)getSystemService(WINDOW_SERVICE);
    _prefs = DirectBootAwarePreferences.get_shared_preferences(this);
    bindImeBridge();
  }

  @Override
  public void onDestroy()
  {
    hideOverlay();
    unbindImeBridge();
    super.onDestroy();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId)
  {
    String action = (intent == null) ? null : intent.getAction();

    if (ACTION_STOP.equals(action))
    {
      _prefs.edit().putBoolean(PREF_ALWAYS_ON_OVERLAY, false).apply();
      hideOverlay();
      stopForeground(true);
      stopSelf();
      return START_NOT_STICKY;
    }

    _prefs.edit().putBoolean(PREF_ALWAYS_ON_OVERLAY, true).apply();

    if (!Settings.canDrawOverlays(this))
    {
      stopSelf();
      return START_NOT_STICKY;
    }

    ensureConfigInitialized();
    startForeground(NOTIF_ID, buildNotification());
    showOverlayIfNeeded();

    return START_STICKY;
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent)
  {
    return null;
  }

  private void bindImeBridge()
  {
    if (_bridgeBound)
      return;
    Intent i = new Intent(this, ImeBridgeService.class);
    try
    {
      bindService(i, _bridgeConn, Context.BIND_AUTO_CREATE);
    }
    catch (Exception ignored)
    {
      // Best-effort; without bridge, keys won't reach editors.
      _bridgeBound = false;
      _imeBridge = null;
    }
  }

  private void unbindImeBridge()
  {
    if (!_bridgeBound)
      return;
    try
    {
      unbindService(_bridgeConn);
    }
    catch (Exception ignored) {}
    _bridgeBound = false;
    _imeBridge = null;
  }

  private void ensureConfigInitialized()
  {
    if (Config.globalConfig() != null)
      return;

    // Use a handler that forwards to ImeBridgeService
    Config.IKeyEventHandler handler = new OverlayKeyEventHandler();

    boolean unfolded = false;
    Config.initGlobalConfig(_prefs, getResources(), handler, unfolded);
  }

  private void showOverlayIfNeeded()
  {
    if (_overlayView != null)
      return;

    Config cfg = Config.globalConfig();
    Context themed = new ContextThemeWrapper(this, cfg.theme);
    View v = View.inflate(themed, R.layout.keyboard, null);

    if (v instanceof Keyboard2View)
    {
      Keyboard2View kv = (Keyboard2View)v;
      kv.setKeyboard(resolveDefaultLayout(cfg));
    }

    WindowManager.LayoutParams lp = buildLayoutParams();
    _wm.addView(v, lp);
    _overlayView = v;

    _overlayView.requestApplyInsets();
  }

  private void hideOverlay()
  {
    if (_overlayView == null)
      return;
    try
    {
      _wm.removeViewImmediate(_overlayView);
    }
    catch (Exception ignored) {}
    _overlayView = null;
  }

  private WindowManager.LayoutParams buildLayoutParams()
  {
    final int type;
    if (Build.VERSION.SDK_INT >= 26)
      type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
    else
      type = WindowManager.LayoutParams.TYPE_PHONE;

    WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        type,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    );

    lp.gravity = Gravity.BOTTOM;
    lp.setTitle("AlwaysOnKeyboardOverlay");
    return lp;
  }

  private KeyboardData resolveDefaultLayout(Config cfg)
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

    try
    {
      return LayoutModifier.modify_layout(layout);
    }
    catch (Exception ignored)
    {
      return layout;
    }
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
    if (Build.VERSION.SDK_INT < 26)
      return;

    NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    if (nm == null)
      return;

    NotificationChannel ch = new NotificationChannel(
        CHANNEL_ID,
        "Always-on keyboard",
        NotificationManager.IMPORTANCE_LOW
    );
    nm.createNotificationChannel(ch);
  }

  private void sendCommitText(String text)
  {
    if (text == null || text.length() == 0)
      return;
    if (_imeBridge == null)
      return;

    Bundle b = new Bundle();
    b.putString("text", text);

    Message m = Message.obtain(null, ImeBridgeService.MSG_COMMIT_TEXT);
    m.setData(b);
    try
    {
      _imeBridge.send(m);
    }
    catch (RemoteException ignored) {}
  }

  private void sendKeyDownUp(int keyCode, int metaState)
  {
    if (_imeBridge == null)
      return;

    Bundle b = new Bundle();
    b.putInt("keyCode", keyCode);
    b.putInt("metaState", metaState);

    Message m = Message.obtain(null, ImeBridgeService.MSG_SEND_KEY_DOWN_UP);
    m.setData(b);
    try
    {
      _imeBridge.send(m);
    }
    catch (RemoteException ignored) {}
  }

  private void sendContextMenu(int id)
  {
    if (_imeBridge == null)
      return;

    Bundle b = new Bundle();
    b.putInt("id", id);

    Message m = Message.obtain(null, ImeBridgeService.MSG_CONTEXT_MENU);
    m.setData(b);
    try
    {
      _imeBridge.send(m);
    }
    catch (RemoteException ignored) {}
  }

  private final class OverlayKeyEventHandler implements Config.IKeyEventHandler
  {
    @Override
    public void key_down(KeyValue value, boolean is_swipe)
    {
      // no-op
    }

    @Override
    public void mods_changed(Pointers.Modifiers mods)
    {
      // We keep it simple for now: do not forward modifier key-down/up events.
      // Most normal typing is already resolved by KeyModifier before key_up().
    }

    @Override
    public void key_up(KeyValue value, Pointers.Modifiers mods)
    {
      if (value == null)
        return;

      switch (value.getKind())
      {
        case Char:
          sendCommitText(String.valueOf(value.getChar()));
          return;

        case String:
          sendCommitText(value.getString());
          return;

        case Keyevent:
          sendKeyDownUp(value.getKeyevent(), 0);
          return;

        case Editing:
          handleEditing(value.getEditing());
          return;

        case Slider:
          handleSlider(value.getSlider(), value.getSliderRepeat());
          return;

        case Event:
          if (value.getEvent() == KeyValue.Event.CONFIG)
          {
            Intent i = new Intent(AlwaysOnOverlayService.this, SettingsActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
          }
          return;

        default:
          // Ignore Macro/Compose/Modifiers/etc in this minimal bridge
          return;
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

        case PASTE_PLAIN: sendContextMenu(android.R.id.pasteAsPlainText); break;
        case UNDO: sendContextMenu(android.R.id.undo); break;
        case REDO: sendContextMenu(android.R.id.redo); break;
        case REPLACE: sendContextMenu(android.R.id.replaceText); break;
        case SHARE: sendContextMenu(android.R.id.shareText); break;
        case ASSIST: sendContextMenu(android.R.id.textAssist); break;
        case AUTOFILL: sendContextMenu(android.R.id.autofill); break;

        case DELETE_WORD:
          sendKeyDownUp(KeyEvent.KEYCODE_DEL,
              KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON);
          break;

        case FORWARD_DELETE_WORD:
          sendKeyDownUp(KeyEvent.KEYCODE_FORWARD_DEL,
              KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON);
          break;

        case SELECTION_CANCEL:
          sendKeyDownUp(KeyEvent.KEYCODE_ESCAPE, 0);
          break;
      }
    }

    private void handleSlider(KeyValue.Slider s, int r)
    {
      int repeat = r;
      if (repeat == 0)
        return;

      switch (s)
      {
        case Cursor_left:
          if (repeat > 0) repeatKey(KeyEvent.KEYCODE_DPAD_LEFT, repeat);
          else repeatKey(KeyEvent.KEYCODE_DPAD_RIGHT, -repeat);
          break;

        case Cursor_right:
          if (repeat > 0) repeatKey(KeyEvent.KEYCODE_DPAD_RIGHT, repeat);
          else repeatKey(KeyEvent.KEYCODE_DPAD_LEFT, -repeat);
          break;

        case Cursor_up:
          if (repeat > 0) repeatKey(KeyEvent.KEYCODE_DPAD_UP, repeat);
          else repeatKey(KeyEvent.KEYCODE_DPAD_DOWN, -repeat);
          break;

        case Cursor_down:
          if (repeat > 0) repeatKey(KeyEvent.KEYCODE_DPAD_DOWN, repeat);
          else repeatKey(KeyEvent.KEYCODE_DPAD_UP, -repeat);
          break;

        case Selection_cursor_left:
        case Selection_cursor_right:
          // Keep minimal: ignore selection sliders for now
          break;
      }
    }

    private void repeatKey(int keyCode, int n)
    {
      while (n-- > 0)
        sendKeyDownUp(keyCode, 0);
    }
  }
}
