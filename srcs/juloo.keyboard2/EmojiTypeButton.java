package juloo.keyboard2;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

public class EmojiTypeButton extends Button
	implements View.OnTouchListener
{
	private int		_emojiType;

	public EmojiTypeButton(Context context, AttributeSet attrs)
	{
		super(context);
		setBackgroundColor(getResources().getColor(R.color.emoji_button_bg));
		setOnTouchListener(this);
		_emojiType = getTypeByString(attrs.getAttributeValue(null, "emoji_type"));
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
		if (str.equals("LAST_USE"))
			return (EmojiGridView.TYPE_LAST_USE);
		if (str.equals("EMOTICONS"))
			return (Emoji.TYPE_EMOTICONS);
		if (str.equals("TRANSPORT"))
			return (Emoji.TYPE_TRANSPORT);
		if (str.equals("FOOD"))
			return (Emoji.TYPE_FOOD);
		if (str.equals("NATURE"))
			return (Emoji.TYPE_NATURE);
		if (str.equals("FEST"))
			return (Emoji.TYPE_FEST);
		if (str.equals("ANIMAL"))
			return (Emoji.TYPE_ANIMAL);
		if (str.equals("HUMAN"))
			return (Emoji.TYPE_HUMAN);
		if (str.equals("UNCATEGORIZED"))
			return (Emoji.TYPE_UNCATEGORIZED);
		if (str.equals("DINGBATS"))
			return (Emoji.TYPE_DINGBATS);
		return (-1);
	}
}
