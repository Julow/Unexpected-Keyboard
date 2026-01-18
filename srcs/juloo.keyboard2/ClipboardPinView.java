package juloo.keyboard2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

public final class ClipboardPinView extends NonScrollListView {
  SnippetManager _manager;
  ClipboardPinEntriesAdapter _adapter;
  int _draggingPos = -1;

  // Header views
  View _headerView;
  TextView _headerPath;
  View _headerBack;
  View _headerAddFolder;
  View _headerFullScreen;

  public ClipboardPinView(Context ctx, AttributeSet attrs) {
    super(ctx, attrs);
    _manager = SnippetManager.get(ctx);

    // Register listener
    _manager.addListener(new SnippetManager.OnSnippetsChangeListener() {
      @Override
      public void onSnippetsChanged() {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
          update_view();
        } else {
          post(new Runnable() {
            @Override
            public void run() {
              update_view();
            }
          });
        }
      }
    });

    // Setup Header
    _headerView = View.inflate(ctx, R.layout.clipboard_list_header, null);

    _headerPath = (TextView) _headerView.findViewById(R.id.clipboard_header_path);

    // Initialize buttons
    _headerBack = _headerView.findViewById(R.id.clipboard_header_back);
    _headerAddFolder = _headerView.findViewById(R.id.clipboard_header_add_folder);
    _headerFullScreen = _headerView.findViewById(R.id.clipboard_header_fullscreen);

    // Bind Listeners
    _headerBack.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        go_up();
      }
    });

    _headerAddFolder.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        prompt_new_folder();
      }
    });

    _headerView.findViewById(R.id.clipboard_header_search).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        start_search();
      }
    });

    _headerView.findViewById(R.id.clipboard_header_add_snippet).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        prompt_new_snippet();
      }
    });

    _headerFullScreen.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        open_fullscreen();
      }
    });

    _headerView.findViewById(R.id.clipboard_header_search_close).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        // No-op
      }
    });

    // Removed text watcher

    addHeaderView(_headerView);
    _adapter = new ClipboardPinEntriesAdapter();
    setAdapter(_adapter);
    update_view();
  }

  void start_search() {
    android.content.Intent intent = new android.content.Intent(getContext(), SnippetSearchActivity.class);
    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
    getContext().startActivity(intent);
  }

  /** Pin a clipboard and persist the change. */
  public void add_entry(String text) {
    _manager.getCurrentFolder().addItem(new Snippet(text));
    _manager.save();
    update_view();
  }

  /** Remove the entry. */
  public void remove_entry(int pos) {

    _manager.getCurrentFolder().removeItem(pos);
    _manager.save();
    update_view();
  }

  /** Send the specified entry to the editor. */
  public void paste_entry(final Snippet snippet) {

    ClipboardHistoryService.paste(snippet.content);
  }

  void update_view() {

    // Update Header
    SnippetFolder current = _manager.getCurrentFolder();
    if (_manager.isAtRoot()) {
      _headerPath.setText("Snippets");
      if (getActivity() instanceof SnippetManagerActivity) {
        _headerBack.setVisibility(View.VISIBLE);
      } else {
        _headerBack.setVisibility(View.GONE);
      }
    } else {
      _headerPath.setText(current.name);
      _headerBack.setVisibility(View.VISIBLE);
    }

    _adapter.notifyDataSetChanged();
    // Ensure layout updates if size changed
    requestLayout();
  }

  void go_up() {

    SnippetFolder current = _manager.getCurrentFolder();
    SnippetFolder parent = _manager.getParent(current);
    if (parent != null) {
      _manager.setCurrentFolder(parent);
      update_view();
    } else if (_manager.isAtRoot() && getActivity() instanceof SnippetManagerActivity) {
      _manager.shouldRestoreClipboardView = true;
      getActivity().finish();
    } else if (!_manager.isAtRoot()) {
      // Fallback if parent not found (orphan?), go root
      _manager.setCurrentFolder(_manager.getRoot());
      update_view();
    }
  }

  void prompt_new_folder() {
    android.content.Intent intent = new android.content.Intent(getContext(), SnippetFolderCreationActivity.class);
    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
    getContext().startActivity(intent);
  }

  void reorder_entry(int from, int to) {
    SnippetFolder current = _manager.getCurrentFolder();
    SnippetItem item = current.items.remove(from);
    current.items.add(to, item);
    _manager.save();
    update_view();
  }

  void move_entry(String uuid) {
    android.content.Intent intent = new android.content.Intent(getContext(), SnippetMoveActivity.class);
    intent.putExtra(SnippetMoveActivity.EXTRA_ITEM_UUID, uuid);
    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
    getContext().startActivity(intent);
  }

  void open_fullscreen() {
    android.content.Intent intent = new android.content.Intent(getContext(), SnippetManagerActivity.class);
    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
    getContext().startActivity(intent);
  }

  void prompt_new_snippet() {
    android.content.Intent intent = new android.content.Intent(getContext(), SnippetCreationActivity.class);
    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
    getContext().startActivity(intent);
  }

  class ClipboardPinEntriesAdapter extends BaseAdapter {
    static final int TYPE_SNIPPET = 0;
    static final int TYPE_FOLDER = 1;

    public ClipboardPinEntriesAdapter() {
    }

    @Override
    public int getCount() {
      return _manager.getCurrentFolder().items.size();
    }

    @Override
    public Object getItem(int pos) {
      return _manager.getCurrentFolder().items.get(pos);
    }

    @Override
    public long getItemId(int pos) {
      return pos;
    }

    @Override
    public int getViewTypeCount() {
      return 2;
    }

    @Override
    public int getItemViewType(int position) {
      SnippetItem item = (SnippetItem) getItem(position);
      return (item instanceof SnippetFolder) ? TYPE_FOLDER : TYPE_SNIPPET;
    }

    @Override
    public View getView(final int pos, View v, ViewGroup _parent) {
      final SnippetItem item = (SnippetItem) getItem(pos);
      int type = getItemViewType(pos);

      if (v == null) {
        if (type == TYPE_FOLDER) {
          v = View.inflate(getContext(), R.layout.clipboard_folder_entry, null);
        } else {
          v = View.inflate(getContext(), R.layout.clipboard_pin_entry, null);
        }
      }

      if (type == TYPE_FOLDER) {
        SnippetFolder folder = (SnippetFolder) item;
        ((TextView) v.findViewById(R.id.clipboard_folder_text)).setText(folder.name);

        // Open folder on click
        v.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            _manager.setCurrentFolder((SnippetFolder) item);
            update_view();
          }
        });

        // Edit folder
        v.findViewById(R.id.clipboard_folder_edit).setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            android.content.Intent intent = new android.content.Intent(getContext(),
                SnippetFolderCreationActivity.class);
            intent.putExtra(SnippetFolderCreationActivity.EXTRA_UUID, item.uuid);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
          }
        });

        // Move folder
        View moveBtn = v.findViewById(R.id.clipboard_folder_move);
        moveBtn.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            move_entry(item.uuid);
          }
        });
        moveBtn.setOnLongClickListener(new View.OnLongClickListener() {
          @Override
          public boolean onLongClick(View v) {
            _draggingPos = pos;
            v.startDragAndDrop(android.content.ClipData.newPlainText("", ""), new View.DragShadowBuilder(v), null, 0);
            update_view();
            return true;
          }
        });

        // Remove folder button
        v.findViewById(R.id.clipboard_folder_remove).setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            confirm_remove(pos);
          }
        });

      } else {
        final Snippet snippet = (Snippet) item;
        TextView tv = (TextView) v.findViewById(R.id.clipboard_pin_text);
        tv.setText(snippet.content);
        tv.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            paste_entry(snippet);
          }
        });

        // Edit snippet
        v.findViewById(R.id.clipboard_pin_edit).setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            android.content.Intent intent = new android.content.Intent(getContext(), SnippetCreationActivity.class);
            intent.putExtra(SnippetCreationActivity.EXTRA_UUID, item.uuid);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
          }
        });

        View moveBtn = v.findViewById(R.id.clipboard_pin_move);
        moveBtn.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            move_entry(item.uuid);
          }
        });
        moveBtn.setOnLongClickListener(new View.OnLongClickListener() {
          @Override
          public boolean onLongClick(View v) {
            _draggingPos = pos;
            v.startDragAndDrop(android.content.ClipData.newPlainText("", ""), new View.DragShadowBuilder(v), null, 0);
            update_view();
            return true;
          }
        });

        v.findViewById(R.id.clipboard_pin_remove).setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                confirm_remove(pos);
              }
            });

      }

      if (pos == _draggingPos) {
        v.setAlpha(0.3f);
      } else {
        v.setAlpha(1.0f);
      }

      v.setOnDragListener(new View.OnDragListener() {

        @Override
        public boolean onDrag(View v, android.view.DragEvent event) {
          switch (event.getAction()) {
            case android.view.DragEvent.ACTION_DRAG_STARTED:
              return true;
            case android.view.DragEvent.ACTION_DRAG_ENTERED:
              if (_draggingPos != -1 && _draggingPos != pos) {
                v.setBackgroundColor(0x40888888);
              }
              return true;
            case android.view.DragEvent.ACTION_DRAG_EXITED:
              v.setBackground(null);
              return true;
            case android.view.DragEvent.ACTION_DROP:
              v.setBackground(null);
              if (_draggingPos != -1 && _draggingPos != pos) {
                reorder_entry(_draggingPos, pos);
                _draggingPos = -1;
              }
              return true;
            case android.view.DragEvent.ACTION_DRAG_ENDED:
              v.setBackground(null);
              if (_draggingPos != -1) {
                _draggingPos = -1;
                post(new Runnable() {
                  @Override
                  public void run() {
                    update_view();
                  }
                });
              }
              return true;
          }
          return false;
        }

      });

      return v;
    }

    void confirm_remove(final int pos) {
      AlertDialog d = new AlertDialog.Builder(getContext())
          .setTitle(R.string.clipboard_remove_confirm)
          .setPositiveButton(R.string.clipboard_remove_confirmed,
              new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface _dialog, int _which) {
                  remove_entry(pos);
                }
              })
          .setNegativeButton(android.R.string.cancel, null)
          .create();
      Utils.show_dialog_on_ime(d, getWindowToken());
    }

  }

  private android.app.Activity getActivity() {
    Context context = getContext();
    while (context instanceof android.content.ContextWrapper) {
      if (context instanceof android.app.Activity) {
        return (android.app.Activity) context;
      }
      context = ((android.content.ContextWrapper) context).getBaseContext();
    }
    return null;
  }
}
