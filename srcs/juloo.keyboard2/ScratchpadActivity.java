package juloo.keyboard2;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ScratchpadActivity extends Activity
{

  private EditText mInput;
  private static final String PREFS_NAME = "scratchpad";
  private static final String KEY_CONTENT = "content";

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.scratchpad);

    mInput = findViewById(R.id.scratchpad_input);
    Button btnClose = findViewById(R.id.btn_close);
    Button btnCopy = findViewById(R.id.btn_copy);

    // Apply theme
    int labelColor = 0xFF000000;
    int bgColor = 0xFFFFFFFF;
    int keyColor = 0xFFCCCCCC;

    Config config = Config.globalConfig();
    if (config != null)
    {
      android.view.ContextThemeWrapper wrapper = new android.view.ContextThemeWrapper(this, config.theme);
      android.content.res.TypedArray ta = wrapper.obtainStyledAttributes(new int[] {
          R.attr.colorLabel,
          R.attr.colorKeyboard,
          R.attr.colorKey
      });
      labelColor = ta.getColor(0, labelColor);
      bgColor = ta.getColor(1, bgColor);
      keyColor = ta.getColor(2, keyColor);
      ta.recycle();
    }

    View root = findViewById(R.id.scratchpad_root);
    if (root != null)
    {
      root.setBackgroundColor(bgColor);
    }

    mInput.setTextColor(labelColor);
    mInput.setHintTextColor((labelColor & 0x00FFFFFF) | 0x80000000);
    // Give input a slight background to distinguish it
    // Use key color for consistency
    mInput.setBackgroundResource(R.drawable.rect_rounded);
    // We need to apply the tint to the background drawable
    mInput.setBackgroundTintList(android.content.res.ColorStateList.valueOf(keyColor));

    TextView title = findViewById(R.id.scratchpad_title);
    if (title != null)
    {
      title.setTextColor(labelColor);
    }

    btnClose.setTextColor(labelColor);
    btnClose.setBackgroundTintList(android.content.res.ColorStateList.valueOf(keyColor));

    btnCopy.setTextColor(labelColor);
    btnCopy.setBackgroundTintList(android.content.res.ColorStateList.valueOf(keyColor));

    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    String content = prefs.getString(KEY_CONTENT, "");
    mInput.setText(content);
    if (content.length() > 0)
    {
      mInput.setSelection(content.length());
    }

    btnClose.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        finish();
      }
    });

    btnCopy.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        String text = mInput.getText().toString();
        if (!text.isEmpty())
        {
          ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
          ClipData clip = ClipData.newPlainText("Scratchpad", text);
          clipboard.setPrimaryClip(clip);
          Toast.makeText(ScratchpadActivity.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
          finish();
        }
      }
    });
  }

  @Override
  protected void onPause()
  {
    super.onPause();
    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    prefs.edit().putString(KEY_CONTENT, mInput.getText().toString()).apply();
  }
}
