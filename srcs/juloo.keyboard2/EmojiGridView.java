package juloo.keyboard2;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EmojiGridView extends GridView
  implements GridView.OnItemClickListener
{
  public static final int GROUP_LAST_USE = -1;

  private static final String LAST_USE_PREF = "emoji_last_use";

  private List<Emoji> _emojiArray;
  private HashMap<Emoji, Integer> _lastUsed;

  /*
   ** TODO: adapt column width and emoji size
   ** TODO: use ArraySet instead of Emoji[]
   */
  public EmojiGridView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    Emoji.init(context.getResources());
    migrateOldPrefs(); // TODO: Remove at some point in future
    setOnItemClickListener(this);
    loadLastUsed();
    setEmojiGroup((_lastUsed.size() == 0) ? 0 : GROUP_LAST_USE);
  }

  public void setEmojiGroup(int group)
  {
    _emojiArray = (group == GROUP_LAST_USE) ? getLastEmojis() : Emoji.getEmojisByGroup(group);
    setAdapter(new EmojiViewAdpater(getContext(), _emojiArray));
  }

  public void onItemClick(AdapterView<?> parent, View v, int pos, long id)
  {
    Config config = Config.globalConfig();
    Integer used = _lastUsed.get(_emojiArray.get(pos));
    _lastUsed.put(_emojiArray.get(pos), (used == null) ? 1 : used.intValue() + 1);
    config.handler.key_up(_emojiArray.get(pos).kv(), Pointers.Modifiers.EMPTY);
    saveLastUsed(); // TODO: opti
  }

  private List<Emoji> getLastEmojis()
  {
    List<Emoji> list = new ArrayList<>(_lastUsed.keySet());
    Collections.sort(list, new Comparator<Emoji>()
        {
          public int compare(Emoji a, Emoji b)
          {
            return _lastUsed.get(b) - _lastUsed.get(a);
          }
        });
    return list;
  }

  private void saveLastUsed()
  {
    SharedPreferences.Editor edit;
    try { edit = emojiSharedPreferences().edit(); }
    catch (Exception _e) { return; }
    HashSet<String> set = new HashSet<String>();
    for (Emoji emoji : _lastUsed.keySet())
      set.add(String.valueOf(_lastUsed.get(emoji)) + "-" + emoji.kv().getString());
    edit.putStringSet(LAST_USE_PREF, set);
    edit.apply();
  }

  private void loadLastUsed()
  {
    _lastUsed = new HashMap<Emoji, Integer>();
    SharedPreferences prefs;
    // Storage might not be available (eg. the device is locked), avoid
    // crashing.
    try { prefs = emojiSharedPreferences(); }
    catch (Exception _e) { return; }
    Set<String> lastUseSet = prefs.getStringSet(LAST_USE_PREF, null);
    if (lastUseSet != null)
      for (String emojiData : lastUseSet)
      {
        String[] data = emojiData.split("-", 2);
        Emoji emoji;
        if (data.length != 2)
          continue ;
        emoji = Emoji.getEmojiByString(data[1]);
        if (emoji == null)
          continue ;
        _lastUsed.put(emoji, Integer.valueOf(data[0]));
      }
  }

  SharedPreferences emojiSharedPreferences()
  {
    return getContext().getSharedPreferences("emoji_last_use", Context.MODE_PRIVATE);
  }

  private void migrateOldPrefs()
  {
    final String MIGRATION_CHECK_KEY = "MIGRATION_COMPLETE";

    SharedPreferences prefs;
    try { prefs = emojiSharedPreferences(); }
    catch (Exception e) { return; }

    Set<String> lastUsed = prefs.getStringSet(LAST_USE_PREF, null);
    if (lastUsed != null && !prefs.getBoolean(MIGRATION_CHECK_KEY, false))
    {
      SharedPreferences.Editor edit = prefs.edit();
      edit.clear();

      Set<String> lastUsedNew = new HashSet<>();
      for (String entry : lastUsed)
      {
        String[] data = entry.split("-", 2);
        try
        {
          lastUsedNew.add(Integer.parseInt(data[0]) + "-" + Emoji.mapOldNameToValue(data[1]));
        }
        catch (IllegalArgumentException ignored) {}
      }
      edit.putStringSet(LAST_USE_PREF, lastUsedNew);

      edit.putBoolean(MIGRATION_CHECK_KEY, true);
      edit.apply();
    }
  }

  static class EmojiView extends TextView
  {
    public EmojiView(Context context)
    {
      super(context);
      Typeface typeface = context.getResources().getFont(R.font.noto_color_emoji_compat);
      this.setTypeface(typeface);
    }

    public void setEmoji(Emoji emoji)
    {
      setText(emoji.kv().getString());
    }
  }

  static class EmojiViewAdpater extends BaseAdapter
  {
    Context _button_context;

    List<Emoji> _emojiArray;

    public EmojiViewAdpater(Context context, List<Emoji> emojiArray)
    {
      _button_context = new ContextThemeWrapper(context, R.style.emojiGridButton);
      _emojiArray = emojiArray;
    }

    public int getCount()
    {
      if (_emojiArray == null)
        return (0);
      return (_emojiArray.size());
    }

    public Object getItem(int pos)
    {
      return (_emojiArray.get(pos));
    }

    public long getItemId(int pos)
    {
      return (pos);
    }

    public View getView(int pos, View convertView, ViewGroup parent)
    {
      EmojiView view = (EmojiView)convertView;

      if (view == null)
        view = new EmojiView(_button_context);
      view.setEmoji(_emojiArray.get(pos));
      return view;
    }
  }
}
