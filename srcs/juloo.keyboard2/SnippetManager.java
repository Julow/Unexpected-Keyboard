package juloo.keyboard2;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SnippetManager {
    private static SnippetManager instance;
    private static final String PREF_NAME = "pinned_clipboards";
    private static final String PREF_KEY_OLD = "pinned";
    private static final String PREF_KEY_NEW = "snippets_root";

    private final SharedPreferences prefs;
    private SnippetFolder root;
    private SnippetFolder currentFolder;

    public boolean shouldRestoreClipboardView = false;

    private SnippetManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        load();
    }

    public static synchronized SnippetManager get(Context context) {
        if (instance == null) {
            instance = new SnippetManager(context);
        }
        return instance;
    }

    private void load() {
        String json = prefs.getString(PREF_KEY_NEW, null);
        if (json != null) {
            try {
                JSONObject obj = new JSONObject(json);
                root = (SnippetFolder) deserialize(obj);
            } catch (JSONException e) {
                // Fallback or error handling
                root = new SnippetFolder("Root");
            }
        } else {
            // Check for migration
            root = new SnippetFolder("Root");
            migrateOldPinnedItems();
        }
        if (root.items.isEmpty()) {
            root.addItem(new Snippet("Welcome to Snippets!"));
            root.addItem(new Snippet("Long press move button to drag."));
            save();
        }
        currentFolder = root;
    }

    private void migrateOldPinnedItems() {
        String oldJson = prefs.getString(PREF_KEY_OLD, null);
        if (oldJson == null)
            return;

        try {
            JSONArray arr = new JSONArray(oldJson);
            for (int i = 0; i < arr.length(); i++) {
                String content = arr.getString(i);
                root.addItem(new Snippet(content));
            }
            save(); // Save immediately in new format
            // Optional: clear old key? Maybe keep for safety for now.
        } catch (JSONException e) {
            // Ignore malformed old data
        }
    }

    public void save() {
        try {
            JSONObject obj = serialize(root);
            prefs.edit().putString(PREF_KEY_NEW, obj.toString()).apply();
            notifyListeners();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public SnippetFolder getRoot() {
        return root;
    }

    public SnippetFolder getCurrentFolder() {
        return currentFolder;
    }

    public void setCurrentFolder(SnippetFolder folder) {
        this.currentFolder = folder;
    }

    public boolean isAtRoot() {
        return currentFolder == root;
    }

    // Rudimentary parent lookup, could be optimized with backward links
    public SnippetFolder getParent(SnippetItem target) {
        if (target == root)
            return null;
        return findParentRecursive(root, target);
    }

    private SnippetFolder findParentRecursive(SnippetFolder current, SnippetItem target) {
        for (SnippetItem item : current.items) {
            if (item == target)
                return current;
            if (item instanceof SnippetFolder) {
                SnippetFolder result = findParentRecursive((SnippetFolder) item, target);
                if (result != null)
                    return result;
            }
        }
        return null;
    }

    public List<SnippetFolder> getAllFolders() {
        List<SnippetFolder> folders = new ArrayList<>();
        collectFoldersRecursive(root, folders);
        return folders;
    }

    private void collectFoldersRecursive(SnippetFolder current, List<SnippetFolder> acc) {
        acc.add(current);
        for (SnippetItem item : current.items) {
            if (item instanceof SnippetFolder) {
                collectFoldersRecursive((SnippetFolder) item, acc);
            }
        }
    }

    public SnippetItem findItem(String uuid) {
        return findItemRecursive(root, uuid);
    }

    private SnippetItem findItemRecursive(SnippetFolder current, String uuid) {
        if (current.uuid.equals(uuid))
            return current;
        for (SnippetItem item : current.items) {
            if (item.uuid.equals(uuid))
                return item;
            if (item instanceof SnippetFolder) {
                SnippetItem result = findItemRecursive((SnippetFolder) item, uuid);
                if (result != null)
                    return result;
            }
        }
        return null;
    }

    public List<SnippetItem> findSnippets(String query) {
        List<SnippetItem> results = new ArrayList<>();
        if (query == null || query.isEmpty())
            return results;
        findSnippetsRecursive(root, query.toLowerCase(), results);
        return results;
    }

    private void findSnippetsRecursive(SnippetFolder current, String query, List<SnippetItem> results) {
        for (SnippetItem item : current.items) {
            if (item instanceof Snippet) {
                Snippet snippet = (Snippet) item;
                if (snippet.content.toLowerCase().contains(query)) {
                    results.add(snippet);
                }
            } else if (item instanceof SnippetFolder) {
                // Should we search folder names? Maybe. For now just content inside.
                // If user wants to find a folder, they probably browse.
                // But let's check folder name too just in case.
                if (item.name.toLowerCase().contains(query)) {
                    results.add(item);
                }
                findSnippetsRecursive((SnippetFolder) item, query, results);
            }
        }
    }

    public void moveItem(SnippetItem item, SnippetFolder target) {
        SnippetFolder parent = getParent(item);
        if (parent != null) {
            parent.removeItem(item);
            target.addItem(item);
            save();
        }
    }

    private JSONObject serialize(SnippetItem item) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("uuid", item.uuid);
        obj.put("name", item.name);

        if (item instanceof Snippet) {
            obj.put("type", "snippet");
            obj.put("content", ((Snippet) item).content);
        } else if (item instanceof SnippetFolder) {
            obj.put("type", "folder");
            JSONArray children = new JSONArray();
            for (SnippetItem child : ((SnippetFolder) item).items) {
                children.put(serialize(child));
            }
            obj.put("items", children);
        }
        return obj;
    }

    private SnippetItem deserialize(JSONObject obj) throws JSONException {
        String type = obj.optString("type");
        if (type.isEmpty()) {
            // Infer type if missing
            if (obj.has("content")) {
                type = "snippet";
            } else {
                type = "folder";
            }
        }

        String uuid = obj.optString("uuid");
        String name = obj.getString("name");

        if ("snippet".equals(type)) {
            String content = obj.getString("content");
            return new Snippet(uuid, name, content);
        } else {
            SnippetFolder folder = new SnippetFolder(uuid, name);
            JSONArray items = obj.optJSONArray("items");
            if (items != null) {
                for (int i = 0; i < items.length(); i++) {
                    folder.addItem(deserialize(items.getJSONObject(i)));
                }
            }
            return folder;
        }
    }

    // Listener interface
    public interface OnSnippetsChangeListener {
        void onSnippetsChanged();
    }

    private List<OnSnippetsChangeListener> listeners = new ArrayList<>();

    public void addListener(OnSnippetsChangeListener l) {
        listeners.add(l);
    }

    public void removeListener(OnSnippetsChangeListener l) {
        listeners.remove(l);
    }

    private void notifyListeners() {
        for (OnSnippetsChangeListener l : listeners) {
            l.onSnippetsChanged();
        }
    }
}
