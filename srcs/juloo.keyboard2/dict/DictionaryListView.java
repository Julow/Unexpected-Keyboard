package juloo.keyboard2.dict;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import juloo.cdict.Cdict;
import juloo.keyboard2.Config;
import juloo.keyboard2.DeviceLocales;
import juloo.keyboard2.Logs;
import juloo.keyboard2.R;
import juloo.keyboard2.Utils;

public class DictionaryListView extends LinearLayout
{
  List<DictView> _dict_views;
  Dictionaries _dictionaries;
  Set<String> _pending = new HashSet();

  public DictionaryListView(Context ctx, AttributeSet attrs)
  {
    super(ctx, attrs);
    setOrientation(LinearLayout.VERTICAL);
    _dictionaries = new Dictionaries(ctx);
    inflate_views(ctx);
  }

  void inflate_views(Context ctx)
  {
    DeviceLocales locales = DeviceLocales.load(ctx);
    SupportedDictionaries ds = new SupportedDictionaries(ctx.getResources());
    DownloadBtnListener listener = this.new DownloadBtnListener();
    _dict_views = new ArrayList<DictView>();
    for (DeviceLocales.Loc loc : locales.installed)
    {
      int idx = (loc.dictionary != null) ? ds.find(loc.dictionary) : -1;
      if (idx >= 0)
      {
        DictView dv = new DictView(ctx, ds, idx, listener);
        addView(dv.view);
        _dict_views.add(dv);
      }
    }
    refresh();
  }

  /** Update the "installed" status of item views. Meaning whether the
      "download" or "delete" button is shown. */
  void refresh()
  {
    Set<String> installed = _dictionaries.get_installed();
    for (DictView d : _dict_views)
      d.refresh(installed, _pending);
  }

  void toggle_installed(String dict_name)
  {
    run_dictionary_action(dict_name, new Runnable()
        {
          public void run()
          {
            if (_dictionaries.get_installed().contains(dict_name))
              _dictionaries.uninstall(dict_name);
            else if (install_dictionary_from_internet(dict_name))
              post_toast(R.string.dictionaries_download_success);
            else
              post_toast(R.string.dictionaries_download_failed);
          }
        });
  }

  /** Run action [r] for dictionary [name] if no action is already running for
      that dictionary. Calls [refresh] after the action completed. */
  void run_dictionary_action(String name, Runnable r)
  {
    if (_pending.contains(name))
      return;
    _pending.add(name);
    (new Thread()
     {
       public void run()
       {
         r.run();
         post(new Runnable()
             {
               public void run()
               {
                 _pending.remove(name);
                 refresh();
               }
             });
       }
     }).start();
    refresh();
  }

  final class DownloadBtnListener implements View.OnClickListener
  {
    @Override
    public void onClick(View v)
    {
      for (DictView dv : _dict_views)
        if (dv.download_button == v)
          toggle_installed(dv.dict_name);
    }
  }

  static final class DictView
  {
    public final View view;
    public final String dict_name;
    public final View download_button;

    public DictView(Context ctx, SupportedDictionaries ds, int dict_index,
        DownloadBtnListener on_click)
    {
      view = View.inflate(ctx, R.layout.dictionary_download_item, null);
      dict_name = ds.dict_name(dict_index);
      float size_mb = ds.size(dict_index) / 1048576.f;
      ((TextView)view.findViewById(R.id.dictionary_download_locale))
        .setText(ds.display_name(dict_index));
      ((TextView)view.findViewById(R.id.dictionary_download_size))
        .setText(NumberFormat.getInstance().format(size_mb) + "MB");
      download_button = view.findViewById(R.id.dictionary_download_button);
      download_button.setOnClickListener(on_click);
    }

    public void refresh(Set<String> installed, Set<String> pending)
    {
      download_button.setBackgroundResource(installed.contains(dict_name)
          ? R.drawable.ic_delete : R.drawable.ic_download);
      download_button.setVisibility(pending.contains(dict_name)
          ? View.GONE : View.VISIBLE);
    }
  }

  static final String DICT_REPO_URL =
    "https://github.com/Julow/Unexpected-Keyboard-dictionaries/raw/refs/heads/main";

  static URL url_of_dictionary(String dict_name)
      throws MalformedURLException
  {
    int format_version = 0;
    return new URL(DICT_REPO_URL + "/v" + format_version + "/" + dict_name
        + ".dict");
  }

  /** Returns [true] on success. */
  boolean install_dictionary_from_internet(String dict_name)
  {
    try
    {
      // Remote files are compressed with gzip at rest. Do not use server side
      // compression and force decompression.
      URLConnection con = url_of_dictionary(dict_name).openConnection();
      con.setRequestProperty("Accept-Encoding", "identity");
      byte[] data = Utils.read_all_bytes(new GZIPInputStream(con.getInputStream()));
      Cdict.of_bytes(data); // Check that the dictionary can load.
      _dictionaries.install(dict_name, data);
      return true;
    }
    catch (Exception e)
    {
      Logs.exn("Failed to install dictionary from the internet", e);
      return false;
    }
  }

  void post_toast(int msg_id)
  {
    post(new Runnable()
        {
          public void run()
          {
            Toast.makeText(getContext(), msg_id, Toast.LENGTH_SHORT).show();
          }
        });
  }
}
