package juloo.keyboard2;

import android.content.Context;
import android.util.AttributeSet;

public class EmojiBottomRow extends Keyboard2View
{
  public EmojiBottomRow(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    KeyboardData kw = KeyboardData.load(getResources(), R.xml.emoji_bottom_row);
    setKeyboard(kw);
  }
}
