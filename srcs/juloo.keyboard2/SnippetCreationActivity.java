package juloo.keyboard2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.EditText;

public class SnippetCreationActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        final EditText input = new EditText(this);
        input.setHint("Snippet content");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("New Snippet")
                .setView(input)
                .setNegativeButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String content = input.getText().toString();
                        if (!content.isEmpty()) {
                            SnippetManager manager = SnippetManager.get(SnippetCreationActivity.this);
                            manager.getCurrentFolder().addItem(new Snippet(content));
                            manager.save();
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

        // Show keyboard immediately
        input.requestFocus();
    }
}
