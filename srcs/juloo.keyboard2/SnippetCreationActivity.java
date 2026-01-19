package juloo.keyboard2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.EditText;

public class SnippetCreationActivity extends Activity {
    public static final String EXTRA_UUID = "uuid";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        final String uuid = getIntent().getStringExtra(EXTRA_UUID);
        final SnippetManager manager = SnippetManager.get(this);
        final Snippet existingSnippet;

        final android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding / 2, padding, padding / 2);

        final EditText nameInput = new EditText(this);
        nameInput.setHint("Label (Optional)");
        layout.addView(nameInput);

        final EditText contentInput = new EditText(this);
        contentInput.setHint("Snippet content");
        layout.addView(contentInput);

        String title = "New Snippet";
        String positiveButton = "Create";

        if (uuid != null) {
            SnippetItem item = manager.findItem(uuid);
            if (item instanceof Snippet) {
                existingSnippet = (Snippet) item;
                nameInput.setText(existingSnippet.name);
                contentInput.setText(existingSnippet.content);
                // contentInput.setSelection(existingSnippet.content.length());
                title = "Edit Snippet";
                positiveButton = "Save";
            } else {
                existingSnippet = null;
            }
        } else {
            existingSnippet = null;
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(layout)
                .setNegativeButton(positiveButton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = nameInput.getText().toString();
                        String content = contentInput.getText().toString();

                        if (name.isEmpty() && !content.isEmpty()) {
                            name = content; // Fallback name to content if name is empty
                        }

                        if (!content.isEmpty()) {
                            if (existingSnippet != null) {
                                manager.updateSnippet(existingSnippet, name, content);
                            } else {
                                Snippet newSnippet = new Snippet(content);
                                newSnippet.name = name;
                                manager.getCurrentFolder().addItem(newSnippet);
                                manager.save();
                            }
                        }
                        finish();
                    }
                })
                .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                })
                .create();

        dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();

        if (existingSnippet == null) {
            nameInput.requestFocus();
        } else {
            contentInput.requestFocus();
        }
    }
}
