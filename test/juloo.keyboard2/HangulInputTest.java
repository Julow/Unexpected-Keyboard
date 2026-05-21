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

  private static void type(KeyEventHandler handler, String key)
  {
    handler.key_up(KeyValue.getKeyByName(key), Pointers.Modifiers.EMPTY);
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
  public void compoundFinalsSplitToInitialIndices()
  {
    assertEquals(0, KeyEventHandler.split_compound_final_second_initial(2));   // ㄲ→ㄱ
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
    assertEquals(9, KeyEventHandler.split_compound_final_second_initial(20));  // ㅆ→ㅅ
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
  public void simpleBatchimWordsKeepFinalsBeforeNextInitials()
  {
    assertEquals("한국", typeHangul("ㅎ", "ㅏ", "ㄴ", "ㄱ", "ㅜ", "ㄱ"));
    assertEquals("감기", typeHangul("ㄱ", "ㅏ", "ㅁ", "ㄱ", "ㅣ"));
    assertEquals("밥상", typeHangul("ㅂ", "ㅏ", "ㅂ", "ㅅ", "ㅏ", "ㅇ"));
    assertEquals("꽃길", typeHangul("ㄲ", "ㅗ", "ㅊ", "ㄱ", "ㅣ", "ㄹ"));
    assertEquals("하면", typeHangul("ㅎ", "ㅏ", "ㅁ", "ㅕ", "ㄴ"));
  }

  @Test
  public void iotizedMedialsMoveSimpleFinals()
  {
    assertEquals("가냐", typeHangul("ㄱ", "ㅏ", "ㄴ", "ㅑ"));
    assertEquals("가냬", typeHangul("ㄱ", "ㅏ", "ㄴ", "ㅒ"));
    assertEquals("가녀", typeHangul("ㄱ", "ㅏ", "ㄴ", "ㅕ"));
    assertEquals("가녜", typeHangul("ㄱ", "ㅏ", "ㄴ", "ㅖ"));
    assertEquals("가뇨", typeHangul("ㄱ", "ㅏ", "ㄴ", "ㅛ"));
    assertEquals("가뉴", typeHangul("ㄱ", "ㅏ", "ㄴ", "ㅠ"));
  }

  @Test
  public void directDoubleFinalsMoveAsWholeFinals()
  {
    assertEquals("바께", typeHangul("ㅂ", "ㅏ", "ㄲ", "ㅔ"));
    assertEquals("이써", typeHangul("ㅇ", "ㅣ", "ㅆ", "ㅓ"));
  }

  @Test
  public void sequentialDoubleFinalsSplitBeforeMedials()
  {
    assertEquals("박게", typeHangul("ㅂ", "ㅏ", "ㄱ", "ㄱ", "ㅔ"));
    assertEquals("잇서", typeHangul("ㅇ", "ㅣ", "ㅅ", "ㅅ", "ㅓ"));
  }

  @Test
  public void compoundBatchimWordsKeepFinalsBeforeNextInitials()
  {
    assertEquals("읽다", typeHangul("ㅇ", "ㅣ", "ㄹ", "ㄱ", "ㄷ", "ㅏ"));
    assertEquals("많다", typeHangul("ㅁ", "ㅏ", "ㄴ", "ㅎ", "ㄷ", "ㅏ"));
    assertEquals("없다", typeHangul("ㅇ", "ㅓ", "ㅂ", "ㅅ", "ㄷ", "ㅏ"));
    assertEquals("앉다", typeHangul("ㅇ", "ㅏ", "ㄴ", "ㅈ", "ㄷ", "ㅏ"));
    assertEquals("핥다", typeHangul("ㅎ", "ㅏ", "ㄹ", "ㅌ", "ㄷ", "ㅏ"));
    assertEquals("읊다", typeHangul("ㅇ", "ㅡ", "ㄹ", "ㅍ", "ㄷ", "ㅏ"));
    assertEquals("싫다", typeHangul("ㅅ", "ㅣ", "ㄹ", "ㅎ", "ㄷ", "ㅏ"));
  }

  @Test
  public void compoundBatchimSplitsBeforePlainMedials()
  {
    assertEquals("넉서", typeHangul("ㄴ", "ㅓ", "ㄱ", "ㅅ", "ㅓ"));
    assertEquals("안자", typeHangul("ㅇ", "ㅏ", "ㄴ", "ㅈ", "ㅏ"));
    assertEquals("만하", typeHangul("ㅁ", "ㅏ", "ㄴ", "ㅎ", "ㅏ"));
    assertEquals("달가", typeHangul("ㄷ", "ㅏ", "ㄹ", "ㄱ", "ㅏ"));
    assertEquals("살마", typeHangul("ㅅ", "ㅏ", "ㄹ", "ㅁ", "ㅏ"));
    assertEquals("발바", typeHangul("ㅂ", "ㅏ", "ㄹ", "ㅂ", "ㅏ"));
    assertEquals("골사", typeHangul("ㄱ", "ㅗ", "ㄹ", "ㅅ", "ㅏ"));
    assertEquals("할타", typeHangul("ㅎ", "ㅏ", "ㄹ", "ㅌ", "ㅏ"));
    assertEquals("을퍼", typeHangul("ㅇ", "ㅡ", "ㄹ", "ㅍ", "ㅓ"));
    assertEquals("실허", typeHangul("ㅅ", "ㅣ", "ㄹ", "ㅎ", "ㅓ"));
    assertEquals("업서", typeHangul("ㅇ", "ㅓ", "ㅂ", "ㅅ", "ㅓ"));
  }

  @Test
  public void explicitIeungKeepsCompoundBatchimBeforeMedials()
  {
    assertEquals("간아", typeHangul("ㄱ", "ㅏ", "ㄴ", "ㅇ", "ㅏ"));
    assertEquals("난예", typeHangul("ㄴ", "ㅏ", "ㄴ", "ㅇ", "ㅖ"));
    assertEquals("많아", typeHangul("ㅁ", "ㅏ", "ㄴ", "ㅎ", "ㅇ", "ㅏ"));
    assertEquals("읽어", typeHangul("ㅇ", "ㅣ", "ㄹ", "ㄱ", "ㅇ", "ㅓ"));
    assertEquals("없어", typeHangul("ㅇ", "ㅓ", "ㅂ", "ㅅ", "ㅇ", "ㅓ"));
    assertEquals("앉아", typeHangul("ㅇ", "ㅏ", "ㄴ", "ㅈ", "ㅇ", "ㅏ"));
  }

  @Test
  public void expectedSelectionUpdatesKeepHangulState()
  {
    TestReceiver receiver = new TestReceiver();
    KeyEventHandler handler = new KeyEventHandler(receiver, null);

    type(handler, "ㄱ");
    handler.selection_updated(0, 1, 1);
    type(handler, "ㅏ");

    assertEquals("가", receiver.text.toString());
  }

  @Test
  public void externalSelectionChangesResetHangulState()
  {
    TestReceiver receiver = new TestReceiver();
    KeyEventHandler handler = new KeyEventHandler(receiver, null);

    type(handler, "ㄱ");
    type(handler, "ㅏ");
    type(handler, "ㄴ");
    handler.selection_updated(1, 0, 0);
    type(handler, "ㅏ");

    assertEquals("간ㅏ", receiver.text.toString());
  }

  @Test
  public void plainMedialsStillMoveFinals()
  {
    assertEquals("가나", typeHangul("ㄱ", "ㅏ", "ㄴ", "ㅏ"));
    assertEquals("나녜", typeHangul("ㄴ", "ㅏ", "ㄴ", "ㅖ"));
    assertEquals("적극적", typeHangul("ㅈ", "ㅓ", "ㄱ", "ㄱ", "ㅡ", "ㄱ", "ㅈ", "ㅓ", "ㄱ"));
  }
}
