package juloo.keyboard2;

import android.view.KeyEvent;
import java.util.HashMap;

class KeyValue
{
	public static final int	EVENT_NONE = -1;
	public static final int	EVENT_BACKSPACE = -2;
	public static final int	EVENT_DELETE = -3;

	public static final int FLAG_KEEP_ON = 1;
	public static final int FLAG_CTRL = (1 << 1);
	public static final int FLAG_SHIFT = (1 << 2);
	public static final int FLAG_ALT = (1 << 3);

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

	private KeyValue(String name)
	{
		this(name, name, name.charAt(0), EVENT_NONE, 0);
	}

	private KeyValue(String name, String symbol, char c)
	{
		this(name, symbol, c, EVENT_NONE, 0);
	}

	private KeyValue(String name, String symbol, int eventCode)
	{
		this(name, symbol, '\0', eventCode, 0);
	}

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
		String chars = "abcdefghijklmnopqrstuvwxyz"
			+ "àçéèêë"
			+ "0123456789<>"
			+ "&é\"'(-_)=°+"
			+ "~#{[|`\\^@]}"
			+ "^$ù*,;:!¨£%µ?./§";
		for (int i = 0; i < chars.length(); i++)
			new KeyValue(chars.substring(i, i + 1));

		new KeyValue("shift", "⇧", '\0', EVENT_NONE, FLAG_KEEP_ON | FLAG_SHIFT);
		new KeyValue("ctrl", "Ctrl", '\0', EVENT_NONE, FLAG_KEEP_ON | FLAG_CTRL);
		new KeyValue("alt", "Alt", '\0', EVENT_NONE, FLAG_KEEP_ON | FLAG_ALT);

		new KeyValue("backspace", "⌫", EVENT_BACKSPACE);
		new KeyValue("delete", "⌦", EVENT_DELETE);

		new KeyValue("enter", "↵", KeyEvent.KEYCODE_ENTER);
		new KeyValue("up", "↑", KeyEvent.KEYCODE_DPAD_UP);
		new KeyValue("right", "→", KeyEvent.KEYCODE_DPAD_RIGHT);
		new KeyValue("down", "↓", KeyEvent.KEYCODE_DPAD_DOWN);
		new KeyValue("left", "←", KeyEvent.KEYCODE_DPAD_LEFT);
		new KeyValue("page_up", "⇞", KeyEvent.KEYCODE_PAGE_DOWN);
		new KeyValue("page_down", "⇟", KeyEvent.KEYCODE_PAGE_UP);
		new KeyValue("home", "↖", KeyEvent.KEYCODE_HOME);
		new KeyValue("end", "↗", KeyEvent.KEYCODE_MOVE_END);

		new KeyValue("tab", "↹", '\t');
		new KeyValue("space", " ", ' ');
	}
}
