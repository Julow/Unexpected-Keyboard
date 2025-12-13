package juloo.keyboard2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * Foreground service that displays a persistent bottom overlay containing a Keyboard2View.
 *
 * Minimal version: focuses on "always visible". It does not inject input into other apps
 * when no editor is focused.
 */
public final class AlwaysOnOverlayService extends Service
{
  // Actions
  public static final String ACTION_START = "juloo.keyboard2.action.ALWAYS_ON_OVERLAY_START";
  public static final String ACTION_STOP  = "juloo.keyboard2.action.ALWAYS_ON_OVERLAY_STOP";

  // Pref key (device-protected prefs recommended)
  public static final String PREF_ALWAYS_ON_OVERLAY = "always_on_overlay";

  // Notification
  private static final String CHANNEL_ID = "always_on_overlay";
  private static final int NOTIF_ID = 4242;

  private WindowManager _wm;
  private View _overlayView;

  private SharedPreferences _prefs;

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

    // Default is START
    _prefs.edit().putBoolean(PREF_ALWAYS_ON_OVERLAY, true).apply();

    if (!Settings.canDrawOverlays(this))
    {
      // No permission: do not crash; just stop.
      stopSelf();
      return START_NOT_STICKY;
    }

    ensureConfigInitialized();
    startForeground(NOTIF_ID, buildNotification());
    showOverlayIfNeeded();

    return START_STICKY;
  }

  @Override
  public void onDestroy()
  {
    hideOverlay();
    super.onDestroy();
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent)
  {
    return null;
  }

  private void ensureConfigInitialized()
  {
    if (Config.globalConfig() != null)
      return;

    // Overlay mode handler: minimal implementation (no real InputConnection).
    Config.IKeyEventHandler handler = new OverlayKeyEventHandler();

    // Fold state: simplest is false (can be improved later)
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
    catch (Exception ignored)
    {
      // Best-effort cleanup
    }
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
    lp.x = 0;
    lp.y = 0;
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

    // Tap notification -> open SettingsActivity
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

  /**
   * Minimal overlay-mode key handler.
   * First version does nothing (UI-only always-on overlay).
   */
  private static final class OverlayKeyEventHandler implements Config.IKeyEventHandler
  {
    @Override
    public void key_down(KeyValue value, boolean is_swipe) {}

    @Override
    public void key_up(KeyValue value, Pointers.Modifiers mods) {}

    @Override
    public void mods_changed(Pointers.Modifiers mods) {}
  }
}
