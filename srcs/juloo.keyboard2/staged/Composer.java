package juloo.keyboard2.staged;

import java.util.Collections;
import java.util.List;

/** An input method that keeps the text being typed in the composing region of
    the editor, so that it can be rewritten as more keys are pressed.

    Scripts such as Hangul, Kana and Pinyin cannot be typed one key at a time:
    a character is made of several keys, and a key can also change the
    characters that were typed before it. For example, typing ㅅㅏㄹ then ㅏ on a
    Korean layout must rewrite 살 into 사라. Committing each key immediately, as
    the keyboard does for other scripts, makes that impossible.

    Instead, a [Composer] accumulates the keys in a buffer and
    [KeyEventHandler]({@link juloo.keyboard2.KeyEventHandler}) sends the buffer
    to the editor as composing text. The composing text belongs to the editor
    until it is committed, so it can be replaced at will.

    One composer is active at a time, depending on the `script` attribute of
    the current layout. A composer must not depend on the Android framework:
    the layouts and the composition algorithm are plain Java so that they can
    be tested without a device. */
public interface Composer
{
  /** A key that writes [s] was pressed. Return [true] when the key was
      consumed by the composition, in which case the caller sends
      [text] to the editor again. Return [false] when the key must be handled
      as usual, after the composition has been committed. */
  public boolean type(String s);

  /** The backspace key was pressed. Return [true] when the key was consumed.
      Return [false] when there is nothing to erase in the composition,
      leaving the key to the editor. */
  public boolean backspace();

  /** The text currently being composed. Never [null]. Empty when nothing is
      being composed. */
  public String text();

  /** Candidates that can replace the whole composition, the best one first.
      Empty when the composition does not have candidates, which is the case
      of scripts that are typed character by character, such as Hangul and
      Kana. */
  public List<String> candidates();

  /** Forget the composition without committing it. */
  public void reset();

  /** A composer that never composes anything, used for scripts that are typed
      one key at a time. */
  public static final Composer NONE = new Composer()
  {
    public boolean type(String s) { return false; }
    public boolean backspace() { return false; }
    public String text() { return ""; }
    public List<String> candidates() { return Collections.emptyList(); }
    public void reset() {}
  };
}
