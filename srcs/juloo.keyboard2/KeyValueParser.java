package juloo.keyboard2;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
Parse a key definition. The syntax for a key definition is:
- [(symbol):(key_action)]
- [:(kind) (attributes):(payload)].
- If [str] doesn't start with a [:] character, it is interpreted as an
  arbitrary string key.

[key_action] is:
- ['Arbitrary string']
- [(key_action),(key_action),...]
- [keyevent:(code)]
- [(key_name)]

For the different kinds and attributes, see doc/Possible-key-values.md.

Examples:
- [:str flags=dim,small symbol='MyKey':'My arbitrary string'].
- [:str:'My arbitrary string'].

*/
public final class KeyValueParser
{
  static Pattern KEYDEF_TOKEN;
  static Pattern QUOTED_PAT;
  static Pattern WORD_PAT;

  static public KeyValue parse(String input) throws ParseError
  {
    int symbol_ends = 0;
    final int input_len = input.length();
    while (symbol_ends < input_len && input.charAt(symbol_ends) != ':')
        symbol_ends++;
    if (symbol_ends == 0) // Old syntax
      return Starting_with_colon.parse(input);
    if (symbol_ends == input_len) // String key
      return KeyValue.makeStringKey(input);
    String symbol = input.substring(0, symbol_ends);
    init();
    Matcher m = KEYDEF_TOKEN.matcher(input);
    m.region(symbol_ends + 1, input_len);
    KeyValue first_key = parse_key_def(m);
    if (!parse_comma(m)) // Input is a single key def with a specified symbol
      return first_key.withSymbol(symbol);
    // Input is a macro
    ArrayList<KeyValue> keydefs = new ArrayList<KeyValue>();
    keydefs.add(first_key);
    do { keydefs.add(parse_key_def(m)); }
    while (parse_comma(m));
    return KeyValue.makeMacro(symbol, keydefs.toArray(new KeyValue[]{}), 0);
  }

  static void init()
  {
    if (KEYDEF_TOKEN != null)
      return;
    KEYDEF_TOKEN = Pattern.compile("'|,|keyevent:|(?:[^\\\\',]+|\\\\.)+");
    QUOTED_PAT = Pattern.compile("((?:[^'\\\\]+|\\\\')*)'");
    WORD_PAT = Pattern.compile("[a-zA-Z0-9_]+|.");
  }

  static KeyValue key_by_name_or_str(String str)
  {
    KeyValue k = KeyValue.getSpecialKeyByName(str);
    if (k != null)
      return k;
    return KeyValue.makeStringKey(str);
  }

  static KeyValue parse_key_def(Matcher m) throws ParseError
  {
    if (!match(m, KEYDEF_TOKEN))
      parseError("Expected key definition", m);
    String token = m.group(0);
    switch (token)
    {
      case "'": return parse_string_keydef(m);
      case ",": parseError("Unexpected comma", m); return null;
      case "keyevent:": return parse_keyevent_keydef(m);
      default: return key_by_name_or_str(remove_escaping(token));
    }
  }

  static KeyValue parse_string_keydef(Matcher m) throws ParseError
  {
    if (!match(m, QUOTED_PAT))
      parseError("Unterminated quoted string", m);
    return KeyValue.makeStringKey(remove_escaping(m.group(1)));
  }

  static KeyValue parse_keyevent_keydef(Matcher m) throws ParseError
  {
    if (!match(m, WORD_PAT))
      parseError("Expected keyevent code", m);
    int eventcode = 0;
    try { eventcode = Integer.parseInt(m.group(0)); }
    catch (Exception _e)
    { parseError("Expected an integer payload", m); }
    return KeyValue.keyeventKey("", eventcode, 0);
  }

  /** Returns [true] if the next token is a comma, [false] if it is the end of the input. Throws an error otherwise. */
  static boolean parse_comma(Matcher m) throws ParseError
  {
    if (!match(m, KEYDEF_TOKEN))
      return false;
    String token = m.group(0);
    if (!token.equals(","))
      parseError("Expected comma instead of '"+ token + "'", m);
    return true;
  }

  static String remove_escaping(String s)
  {
    if (!s.contains("\\"))
      return s;
    StringBuilder out = new StringBuilder(s.length());
    final int len = s.length();
    int prev = 0, i = 0;
    for (; i < len; i++)
      if (s.charAt(i) == '\\')
      {
        out.append(s, prev, i);
        prev = i + 1;
      }
    out.append(s, prev, i);
    return out.toString();
  }

  /**
    Parse a key definition starting with a [:]. This is the old syntax and is
    kept for compatibility.
    */
  final static class Starting_with_colon
  {
    static Pattern START_PAT;
    static Pattern ATTR_PAT;
    static Pattern QUOTED_PAT;
    static Pattern PAYLOAD_START_PAT;
    static Pattern WORD_PAT;

    static public KeyValue parse(String str) throws ParseError
    {
      String symbol = null;
      int flags = 0;
      init();
      // Kind
      Matcher m = START_PAT.matcher(str);
      if (!m.lookingAt())
        parseError("Expected kind, for example \":str ...\".", m);
      String kind = m.group(1);
      // Attributes
      while (true)
      {
        if (!match(m, ATTR_PAT))
          break;
        String attr_name = m.group(1);
        String attr_value = parseSingleQuotedString(m);
        switch (attr_name)
        {
          case "flags":
            flags = parseFlags(attr_value, m);
            break;
          case "symbol":
            symbol = attr_value;
            break;

          default:
            parseError("Unknown attribute "+attr_name, m);
        }
      }
      // Payload
      if (!match(m, PAYLOAD_START_PAT))
        parseError("Unexpected character", m);
      String payload;
      switch (kind)
      {
        case "str":
          payload = parseSingleQuotedString(m);
          if (symbol == null)
            return KeyValue.makeStringKey(payload, flags);
          return KeyValue.makeStringKey(payload, flags).withSymbol(symbol);

        case "char":
          payload = parsePayloadWord(m);
          if (payload.length() != 1)
            parseError("Expected a single character payload", m);
          return KeyValue.makeCharKey(payload.charAt(0), symbol, flags);

        case "keyevent":
          payload = parsePayloadWord(m);
          int eventcode = 0;
          try { eventcode = Integer.parseInt(payload); }
          catch (Exception _e)
          { parseError("Expected an integer payload", m); }
          if (symbol == null)
            symbol = String.valueOf(eventcode);
          return KeyValue.keyeventKey(symbol, eventcode, flags);

        default: break;
      }
      parseError("Unknown kind '"+kind+"'", m, 1);
      return null; // Unreachable
    }

    static String parseSingleQuotedString(Matcher m) throws ParseError
    {
      if (!match(m, QUOTED_PAT))
        parseError("Expected quoted string", m);
      return m.group(1).replace("\\'", "'");
    }

    static String parsePayloadWord(Matcher m) throws ParseError
    {
      if (!match(m, WORD_PAT))
        parseError("Expected a word after ':' made of [a-zA-Z0-9_]", m);
      return m.group(0);
    }

    static int parseFlags(String s, Matcher m) throws ParseError
    {
      int flags = 0;
      for (String f : s.split(","))
      {
        switch (f)
        {
          case "dim": flags |= KeyValue.FLAG_SECONDARY; break;
          case "small": flags |= KeyValue.FLAG_SMALLER_FONT; break;
          default: parseError("Unknown flag "+f, m);
        }
      }
      return flags;
    }

    static boolean match(Matcher m, Pattern pat)
    {
      try { m.region(m.end(), m.regionEnd()); } catch (Exception _e) {}
      m.usePattern(pat);
      return m.lookingAt();
    }

    static void init()
    {
      if (START_PAT != null)
        return;
      START_PAT = Pattern.compile(":(\\w+)");
      ATTR_PAT = Pattern.compile("\\s*(\\w+)\\s*=");
      QUOTED_PAT = Pattern.compile("'(([^'\\\\]+|\\\\')*)'");
      PAYLOAD_START_PAT = Pattern.compile("\\s*:");
      WORD_PAT = Pattern.compile("[a-zA-Z0-9_]*");
    }
  }

  static boolean match(Matcher m, Pattern pat)
  {
    try { m.region(m.end(), m.regionEnd()); } catch (Exception _e) {}
    m.usePattern(pat);
    return m.lookingAt();
  }

  static void parseError(String msg, Matcher m) throws ParseError
  {
    parseError(msg, m, m.regionStart());
  }

  static void parseError(String msg, Matcher m, int i) throws ParseError
  {
    StringBuilder msg_ = new StringBuilder("Syntax error");
    try
    {
      msg_.append(" at token '").append(m.group(0)).append("'");
    } catch (IllegalStateException _e) {}
    msg_.append(" at position ");
    msg_.append(i);
    msg_.append(": ");
    msg_.append(msg);
    throw new ParseError(msg_.toString());
  }

  public static class ParseError extends Exception
  {
    public ParseError(String msg) { super(msg); }
  };
}
