package juloo.keyboard2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
Parse a key definition. The syntax for a key definition is:
- [:(kind) (attributes):(payload)].
- If [str] doesn't start with a [:] character, it is interpreted as an
  arbitrary string key.

[(kind)] specifies the kind of the key, it can be:
- [str]: An arbitrary string key. The payload is the string to output when
  typed and is quoted by single quotes ([']). The payload can contain single
  quotes if they are escaped with a backslash ([\']).

The [(attributes)] part is a space-separated list of attributes, all optional,
of the form: [attrname='attrvalue'].

Attributes can be:
- [flags]: Add flags that change the behavior of the key.
  Value is a coma separated list of:
  - [dim]: Make the symbol dimmer on the keyboard.
  - [small]: Make the symbol smaller on the keyboard.
- [symbol]: Specify the symbol that is rendered on the keyboard.
  It can contain single quotes if they are escaped: ([\']).

Examples:
- [:str flags=dim,small symbol='MyKey':'My arbitrary string'].
- [:str:'My arbitrary string'].

*/
public final class KeyValueParser
{
  static Pattern START_PAT;
  static Pattern ATTR_PAT;
  static Pattern QUOTED_PAT;
  static Pattern PAYLOAD_START_PAT;

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
    switch (kind)
    {
      case "str":
        String payload = parseSingleQuotedString(m);
        if (symbol == null)
          return KeyValue.makeStringKey(payload, flags);
        return KeyValue.makeStringKeyWithSymbol(payload, symbol, flags);

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
      char c = m.group(0).charAt(0);
      msg_.append(" at character '").append(c).append("'");
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
