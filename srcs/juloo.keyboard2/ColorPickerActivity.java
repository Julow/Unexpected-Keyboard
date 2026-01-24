package juloo.keyboard2;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

public class ColorPickerActivity extends Activity {

    private ColorMatrixView mColorMatrix;
    private SeekBar mHueSeekBar, mAlphaSeekBar;
    private View mPreview;
    private EditText mInput;
    private Button mHexBtn, mRgbBtn, mHsvBtn;

    private boolean mIsUpdating = false;

    // Current State
    private float[] mHsv = new float[] { 0f, 1f, 1f };
    private int mAlpha = 255;
    private int mColor = Color.RED;

    private enum Format {
        HEX, RGB, HSV
    }

    private Format mFormat = Format.HEX;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // UI Setup
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

        // 1. Color Matrix (Sat/Val)
        mColorMatrix = new ColorMatrixView(this);
        LinearLayout.LayoutParams matrixParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (200 * getResources().getDisplayMetrics().density));
        mColorMatrix.setLayoutParams(matrixParams);
        mColorMatrix.setOnColorChangedListener(new ColorMatrixView.OnColorChangedListener() {
            @Override
            public void onColorChanged(float sat, float val) {
                if (mIsUpdating)
                    return;
                mHsv[1] = sat;
                mHsv[2] = val;
                updateColor(true);
            }
        });
        layout.addView(mColorMatrix);

        // 2. Hue Slider
        mHueSeekBar = new SeekBar(this);
        mHueSeekBar.setMax(360);
        setupHueGradient(mHueSeekBar);
        mHueSeekBar.setPadding(0, padding, 0, padding);
        mHueSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && !mIsUpdating) {
                    mHsv[0] = (float) progress;
                    updateColor(true);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        layout.addView(mHueSeekBar);

        // 3. Alpha Slider
        mAlphaSeekBar = new SeekBar(this);
        mAlphaSeekBar.setMax(255);
        mAlphaSeekBar.setProgress(255);
        mAlphaSeekBar.setPadding(0, 0, 0, padding);
        mAlphaSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && !mIsUpdating) {
                    mAlpha = progress;
                    updateColor(true);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        layout.addView(mAlphaSeekBar);

        // 4. Preview & Input Row
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        mPreview = new View(this);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                (int) (40 * getResources().getDisplayMetrics().density),
                (int) (40 * getResources().getDisplayMetrics().density));
        previewParams.rightMargin = padding;
        mPreview.setLayoutParams(previewParams);
        mPreview.setBackgroundColor(mColor);
        // Add border to preview
        android.graphics.drawable.GradientDrawable border = new android.graphics.drawable.GradientDrawable();
        border.setColor(mColor);
        border.setStroke(2, labelColor);
        mPreview.setBackground(border);

        row.addView(mPreview);

        mInput = new EditText(this);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT,
                1);
        mInput.setLayoutParams(inputParams);
        mInput.setHint("#AARRGGBB");
        mInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        mInput.setTextColor(labelColor);
        mInput.setHintTextColor((labelColor & 0x00FFFFFF) | 0x80000000);
        mInput.setBackgroundResource(R.drawable.rect_rounded);
        mInput.setBackgroundTintList(ColorStateList.valueOf(keyColor));
        mInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mIsUpdating)
                    return;
                parseInput(s.toString());
            }
        });
        row.addView(mInput);
        layout.addView(row);

        // 5. Format Selector
        LinearLayout formatLayout = new LinearLayout(this);
        formatLayout.setOrientation(LinearLayout.HORIZONTAL);
        formatLayout.setPadding(0, padding, 0, padding);

        mHexBtn = createFormatButton("HEX", Format.HEX, labelColor, keyColor);
        mRgbBtn = createFormatButton("RGB", Format.RGB, labelColor, keyColor);
        mHsvBtn = createFormatButton("HSV", Format.HSV, labelColor, keyColor);

        formatLayout.addView(mHexBtn);
        formatLayout.addView(mRgbBtn);
        formatLayout.addView(mHsvBtn);
        layout.addView(formatLayout);

        // 6. Action Buttons
        LinearLayout actionButtons = new LinearLayout(this);
        actionButtons.setOrientation(LinearLayout.HORIZONTAL);

        Button cancelBtn = new Button(this);
        cancelBtn.setText("Cancel");
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        cancelBtn.setLayoutParams(cancelParams);
        cancelBtn.setTextColor(labelColor);
        cancelBtn.setBackgroundTintList(ColorStateList.valueOf(keyColor));
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Button insertBtn = new Button(this);
        insertBtn.setText("Insert");
        LinearLayout.LayoutParams insertParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        insertBtn.setLayoutParams(insertParams);
        insertBtn.setTextColor(labelColor);
        insertBtn.setBackgroundTintList(ColorStateList.valueOf(keyColor));
        insertBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ClipboardHistoryService.paste(mInput.getText().toString());
                    }
                }, 200);
            }
        });

        actionButtons.addView(cancelBtn);
        actionButtons.addView(insertBtn);
        layout.addView(actionButtons);

        setContentView(layout);

        // Initial state
        updateFormatButtons();
        updateColor(false);
    }

    private Button createFormatButton(String text, final Format format, int textColor, int bgColor) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(textColor);
        btn.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        btn.setLayoutParams(params);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFormat = format;
                updateFormatButtons();
                updateInputText();
            }
        });
        return btn;
    }

    private void updateFormatButtons() {
        mHexBtn.setTypeface(null,
                mFormat == Format.HEX ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        mRgbBtn.setTypeface(null,
                mFormat == Format.RGB ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        mHsvBtn.setTypeface(null,
                mFormat == Format.HSV ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void setupHueGradient(SeekBar seekBar) {
        // Rainbow gradient
        int[] colors = new int[] {
                0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000
        };
        android.graphics.drawable.GradientDrawable rainbow = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT, colors);
        rainbow.setCornerRadius(8f);
        seekBar.setBackground(rainbow);
    }

    private void updateColor(boolean fromUi) {
        if (!fromUi) {
            // Update logic from stored state to UI
        }

        mColor = Color.HSVToColor(mAlpha, mHsv);

        mIsUpdating = true;

        mColorMatrix.setHue(mHsv[0]);
        mColorMatrix.setSatVal(mHsv[1], mHsv[2]); // Update cursor pos

        mHueSeekBar.setProgress((int) mHsv[0]);
        mAlphaSeekBar.setProgress(mAlpha);

        android.graphics.drawable.GradientDrawable border = (android.graphics.drawable.GradientDrawable) mPreview
                .getBackground();
        border.setColor(mColor);

        updateInputText();

        mIsUpdating = false;
    }

    private void updateInputText() {
        String text = "";
        switch (mFormat) {
            case HEX:
                text = String.format("#%02X%06X", mAlpha, (mColor & 0xFFFFFF));
                break;
            case RGB:
                text = String.format(Locale.US, "%d, %d, %d, %d", Color.red(mColor), Color.green(mColor),
                        Color.blue(mColor), mAlpha);
                break;
            case HSV:
                text = String.format(Locale.US, "%.0f, %.2f, %.2f, %d", mHsv[0], mHsv[1], mHsv[2], mAlpha);
                break;
        }
        if (!mInput.getText().toString().equals(text)) {
            boolean wasUpdating = mIsUpdating;
            mIsUpdating = true;
            mInput.setText(text);
            mIsUpdating = wasUpdating;
        }
    }

    private void parseInput(String input) {
        try {
            if (mFormat == Format.HEX) {
                mColor = Color.parseColor(input);
                Color.colorToHSV(mColor, mHsv);
                mAlpha = Color.alpha(mColor);
            } else if (mFormat == Format.RGB) {
                String[] parts = input.split(",");
                if (parts.length >= 3) {
                    int r = Integer.parseInt(parts[0].trim());
                    int g = Integer.parseInt(parts[1].trim());
                    int b = Integer.parseInt(parts[2].trim());
                    int a = parts.length > 3 ? Integer.parseInt(parts[3].trim()) : 255;
                    mColor = Color.argb(a, r, g, b);
                    Color.colorToHSV(mColor, mHsv);
                    mAlpha = a;
                }
            } else if (mFormat == Format.HSV) {
                String[] parts = input.split(",");
                if (parts.length >= 3) {
                    float h = Float.parseFloat(parts[0].trim());
                    float s = Float.parseFloat(parts[1].trim());
                    float v = Float.parseFloat(parts[2].trim());
                    int a = parts.length > 3 ? Integer.parseInt(parts[3].trim()) : 255;
                    mHsv[0] = h;
                    mHsv[1] = s;
                    mHsv[2] = v;
                    mAlpha = a;
                    mColor = Color.HSVToColor(mAlpha, mHsv);
                }
            }
            // Update UI to reflect new color (except text)
            mIsUpdating = true;
            mColorMatrix.setHue(mHsv[0]);
            mColorMatrix.setSatVal(mHsv[1], mHsv[2]);
            mHueSeekBar.setProgress((int) mHsv[0]);
            mAlphaSeekBar.setProgress(mAlpha);
            android.graphics.drawable.GradientDrawable border = (android.graphics.drawable.GradientDrawable) mPreview
                    .getBackground();
            border.setColor(mColor);
            mIsUpdating = false;

        } catch (Exception e) {
            // parsing failed, ignore
        }
    }
}
