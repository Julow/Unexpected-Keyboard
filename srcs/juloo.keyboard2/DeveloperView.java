package juloo.keyboard2;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import java.util.UUID;
import java.util.Random;

public class DeveloperView extends LinearLayout {
    private KeyEventHandler _handler;

    public DeveloperView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setEventHandler(KeyEventHandler handler) {
        _handler = handler;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        findViewById(R.id.btn_uuid).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                commitText(UUID.randomUUID().toString());
            }
        });

        findViewById(R.id.btn_random_str).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                commitText(generateRandomString(16));
            }
        });

        findViewById(R.id.btn_timestamp).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                android.widget.PopupMenu popup = new android.widget.PopupMenu(getContext(), v);
                popup.getMenu().add("Linux Epoch (ms)");
                popup.getMenu().add("Linux Epoch (s)");
                popup.getMenu().add("ISO 8601 (UTC)");
                popup.getMenu().add("ISO 8601 (Local)");
                popup.getMenu().add(R.string.pref_timestamp_custom_menu);
                popup.setOnMenuItemClickListener(new android.widget.PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(android.view.MenuItem item) {
                        long now = System.currentTimeMillis();
                        String text = "";
                        String title = item.getTitle().toString();
                        if (title.equals("Linux Epoch (ms)")) {
                            text = String.valueOf(now);
                        } else if (title.equals("Linux Epoch (s)")) {
                            text = String.valueOf(now / 1000);
                        } else if (title.equals("ISO 8601 (UTC)")) {
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",
                                    java.util.Locale.US);
                            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                            text = sdf.format(new java.util.Date(now));
                        } else if (title.equals("ISO 8601 (Local)")) {
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                                    java.util.Locale.US);
                            text = sdf.format(new java.util.Date(now));
                        } else if (title.equals(getContext().getString(R.string.pref_timestamp_custom_menu))) {
                            try {
                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                                        Config.globalConfig().timestamp_format, java.util.Locale.US);
                                text = sdf.format(new java.util.Date(now));
                            } catch (Exception e) {
                                text = "Invalid format";
                            }
                        }
                        commitText(text);
                        return true;
                    }
                });
                popup.show();
            }
        });

        findViewById(R.id.btn_random_num).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                android.content.Intent intent = new android.content.Intent(getContext(), RngActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(intent);
            }
        });

        findViewById(R.id.btn_base64).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                android.content.Intent intent = new android.content.Intent(getContext(), Base64Activity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(intent);
            }
        });

        findViewById(R.id.btn_color_picker).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                android.content.Intent intent = new android.content.Intent(getContext(), ColorPickerActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(intent);
            }
        });

        findViewById(R.id.btn_calculator).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                android.content.Intent intent = new android.content.Intent(getContext(), CalculatorActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(intent);
            }
        });

        findViewById(R.id.btn_url_encode).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                android.content.Intent intent = new android.content.Intent(getContext(), UrlEncodeActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(intent);
            }
        });

        findViewById(R.id.btn_lorem).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                commitText("Lorem ipsum dolor sit amet, consectetur adipiscing elit.");
            }
        });

        findViewById(R.id.btn_back).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (_handler != null && _handler._recv != null) {
                    _handler._recv.handle_event_key(KeyValue.Event.SWITCH_BACK_DEVELOPER);
                }
            }
        });

        setOnApplyWindowInsetsListener(new OnApplyWindowInsetsListener() {
            @Override
            public android.view.WindowInsets onApplyWindowInsets(View v, android.view.WindowInsets insets) {
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                        insets.getSystemWindowInsetBottom());
                return insets.consumeSystemWindowInsets();
            }
        });
    }

    private void commitText(String text) {
        if (_handler != null) {
            _handler.send_text(text);
        }
    }

    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
