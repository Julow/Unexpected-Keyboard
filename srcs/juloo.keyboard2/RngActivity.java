package juloo.keyboard2;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.view.View;
import java.util.Random;

public class RngActivity extends Activity {

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

        // Min Input
        final EditText minInput = new EditText(this);
        minInput.setHint("Min (Default: 0)");
        minInput.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        minInput.setTextColor(labelColor);
        minInput.setHintTextColor((labelColor & 0x00FFFFFF) | 0x80000000);
        minInput.setBackgroundResource(R.drawable.rect_rounded);
        minInput.setBackgroundTintList(android.content.res.ColorStateList.valueOf(keyColor));
        layout.addView(minInput);

        // Max Input
        final EditText maxInput = new EditText(this);
        maxInput.setHint("Max (Default: 100)");
        maxInput.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        maxInput.setTextColor(labelColor);
        maxInput.setHintTextColor((labelColor & 0x00FFFFFF) | 0x80000000);
        maxInput.setBackgroundResource(R.drawable.rect_rounded);
        maxInput.setBackgroundTintList(android.content.res.ColorStateList.valueOf(keyColor));
        layout.addView(maxInput);

        // Buttons Container
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setWeightSum(2);

        Button cancelBtn = new Button(this);
        cancelBtn.setText("Cancel");
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        cancelBtn.setLayoutParams(cancelParams);
        cancelBtn.setTextColor(labelColor);
        cancelBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(keyColor));
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        buttons.addView(cancelBtn);

        Button generateBtn = new Button(this);
        generateBtn.setText("Generate");
        LinearLayout.LayoutParams genParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT,
                1);
        generateBtn.setLayoutParams(genParams);
        generateBtn.setTextColor(labelColor);
        generateBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(keyColor));
        generateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int min = 0;
                int max = 100;

                try {
                    String minStr = minInput.getText().toString();
                    if (minStr.length() > 0)
                        min = Integer.parseInt(minStr);
                } catch (Exception e) {
                }

                try {
                    String maxStr = maxInput.getText().toString();
                    if (maxStr.length() > 0)
                        max = Integer.parseInt(maxStr);
                } catch (Exception e) {
                }

                if (min > max) {
                    int tmp = min;
                    min = max;
                    max = tmp;
                }

                int range = max - min + 1;
                if (range <= 0)
                    range = 1;

                int val = new Random().nextInt(range) + min;

                // Paste back to previous app
                finish();
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ClipboardHistoryService.paste(String.valueOf(val));
                    }
                }, 200);
            }
        });
        buttons.addView(generateBtn);

        layout.addView(buttons);

        setContentView(layout);

        minInput.requestFocus();
    }
}
