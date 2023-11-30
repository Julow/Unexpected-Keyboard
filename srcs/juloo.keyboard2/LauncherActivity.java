package juloo.keyboard2;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LauncherActivity extends Activity
{
  /** Text is replaced when receiving key events. */
  TextView _tryhere_text;
  EditText _tryhere_area;

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.launcher_activity);
    _tryhere_text = (TextView)findViewById(R.id.launcher_tryhere_text);
    _tryhere_area = (EditText)findViewById(R.id.launcher_tryhere_area);
    if (VERSION.SDK_INT > 28)
      _tryhere_area.addOnUnhandledKeyEventListener(
          this.new Tryhere_OnUnhandledKeyEventListener());

  }

  private static final int REQUEST_CODE_OVERLAY_PERMISSION = 1601;
  private void requestOverlayPermission() {
    // Request the permission using an Intent
    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
    startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
  }

  public void startOverlayService(View _btn) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
      // If not, request the permission
      requestOverlayPermission();
    } else {
      startService(new Intent(this, OverlayService.class));
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    //handle response for overlay permission request
    if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
      // Check if the permission was granted
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
        Toast.makeText(this, "Overlay permissions are granted. You can now launch persistent keyboards.", Toast.LENGTH_SHORT).show();
      } else {
        Toast.makeText(this, "Overlay permissions are required for launching persistent keyboard.", Toast.LENGTH_SHORT).show();
      }
    }
  }

  public void launch_imesettings(View _btn)
  {
    startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
  }

  final class Tryhere_OnUnhandledKeyEventListener implements View.OnUnhandledKeyEventListener
  {
    public boolean onUnhandledKeyEvent(View v, KeyEvent ev)
    {
      // Don't handle the back key
      if (ev.getKeyCode() == KeyEvent.KEYCODE_BACK)
        return false;
      // Key release of modifiers would erase interesting data
      if (KeyEvent.isModifierKey(ev.getKeyCode()))
        return false;
      StringBuilder s = new StringBuilder();
      if (ev.isAltPressed()) s.append("Alt+");
      if (ev.isShiftPressed()) s.append("Shift+");
      if (ev.isCtrlPressed()) s.append("Ctrl+");
      if (ev.isMetaPressed()) s.append("Meta+");
      // s.append(ev.getDisplayLabel());
      String kc = KeyEvent.keyCodeToString(ev.getKeyCode());
      s.append(kc.replaceFirst("^KEYCODE_", ""));
      _tryhere_text.setText(s.toString());
      return true;
    }
  }
}
