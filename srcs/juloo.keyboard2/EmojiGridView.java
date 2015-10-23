package juloo.keyboard2;

import android.graphics.Typeface;
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
	public static final int			EMOJI_PANE_HEIGHT = 720;
	public static final int			EMOJI_PANE_BG = 0xFF191919;
	public static final float		EMOJI_SIZE = 32.f;

	public EmojiGridView(Keyboard2 context)
	{
		super(context);
		setOnItemClickListener(this);
		EmojiViewAdpater adpater = new EmojiViewAdpater(context);
		adpater.setEmojiSet(Emoji.getEmojiByType(Emoji.TYPE_EMOTICONS));
		setColumnWidth(COLUMN_WIDTH);
		setBackgroundColor(EMOJI_PANE_BG);
		setAdapter(adpater);
	}

	public void			onItemClick(AdapterView<?> parent, View v, int pos, long id)
	{
		System.out.println("Lol emoji: " + Emoji.getEmojiByType(Emoji.TYPE_EMOTICONS)[pos].getName());
	}

	@Override
	public void			onMeasure(int wSpec, int hSpec)
	{
		super.onMeasure(wSpec, hSpec);
		setNumColumns(getMeasuredWidth() / COLUMN_WIDTH);
		setMeasuredDimension(wSpec, EMOJI_PANE_HEIGHT);
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
			setText(emoji.getBytecode());
		}
	}

	private static class EmojiViewAdpater extends BaseAdapter
	{
		private Keyboard2	_main;

		private Emoji[]		_emojiSet = null;

		public EmojiViewAdpater(Keyboard2 main)
		{
			_main = main;
		}

		public void			setEmojiSet(Emoji[] set)
		{
			_emojiSet = set;
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
