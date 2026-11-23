package juloo.keyboard2;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EmojiGridView extends GridView
  implements GridView.OnItemClickListener, GridView.OnItemLongClickListener
{
  public static final int GROUP_LAST_USE = -1;

  private static final String LAST_USE_PREF = "emoji_last_use";

  /** For emojis that only have skin tone variants, the base emoji and the 5
      uniform skin tones fit on the first row. */
  private static final int SKIN_TONE_POPUP_COLUMNS = 6;

  private List<Emoji> _emojiArray;
  private HashMap<Emoji, Integer> _lastUsed;
  private PopupWindow _variantsPopup = null;

  /*
   ** TODO: adapt column width and emoji size
   ** TODO: use ArraySet instead of Emoji[]
   */
  public EmojiGridView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    Emoji.init(context.getResources());
    setOnItemClickListener(this);
    setOnItemLongClickListener(this);
    loadLastUsed();
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    setEmojiGroup(getDefaultEmojiGroup());
  }

  @Override
  protected void onDetachedFromWindow()
  {
    if (_variantsPopup != null)
      _variantsPopup.dismiss();
    super.onDetachedFromWindow();
  }

  public int getDefaultEmojiGroup()
  {
    return (_lastUsed.isEmpty()) ? 0 : GROUP_LAST_USE;
  }

  public void setEmojiGroup(int group)
  {
    _emojiArray = (group == GROUP_LAST_USE) ? getLastEmojis() : withDefaultVariant(Emoji.getEmojisByGroup(group));
    setAdapter(new EmojiViewAdpater(getContext(), _emojiArray));
  }

  public void onItemClick(AdapterView<?> parent, View v, int pos, long id)
  {
    sendEmoji(_emojiArray.get(pos));
  }

  public boolean onItemLongClick(AdapterView<?> parent, View v, int pos, long id)
  {
    Emoji emoji = _emojiArray.get(pos);
    if (!emoji.hasVariants())
      return false;
    showVariantsPopup(v, emoji);
    return true;
  }

  private void sendEmoji(Emoji emoji)
  {
    Config config = Config.globalConfig();
    Integer used = _lastUsed.get(emoji);
    _lastUsed.put(emoji, (used == null) ? 1 : used.intValue() + 1);
    config.handler.key_up(emoji.kv(), Pointers.Modifiers.EMPTY);
    saveLastUsed(); // TODO: opti
  }

  private static List<Emoji> withDefaultVariant(List<Emoji> emojis)
  {
    Config config = Config.globalConfig();
    List<Emoji> variants = new ArrayList<>(emojis.size());
    for (Emoji emoji : emojis)
      variants.add(emoji.withSkinToneAndGender(config.emoji_skin_tone, config.emoji_gender));
    return variants;
  }

  /** Cells have the same size as [anchor]. The popup scrolls if it is taller
      than the grid. */
  private void showVariantsPopup(View anchor, Emoji emoji)
  {
    if (_variantsPopup != null)
      _variantsPopup.dismiss();
    List<Emoji> variants = emoji.variants();
    GridView grid = (GridView)View.inflate(getContext(), R.layout.emoji_variants_popup, null);
    int rowWidth = (emoji.rowWidth() > 1) ? emoji.rowWidth() : SKIN_TONE_POPUP_COLUMNS;
    int columns = Math.min(rowWidth, getNumColumns());
    int rows = (variants.size() + columns - 1) / columns;
    PopupWindow popup = new PopupWindow(grid, columns * anchor.getWidth(),
        Math.min(rows * anchor.getHeight(), getHeight()));
    grid.setNumColumns(columns);
    grid.setAdapter(new EmojiViewAdpater(getContext(), variants));
    grid.setOnItemClickListener((parent, v, pos, id) -> {
      sendEmoji(variants.get(pos));
      popup.dismiss();
    });
    // A background is required for outside touches to dismiss the popup.
    popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    popup.setOutsideTouchable(true);
    popup.showAsDropDown(anchor);
    _variantsPopup = popup;
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

  static class EmojiView extends TextView
  {
    public EmojiView(Context context)
    {
      super(context);
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
