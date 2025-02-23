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

  @Test
  public void stringKeys() throws Exception
  {
    int state = ComposeKeyData.shift;
    assertEquals(apply("𝕨", state), KeyValue.makeStringKey("𝕎"));
    assertEquals(apply("𝕩", state), KeyValue.makeStringKey("𝕏"));
  }

  KeyValue apply(String seq)
  {
    return ComposeKey.apply(ComposeKeyData.compose, seq);
  }

  KeyValue apply(String seq, int state)
  {
    return ComposeKey.apply(state, seq);
  }
}
