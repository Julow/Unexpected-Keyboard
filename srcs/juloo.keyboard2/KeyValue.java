package juloo.keyboard2;

import android.view.KeyEvent;
import java.util.HashMap;

class KeyValue
{
	public static final int		EVENT_NONE = -1;
	public static final int		EVENT_CONFIG = -2;
	public static final char	CHAR_NONE = '\0';

	public static final int		FLAG_KEEP_ON = 1;
	public static final int		FLAG_LOCK = (1 << 1);
	public static final int		FLAG_CTRL = (1 << 2);
	public static final int		FLAG_SHIFT = (1 << 3);
	public static final int		FLAG_ALT = (1 << 4);
	public static final int		FLAG_NOCHAR = (1 << 5);
	public static final int		FLAG_LOCKED = (1 << 8);

	private String		_name;
	private String		_symbol;
	private char		_char;
	private int			_eventCode;
	private int			_flags;

	public String		getName()
	{
		return (_name);
	}

	public String		getSymbol(boolean upperCase)
	{
		if (upperCase)
			return (_symbol.toUpperCase());
		return (_symbol);
	}

	public char			getChar(boolean upperCase)
	{
		if (upperCase)
			return (Character.toUpperCase(_char));
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

	private KeyValue(String name, String symbol, char c, int eventCode, int flags)
	{
		_name = name;
		_symbol = symbol;
		_char = c;
		_eventCode = eventCode;
		_flags = flags;
		KeyValue.keys.put(name, this);
	}

	public static KeyValue	getKeyByName(String name)
	{
		return (KeyValue.keys.get(name));
	}

	static
	{
		String chars = "àçéèêë<>"
			+ "&é\"'(-_)=°+"
			+ "~#{[|`\\^@]}"
			+ "^$ù*,;:!¨£%µ?./§";
		for (int i = 0; i < chars.length(); i++)
		{
			String key = chars.substring(i, i + 1);
			new KeyValue(key, key, key.charAt(0), EVENT_NONE, 0);
		}

		new KeyValue("shift",	"⇧",	CHAR_NONE,	EVENT_NONE,			FLAG_KEEP_ON | FLAG_NOCHAR | FLAG_LOCK | FLAG_SHIFT);
		new KeyValue("ctrl",	"Ctrl",	CHAR_NONE,	EVENT_NONE,			FLAG_KEEP_ON | FLAG_NOCHAR | FLAG_CTRL);
		new KeyValue("alt",		"Alt",	CHAR_NONE,	EVENT_NONE,			FLAG_KEEP_ON | FLAG_NOCHAR | FLAG_ALT);

		new KeyValue("a",		"a",	'a',		KeyEvent.KEYCODE_A,	0);
		new KeyValue("b",		"b",	'b',		KeyEvent.KEYCODE_B,	0);
		new KeyValue("c",		"c",	'c',		KeyEvent.KEYCODE_C,	0);
		new KeyValue("d",		"d",	'd',		KeyEvent.KEYCODE_D,	0);
		new KeyValue("e",		"e",	'e',		KeyEvent.KEYCODE_E,	0);
		new KeyValue("f",		"f",	'f',		KeyEvent.KEYCODE_F,	0);
		new KeyValue("g",		"g",	'g',		KeyEvent.KEYCODE_G,	0);
		new KeyValue("h",		"h",	'h',		KeyEvent.KEYCODE_H,	0);
		new KeyValue("i",		"i",	'i',		KeyEvent.KEYCODE_I,	0);
		new KeyValue("j",		"j",	'j',		KeyEvent.KEYCODE_J,	0);
		new KeyValue("k",		"k",	'k',		KeyEvent.KEYCODE_K,	0);
		new KeyValue("l",		"l",	'l',		KeyEvent.KEYCODE_L,	0);
		new KeyValue("m",		"m",	'm',		KeyEvent.KEYCODE_M,	0);
		new KeyValue("n",		"n",	'n',		KeyEvent.KEYCODE_N,	0);
		new KeyValue("o",		"o",	'o',		KeyEvent.KEYCODE_O,	0);
		new KeyValue("p",		"p",	'p',		KeyEvent.KEYCODE_P,	0);
		new KeyValue("q",		"q",	'q',		KeyEvent.KEYCODE_Q,	0);
		new KeyValue("r",		"r",	'r',		KeyEvent.KEYCODE_R,	0);
		new KeyValue("s",		"s",	's',		KeyEvent.KEYCODE_S,	0);
		new KeyValue("t",		"t",	't',		KeyEvent.KEYCODE_T,	0);
		new KeyValue("u",		"u",	'u',		KeyEvent.KEYCODE_U,	0);
		new KeyValue("v",		"v",	'v',		KeyEvent.KEYCODE_V,	0);
		new KeyValue("w",		"w",	'w',		KeyEvent.KEYCODE_W,	0);
		new KeyValue("x",		"x",	'x',		KeyEvent.KEYCODE_X,	0);
		new KeyValue("y",		"y",	'y',		KeyEvent.KEYCODE_Y,	0);
		new KeyValue("z",		"z",	'z',		KeyEvent.KEYCODE_Z,	0);
		new KeyValue("0",		"0",	'0',		KeyEvent.KEYCODE_0,	0);
		new KeyValue("1",		"1",	'1',		KeyEvent.KEYCODE_1,	0);
		new KeyValue("2",		"2",	'2',		KeyEvent.KEYCODE_2,	0);
		new KeyValue("3",		"3",	'3',		KeyEvent.KEYCODE_3,	0);
		new KeyValue("4",		"4",	'4',		KeyEvent.KEYCODE_4,	0);
		new KeyValue("5",		"5",	'5',		KeyEvent.KEYCODE_5,	0);
		new KeyValue("6",		"6",	'6',		KeyEvent.KEYCODE_6,	0);
		new KeyValue("7",		"7",	'7',		KeyEvent.KEYCODE_7,	0);
		new KeyValue("8",		"8",	'8',		KeyEvent.KEYCODE_8,	0);
		new KeyValue("9",		"9",	'9',		KeyEvent.KEYCODE_9,	0);

		new KeyValue("config",	"Conf", CHAR_NONE,	EVENT_CONFIG,				0);
		new KeyValue("enter",	"↵", 	CHAR_NONE,	KeyEvent.KEYCODE_ENTER,		0);
		new KeyValue("up",		"↑", 	CHAR_NONE,	KeyEvent.KEYCODE_DPAD_UP,	0);
		new KeyValue("right",	"→", 	CHAR_NONE,	KeyEvent.KEYCODE_DPAD_RIGHT, 0);
		new KeyValue("down",	"↓", 	CHAR_NONE,	KeyEvent.KEYCODE_DPAD_DOWN,	0);
		new KeyValue("left",	"←", 	CHAR_NONE,	KeyEvent.KEYCODE_DPAD_LEFT,	0);
		new KeyValue("page_up",	"⇞", 	CHAR_NONE,	KeyEvent.KEYCODE_PAGE_DOWN,	0);
		new KeyValue("page_down", "⇟", 	CHAR_NONE,	KeyEvent.KEYCODE_PAGE_UP,	0);
		new KeyValue("home",	"↖", 	CHAR_NONE,	KeyEvent.KEYCODE_HOME,		0);
		new KeyValue("end",		"↗", 	CHAR_NONE,	KeyEvent.KEYCODE_MOVE_END,	0);
		new KeyValue("backspace", "⌫",	CHAR_NONE,	KeyEvent.KEYCODE_DEL,		0);
		new KeyValue("delete",	"⌦",	CHAR_NONE,	KeyEvent.KEYCODE_FORWARD_DEL, 0);
		new KeyValue("insert",	"Ins",	CHAR_NONE,	KeyEvent.KEYCODE_INSERT,	0);

		new KeyValue("tab",		"↹",	'\t',		EVENT_NONE,			0);
		new KeyValue("space",	" ",	' ',		EVENT_NONE,			0);
	}
}
