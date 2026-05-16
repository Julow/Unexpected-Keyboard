package juloo.keyboard2;

import org.junit.Test;
import static org.junit.Assert.*;

public class HangulInputTest
{
  @Test
  public void doubleInitialsCombine()
  {
    assertEquals(1, KeyEventHandler.combine_double_initial(0));   // ㄱ+ㄱ = ㄲ
    assertEquals(4, KeyEventHandler.combine_double_initial(3));   // ㄷ+ㄷ = ㄸ
    assertEquals(8, KeyEventHandler.combine_double_initial(7));   // ㅂ+ㅂ = ㅃ
    assertEquals(10, KeyEventHandler.combine_double_initial(9));  // ㅅ+ㅅ = ㅆ
    assertEquals(13, KeyEventHandler.combine_double_initial(12)); // ㅈ+ㅈ = ㅉ
    assertEquals(-1, KeyEventHandler.combine_double_initial(2));
  }

  @Test
  public void doubleFinalsCombine()
  {
    assertEquals(2, KeyEventHandler.combine_double_final(1));   // ㄱ+ㄱ = ㄲ
    assertEquals(20, KeyEventHandler.combine_double_final(19)); // ㅅ+ㅅ = ㅆ
    assertEquals(0, KeyEventHandler.combine_double_final(4));
  }

  @Test
  public void compoundMedialsRemainSequential()
  {
    assertEquals(9, KeyEventHandler.combine_medial(8, 0));   // ㅗ+ㅏ = ㅘ
    assertEquals(10, KeyEventHandler.combine_medial(8, 1));  // ㅗ+ㅐ = ㅙ
    assertEquals(11, KeyEventHandler.combine_medial(8, 20)); // ㅗ+ㅣ = ㅚ
    assertEquals(14, KeyEventHandler.combine_medial(13, 4)); // ㅜ+ㅓ = ㅝ
    assertEquals(15, KeyEventHandler.combine_medial(13, 5)); // ㅜ+ㅔ = ㅞ
    assertEquals(16, KeyEventHandler.combine_medial(13, 20)); // ㅜ+ㅣ = ㅟ
    assertEquals(19, KeyEventHandler.combine_medial(18, 20)); // ㅡ+ㅣ = ㅢ
  }

  @Test
  public void finalsStayBeforeIotizedMedials()
  {
    assertTrue(KeyEventHandler.should_move_final_to_next_syllable(0));   // ㅏ: 간아 -> 가나
    assertTrue(KeyEventHandler.should_move_final_to_next_syllable(4));   // ㅓ: 간어 -> 가너
    assertTrue(KeyEventHandler.should_move_final_to_next_syllable(20));  // ㅣ: 간이 -> 가니

    assertFalse(KeyEventHandler.should_move_final_to_next_syllable(2));  // ㅑ: 간야 -> 간야
    assertFalse(KeyEventHandler.should_move_final_to_next_syllable(3));  // ㅒ: 간얘 -> 간얘
    assertFalse(KeyEventHandler.should_move_final_to_next_syllable(6));  // ㅕ: 간여 -> 간여
    assertFalse(KeyEventHandler.should_move_final_to_next_syllable(7));  // ㅖ: 난예 -> 난예
    assertFalse(KeyEventHandler.should_move_final_to_next_syllable(12)); // ㅛ: 간요 -> 간요
    assertFalse(KeyEventHandler.should_move_final_to_next_syllable(17)); // ㅠ: 간유 -> 간유

    assertEquals("난예",
      String.valueOf(KeyEventHandler.make_hangul_syllable(2, 0, 4))
      + String.valueOf(KeyEventHandler.make_hangul_syllable(11, 7, 0)));
  }

  @Test
  public void compoundFinalsSplitToInitialIndices()
  {
    assertEquals(9, KeyEventHandler.split_compound_final_second_initial(3));   // ㄳ→ㅅ
    assertEquals(12, KeyEventHandler.split_compound_final_second_initial(5));  // ㄵ→ㅈ
    assertEquals(18, KeyEventHandler.split_compound_final_second_initial(6));  // ㄶ→ㅎ
    assertEquals(0, KeyEventHandler.split_compound_final_second_initial(9));   // ㄺ→ㄱ
    assertEquals(6, KeyEventHandler.split_compound_final_second_initial(10));  // ㄻ→ㅁ
    assertEquals(7, KeyEventHandler.split_compound_final_second_initial(11));  // ㄼ→ㅂ
    assertEquals(9, KeyEventHandler.split_compound_final_second_initial(12));  // ㄽ→ㅅ
    assertEquals(16, KeyEventHandler.split_compound_final_second_initial(13)); // ㄾ→ㅌ
    assertEquals(17, KeyEventHandler.split_compound_final_second_initial(14)); // ㄿ→ㅍ
    assertEquals(18, KeyEventHandler.split_compound_final_second_initial(15)); // ㅀ→ㅎ
    assertEquals(9, KeyEventHandler.split_compound_final_second_initial(18));  // ㅄ→ㅅ
  }

}
