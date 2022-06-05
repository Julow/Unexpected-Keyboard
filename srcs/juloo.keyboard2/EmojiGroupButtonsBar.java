package juloo.keyboard2;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

public class EmojiGroupButtonsBar extends LinearLayout
{
  public EmojiGroupButtonsBar(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    Emoji.init(context.getResources());
    add_group(EmojiGridView.GROUP_LAST_USE, "\uD83D\uDD59");
    for (int i = 0; i < Emoji.num_groups; i++)
    {
      Emoji first = Emoji.getEmojisByGroup(i)[0];
      add_group(i, first.kv().getString());
    }
  }

  private void add_group(int id, String symbol)
  {
    addView(new EmojiTypeButton(getContext(), id, symbol), new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1.f));
  }
}
