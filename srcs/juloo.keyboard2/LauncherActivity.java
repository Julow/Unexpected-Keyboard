package juloo.keyboard2;

import android.app.Activity;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

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

  public void launch_imesettings(View _btn)
  {
    startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
  }

  final class Tryhere_OnUnhandledKeyEventListener implements View.OnUnhandledKeyEventListener
  {
    public boolean onUnhandledKeyEvent(View v, KeyEvent ev)
    {
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
