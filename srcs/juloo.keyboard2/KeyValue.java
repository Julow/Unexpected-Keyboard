package juloo.keyboard2;

import android.view.KeyEvent;
import java.util.HashMap;

enum KeyValue
{
	KEY_A("a", 'a'),
	KEY_B("b", 'b'),
	KEY_C("c", 'c'),
	KEY_D("d", 'd'),
	KEY_E("e", 'e'),
	KEY_F("f", 'f'),
	KEY_G("g", 'g'),
	KEY_H("h", 'h'),
	KEY_I("i", 'i'),
	KEY_J("j", 'j'),
	KEY_K("k", 'k'),
	KEY_L("l", 'l'),
	KEY_M("m", 'm'),
	KEY_N("n", 'n'),
	KEY_O("o", 'o'),
	KEY_P("p", 'p'),
	KEY_Q("q", 'q'),
	KEY_R("r", 'r'),
	KEY_S("s", 's'),
	KEY_T("t", 't'),
	KEY_U("u", 'u'),
	KEY_V("v", 'v'),
	KEY_W("w", 'w'),
	KEY_X("x", 'x'),
	KEY_Y("y", 'y'),
	KEY_Z("z", 'z'),
	KEY_0("0", '0'),
	KEY_1("1", '1'),
	KEY_2("2", '2'),
	KEY_3("3", '3'),
	KEY_4("4", '4'),
	KEY_5("5", '5'),
	KEY_6("6", '6'),
	KEY_7("7", '7'),
	KEY_8("8", '8'),
	KEY_9("9", '9'),
	KEY_AND("&", '&'),
	KEY_E2("é", 'é'),
	KEY_DQUOTE("\"", '"'),
	KEY_QUOTE("'", '\''),
	KEY_PARENTHESIS("(", '('),
	KEY_MINUS("-", '-'),
	KEY_E3("è", 'è'),
	KEY_UNDERSCORE("_", '_'),
	KEY_C2("ç", 'ç'),
	KEY_A2("à", 'à'),
	KEY_TILDE("~", '~'),
	KEY_DIESE("#", '#'),
	KEY_BLOCK("{", '{'),
	KEY_SQUARE("[", '['),
	KEY_PIPE("|", '|'),
	KEY_BACKQUOTE("`", '`'),
	KEY_BACKSLASH("\\", '\\'),
	KEY_XOR("^", '^'),
	KEY_AROBASE("@", '@'),
	KEY_ENTER("enter", '\n'),
	KEY_SPACE("space", ' '),
	KEY_DEL("del", '\u007F');

	private String		_name;
	private char		_char;

	private KeyValue(String name, char c)
	{
		_name = name;
		_char = c;
	}

	public String		getName()
	{
		return (_name);
	}

	public char			getChar()
	{
		return (_char);
	}

	private static HashMap<String, KeyValue> keys = new HashMap<String, KeyValue>();

	static
	{
		for (KeyValue k : KeyValue.values())
			keys.put(k.getName(), k);
	}

	public static KeyValue	getKeyByName(String name)
	{
		return (KeyValue.keys.get(name));
	}
}
