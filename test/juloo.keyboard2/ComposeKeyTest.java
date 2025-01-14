package juloo.keyboard2;

import juloo.keyboard2.ComposeKey;
import juloo.keyboard2.ComposeKeyData;
import juloo.keyboard2.KeyValue;
import org.junit.Test;
import static org.junit.Assert.*;

public class ComposeKeyTest
{
  public ComposeKeyTest() {}

  @Test
  public void composeEquals() throws Exception
  {
    // From Compose.pre
    assertEquals(apply("'e"), KeyValue.makeStringKey("é"));
    assertEquals(apply("e'"), KeyValue.makeStringKey("é"));
    // From extra.json
    assertEquals(apply("Vc"), KeyValue.makeStringKey("Č"));
    assertEquals(apply("\\n"), KeyValue.getKeyByName("\\n"));
    // From arabic.json
    assertEquals(apply("اا"), KeyValue.getKeyByName("combining_alef_above"));
    assertEquals(apply("ل۷"), KeyValue.makeStringKey("ڵ"));
    assertEquals(apply("۷ل"), KeyValue.makeStringKey("ڵ"));
    // From cyrillic.json
    assertEquals(apply(",г"), KeyValue.makeStringKey("ӻ"));
    assertEquals(apply("г,"), KeyValue.makeStringKey("ӻ"));
    assertEquals(apply("ач"), KeyValue.getKeyByName("combining_aigu"));
  }

  @Test
  public void fnEquals() throws Exception
  {
    int state = ComposeKeyData.fn;
    assertEquals(apply("<", state), KeyValue.makeStringKey("«"));
    assertEquals(apply("{", state), KeyValue.makeStringKey("‹"));
    // Named key
    assertEquals(apply("1", state), KeyValue.getKeyByName("f1"));
    assertEquals(apply(" ", state), KeyValue.getKeyByName("nbsp"));
    // Named 1-char key
    assertEquals(apply("ய", state), KeyValue.makeStringKey("௰", KeyValue.FLAG_SMALLER_FONT));
  }

  KeyValue apply(String seq) throws Exception
  {
    return apply(seq, ComposeKeyData.compose);
  }

  KeyValue apply(String seq, int state) throws Exception
  {
    KeyValue r = null;
    for (int i = 0; i < seq.length(); i++)
    {
      r = ComposeKey.apply(state, seq.charAt(i));
      if (r.getKind() == KeyValue.Kind.Compose_pending)
        state = r.getPendingCompose();
      else if (i + 1 < seq.length())
        throw new Exception("Sequence too long: " + seq);
    }
    return r;
  }
}
