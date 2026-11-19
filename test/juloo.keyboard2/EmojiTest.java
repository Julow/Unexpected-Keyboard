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
  public void groups_contain_no_gender_variants()
  {
    List<Emoji> all = allEmojis();
    assertTrue(all.contains(emoji("🏃")));
    assertFalse(all.contains(emoji("🏃‍♂️")));
    assertFalse(all.contains(emoji("🏃‍♀️")));
    assertFalse(all.contains(emoji("👨‍👩‍👦")));
    assertFalse(all.contains(emoji("👫")));
  }

  @Test
  public void group_boundaries()
  {
    assertEquals("😀", str(Emoji.getEmojisByGroup(0).get(0)));
    assertEquals("👋", str(Emoji.getEmojisByGroup(1).get(0)));
    assertEquals("🏁", str(Emoji.getEmojisByGroup(8).get(0)));
  }

  static String variant(String emoji, int skinTone, int gender)
  {
    return str(emoji(emoji).withSkinToneAndGender(skinTone, gender));
  }

  @Test
  public void emoji_without_variants()
  {
    Emoji e = emoji("😀");
    assertFalse(e.hasVariants());
    assertSame(e, e.withSkinToneAndGender(Emoji.NO_SKIN_TONE, Emoji.NON_SPECIFIC_GENDER));
    assertSame(e, e.withSkinToneAndGender(3, Emoji.FEMALE));
  }

  @Test
  public void skin_tone_variants()
  {
    Emoji e = emoji("👋");
    assertTrue(e.hasVariants());
    assertEquals(6, e.variants().size());
    assertEquals("👋", variant("👋", Emoji.NO_SKIN_TONE, Emoji.NON_SPECIFIC_GENDER));
    assertEquals("👋🏻", variant("👋", 1, Emoji.NON_SPECIFIC_GENDER));
    assertEquals("👋🏽", variant("👋", 3, Emoji.MALE));
    assertEquals("👋🏿", variant("👋", 5, Emoji.FEMALE));
  }

  @Test
  public void base_with_variation_selector()
  {
    // The base contains U+FE0F but its toned variants don't.
    assertEquals("🖐🏽", variant("🖐️", 3, Emoji.NON_SPECIFIC_GENDER));
  }

  @Test
  public void gender_variants()
  {
    assertEquals(3, emoji("🧞").variants().size());
    assertFalse(emoji("🧞").hasSkinTones());
    assertEquals("🧞", variant("🧞", 3, Emoji.NON_SPECIFIC_GENDER));
    assertEquals("🧞‍♂️", variant("🧞", 3, Emoji.MALE));
    assertEquals("🧞‍♀️", variant("🧞", 3, Emoji.FEMALE));
  }

  @Test
  public void skin_tone_and_gender_variants()
  {
    assertEquals(18, emoji("🏃").variants().size());
    assertTrue(emoji("🏃").hasSkinTones());
    assertEquals("🏃‍♂️", variant("🏃", Emoji.NO_SKIN_TONE, Emoji.MALE));
    assertEquals("🏃🏽‍♀️", variant("🏃", 3, Emoji.FEMALE));
    assertEquals("🧑🏻‍⚕️", variant("🧑‍⚕️", 1, Emoji.NON_SPECIFIC_GENDER));
    assertEquals("👨🏻‍⚕️", variant("🧑‍⚕️", 1, Emoji.MALE));
    assertEquals("👩🏿‍⚕️", variant("🧑‍⚕️", 5, Emoji.FEMALE));
  }

  @Test
  public void variants_ordered_by_skin_tone_then_gender()
  {
    List<Emoji> vs = emoji("🏃").variants();
    String[] expected = { "🏃", "🏃‍♂️", "🏃‍♀️", "🏃🏻", "🏃🏻‍♂️", "🏃🏻‍♀️" };
    for (int i = 0; i < expected.length; i++)
      assertEquals(expected[i], str(vs.get(i)));
  }

  @Test
  public void irregular_gender_names()
  {
    assertEquals("👦🏼", variant("🧒", 2, Emoji.MALE));
    assertEquals("👵", variant("🧓", Emoji.NO_SKIN_TONE, Emoji.FEMALE));
    assertEquals("🤴🏾", variant("🫅", 4, Emoji.MALE));
    assertEquals("🧜‍♀️", variant("🧜", Emoji.NO_SKIN_TONE, Emoji.FEMALE));
    // The non-specific version comes after the gendered ones in Unicode.
    assertEquals("🎅🏻", variant("🧑‍🎄", 1, Emoji.MALE));
    assertEquals("🤶", variant("🧑‍🎄", Emoji.NO_SKIN_TONE, Emoji.FEMALE));
    assertEquals("👱🏻‍♀️", variant("👱", 1, Emoji.FEMALE));
    assertEquals("🧔‍♂️", variant("🧔", Emoji.NO_SKIN_TONE, Emoji.MALE));
  }

  @Test
  public void dancers_are_not_grouped()
  {
    assertEquals(6, emoji("🕺").variants().size());
    assertEquals(6, emoji("💃").variants().size());
    assertEquals("💃", variant("💃", Emoji.NO_SKIN_TONE, Emoji.MALE));
  }

  @Test
  public void multi_person_skin_tones()
  {
    Emoji e = emoji("🤝");
    assertEquals(26, e.variants().size());
    assertEquals("🤝🏻", variant("🤝", 1, Emoji.NON_SPECIFIC_GENDER));
    assertTrue(e.variants().contains(emoji("🫱🏻‍🫲🏼")));
    Emoji bunny = emoji("👯");
    assertEquals(26 * 3, bunny.variants().size());
    assertTrue(bunny.variants().contains(emoji("🧑🏻‍🐰‍🧑🏼")));
    assertEquals("👯🏻‍♀️", variant("👯", 1, Emoji.FEMALE));
  }

  @Test
  public void couples_ignore_default_gender()
  {
    Emoji kiss = emoji("💏");
    assertEquals(26 * 4, kiss.variants().size());
    assertEquals("💏🏼", variant("💏", 2, Emoji.MALE));
    assertEquals("💑", variant("💑", Emoji.NO_SKIN_TONE, Emoji.FEMALE));
    assertEquals("🧑🏻‍🤝‍🧑🏻", variant("🧑‍🤝‍🧑", 1, Emoji.MALE));
    assertTrue(kiss.variants().contains(emoji("👩🏽‍❤️‍💋‍👨🏿")));
    assertTrue(emoji("🧑‍🤝‍🧑").variants().contains(emoji("👫🏾")));
  }

  @Test
  public void families()
  {
    Emoji family = emoji("👪");
    assertEquals(30, family.variants().size());
    assertEquals("👪", variant("👪", 3, Emoji.MALE));
    assertTrue(family.variants().contains(emoji("👨‍👩‍👧‍👦")));
    assertTrue(family.variants().contains(emoji("🧑‍🧑‍🧒")));
  }

  @Test
  public void variant_shares_variants_of_its_base()
  {
    Emoji base = emoji("🏃");
    Emoji variant = emoji("🏃🏽‍♀️");
    assertNotSame(base, variant);
    assertTrue(variant.hasVariants());
    assertSame(base.variants(), variant.variants());
    assertEquals(base.rowWidth(), variant.rowWidth());
  }

  @Test
  public void specified_variations_are_kept()
  {
    assertEquals("🏃🏻‍♂️", variant("🏃🏻‍♂️", 5, Emoji.FEMALE));
    assertEquals("🏃🏽‍♀️", variant("🏃‍♀️", 3, Emoji.MALE));
    assertEquals("🏃🏻‍♀️", variant("🏃🏻", 5, Emoji.FEMALE));
    assertEquals("👸", variant("👸", Emoji.NO_SKIN_TONE, Emoji.NON_SPECIFIC_GENDER));
    assertEquals("👩🏽", variant("👩", 3, Emoji.NON_SPECIFIC_GENDER));
    assertEquals("👨‍👩‍👦", variant("👨‍👩‍👦", 3, Emoji.MALE));
    assertEquals("👩🏼‍❤️‍💋‍👩🏼", variant("👩‍❤️‍💋‍👩", 2, Emoji.MALE));
    assertEquals("🫱🏻‍🫲🏼", variant("🫱🏻‍🫲🏼", 5, Emoji.NON_SPECIFIC_GENDER));
    assertEquals("🧑🏻‍❤️‍💋‍🧑🏼", variant("🧑🏻‍❤️‍💋‍🧑🏼", 3, Emoji.FEMALE));
  }

  @Test
  public void variant_of_string()
  {
    assertEquals("👋🏽", Emoji.withSkinToneAndGender("👋", 3, Emoji.MALE));
    assertEquals("🏃🏿‍♀️", Emoji.withSkinToneAndGender("🏃", 5, Emoji.FEMALE));
    assertEquals("👸🏻", Emoji.withSkinToneAndGender("👸", 1, Emoji.NON_SPECIFIC_GENDER));
    assertEquals("😀", Emoji.withSkinToneAndGender("😀", 3, Emoji.MALE));
    assertEquals("not an emoji", Emoji.withSkinToneAndGender("not an emoji", 3, Emoji.MALE));
    assertNull(Emoji.withSkinToneAndGender(null, 3, Emoji.MALE));
  }

  @Test
  public void every_skin_tone_is_uniform()
  {
    for (Emoji e : allEmojis())
    {
      if (!e.hasSkinTones())
        continue;
      for (int tone = 1; tone <= 5; tone++)
      {
        int modifier = 0x1F3FA + tone;
        for (int gender = 0; gender < Emoji.GENDERS; gender++)
        {
          String s = str(e.withSkinToneAndGender(tone, gender));
          assertTrue(s, s.codePoints().allMatch(c ->
                c == modifier || !isSkinToneModifier(c)));
          assertTrue(s, hasSkinToneModifier(s));
        }
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
