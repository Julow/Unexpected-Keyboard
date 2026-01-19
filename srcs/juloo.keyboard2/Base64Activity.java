package juloo.keyboard2;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.widget.Toast;

public class Base64Activity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Allow automatic keyboard opening
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding / 2, padding, padding / 2);

        int labelColor = 0xFF000000;
        int bgColor = 0xFFFFFFFF;
        int keyColor = 0xFFCCCCCC;

        Config config = Config.globalConfig();
        if (config != null) {
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

        layout.setBackgroundColor(bgColor);

        // Input Label
        TextView inputLabel = new TextView(this);
        inputLabel.setText("Input:");
        inputLabel.setTextColor(labelColor);
        layout.addView(inputLabel);

        // Input Field
        final EditText inputParams = new EditText(this);
        inputParams.setHint("Enter text here...");
        inputParams.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        inputParams.setMinLines(3);
        inputParams.setMaxLines(5);
        inputParams.setGravity(Gravity.TOP | Gravity.START);
        inputParams.setTextColor(labelColor);
        inputParams.setHintTextColor((labelColor & 0x00FFFFFF) | 0x80000000); // 50% opacity
        inputParams.setBackgroundResource(R.drawable.rect_rounded);
        inputParams.setBackgroundTintList(android.content.res.ColorStateList.valueOf(keyColor));
        layout.addView(inputParams);

        // Action Buttons (Encode/Decode)
        LinearLayout actionButtons = new LinearLayout(this);
        actionButtons.setOrientation(LinearLayout.HORIZONTAL);
        actionButtons.setWeightSum(2);

        Button encodeBtn = new Button(this);
        encodeBtn.setText("Encode");
        LinearLayout.LayoutParams encodeParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        encodeBtn.setLayoutParams(encodeParams);
        encodeBtn.setTextColor(labelColor);
        encodeBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(keyColor));

        Button decodeBtn = new Button(this);
        decodeBtn.setText("Decode");
        LinearLayout.LayoutParams decodeParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        decodeBtn.setLayoutParams(decodeParams);
        decodeBtn.setTextColor(labelColor);
        decodeBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(keyColor));

        actionButtons.addView(encodeBtn);
        actionButtons.addView(decodeBtn);
        layout.addView(actionButtons);

        // Output Label
        TextView outputLabel = new TextView(this);
        outputLabel.setText("Output:");
        outputLabel.setPadding(0, padding / 2, 0, 0);
        outputLabel.setTextColor(labelColor);
        layout.addView(outputLabel);

        // Output Field (Read Only)
        final EditText outputField = new EditText(this);
        outputField.setHint("Result will appear here...");
        outputField.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        outputField.setMinLines(3);
        outputField.setMaxLines(5);
        outputField.setGravity(Gravity.TOP | Gravity.START);
        outputField.setKeyListener(null); // Read only
        outputField.setTextColor(labelColor);
        outputField.setHintTextColor((labelColor & 0x00FFFFFF) | 0x80000000);
        outputField.setBackgroundResource(R.drawable.rect_rounded);
        outputField.setBackgroundTintList(android.content.res.ColorStateList.valueOf(keyColor));
        layout.addView(outputField);

        // Result Buttons (Copy/Insert)
        LinearLayout resultButtons = new LinearLayout(this);
        resultButtons.setOrientation(LinearLayout.HORIZONTAL);
        resultButtons.setWeightSum(2);

        Button copyBtn = new Button(this);
        copyBtn.setText("Copy");
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT,
                1);
        copyBtn.setLayoutParams(copyParams);
        copyBtn.setTextColor(labelColor);
        copyBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(keyColor));

        Button insertBtn = new Button(this);
        insertBtn.setText("Insert");
        LinearLayout.LayoutParams insertParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        insertBtn.setLayoutParams(insertParams);
        insertBtn.setTextColor(labelColor);
        insertBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(keyColor));

        resultButtons.addView(copyBtn);
        resultButtons.addView(insertBtn);
        layout.addView(resultButtons);

        setContentView(layout);
        inputParams.requestFocus();

        // Listeners
        encodeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String input = inputParams.getText().toString();
                    if (input.isEmpty())
                        return;
                    String encoded = Base64.encodeToString(input.getBytes(), Base64.DEFAULT);
                    outputField.setText(encoded.trim()); // Trim to remove potential trailing newline from Base64
                } catch (Exception e) {
                    outputField.setText("Error: " + e.getMessage());
                }
            }
        });

        decodeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String input = inputParams.getText().toString();
                    if (input.isEmpty())
                        return;
                    byte[] decoded = Base64.decode(input, Base64.DEFAULT);
                    outputField.setText(new String(decoded));
                } catch (IllegalArgumentException e) {
                    outputField.setText("Error: Invalid Base64 input");
                } catch (Exception e) {
                    outputField.setText("Error: " + e.getMessage());
                }
            }
        });

        copyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = outputField.getText().toString();
                if (text.isEmpty())
                    return;
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Base64 Result", text);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(Base64Activity.this, "Copied to Clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        insertBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String text = outputField.getText().toString();
                if (text.isEmpty() || text.startsWith("Error:"))
                    return;

                finish();
                // Delay paste to allow focus to return to editor
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ClipboardHistoryService.paste(text);
                    }
                }, 200);
            }
        });
    }
}
