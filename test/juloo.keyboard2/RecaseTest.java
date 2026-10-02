package juloo.keyboard2;

import juloo.keyboard2.Recase;
import org.junit.Test;
import static org.junit.Assert.*;

public class RecaseTest
{
  public RecaseTest() {}

  @Test
  public void upper() throws Exception
  {
    assertEquals("ABC", Recase.apply("aBc", Recase.Style.UPPER));
    assertEquals("ABC", Recase.apply("ABC", Recase.Style.UPPER));
    assertEquals("", Recase.apply("", Recase.Style.UPPER));
  }

  @Test
  public void lower() throws Exception
  {
    assertEquals("abc", Recase.apply("aBc", Recase.Style.LOWER));
    assertEquals("abc", Recase.apply("abc", Recase.Style.LOWER));
    assertEquals("", Recase.apply("", Recase.Style.LOWER));
  }

  @Test
  public void smallCaps() throws Exception
  {
    assertEquals("ᴀʙᴄ", Recase.apply("abc", Recase.Style.SMALL_CAPS));
    assertEquals("ᴛᴇꜱᴛ", Recase.apply("Test", Recase.Style.SMALL_CAPS));
    // Characters without a small capital variant are not changed.
    assertEquals("123 -", Recase.apply("123 -", Recase.Style.SMALL_CAPS));
    assertEquals("", Recase.apply("", Recase.Style.SMALL_CAPS));
  }

  @Test
  public void spaced() throws Exception
  {
    assertEquals("a b c", Recase.apply("abc", Recase.Style.SPACED));
    // Existing spaces are not doubled.
    assertEquals("a b c d e f", Recase.apply("abc def", Recase.Style.SPACED));
    assertEquals("a b", Recase.apply("a b", Recase.Style.SPACED));
    assertEquals("", Recase.apply("", Recase.Style.SPACED));
  }

  @Test
  public void codePoints() throws Exception
  {
    // Surrogate pairs are not split by the spaced style.
    String x = new String(Character.toChars(0x1D569));
    assertEquals("a " + x, Recase.apply("a" + x, Recase.Style.SPACED));
    assertEquals(x, Recase.apply(x, Recase.Style.SMALL_CAPS));
    assertEquals(x, Recase.apply(x, Recase.Style.UPPER));
  }

  /** The keys that trigger the transformations. */
  @Test
  public void keyNames() throws Exception
  {
    assertEditing("uppercase", KeyValue.Editing.UPPERCASE);
    assertEditing("lowercase", KeyValue.Editing.LOWERCASE);
    assertEditing("smallcaps", KeyValue.Editing.SMALL_CAPS);
    assertEditing("letterspaced", KeyValue.Editing.LETTER_SPACED);
  }

  /** Small caps stays activated until pressed again. */
  @Test
  public void smallCapsIsSticky() throws Exception
  {
    KeyValue k = KeyValue.getKeyByName("accent_small_caps");
    assertTrue(k.hasFlagsAny(KeyValue.FLAG_STICKY));
    assertTrue(k.hasFlagsAny(KeyValue.FLAG_LATCH));
    // Other modifiers are not sticky.
    assertFalse(KeyValue.getKeyByName("accent_aigu")
        .hasFlagsAny(KeyValue.FLAG_STICKY));
    assertFalse(KeyValue.SHIFT.hasFlagsAny(KeyValue.FLAG_STICKY));
  }

  void assertEditing(String name, KeyValue.Editing editing)
  {
    KeyValue k = KeyValue.getKeyByName(name);
    assertEquals(KeyValue.Kind.Editing, k.getKind());
    assertEquals(editing, k.getEditing());
  }
}
