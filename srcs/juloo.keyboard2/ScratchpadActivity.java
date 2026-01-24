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

public class ScratchpadActivity extends Activity {

    private EditText mInput;
    private static final String PREFS_NAME = "scratchpad";
    private static final String KEY_CONTENT = "content";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scratchpad);

        mInput = findViewById(R.id.scratchpad_input);
        Button btnClose = findViewById(R.id.btn_close);
        Button btnCopy = findViewById(R.id.btn_copy);

        // Apply theme
        Config config = Config.globalConfig();
        if (config != null) {
            Theme theme = new Theme(this, null); // Re-instantiating Theme might be heavy, but allows accessing colors
                                                 // if not static.
            // Actually Config has a 'theme' int ID, but Theme class extracts attributes
            // from context's theme.
            // We need to construct a Theme object using the context which has the correct
            // style applied.
            // But Activity uses Theme.Dialog. We can try to use the raw colors if we can
            // get them.
            // Let's use the colors from the keyboard view if possible, or construct a
            // Theme.
            // The issue is 'Theme' constructor reads from attributes.
            // Let's rely on manually setting colors if we can get them from a static place
            // or Config.
            // Wait, Theme is instantiated in Keyboard2View. We don't have access to that
            // instance here.

            // Allow manual applying of colors if we can simply create a Theme object with
            // the correct style.
            // Config.theme holds the R.style ref.
            getTheme().applyStyle(config.theme, true);
            Theme t = new Theme(this, null);

            findViewById(android.R.id.content).setBackgroundColor(t.colorNavBar); // Use navbar color for dialog bg? Or
                                                                                  // key color.
            View root = findViewById(R.id.scratchpad_root);
            if (root != null)
                root.setBackgroundColor(t.colorKey); // Use key color for background

            mInput.setTextColor(t.labelColor);
            mInput.setHintTextColor(t.greyedLabelColor);
            // Give input a slight background to distinguish it
            // Use greyedLabelColor with very low alpha for background
            int bgColor = t.greyedLabelColor & 0x00FFFFFF | 0x20000000;
            mInput.setBackgroundColor(bgColor);

            TextView title = findViewById(R.id.scratchpad_title);
            if (title != null)
                title.setTextColor(t.labelColor);

            btnClose.setTextColor(t.labelColor);
            btnCopy.setTextColor(t.labelColor);
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String content = prefs.getString(KEY_CONTENT, "");
        mInput.setText(content);
        if (content.length() > 0) {
            mInput.setSelection(content.length());
        }

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = mInput.getText().toString();
                if (!text.isEmpty()) {
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
    protected void onPause() {
        super.onPause();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(KEY_CONTENT, mInput.getText().toString()).apply();
    }
}
