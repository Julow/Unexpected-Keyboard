package juloo.keyboard2;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import java.util.HashMap;

public class EmojiTypeButton extends Button
	implements View.OnTouchListener
{
	private int		_emojiType;

	public EmojiTypeButton(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		EmojiTypeDef		def = _types.get(attrs.getAttributeValue(null, "emoji_type"));

		_emojiType = def.getTypeId();
		setText(def.getButtonText());
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

	private static HashMap<String, EmojiTypeDef>	_types = new HashMap<String, EmojiTypeDef>();

	static
	{
		_types.put("LAST_USE",	new EmojiTypeDef(EmojiGridView.TYPE_LAST_USE,	"\uD83D\uDD59"));
		_types.put("EMOTICONS",	new EmojiTypeDef(Emoji.TYPE_EMOTICONS,			"\uD83D\uDE03"));
		_types.put("TRANSPORT",	new EmojiTypeDef(Emoji.TYPE_TRANSPORT,			"\uD83D\uDE8C"));
		_types.put("FOOD",		new EmojiTypeDef(Emoji.TYPE_FOOD,				"\uD83C\uDF55"));
		_types.put("NATURE",	new EmojiTypeDef(Emoji.TYPE_NATURE,				"\uD83C\uDF37"));
		_types.put("FEST",		new EmojiTypeDef(Emoji.TYPE_FEST,				"\uD83C\uDF88"));
		_types.put("ANIMAL",	new EmojiTypeDef(Emoji.TYPE_ANIMAL,				"\uD83D\uDC31"));
		_types.put("HUMAN",		new EmojiTypeDef(Emoji.TYPE_HUMAN,				"\uD83D\uDC9C"));
		_types.put("UNCATEGORIZED", new EmojiTypeDef(Emoji.TYPE_UNCATEGORIZED,	"\uD83D\uDCA5"));
		_types.put("DINGBATS",	new EmojiTypeDef(Emoji.TYPE_DINGBATS,			"\u2705"));
	}

	private static class EmojiTypeDef
	{
		private int		_typeId;
		private String	_buttonText;

		public EmojiTypeDef(int typeId, String buttonText)
		{
			_typeId = typeId;
			_buttonText = buttonText;
		}

		public int		getTypeId()
		{
			return (_typeId);
		}

		public String	getButtonText()
		{
			return (_buttonText);
		}
	}
}
