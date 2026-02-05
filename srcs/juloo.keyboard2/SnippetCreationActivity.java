package juloo.keyboard2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;

public class SnippetCreationActivity extends Activity
{
  public static final String EXTRA_UUID = "uuid";

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
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

    final android.widget.LinearLayout tagsContainer = new android.widget.LinearLayout(this);
    tagsContainer.setOrientation(android.widget.LinearLayout.HORIZONTAL);
    // Basic horizontal scroll for now since FlowLayout involves more code/custom
    // view
    final android.widget.HorizontalScrollView tagsScroll = new android.widget.HorizontalScrollView(this);
    tagsScroll.addView(tagsContainer);
    layout.addView(tagsScroll);

    final android.widget.AutoCompleteTextView tagInput = new android.widget.AutoCompleteTextView(this);
    tagInput.setHint("Type tag and press enter");
    tagInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
    tagInput.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);

    final java.util.List<String> allTags = manager.getAllTags();
    final java.util.List<String> currentTags = new java.util.ArrayList<>();

    final Runnable updateTagsView = new Runnable()
    {
      @Override
      public void run()
      {
        tagsContainer.removeAllViews();
        final float density = getResources().getDisplayMetrics().density;

        // Update Adapter
        java.util.List<String> availableTags = new java.util.ArrayList<>(allTags);
        availableTags.removeAll(currentTags);
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
            SnippetCreationActivity.this,
            android.R.layout.simple_dropdown_item_1line, availableTags);
        tagInput.setAdapter(adapter);

        for (final String tag : currentTags)
        {
          android.widget.TextView chip = new android.widget.TextView(SnippetCreationActivity.this);
          chip.setText(tag);
          chip.setBackgroundResource(R.drawable.rect_rounded);
          chip.setTextColor(0xFFFFFFFF);
          chip.setTextSize(14f);

          android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
              android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
              android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
          params.setMargins((int) (4 * density), (int) (4 * density), (int) (4 * density),
              (int) (4 * density));
          chip.setLayoutParams(params);

          chip.setOnClickListener(new android.view.View.OnClickListener()
          {
            @Override
            public void onClick(android.view.View v)
            {
              currentTags.remove(tag);
              run(); // update view
            }
          });
          tagsContainer.addView(chip);
        }
      }
    };

    tagInput.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener()
    {
      @Override
      public void onItemClick(android.widget.AdapterView<?> parent, android.view.View view, int position,
          long id)
      {
        String selected = (String) parent.getItemAtPosition(position);
        if (!currentTags.contains(selected))
        {
          currentTags.add(selected);
          updateTagsView.run();
        }
        tagInput.setText("");
      }
    });

    // Initial setup
    android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
        android.R.layout.simple_dropdown_item_1line, allTags);
    tagInput.setAdapter(adapter);

    layout.addView(tagInput);



    tagInput.setOnEditorActionListener(new android.widget.TextView.OnEditorActionListener()
    {
      @Override
      public boolean onEditorAction(android.widget.TextView v, int actionId, android.view.KeyEvent event)
      {
        if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
            (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER
                && event.getAction() == android.view.KeyEvent.ACTION_DOWN))
        {
          String text = tagInput.getText().toString().trim();
          if (!text.isEmpty())
          {
            if (!currentTags.contains(text))
            {
              currentTags.add(text);
              updateTagsView.run();
            }
            tagInput.setText("");
            // Scroll to end
            tagsScroll.post(new Runnable()
            {
              public void run()
              {
                tagsScroll.fullScroll(android.view.View.FOCUS_RIGHT);
              }
            });
          }
          return true;
        }
        return false;
      }
    });

    String title = "New Snippet";
    String positiveButton = "Create";

    if (uuid != null)
    {
      SnippetItem item = manager.findItem(uuid);
      if (item instanceof Snippet)
      {
        existingSnippet = (Snippet) item;
        nameInput.setText(existingSnippet.name);
        contentInput.setText(existingSnippet.content);


        if (existingSnippet.tags != null)
        {
          currentTags.addAll(existingSnippet.tags);
          updateTagsView.run();
        }

        title = "Edit Snippet";
        positiveButton = "Save";
      }
      else
      {
        existingSnippet = null;
      }
    }
    else
    {
      existingSnippet = null;
    }

    AlertDialog dialog = new AlertDialog.Builder(this)
        .setTitle(title)
        .setView(layout)
        .setNegativeButton(positiveButton, new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            String name = nameInput.getText().toString();
            String content = contentInput.getText().toString();
            String pendingTag = tagInput.getText().toString().trim();
            if (!pendingTag.isEmpty() && !currentTags.contains(pendingTag))
            {
              currentTags.add(pendingTag);
            }

            if (name.isEmpty() && !content.isEmpty())
            {
              name = content; // Fallback name to content if name is empty
            }

            if (!content.isEmpty())
            {
              if (existingSnippet != null)
              {
                manager.updateSnippet(existingSnippet, name, content, currentTags);
              }
              else
              {
                Snippet newSnippet = new Snippet(content);
                newSnippet.name = name;
                newSnippet.tags = currentTags;

                manager.getCurrentFolder().addItem(newSnippet);
                manager.save();
              }
            }
            finish();
          }
        })
        .setPositiveButton("Cancel", new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
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
        .create();

    dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    dialog.show();

    if (existingSnippet == null)
    {
      nameInput.requestFocus();
    }
    else
    {
      contentInput.requestFocus();
    }
  }
}
