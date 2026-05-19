package juloo.keyboard2;

import android.os.Handler;
import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;
import org.junit.Test;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import static org.junit.Assert.*;
import juloo.keyboard2.suggestions.Suggestions;

public class HangulInputTest
{
  static class TestReceiver implements KeyEventHandler.IReceiver
  {
    final StringBuilder text = new StringBuilder();
    final InputConnection inputConnection = (InputConnection)Proxy.newProxyInstance(
        InputConnection.class.getClassLoader(),
        new Class<?>[] { InputConnection.class },
        new InvocationHandler()
        {
          @Override
          public Object invoke(Object proxy, Method method, Object[] args)
          {
            String name = method.getName();
            if (name.equals("commitText"))
            {
              text.append((CharSequence)args[0]);
              return true;
            }
            if (name.equals("deleteSurroundingText"))
            {
              int before = (Integer)args[0];
              int start = Math.max(0, text.length() - before);
              text.delete(start, text.length());
              return true;
            }
            if (name.equals("beginBatchEdit") || name.equals("endBatchEdit"))
              return true;
            if (name.equals("sendKeyEvent"))
            {
              KeyEvent event = (KeyEvent)args[0];
              if (event.getAction() == KeyEvent.ACTION_UP
                  && event.getKeyCode() == KeyEvent.KEYCODE_DEL
                  && text.length() > 0)
                text.delete(text.length() - 1, text.length());
              return true;
            }
            if (name.equals("getTextAfterCursor"))
              return "";
            if (name.equals("getCursorCapsMode"))
              return 0;
            if (method.getReturnType() == Boolean.TYPE)
              return false;
            if (method.getReturnType() == Integer.TYPE)
              return 0;
            return null;
          }
        });

    @Override
    public void handle_event_key(KeyValue.Event ev) {}
    @Override
    public void set_shift_state(boolean state, boolean lock) {}
    @Override
    public void set_compose_pending(boolean pending) {}
    @Override
    public void selection_state_changed(boolean selection_is_ongoing) {}
    @Override
    public InputConnection getCurrentInputConnection() { return inputConnection; }
    @Override
    public Handler getHandler() { return null; }
    @Override
    public void set_suggestions(Suggestions suggestions) {}
  }

  private static String typeHangul(String... keys)
  {
    TestReceiver receiver = new TestReceiver();
    KeyEventHandler handler = new KeyEventHandler(receiver, null);
    for (String key : keys)
      handler.key_up(KeyValue.getKeyByName(key), Pointers.Modifiers.EMPTY);
    return receiver.text.toString();
  }

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


  @Test
  public void wordsKeepFinalsBeforeNextInitials()
  {
    assertEquals("만화", typeHangul("ㅁ", "ㅏ", "ㄴ", "ㅎ", "ㅗ", "ㅏ"));
    assertEquals("만화", typeHangul("ㅁ", "ㅏ", "ㄴ", "ㅎ", "ㅘ"));
    assertEquals("한글", typeHangul("ㅎ", "ㅏ", "ㄴ", "ㄱ", "ㅡ", "ㄹ"));
    assertEquals("안녕", typeHangul("ㅇ", "ㅏ", "ㄴ", "ㄴ", "ㅕ", "ㅇ"));
  }

  @Test
  public void plainMedialsStillMoveFinals()
  {
    assertEquals("가나", typeHangul("ㄱ", "ㅏ", "ㄴ", "ㅏ"));
    assertEquals("난예", typeHangul("ㄴ", "ㅏ", "ㄴ", "ㅖ"));
  }
}
