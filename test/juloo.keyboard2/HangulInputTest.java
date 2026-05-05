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
}
