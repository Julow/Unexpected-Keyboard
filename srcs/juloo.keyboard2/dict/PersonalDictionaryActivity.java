package juloo.keyboard2.dict;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import juloo.keyboard2.R;

public class PersonalDictionaryActivity extends Activity
{
  public static final String EXTRA_WORD = "word";
  public static final String EXTRA_REPLACEMENT = "replacement";

  PersonalDictionary _dict;
  LinearLayout _list_container;

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    _dict = PersonalDictionary.instance(this);
    setContentView(R.layout.personal_dict_activity);
    _list_container = (LinearLayout)findViewById(R.id.personal_dict_list);
    setTitle(R.string.personal_dict_title);
    if (getActionBar() != null)
      getActionBar().setDisplayHomeAsUpEnabled(true);

    String prefill_word = getIntent().getStringExtra(EXTRA_WORD);
    String prefill_replacement = getIntent().getStringExtra(EXTRA_REPLACEMENT);
    if (prefill_word != null && !prefill_word.isEmpty())
      show_add_dialog(prefill_word, prefill_replacement != null ? prefill_replacement : "");

    refresh_list();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    getMenuInflater().inflate(R.menu.personal_dict_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    int id = item.getItemId();
    if (id == R.id.personal_dict_menu_add)
    {
      show_add_dialog(null, null);
      return true;
    }
    if (id == android.R.id.home)
    {
      finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  void show_add_dialog(String initial_word, String initial_replacement)
  {
    show_word_dialog(
        getString(R.string.personal_dict_add_title),
        initial_word != null ? initial_word : "",
        initial_replacement != null ? initial_replacement : "",
        new OnSaveListener()
        {
          @Override
          public void on_save(String word, String replacement)
          {
            _dict.add(word, replacement);
            refresh_list();
          }
        });
  }

  void show_edit_dialog(PersonalDictionary.Entry entry)
  {
    boolean has_replacement = !entry.word.equals(entry.replacement);
    show_word_dialog(
        getString(R.string.personal_dict_edit_title),
        entry.word,
        has_replacement ? entry.replacement : "",
        new OnSaveListener()
        {
          @Override
          public void on_save(String word, String replacement)
          {
            _dict.remove(entry.word);
            _dict.add(word, replacement);
            refresh_list();
          }
        });
  }

  interface OnSaveListener
  {
    void on_save(String word, String replacement);
  }

  void show_word_dialog(String title, String initial_word, String initial_replacement,
      OnSaveListener listener)
  {
    LinearLayout form = new LinearLayout(this);
    form.setOrientation(LinearLayout.VERTICAL);
    int pad = dp(16);
    form.setPadding(pad, pad, pad, 0);

    EditText word_field = new EditText(this);
    word_field.setHint(R.string.personal_dict_word_hint);
    word_field.setText(initial_word);
    word_field.setSingleLine(true);
    form.addView(word_field);

    EditText replacement_field = new EditText(this);
    replacement_field.setHint(R.string.personal_dict_replacement_hint);
    replacement_field.setText(initial_replacement);
    replacement_field.setSingleLine(true);
    form.addView(replacement_field);

    AlertDialog.Builder builder = new AlertDialog.Builder(this)
      .setTitle(title)
      .setView(form)
      .setPositiveButton(R.string.personal_dict_save, new DialogInterface.OnClickListener()
          {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
              String word = word_field.getText().toString().trim();
              if (word.isEmpty())
              {
                Toast.makeText(PersonalDictionaryActivity.this,
                    R.string.personal_dict_word_empty, Toast.LENGTH_SHORT).show();
                return;
              }
              String replacement = replacement_field.getText().toString().trim();
              listener.on_save(word, replacement.isEmpty() ? word : replacement);
            }
          })
      .setNegativeButton(android.R.string.cancel, null);

    if (!initial_word.isEmpty())
    {
      builder.setNeutralButton(R.string.personal_dict_delete, new DialogInterface.OnClickListener()
          {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
              _dict.remove(initial_word);
              refresh_list();
            }
          });
    }

    builder.show();
  }

  void refresh_list()
  {
    _list_container.removeAllViews();
    List<PersonalDictionary.Entry> entries = _dict.get_all();
    if (entries.isEmpty())
    {
      TextView empty = new TextView(this);
      empty.setText(R.string.personal_dict_empty);
      int pad = dp(16);
      empty.setPadding(pad, pad, pad, pad);
      _list_container.addView(empty);
    }
    else
    {
      for (PersonalDictionary.Entry e : entries)
      {
        View row = View.inflate(this, R.layout.personal_dict_entry, null);
        ((TextView)row.findViewById(R.id.personal_dict_entry_word)).setText(e.word);
        TextView repl_view = (TextView)row.findViewById(R.id.personal_dict_entry_replacement);
        boolean has_replacement = !e.word.equals(e.replacement);
        repl_view.setText(has_replacement ? ("→ " + e.replacement) : "");
        repl_view.setVisibility(has_replacement ? View.VISIBLE : View.GONE);
        row.setOnClickListener(new View.OnClickListener()
            {
              @Override
              public void onClick(View v) { show_edit_dialog(e); }
            });
        _list_container.addView(row);
      }
    }
  }

  int dp(int value)
  {
    return (int)(value * getResources().getDisplayMetrics().density);
  }
}
