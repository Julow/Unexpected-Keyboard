package juloo.keyboard2;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import androidx.core.content.ContextCompat;

/** Minimal Activity used by the IME service to request microphone permission. */
public final class VoicePermissionActivity extends Activity
{
  private static final int REQUEST_RECORD_AUDIO = 1;

  @Override
  protected void onCreate(Bundle state)
  {
    super.onCreate(state);
    if (Build.VERSION.SDK_INT < 23)
    {
      Toast.makeText(this, R.string.toast_voice_permission_granted,
          Toast.LENGTH_SHORT).show();
      finish();
      return;
    }
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        == PackageManager.PERMISSION_GRANTED)
    {
      Toast.makeText(this, R.string.toast_voice_permission_granted,
          Toast.LENGTH_SHORT).show();
      finish();
      return;
    }
    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},
        REQUEST_RECORD_AUDIO);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions,
      int[] grantResults)
  {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode != REQUEST_RECORD_AUDIO)
      return;

    boolean granted = grantResults.length > 0
        && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    Toast.makeText(this, granted
        ? R.string.toast_voice_permission_granted
        : R.string.toast_voice_permission_denied,
        Toast.LENGTH_SHORT).show();
    finish();
  }
}
