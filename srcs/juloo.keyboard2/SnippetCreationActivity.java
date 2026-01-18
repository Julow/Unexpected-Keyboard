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

        final EditText input = new EditText(this);
        String title = "New Snippet";
        String positiveButton = "Create";

        if (uuid != null) {
            SnippetItem item = manager.findItem(uuid);
            if (item instanceof Snippet) {
                existingSnippet = (Snippet) item;
                input.setText(existingSnippet.content);
                input.setSelection(existingSnippet.content.length());
                title = "Edit Snippet";
                positiveButton = "Save";
            } else {
                existingSnippet = null;
            }
        } else {
            existingSnippet = null;
        }

        input.setHint("Snippet content");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(input)
                .setNegativeButton(positiveButton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String content = input.getText().toString();
                        if (!content.isEmpty()) {
                            if (existingSnippet != null) {
                                manager.updateSnippet(existingSnippet, content);
                            } else {
                                manager.getCurrentFolder().addItem(new Snippet(content));
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

        input.requestFocus();
    }
}
