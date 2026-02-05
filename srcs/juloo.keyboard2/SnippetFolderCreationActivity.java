package juloo.keyboard2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.EditText;

public class SnippetFolderCreationActivity extends Activity
{
  public static final String EXTRA_UUID = "uuid";

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    final String uuid = getIntent().getStringExtra(EXTRA_UUID);
    final SnippetManager manager = SnippetManager.get(this);
    final SnippetFolder existingFolder;

    final EditText input = new EditText(this);
    String title = "New Folder";
    String positiveButton = "Create";

    if (uuid != null)
    {
      SnippetItem item = manager.findItem(uuid);
      if (item instanceof SnippetFolder)
      {
        existingFolder = (SnippetFolder) item;
        input.setText(existingFolder.name);
        input.setSelection(existingFolder.name.length());
        title = "Edit Folder";
        positiveButton = "Save";
      }
      else
      {
        existingFolder = null;
      }
    }
    else
    {
      existingFolder = null;
    }

    new AlertDialog.Builder(this)
        .setTitle(title)
        .setView(input)
        .setPositiveButton("Cancel", new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            finish();
          }
        })
        .setNegativeButton(positiveButton, new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            String name = input.getText().toString();
            if (!name.isEmpty())
            {
              if (existingFolder != null)
              {
                manager.updateFolder(existingFolder, name);
              }
              else
              {
                manager.getCurrentFolder().addItem(new SnippetFolder(name));
                manager.save();
              }
            }
            finish();
          }
        })
        .setOnCancelListener(new DialogInterface.OnCancelListener()
        {
          @Override
          public void onCancel(DialogInterface dialog)
          {
            finish();
          }
        })
        .show();

    input.requestFocus();
  }
}
