package juloo.keyboard2;

import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import java.util.HashMap;

class KeyValue
{
	public static final int		EVENT_NONE = -1;
	public static final int		EVENT_CONFIG = -2;
	public static final int		EVENT_SWITCH_TEXT = -3;
	public static final int		EVENT_SWITCH_NUMERIC = -4;
	public static final int		EVENT_SWITCH_EMOJI = -5;
	public static final int		EVENT_SWITCH_BACK_EMOJI = -6;
	public static final char	CHAR_NONE = '\0';

	public static final int		FLAG_KEEP_ON = 1;
	public static final int		FLAG_LOCK = (1 << 1);
	public static final int		FLAG_CTRL = (1 << 2);
	public static final int		FLAG_SHIFT = (1 << 3);
	public static final int		FLAG_ALT = (1 << 4);
	public static final int		FLAG_NOREPEAT = (1 << 5);
	public static final int		FLAG_NOCHAR = (1 << 6);
	public static final int		FLAG_LOCKED = (1 << 8);

	public static final int		FLAG_KEY_FONT = (1 << 12);

	public static final int		FLAG_ACCENT1 = (1 << 16);
	public static final int		FLAG_ACCENT2 = (1 << 17);
	public static final int		FLAG_ACCENT3 = (1 << 18);
	public static final int		FLAG_ACCENT4 = (1 << 19);
	public static final int		FLAG_ACCENT5 = (1 << 20);
	public static final int		FLAG_ACCENT6 = (1 << 21);

	private String		_name;
	private String		_symbol;
	private char		_char;
	private int			_eventCode;
	private int			_flags;

	private int			_cacheFlags;
	private String		_cacheSymbol;

	public String		getName()
	{
		return (_name);
	}

	public String		getSymbol(int flags)
	{
		if (_symbol == null)
		{
			if (flags != _cacheFlags)
			{
				_cacheSymbol = String.valueOf(getChar(flags));
				_cacheFlags = flags;
			}
			return (_cacheSymbol);
		}
		return (_symbol);
	}

	public char			getChar(int flags)
	{
		if (flags != 0)
		{
			char c = _char;
			if ((flags & FLAG_SHIFT) != 0)
				c = Character.toUpperCase(_char);
			if ((flags & FLAG_ACCENT1) != 0)
				c = (char)KeyCharacterMap.getDeadChar('\u02CB', (int)c);
			if ((flags & FLAG_ACCENT2) != 0)
				c = (char)KeyCharacterMap.getDeadChar('\u00B4', (int)c);
			if ((flags & FLAG_ACCENT3) != 0)
				c = (char)KeyCharacterMap.getDeadChar('\u02C6', (int)c);
			if ((flags & FLAG_ACCENT4) != 0)
				c = (char)KeyCharacterMap.getDeadChar('\u02DC', (int)c);
			if ((flags & FLAG_ACCENT5) != 0)
				c = (char)KeyCharacterMap.getDeadChar('\u00B8', (int)c);
			if ((flags & FLAG_ACCENT6) != 0)
				c = (char)KeyCharacterMap.getDeadChar('\u00A8', (int)c);
			if (c != 0)
				return (c);
		}
		return (_char);
	}

	public int			getEventCode()
	{
		return (_eventCode);
	}

	public int			getFlags()
	{
		return (_flags);
	}

	private static HashMap<String, KeyValue> keys = new HashMap<String, KeyValue>();

	protected KeyValue(String name, String symbol, char c, int eventCode, int flags)
	{
		_name = name;
		_symbol = symbol;
		_char = c;
		_eventCode = eventCode;
		_flags = flags;
		_cacheFlags = -1;
		KeyValue.keys.put(name, this);
	}

	public static KeyValue	getKeyByName(String name)
	{
		return (KeyValue.keys.get(name));
	}

	static
	{
		String chars = "<>&\"'(-_)=°+"
			+ "~#{[|`\\^@]}"
			+ "^$*,;:!£%µ?./§";
		for (int i = 0; i < chars.length(); i++)
		{
			String key = chars.substring(i, i + 1);
			new KeyValue(key, key, key.charAt(0), EVENT_NONE, 0);
		}

		new KeyValue("shift",	"⇧",		CHAR_NONE,	EVENT_NONE,	FLAG_KEEP_ON | FLAG_NOCHAR | FLAG_NOREPEAT | FLAG_LOCK | FLAG_SHIFT);
		new KeyValue("ctrl",	"Ctrl",		CHAR_NONE,	EVENT_NONE,	FLAG_KEEP_ON | FLAG_NOCHAR | FLAG_NOREPEAT | FLAG_CTRL);
		new KeyValue("alt",		"Alt",		CHAR_NONE,	EVENT_NONE,	FLAG_KEEP_ON | FLAG_NOCHAR | FLAG_NOREPEAT | FLAG_ALT);
		new KeyValue("accent1",	"\u02CB",	CHAR_NONE,	EVENT_NONE,	FLAG_KEEP_ON | FLAG_NOCHAR | FLAG_NOREPEAT | FLAG_ACCENT1);
		new KeyValue("accent2",	"\u00B4",	CHAR_NONE,	EVENT_NONE,	FLAG_KEEP_ON | FLAG_NOCHAR | FLAG_NOREPEAT | FLAG_ACCENT2);
		new KeyValue("accent3",	"\u02C6",	CHAR_NONE,	EVENT_NONE,	FLAG_KEEP_ON | FLAG_NOCHAR | FLAG_NOREPEAT | FLAG_ACCENT3);
		new KeyValue("accent4",	"\u02DC",	CHAR_NONE,	EVENT_NONE,	FLAG_KEEP_ON | FLAG_NOCHAR | FLAG_NOREPEAT | FLAG_ACCENT4);
		new KeyValue("accent5",	"\u00B8",	CHAR_NONE,	EVENT_NONE,	FLAG_KEEP_ON | FLAG_NOCHAR | FLAG_NOREPEAT | FLAG_ACCENT5);
		new KeyValue("accent6",	"\u00A8",	CHAR_NONE,	EVENT_NONE,	FLAG_KEEP_ON | FLAG_NOCHAR | FLAG_NOREPEAT | FLAG_ACCENT6);

		new KeyValue("a",		null,	'a',		KeyEvent.KEYCODE_A,		0);
		new KeyValue("b",		null,	'b',		KeyEvent.KEYCODE_B,		0);
		new KeyValue("c",		null,	'c',		KeyEvent.KEYCODE_C,		0);
		new KeyValue("d",		null,	'd',		KeyEvent.KEYCODE_D,		0);
		new KeyValue("e",		null,	'e',		KeyEvent.KEYCODE_E,		0);
		new KeyValue("f",		null,	'f',		KeyEvent.KEYCODE_F,		0);
		new KeyValue("g",		null,	'g',		KeyEvent.KEYCODE_G,		0);
		new KeyValue("h",		null,	'h',		KeyEvent.KEYCODE_H,		0);
		new KeyValue("i",		null,	'i',		KeyEvent.KEYCODE_I,		0);
		new KeyValue("j",		null,	'j',		KeyEvent.KEYCODE_J,		0);
		new KeyValue("k",		null,	'k',		KeyEvent.KEYCODE_K,		0);
		new KeyValue("l",		null,	'l',		KeyEvent.KEYCODE_L,		0);
		new KeyValue("m",		null,	'm',		KeyEvent.KEYCODE_M,		0);
		new KeyValue("n",		null,	'n',		KeyEvent.KEYCODE_N,		0);
		new KeyValue("o",		null,	'o',		KeyEvent.KEYCODE_O,		0);
		new KeyValue("p",		null,	'p',		KeyEvent.KEYCODE_P,		0);
		new KeyValue("q",		null,	'q',		KeyEvent.KEYCODE_Q,		0);
		new KeyValue("r",		null,	'r',		KeyEvent.KEYCODE_R,		0);
		new KeyValue("s",		null,	's',		KeyEvent.KEYCODE_S,		0);
		new KeyValue("t",		null,	't',		KeyEvent.KEYCODE_T,		0);
		new KeyValue("u",		null,	'u',		KeyEvent.KEYCODE_U,		0);
		new KeyValue("v",		null,	'v',		KeyEvent.KEYCODE_V,		0);
		new KeyValue("w",		null,	'w',		KeyEvent.KEYCODE_W,		0);
		new KeyValue("x",		null,	'x',		KeyEvent.KEYCODE_X,		0);
		new KeyValue("y",		null,	'y',		KeyEvent.KEYCODE_Y,		0);
		new KeyValue("z",		null,	'z',		KeyEvent.KEYCODE_Z,		0);
		new KeyValue("0",		null,	'0',		KeyEvent.KEYCODE_0,		0);
		new KeyValue("1",		null,	'1',		KeyEvent.KEYCODE_1,		0);
		new KeyValue("2",		null,	'2',		KeyEvent.KEYCODE_2,		0);
		new KeyValue("3",		null,	'3',		KeyEvent.KEYCODE_3,		0);
		new KeyValue("4",		null,	'4',		KeyEvent.KEYCODE_4,		0);
		new KeyValue("5",		null,	'5',		KeyEvent.KEYCODE_5,		0);
		new KeyValue("6",		null,	'6',		KeyEvent.KEYCODE_6,		0);
		new KeyValue("7",		null,	'7',		KeyEvent.KEYCODE_7,		0);
		new KeyValue("8",		null,	'8',		KeyEvent.KEYCODE_8,		0);
		new KeyValue("9",		null,	'9',		KeyEvent.KEYCODE_9,		0);

		new KeyValue("config",				"Conf",			CHAR_NONE,	EVENT_CONFIG,				FLAG_NOREPEAT);
		new KeyValue("switch_text",			"ABC",			CHAR_NONE,	EVENT_SWITCH_TEXT,			FLAG_NOREPEAT);
		new KeyValue("switch_numeric",		"123+",			CHAR_NONE,	EVENT_SWITCH_NUMERIC,		FLAG_NOREPEAT);
		new KeyValue("switch_emoji",		"\uD83D\uDE03",	CHAR_NONE,	EVENT_SWITCH_EMOJI,			FLAG_NOREPEAT);
		new KeyValue("switch_back_emoji",	"ABC",			CHAR_NONE,	EVENT_SWITCH_BACK_EMOJI,	FLAG_NOREPEAT);

		new KeyValue("esc",		"Esc",		CHAR_NONE,	KeyEvent.KEYCODE_ESCAPE,		0);
		new KeyValue("enter",	"\uE800",	CHAR_NONE,	KeyEvent.KEYCODE_ENTER,			FLAG_KEY_FONT);
		new KeyValue("up",		"\uE80B",	CHAR_NONE,	KeyEvent.KEYCODE_DPAD_UP,		FLAG_KEY_FONT);
		new KeyValue("right",	"\uE80C",	CHAR_NONE,	KeyEvent.KEYCODE_DPAD_RIGHT,	FLAG_KEY_FONT);
		new KeyValue("down",	"\uE809",	CHAR_NONE,	KeyEvent.KEYCODE_DPAD_DOWN,		FLAG_KEY_FONT);
		new KeyValue("left",	"\uE80A",	CHAR_NONE,	KeyEvent.KEYCODE_DPAD_LEFT,		FLAG_KEY_FONT);
		new KeyValue("page_up",	"⇞",		CHAR_NONE,	KeyEvent.KEYCODE_PAGE_DOWN,		0);
		new KeyValue("page_down", "⇟",		CHAR_NONE,	KeyEvent.KEYCODE_PAGE_UP,		0);
		new KeyValue("home",	"↖",		CHAR_NONE,	KeyEvent.KEYCODE_HOME,			0);
		new KeyValue("end",		"↗",		CHAR_NONE,	KeyEvent.KEYCODE_MOVE_END,		0);
		new KeyValue("backspace", "⌫",		CHAR_NONE,	KeyEvent.KEYCODE_DEL,			0);
		new KeyValue("delete",	"⌦",		CHAR_NONE,	KeyEvent.KEYCODE_FORWARD_DEL,	0);
		new KeyValue("insert",	"Ins",		CHAR_NONE,	KeyEvent.KEYCODE_INSERT,		0);

		new KeyValue("tab",		"↹",		'\t',		KeyEvent.KEYCODE_TAB,			0);
		new KeyValue("space",	null,		' ',		KeyEvent.KEYCODE_SPACE,			0);
	}
}
