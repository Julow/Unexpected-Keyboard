package juloo.keyboard2;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class EmojiTest
{
  public EmojiTest() {}

  @BeforeClass
  public static void loadEmojis() throws Exception
  {
    Emoji.load(new BufferedReader(new InputStreamReader(
            new FileInputStream("res/raw/emojis.txt"), "UTF-8")));
  }

  static Emoji emoji(String s)
  {
    Emoji e = Emoji.getEmojiByString(s);
    assertNotNull("Unknown emoji " + s, e);
    return e;
  }

  static String str(Emoji e)
  {
    return e.kv().getString();
  }

  static boolean isSkinToneModifier(int c)
  {
    return c >= 0x1F3FB && c <= 0x1F3FF;
  }

  static boolean hasSkinToneModifier(String s)
  {
    return s.codePoints().anyMatch(EmojiTest::isSkinToneModifier);
  }

  /** Base emojis of every group. */
  static List<Emoji> allEmojis()
  {
    List<Emoji> all = new ArrayList<>();
    for (int g = 0; g < Emoji.getNumGroups(); g++)
      all.addAll(Emoji.getEmojisByGroup(g));
    return all;
  }

  @Test
  public void groups_contain_no_skin_tone_variants()
  {
    assertEquals(9, Emoji.getNumGroups());
    for (Emoji e : allEmojis())
      assertFalse(str(e), hasSkinToneModifier(str(e)));
  }

  @Test
  public void group_boundaries()
  {
    assertEquals("😀", str(Emoji.getEmojisByGroup(0).get(0)));
    assertEquals("👋", str(Emoji.getEmojisByGroup(1).get(0)));
    assertEquals("🏁", str(Emoji.getEmojisByGroup(8).get(0)));
  }

  @Test
  public void emoji_without_variants()
  {
    Emoji e = emoji("😀");
    assertFalse(e.hasVariants());
    assertSame(e, e.withSkinTone(Emoji.NO_SKIN_TONE));
    assertSame(e, e.withSkinTone(3));
  }

  @Test
  public void single_tone_variants()
  {
    Emoji e = emoji("👋");
    assertTrue(e.hasVariants());
    assertEquals(6, e.variants().size());
    assertSame(e, e.withSkinTone(Emoji.NO_SKIN_TONE));
    assertEquals("👋🏻", str(e.withSkinTone(1)));
    assertEquals("👋🏼", str(e.withSkinTone(2)));
    assertEquals("👋🏽", str(e.withSkinTone(3)));
    assertEquals("👋🏾", str(e.withSkinTone(4)));
    assertEquals("👋🏿", str(e.withSkinTone(5)));
  }

  @Test
  public void base_with_variation_selector()
  {
    // The base contains U+FE0F but its toned variants don't.
    Emoji e = emoji("🖐️");
    assertEquals("🖐🏽", str(e.withSkinTone(3)));
  }

  @Test
  public void multi_person_variants()
  {
    Emoji e = emoji("🤝");
    assertEquals(26, e.variants().size());
    assertEquals("🤝🏻", str(e.withSkinTone(1)));
    assertEquals("🤝🏿", str(e.withSkinTone(5)));
    assertTrue(e.variants().contains(emoji("🫱🏻‍🫲🏼")));
    Emoji bunny = emoji("👯");
    assertEquals(26, bunny.variants().size());
    assertTrue(bunny.variants().contains(emoji("🧑🏻‍🐰‍🧑🏼")));
  }

  @Test
  public void variant_shares_variants_of_its_base()
  {
    Emoji base = emoji("👋");
    Emoji variant = emoji("👋🏽");
    assertNotSame(base, variant);
    assertTrue(variant.hasVariants());
    assertSame(base.variants(), variant.variants());
    assertSame(base, variant.withSkinTone(Emoji.NO_SKIN_TONE));
  }

  @Test
  public void skin_tone_of_string()
  {
    assertEquals("👋🏽", Emoji.withSkinTone("👋", 3));
    assertEquals("👋", Emoji.withSkinTone("👋", Emoji.NO_SKIN_TONE));
    assertEquals("👋🏿", Emoji.withSkinTone("👋🏻", 5));
    assertEquals("😀", Emoji.withSkinTone("😀", 3));
    assertEquals("not an emoji", Emoji.withSkinTone("not an emoji", 3));
    assertNull(Emoji.withSkinTone(null, 3));
  }

  @Test
  public void every_skin_tone_is_uniform()
  {
    for (Emoji e : allEmojis())
    {
      if (!e.hasVariants())
        continue;
      for (int tone = 1; tone <= 5; tone++)
      {
        int modifier = 0x1F3FA + tone;
        String s = str(e.withSkinTone(tone));
        assertTrue(s, s.codePoints().allMatch(c ->
              c == modifier || !isSkinToneModifier(c)));
        assertTrue(s, hasSkinToneModifier(s));
      }
    }
  }

  @Test
  public void every_variant_is_listed()
  {
    int count = 0;
    for (Emoji e : allEmojis())
      count += e.variants().size();
    assertEquals(3944, count);
  }
}
