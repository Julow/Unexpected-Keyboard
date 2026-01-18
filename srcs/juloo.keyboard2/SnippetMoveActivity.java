package juloo.keyboard2;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class SnippetMoveActivity extends Activity {
    public static final String EXTRA_ITEM_UUID = "item_uuid";
    public static final String EXTRA_CONTENT = "content";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.clipboard_move_activity);

        String uuid = getIntent().getStringExtra(EXTRA_ITEM_UUID);
        final String content = getIntent().getStringExtra(EXTRA_CONTENT);

        if (uuid == null && content == null) {
            finish();
            return;
        }

        final SnippetManager manager = SnippetManager.get(this);
        final SnippetItem item = (uuid != null) ? manager.findItem(uuid) : null;

        if (uuid != null && item == null) {
            finish();
            return;
        }

        if (content != null) {
            setTitle("Save to...");
        } else {
            setTitle("Move to...");
        }

        List<SnippetFolder> allFolders = manager.getAllFolders();
        final List<SnippetFolder> validTargets = new ArrayList<>();

        // Filter out invalid targets (itself or children of itself)
        // Also exclude current parent properly?
        // Current implementation is simple: just allow moving to any folder that is not
        // itself or its child.
        // If item is not a folder, any folder is valid except maybe checking if it's
        // already there (handled by manager logic likely just no-op)

        for (SnippetFolder f : allFolders) {
            if (content != null || isValidTarget(f, item)) {
                validTargets.add(f);
            }
        }

        ListView list = (ListView) findViewById(R.id.folder_list);
        list.setAdapter(new ArrayAdapter<SnippetFolder>(this, android.R.layout.simple_list_item_1, validTargets) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                SnippetFolder folder = getItem(position);
                tv.setText(getFolderPath(manager, folder));
                return tv;
            }
        });

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SnippetFolder target = validTargets.get(position);
                if (content != null) {
                    target.addItem(new Snippet(content));
                    manager.save();
                } else if (item != null) {
                    manager.moveItem(item, target);
                }
                finish();
            }
        });
    }

    private boolean isValidTarget(SnippetFolder target, SnippetItem item) {
        if (target == item)
            return false;

        // Check if target is a descendant of item (if item is a folder)
        if (item instanceof SnippetFolder) {
            if (isDescendant(target, (SnippetFolder) item))
                return false;
        }

        return true;
    }

    private boolean isDescendant(SnippetItem potentialDescendant, SnippetFolder ancestor) {
        for (SnippetItem child : ancestor.items) {
            if (child == potentialDescendant)
                return true;
            if (child instanceof SnippetFolder) {
                if (isDescendant(potentialDescendant, (SnippetFolder) child))
                    return true;
            }
        }
        return false;
    }

    private String getFolderPath(SnippetManager manager, SnippetFolder folder) {
        if (folder == manager.getRoot())
            return "Root";
        StringBuilder sb = new StringBuilder(folder.name);
        SnippetFolder parent = manager.getParent(folder);
        while (parent != null) {
            if (parent == manager.getRoot()) {
                sb.insert(0, "Root / ");
                break;
            }
            sb.insert(0, parent.name + " / ");
            parent = manager.getParent(parent);
        }
        return sb.toString();
    }
}
