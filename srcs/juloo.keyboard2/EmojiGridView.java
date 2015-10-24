package juloo.keyboard2;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

public class EmojiGridView extends GridView
	implements GridView.OnItemClickListener
{
	public static final int			COLUMN_WIDTH = 192;
	public static final float		EMOJI_SIZE = 32.f;

	private int				_emojiType = Emoji.TYPE_EMOTICONS;

	/*
	** TODO: save last emoji type
	** TODO: adapt column width and emoji size
	*/
	public EmojiGridView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setOnItemClickListener(this);
		setColumnWidth(COLUMN_WIDTH);
		setEmojiType(Emoji.TYPE_EMOTICONS);
	}

	/*
	** TODO: type (-1) for lastest used
	*/
	public void			setEmojiType(int type)
	{
		_emojiType = type;
		setAdapter(new EmojiViewAdpater((Keyboard2)getContext(), type));
	}

	public void			onItemClick(AdapterView<?> parent, View v, int pos, long id)
	{
		Keyboard2			main = (Keyboard2)getContext();

		main.handleKeyUp(Emoji.getEmojisByType(_emojiType)[pos], 0);
	}

	@Override
	public void			onMeasure(int wSpec, int hSpec)
	{
		super.onMeasure(wSpec, hSpec);
		setNumColumns(getMeasuredWidth() / COLUMN_WIDTH);
	}

	private static class EmojiView extends TextView
	{
		private static ViewGroup.LayoutParams	_layoutParams = null;

		public EmojiView(Keyboard2 context)
		{
			super(context);
			setTextSize(EMOJI_SIZE);
			setGravity(Gravity.CENTER);
			if (_layoutParams == null)
				_layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
			setLayoutParams(_layoutParams);
		}

		public void			setEmoji(Emoji emoji)
		{
			setText(emoji.getSymbol(0));
		}
	}

	private static class EmojiViewAdpater extends BaseAdapter
	{
		private Keyboard2	_main;

		private Emoji[]		_emojiSet = null;

		public EmojiViewAdpater(Keyboard2 main, int type)
		{
			_main = main;
			_emojiSet = Emoji.getEmojisByType(type);
		}

		public int			getCount()
		{
			if (_emojiSet == null)
				return (0);
			return (_emojiSet.length);
		}

		public Object		getItem(int pos)
		{
			return (_emojiSet[pos]);
		}

		public long			getItemId(int pos)
		{
			return (pos);
		}

		public View			getView(int pos, View convertView, ViewGroup parent)
		{
			EmojiView			view = (EmojiView)convertView;

			if (view == null)
				view = new EmojiView(_main);
			view.setEmoji(_emojiSet[pos]);
			return (view);
		}
	}
}
