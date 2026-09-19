package juloo.keyboard2;

import juloo.keyboard2.ComposeKey;
import juloo.keyboard2.ComposeKeyData;
import juloo.keyboard2.KeyValue;
import org.junit.Test;
import static juloo.keyboard2.TestUtils.*;
import static org.junit.Assert.*;

public class ComposeKeyTest
{
  public ComposeKeyTest() {}

  @Test
  public void composeEquals() throws Exception
  {
    // From Compose.pre
    assertEquals(apply("'e"), str("é"));
    assertEquals(apply("e'"), str("é"));
    // From extra.json
    assertEquals(apply("Vc"), str("Č"));
    assertEquals(apply("\\n"), key("\\n"));
    // From arabic.json
    assertEquals(apply("اا"), key("combining_alef_above"));
    assertEquals(apply("ل۷"), str("ڵ"));
    assertEquals(apply("۷ل"), str("ڵ"));
    // From cyrillic.json
    assertEquals(apply(",г"), str("ӻ"));
    assertEquals(apply("г,"), str("ӻ"));
    assertEquals(apply("ач"), key("combining_aigu"));
  }

  @Test
  public void fnEquals() throws Exception
  {
    int state = ComposeKeyData.fn;
    assertEquals(apply("<", state), str("«"));
    assertEquals(apply("{", state), str("‹"));
    // Named key
    assertEquals(apply("1", state), key("f1"));
    assertEquals(apply(" ", state), key("nbsp"));
    // Named 1-char key
    assertEquals(apply("ய", state), str("௰", KeyValue.FLAG_SMALLER_FONT));
  }

  @Test
  public void stringKeys() throws Exception
  {
    int state = ComposeKeyData.shift;
    assertEquals(apply("𝕨", state), str("𝕎"));
    assertEquals(apply("𝕩", state), str("𝕏"));
    state = ComposeKeyData.accent_small_caps;
    assertEquals(apply("œ", state), str("ɶ"));
    assertEquals(apply("Œ", state), str("ɶ"));
    assertEquals(apply("ɹ", state), str("ʁ"));
    assertEquals(apply("ɠ", state), str("ʛ"));
  }

  @Test
  public void spaceKey() throws Exception
  {
    int state = ComposeKeyData.compose;
    assertEquals(apply("- ", state), str("~"));
    assertEquals(apply(" -", state), str("~"));
    assertEquals(apply("  ", state), key("nbsp"));
    assertEquals(apply(apply(" "), key("space")), key("nbsp"));
  }

  KeyValue apply(String seq)
  {
    return ComposeKey.apply(ComposeKeyData.compose, seq);
  }

  KeyValue apply(String seq, int state)
  {
    return ComposeKey.apply(state, seq);
  }

  KeyValue apply(KeyValue prev, KeyValue next)
  {
    if (prev.getKind() != KeyValue.Kind.Compose_pending)
      return null;
    return ComposeKey.apply(prev.getPendingCompose(), next);
  }
}
