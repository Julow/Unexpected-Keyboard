package juloo.keyboard2;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import java.util.List;

public class SnippetSearchActivity extends Activity {
    private SnippetManager _manager;
    private List<SnippetItem> _results;
    private SearchAdapter _adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snippet_search);

        // Ensure window is set to allow input
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        _manager = SnippetManager.get(this);

        EditText searchInput = findViewById(R.id.search_query);
        ListView resultsList = findViewById(R.id.search_results);

        _adapter = new SearchAdapter();
        resultsList.setAdapter(_adapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    _results = _manager.findSnippets(s.toString());
                    _adapter.notifyDataSetChanged();
                } catch (Exception e) {
                    ((EditText) findViewById(R.id.search_query)).setError("Error: " + e.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        resultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final SnippetItem item = _results.get(position);
                if (item instanceof Snippet) {
                    finish();
                    // Delay paste to allow focus to return to target app
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ClipboardHistoryService.paste(((Snippet) item).content);
                        }
                    }, 200);
                }
            }
        });

        // Close on outside touch? Done by Theme.Dialog usually.
    }

    private class SearchAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return _results == null ? 0 : _results.size();
        }

        @Override
        public Object getItem(int position) {
            return _results.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            try {
                if (convertView == null) {
                    convertView = View.inflate(SnippetSearchActivity.this, R.layout.clipboard_search_entry, null);
                }
                SnippetItem item = _results.get(position);
                TextView text = convertView.findViewById(R.id.clipboard_search_text);

                if (item instanceof Snippet) {
                    Snippet snippet = (Snippet) item;
                    if (snippet.name != null && !snippet.name.equals(snippet.content)) {
                        text.setText(snippet.name + " (" + snippet.content + ")");
                    } else {
                        text.setText(snippet.content);
                    }
                } else if (item instanceof SnippetFolder) {
                    text.setText(((SnippetFolder) item).name + " (Folder)");
                }
                return convertView;
            } catch (Exception e) {
                TextView tv = new TextView(SnippetSearchActivity.this);
                tv.setText("Error in getView: " + e.toString());
                return tv;
            }
        }
    }
}
