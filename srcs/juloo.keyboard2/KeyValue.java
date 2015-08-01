package juloo.keyboard2;

import android.view.KeyEvent;
import java.util.HashMap;

class KeyValue
{
	public static final int	EVENT_NONE = -1;
	public static final int	EVENT_BACKSPACE = -2;
	public static final int	EVENT_DELETE = -3;

	private String		_name;
	private String		_symbol;
	private char		_char;
	private int			_eventCode;

	public String		getName()
	{
		return (_name);
	}

	public String		getSymbol()
	{
		return (_symbol);
	}

	public char			getChar()
	{
		return (_char);
	}

	public int			getEventCode()
	{
		return (_eventCode);
	}

	private static HashMap<String, KeyValue> keys = new HashMap<String, KeyValue>();

	private KeyValue(String name)
	{
		this(name, name, name.charAt(0), EVENT_NONE);
	}

	private KeyValue(String name, String symbol, char c)
	{
		this(name, symbol, c, EVENT_NONE);
	}

	private KeyValue(String name, String symbol, int eventCode)
	{
		this(name, symbol, name.charAt(0), eventCode);
	}

	private KeyValue(String name, String symbol, char c, int eventCode)
	{
		_name = name;
		_symbol = symbol;
		_char = c;
		_eventCode = eventCode;
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

		new KeyValue("shift", "Shift", EVENT_NONE);
		new KeyValue("ctrl", "Ctrl", EVENT_NONE);
		new KeyValue("alt", "Alt", EVENT_NONE);

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
