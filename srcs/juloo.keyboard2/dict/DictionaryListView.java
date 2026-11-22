package juloo.keyboard2.dict;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import juloo.keyboard2.DeviceLocales;
import juloo.keyboard2.R;

public class DictionaryListView extends LinearLayout
{
  Content _content;
  List<DictView> _dict_views;

  public DictionaryListView(Context ctx, AttributeSet attrs)
  {
    super(ctx, attrs);
    setOrientation(LinearLayout.VERTICAL);
    _dict_views = new ArrayList<DictView>();
    _content = content_of_attrs(attrs);
    switch (_content)
    {
      case INSTALLED: break; // Inflated on [refresh]
      case LOCALES: inflate_views_device_locales(ctx); break;
      case ALL: inflate_views_all(ctx); break;
    }
  }

  DictionariesActivity get_activity()
  {
    return (DictionariesActivity)getContext();
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

  void inflate_views_installed(Context ctx, Set<String> installed)
  {
    removeAllViews();
    _dict_views.clear();
    SupportedDictionaries ds = SupportedDictionaries.get(ctx.getResources());
    for (String name : installed)
      inflate_item(ctx, ds, ds.find(name));
  }

  void inflate_item(Context ctx, SupportedDictionaries ds, int i)
  {
    View v = LayoutInflater.from(ctx)
      .inflate(R.layout.dictionary_download_item, this, false);
    _dict_views.add(this.new DictView(v, ds, i));
    addView(v);
  }

  /** Update the "installed" status of item views. */
  void refresh(Set<String> installed)
  {
    switch (_content)
    {
      case INSTALLED:
        inflate_views_installed(getContext(), installed);
        break;
      case LOCALES:
      case ALL:
        for (DictView d : _dict_views)
          d.hide_if_installed(installed);
        break;
    }
  }

  final class DictView implements View.OnClickListener
  {
    public final String dict_name;
    public final View view;

    public DictView(View view, SupportedDictionaries ds, int dict_index)
    {
      this.view = view;
      dict_name = ds.dict_name(dict_index);
      float size_mb = ds.size(dict_index) / 1048576.f;
      ((TextView)view.findViewById(R.id.dictionary_download_locale))
        .setText(ds.display_name(dict_index));
      ((TextView)view.findViewById(R.id.dictionary_download_size))
        .setText(NumberFormat.getInstance().format(size_mb) + "MB");
      ImageView button =
        (ImageView)view.findViewById(R.id.dictionary_download_button);
      button.setImageResource((_content == Content.INSTALLED) ?
          R.drawable.ic_delete : R.drawable.ic_download);
      button.setOnClickListener(this);
    }

    public void hide_if_installed(Set<String> installed)
    {
      view.setVisibility(installed.contains(dict_name) ?
          View.GONE : View.VISIBLE);
    }

    @Override
    public void onClick(View v)
    {
      get_activity().toggle_installed(dict_name);
    }
  }

  enum Content
  {
    INSTALLED,
    LOCALES,
    ALL
  }

  static Content content_of_attrs(AttributeSet attrs)
  {
    switch (attrs.getAttributeValue(null, "content"))
    {
      case "installed": return Content.INSTALLED;
      case "locales": return Content.LOCALES;
      default:
      case "all": return Content.ALL;
    }
  }
}
