package juloo.keyboard2;

import juloo.keyboard2.KeyValue;
import juloo.keyboard2.KeyValueParser;
import org.junit.Test;
import static org.junit.Assert.*;

public class KeyValueParserTest
{
  public KeyValueParserTest() {}

  @Test
  public void parse_key_value() throws Exception
  {
    Utils.parse("'", KeyValue.makeStringKey("'"));
    Utils.parse("\\'", KeyValue.makeStringKey("\\'"));
    Utils.parse("\\,", KeyValue.makeStringKey("\\,"));
    Utils.parse("a\\'b", KeyValue.makeStringKey("a\\'b"));
    Utils.parse("a\\,b", KeyValue.makeStringKey("a\\,b"));
    Utils.parse("a", KeyValue.makeStringKey("a"));
    Utils.parse("abc", KeyValue.makeStringKey("abc"));
    Utils.parse("shift", KeyValue.getSpecialKeyByName("shift"));
    Utils.parse("'a", KeyValue.makeStringKey("'a"));
  }

  @Test
  public void parse_macro() throws Exception
  {
    Utils.parse("symbol:abc", KeyValue.makeMacro("symbol", new KeyValue[]{
      KeyValue.makeStringKey("abc")
    }, 0));
    Utils.parse("copy:ctrl,a,ctrl,c", KeyValue.makeMacro("copy", new KeyValue[]{
      KeyValue.getSpecialKeyByName("ctrl"),
      KeyValue.makeStringKey("a"),
      KeyValue.getSpecialKeyByName("ctrl"),
      KeyValue.makeStringKey("c")
    }, 0));
    Utils.parse("macro:abc,\\'", KeyValue.makeMacro("macro", new KeyValue[]{
      KeyValue.makeStringKey("abc"),
      KeyValue.makeStringKey("'")
    }, 0));
    Utils.parse("macro:abc,\\,", KeyValue.makeMacro("macro", new KeyValue[]{
      KeyValue.makeStringKey("abc"),
      KeyValue.makeStringKey(",")
    }, 0));
    Utils.parse("<2:ctrl,backspace", KeyValue.makeMacro("<2", new KeyValue[]{
      KeyValue.getSpecialKeyByName("ctrl"),
      KeyValue.getSpecialKeyByName("backspace")
    }, 0));
    Utils.expect_error("symbol:");
    Utils.expect_error("unterminated_string:'");
    Utils.expect_error("unterminated_string:abc,'");
    Utils.expect_error("unexpected_quote:abc,,");
    Utils.expect_error("unexpected_quote:,");
  }

  @Test
  /* Using the [symbol:..] syntax but not resulting in a macro. */
  public void parse_non_macro() throws Exception
  {
    Utils.parse("a:b", KeyValue.makeCharKey('b', "a", 0));
  }

  @Test
  public void parse_string_key() throws Exception
  {
    Utils.parse("symbol:'str'", KeyValue.makeMacro("symbol", new KeyValue[]{
      KeyValue.makeStringKey("str")
    }, 0));
    Utils.parse("symbol:'str\\''", KeyValue.makeMacro("symbol", new KeyValue[]{
      KeyValue.makeStringKey("str'")
    }, 0));
    Utils.parse("macro:'str',abc", KeyValue.makeMacro("macro", new KeyValue[]{
      KeyValue.makeStringKey("str"),
      KeyValue.makeStringKey("abc")
    }, 0));
    Utils.parse("macro:abc,'str'", KeyValue.makeMacro("macro", new KeyValue[]{
      KeyValue.makeStringKey("abc"),
      KeyValue.makeStringKey("str")
    }, 0));
    Utils.parse("macro:\\',\\,", KeyValue.makeMacro("macro", new KeyValue[]{
      KeyValue.makeStringKey("'"),
      KeyValue.makeStringKey(","),
    }, 0));
    Utils.parse("macro:a\\'b,a\\,b,a\\xb", KeyValue.makeMacro("macro", new KeyValue[]{
      KeyValue.makeStringKey("a'b"),
      KeyValue.makeStringKey("a,b"),
      KeyValue.makeStringKey("axb")
    }, 0));
    Utils.expect_error("symbol:'");
    Utils.expect_error("symbol:'foo");
  }

  @Test
  public void parse_key_event() throws Exception
  {
    Utils.parse("symbol:keyevent:85", KeyValue.keyeventKey("symbol", 85, 0));
    Utils.parse("macro:keyevent:85,abc", KeyValue.makeMacro("macro", new KeyValue[]{
      KeyValue.keyeventKey("", 85, 0),
      KeyValue.makeStringKey("abc")
    }, 0));
    Utils.parse("macro:abc,keyevent:85", KeyValue.makeMacro("macro", new KeyValue[]{
      KeyValue.makeStringKey("abc"),
      KeyValue.keyeventKey("", 85, 0)
    }, 0));
    Utils.expect_error("symbol:keyevent:");
    Utils.expect_error("symbol:keyevent:85a");
  }

  @Test
  public void parse_old_syntax() throws Exception
  {
    Utils.parse(":str:'Foo'", KeyValue.makeStringKey("Foo"));
    Utils.parse(":str flags='dim':'Foo'", KeyValue.makeStringKey("Foo", KeyValue.FLAG_SECONDARY));
    Utils.parse(":str symbol='Symbol':'Foo'", KeyValue.makeStringKeyWithSymbol("Foo", "Symbol", 0));
    Utils.parse(":str symbol='Symbol' flags='dim':'Foo'", KeyValue.makeStringKeyWithSymbol("Foo", "Symbol", KeyValue.FLAG_SECONDARY));
    Utils.parse(":str flags='dim,small':'Foo'", KeyValue.makeStringKey("Foo", KeyValue.FLAG_SECONDARY | KeyValue.FLAG_SMALLER_FONT));
    Utils.parse(":str flags=',,':'Foo'", KeyValue.makeStringKey("Foo")); // Unintentional
    Utils.expect_error(":unknown:Foo"); // Unknown kind
    Utils.expect_error(":str:Foo"); // Unquoted string
    Utils.expect_error(":str flags:'Foo'"); // Malformed flags
    Utils.expect_error(":str flags=dim:'Foo'"); // Unquoted flags
    Utils.expect_error(":str unknown='foo':'Foo'"); // Unknown flags
    // Unterminated
    Utils.expect_error(":str");
    Utils.expect_error(":str ");
    Utils.expect_error(":str flags");
    Utils.expect_error(":str flags=");
    Utils.expect_error(":str flags='");
    Utils.expect_error(":str flags='' ");
    Utils.expect_error(":str flags='':");
    Utils.expect_error(":str flags='':'");
    // Char
    Utils.parse(":char symbol='a':b", KeyValue.makeCharKey('b', "a", 0));
    Utils.parse(":char:b", KeyValue.makeCharKey('b', "b", 0));
  }

  /** JUnit removes these functions from stacktraces. */
  static class Utils
  {
    static void parse(String key_descr, KeyValue ref) throws Exception
    {
      assertEquals(ref, KeyValue.getKeyByName(key_descr));
    }

    static void expect_error(String key_descr)
    {
      try
      {
        fail("Expected failure but got " + KeyValueParser.parse(key_descr));
      }
      catch (KeyValueParser.ParseError e) {}
    }
  }
}
