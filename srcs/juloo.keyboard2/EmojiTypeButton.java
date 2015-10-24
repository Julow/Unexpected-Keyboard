package juloo.keyboard2;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class EmojiTypeButton extends Button
	implements View.OnTouchListener
{
	private int		_emojiType;

	public EmojiTypeButton(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		_emojiType = getTypeByString(attrs.getAttributeValue(null, "emoji_type"));
		setOnTouchListener(this);
	}

	public boolean			onTouch(View view, MotionEvent event)
	{
		EmojiGridView	emojiGrid;

		if (event.getAction() != MotionEvent.ACTION_DOWN)
			return (false);
		emojiGrid = (EmojiGridView)((ViewGroup)(getParent().getParent())).findViewById(R.id.emoji_grid);
		emojiGrid.setEmojiType(_emojiType);
		return (true);
	}

	public static int		getTypeByString(String str)
	{
		// caca
		if (str.equals("EMOTICONS"))
			return (Emoji.TYPE_EMOTICONS);
		if (str.equals("DINGBATS"))
			return (Emoji.TYPE_DINGBATS);
		if (str.equals("TRANSPORT"))
			return (Emoji.TYPE_TRANSPORT);
		if (str.equals("UNCATEGORIZED"))
			return (Emoji.TYPE_UNCATEGORIZED);
		if (str.equals("ENCLOSED_CHARACTERS"))
			return (Emoji.TYPE_ENCLOSED_CHARACTERS);
		return (-1);
	}
}
