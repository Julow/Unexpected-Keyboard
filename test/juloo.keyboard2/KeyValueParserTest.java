package juloo.keyboard2;

import juloo.keyboard2.KeyValue;
import juloo.keyboard2.KeyValueParser;
import org.junit.Test;
import static org.junit.Assert.*;

public class KeyValueParserTest
{
  public KeyValueParserTest() {}

  @Test
  public void parseStr() throws Exception
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
  }

  @Test
  public void parseChar() throws Exception
  {
    Utils.parse(":char symbol='a':b", KeyValue.makeCharKey('b', "a", 0));
    Utils.parse(":char:b", KeyValue.makeCharKey('b', "b", 0));
  }

  @Test
  public void parseKeyValue() throws Exception
  {
    Utils.parse("\'", KeyValue.makeStringKey("\'"));
    Utils.parse("a", KeyValue.makeStringKey("a"));
    Utils.parse("abc", KeyValue.makeStringKey("abc"));
    Utils.parse("shift", KeyValue.getSpecialKeyByName("shift"));
    Utils.expect_error("\'a");
  }

  @Test
  public void parseMacro() throws Exception
  {
    Utils.parse(":macro symbol='copy':ctrl,a,ctrl,c", KeyValue.makeMacro("copy", new KeyValue[]{
      KeyValue.getSpecialKeyByName("ctrl"),
      KeyValue.makeStringKey("a"),
      KeyValue.getSpecialKeyByName("ctrl"),
      KeyValue.makeStringKey("c")
    }, 0));
    Utils.parse(":macro:abc,'", KeyValue.makeMacro("macro", new KeyValue[]{
      KeyValue.makeStringKey("abc"),
      KeyValue.makeStringKey("'")
    }, 0));
    Utils.expect_error(":macro:");
  }

  /** JUnit removes these functions from stacktraces. */
  static class Utils
  {
    static void parse(String key_descr, KeyValue ref) throws Exception
    {
      assertEquals(ref, KeyValueParser.parse(key_descr));
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
