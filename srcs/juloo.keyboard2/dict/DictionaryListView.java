package juloo.keyboard2.dict;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import juloo.cdict.Cdict;
import juloo.keyboard2.DeviceLocales;
import juloo.keyboard2.Logs;
import juloo.keyboard2.R;
import juloo.keyboard2.Utils;

public class DictionaryListView extends LinearLayout
{
  List<DictView> _dict_views;
  Dictionaries _dictionaries;
  Set<String> _pending = new HashSet<String>();

  public DictionaryListView(Context ctx, AttributeSet attrs)
  {
    super(ctx, attrs);
    setOrientation(LinearLayout.VERTICAL);
    _dictionaries = Dictionaries.instance(ctx);
    _dict_views = new ArrayList<DictView>();
    boolean device_locales =
      attrs.getAttributeBooleanValue(null, "device_locales", true);
    if (device_locales)
      inflate_views_device_locales(ctx);
    else
      inflate_views_all(ctx);
    refresh();
  }

  void inflate_views_device_locales(Context ctx)
  {
    SupportedDictionaries ds = SupportedDictionaries.get(ctx.getResources());
    DeviceLocales locales = DeviceLocales.load(ctx);
    for (DeviceLocales.Loc loc : locales.installed)
    {
      if (loc.dictionary != null)
      {
        int idx = ds.find(loc.dictionary);
        if (idx >= 0)
          inflate_item(ctx, ds, idx);
      }
    }
  }

  void inflate_views_all(Context ctx)
  {
    SupportedDictionaries ds = SupportedDictionaries.get(ctx.getResources());
    for (int i = 0; i < ds.length(); i++)
      inflate_item(ctx, ds, i);
  }

  void inflate_item(Context ctx, SupportedDictionaries ds, int i)
  {
    View v = LayoutInflater.from(ctx)
      .inflate(R.layout.dictionary_download_item, this, false);
    _dict_views.add(this.new DictView(v, ds, i));
    addView(v);
  }

  /** Update the "installed" status of item views. Meaning whether the
      "download" or "delete" button is shown. */
  void refresh()
  {
    Set<String> installed = _dictionaries.get_installed();
    for (DictView d : _dict_views)
      d.refresh(installed);
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

  final class DictView implements View.OnClickListener
  {
    public final String dict_name;
    public final ImageView download_button;

    public DictView(View view, SupportedDictionaries ds, int dict_index)
    {
      dict_name = ds.dict_name(dict_index);
      float size_mb = ds.size(dict_index) / 1048576.f;
      ((TextView)view.findViewById(R.id.dictionary_download_locale))
        .setText(ds.display_name(dict_index));
      ((TextView)view.findViewById(R.id.dictionary_download_size))
        .setText(NumberFormat.getInstance().format(size_mb) + "MB");
      download_button =
        (ImageView)view.findViewById(R.id.dictionary_download_button);
      download_button.setOnClickListener(this);
    }

    public void refresh(Set<String> installed)
    {
      int res =
        _pending.contains(dict_name) ? 0 :
        installed.contains(dict_name) ? R.drawable.ic_delete :
        R.drawable.ic_download;
      download_button.setImageResource(res);
    }

    @Override
    public void onClick(View v)
    {
      toggle_installed(dict_name);
    }
  }

  static final String DICT_REPO_URL =
    "https://raw.githubusercontent.com/Julow/Unexpected-Keyboard-dictionaries/refs/heads/main";

  /** Where to find a dictionary that the repository above doesn't have yet.
      The Japanese dictionary is not published in the repository, it is hosted
      with the [ja-dict-v1] pre-release on this fork. Remove the entry once
      `v<format version>/ja.dict` is published in the repository. */
  static final Map<String, String> FALLBACK_URLS = new HashMap<String, String>();
  static
  {
    FALLBACK_URLS.put("ja",
        "https://github.com/bqv/Unexpected-Keyboard/releases/download/ja-dict-v1/ja.dict.gz");
  }

  /** The URLs to try, in order, to download [dict_name]. The dictionary files
      in the repository are stored gzipped. */
  static URL[] urls_of_dictionary(String dict_name)
      throws MalformedURLException
  {
    int format_version = Cdict.format_version();
    List<URL> urls = new ArrayList<URL>();
    urls.add(new URL(DICT_REPO_URL + "/v" + format_version + "/" + dict_name
        + ".dict"));
    String fallback = FALLBACK_URLS.get(dict_name);
    if (fallback != null)
      urls.add(new URL(fallback));
    return urls.toArray(new URL[urls.size()]);
  }

  /** Returns [true] on success. */
  boolean install_dictionary_from_internet(String dict_name)
  {
    try
    {
      byte[] data = download_dictionary(urls_of_dictionary(dict_name));
      Cdict.of_bytes(data); // Check that the dictionary can load.
      _dictionaries.install(dict_name, data);
      return true;
    }
    catch (Exception e)
    {
      Logs.exn("", e);
      return false;
    }
  }

  /** Download the first reachable URL in [urls] and return its decompressed
      content. Remote files are compressed with gzip at rest, do not ask the
      server for its own compression. Throw the last error if all fail. */
  static byte[] download_dictionary(URL[] urls) throws IOException
  {
    IOException error = null;
    for (URL url : urls)
    {
      try
      {
        URLConnection con = url.openConnection();
        con.setRequestProperty("Accept-Encoding", "identity");
        return Utils.read_all_bytes(new GZIPInputStream(con.getInputStream()));
      }
      catch (IOException e)
      {
        error = e;
      }
    }
    if (error == null)
      throw new IOException("No URL to download the dictionary from");
    throw error;
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
