package juloo.keyboard2;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
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
import java.text.DecimalFormat;

public class CalculatorActivity extends Activity {

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
        inputLabel.setText("Expression:");
        inputLabel.setTextColor(labelColor);
        layout.addView(inputLabel);

        // Input Field
        final EditText inputParams = new EditText(this);
        inputParams.setHint("e.g. 2 + 2 * (3 / 4)");
        inputParams.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        inputParams.setMinLines(2);
        inputParams.setMaxLines(4);
        inputParams.setGravity(Gravity.TOP | Gravity.START);
        inputParams.setTextColor(labelColor);
        inputParams.setHintTextColor((labelColor & 0x00FFFFFF) | 0x80000000); // 50% opacity
        inputParams.setBackgroundResource(R.drawable.rect_rounded);
        inputParams.setBackgroundTintList(android.content.res.ColorStateList.valueOf(keyColor));
        layout.addView(inputParams);

        // Action Buttons
        LinearLayout actionButtons = new LinearLayout(this);
        actionButtons.setOrientation(LinearLayout.HORIZONTAL);
        actionButtons.setWeightSum(1);

        Button calculateBtn = new Button(this);
        calculateBtn.setText("Calculate");
        LinearLayout.LayoutParams calcParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        calculateBtn.setLayoutParams(calcParams);
        calculateBtn.setTextColor(labelColor);
        calculateBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(keyColor));

        actionButtons.addView(calculateBtn);
        layout.addView(actionButtons);

        // Output Label
        TextView outputLabel = new TextView(this);
        outputLabel.setText("Result:");
        outputLabel.setPadding(0, padding / 2, 0, 0);
        outputLabel.setTextColor(labelColor);
        layout.addView(outputLabel);

        // Output Field (Read Only)
        final EditText outputField = new EditText(this);
        outputField.setHint("Result will appear here...");
        outputField.setInputType(android.text.InputType.TYPE_NULL);
        outputField.setMinLines(1);
        outputField.setMaxLines(2);
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
        calculateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String expression = inputParams.getText().toString();
                if (expression.isEmpty())
                    return;
                try {
                    double result = eval(expression);
                    // Format to remove trailing zeros if integer
                    DecimalFormat df = new DecimalFormat("#.##########");
                    outputField.setText(df.format(result));
                } catch (Exception e) {
                    outputField.setText("Error");
                }
            }
        });

        copyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = outputField.getText().toString();
                if (text.isEmpty() || "Error".equals(text))
                    return;
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Calculator Result", text);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(CalculatorActivity.this, "Copied to Clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        insertBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String text = outputField.getText().toString();
                if (text.isEmpty() || "Error".equals(text))
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

    // Simple recursive parser
    public static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ')
                    nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length())
                    throw new RuntimeException("Unexpected: " + (char) ch);
                return x;
            }

            // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor
            // factor = `+` factor | `-` factor | `(` expression `)` | number | number `^`
            // factor

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if (eat('+'))
                        x += parseTerm(); // addition
                    else if (eat('-'))
                        x -= parseTerm(); // subtraction
                    else
                        return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if (eat('*'))
                        x *= parseFactor(); // multiplication
                    else if (eat('/'))
                        x /= parseFactor(); // division
                    else if (eat('%'))
                        x %= parseFactor(); // modulo
                    else
                        return x;
                }
            }

            double parseFactor() {
                if (eat('+'))
                    return parseFactor(); // unary plus
                if (eat('-'))
                    return -parseFactor(); // unary minus

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.')
                        nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }

                if (eat('^'))
                    x = Math.pow(x, parseFactor()); // exponentiation

                return x;
            }
        }.parse();
    }
}
