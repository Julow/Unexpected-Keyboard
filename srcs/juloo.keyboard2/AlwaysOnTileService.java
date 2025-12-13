package juloo.keyboard2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * Quick Settings tile (minimum stub version).
 *
 * Toggle always-on overlay mode. If overlay permission is missing, opens the permission screen.
 */
public final class AlwaysOnTileService extends TileService
{
  private static final String PREF_ALWAYS_ON_OVERLAY = "always_on_overlay";

  private SharedPreferences prefs()
  {
    return DirectBootAwarePreferences.get_shared_preferences(this);
  }

  private boolean isEnabled()
  {
    return prefs().getBoolean(PREF_ALWAYS_ON_OVERLAY, false);
  }

  private void setEnabled(boolean enabled)
  {
    prefs().edit().putBoolean(PREF_ALWAYS_ON_OVERLAY, enabled).apply();
  }

  @Override
  public void onStartListening()
  {
    super.onStartListening();
    updateTileState();
  }

  @Override
  public void onClick()
  {
    super.onClick();

    boolean newState = !isEnabled();

    if (newState)
    {
      // Need overlay permission for the next step (overlay service).
      if (!Settings.canDrawOverlays(this))
      {
        Intent i = new Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + getPackageName())
        );
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (Build.VERSION.SDK_INT >= 24)
          startActivityAndCollapse(i);
        else
          startActivity(i);

        // Do not enable if permission is missing
        setEnabled(false);
        updateTileState();
        return;
      }

      setEnabled(true);
      AlwaysOnOverlayService.start(this);
    }
    else
    {
      setEnabled(false);
      AlwaysOnOverlayService.stop(this);
    }

    updateTileState();
  }

  private void updateTileState()
  {
    Tile t = getQsTile();
    if (t == null)
      return;
    t.setState(isEnabled() ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
    t.updateTile();
  }
}
