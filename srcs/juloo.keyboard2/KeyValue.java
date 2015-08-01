package juloo.keyboard2;

import android.view.KeyEvent;
import java.util.HashMap;

class KeyValue
{
	private String		_name;
	private String		_symbol;
	private char		_char;

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

	private static HashMap<String, KeyValue> keys = new HashMap<String, KeyValue>();

	private KeyValue(String name, String symbol, char c)
	{
		_name = name;
		_symbol = symbol;
		_char = c;
	}

	public static KeyValue	getKeyByName(String name)
	{
		return (KeyValue.keys.get(name));
	}

	private static void		add(String name, String symbol, char c)
	{
		keys.put(name, new KeyValue(name, symbol, c));
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
			add(chars.substring(i, i + 1), chars.substring(i, i + 1), chars.charAt(i));
		add("shift", "Shift", 'S');
		add("ctrl", "Ctrl", 'C');
		add("alt", "Alt", 'A');

		add("back", "⌫", '\u007F');
		add("up", "↑", 'U');
		add("right", "→", 'R');
		add("down", "↓", 'D');
		add("left", "←", 'L');
		add("page_up", "⇞", 'U');
		add("page_down", "⇟", 'D');
		add("home", "↖", 'H');
		add("end", "↗", 'E');
		add("tab", "↹", '\t');
		add("return", "↵", '\n');
		add("space", " ", ' ');
		add("delete", "⌦", 'D');
	}
}
