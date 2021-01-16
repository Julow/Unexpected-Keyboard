package juloo.keyboard2;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/* Emoji "types" are groups. This class is misnamed. */

public class EmojiTypeButton extends Button
	implements View.OnTouchListener
{
	private int		_emojiType;

  static private final int DEFAULT_GROUP = 0;

	public EmojiTypeButton(Context context, int group_id, String symbol)
	{
		super(new ContextThemeWrapper(context, R.style.emojiTypeButton), null, 0);
    _emojiType = group_id;
		setText(symbol);
		setOnTouchListener(this);
	}

	public boolean			onTouch(View view, MotionEvent event)
	{
		EmojiGridView	emojiGrid;

		if (event.getAction() != MotionEvent.ACTION_DOWN)
			return (false);
		emojiGrid = (EmojiGridView)((ViewGroup)(getParent().getParent())).findViewById(R.id.emoji_grid);
		emojiGrid.setEmojiGroup(_emojiType);
		return (true);
	}
}
