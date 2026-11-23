package juloo.keyboard2;

import android.media.AudioManager;
import android.view.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static juloo.keyboard2.TestUtils.*;
import static org.junit.Assert.*;

public class KeypressSoundTest
{
  static class Playback implements KeypressSound.Playback
  {
    int ringer = AudioManager.RINGER_MODE_NORMAL;
    int checks = 0;
    boolean failCheck = false;
    boolean failVolume = false;
    boolean failPlay = false;
    boolean failLoad = false;
    int systemVolume = 5;
    int loads = 0;
    final List<Float> volumes = new ArrayList<Float>();
    final List<Integer> effects = new ArrayList<Integer>();

    public int getRingerMode()
    {
      checks++;
      if (failCheck) throw new SecurityException("Audio service unavailable");
      return ringer;
    }

    public int getSystemVolume()
    {
      if (failVolume) throw new SecurityException("Volume query unavailable");
      return systemVolume;
    }

    public void loadSoundEffects()
    {
      loads++;
      if (failLoad) throw new IllegalStateException("Loading unavailable");
    }

    public void playSoundEffect(int effect, float volume)
    {
      if (failPlay) throw new IllegalStateException("Audio service unavailable");
      effects.add(effect);
      volumes.add(volume);
    }
  }

  @Test
  public void standard_and_special_keys()
  {
    for (String name : new String[]{"a", "1", "!", "shift", "ctrl", "alt", "fn",
        "compose", "tab", "esc", "copy", "paste", "switch_numeric"})
      assertEquals(name, AudioManager.FX_KEYPRESS_STANDARD, KeypressSound.effect_of_key(key(name)));
    assertEquals(AudioManager.FX_KEYPRESS_STANDARD, KeypressSound.effect_of_key(str("hello")));
  }

  @Test
  public void space_delete_and_return_representations()
  {
    assertEffect(AudioManager.FX_KEYPRESS_SPACEBAR, key("space"), key(" "),
        keyevent(KeyEvent.KEYCODE_SPACE));
    assertEffect(AudioManager.FX_KEYPRESS_DELETE, key("backspace"), key("delete"),
        key("delete_word"), key("forward_delete_word"), keyevent(KeyEvent.KEYCODE_DEL));
    assertEffect(AudioManager.FX_KEYPRESS_RETURN, key("enter"), key("action"),
        KeyValue.makeCharKey('\n'), keyevent(KeyEvent.KEYCODE_NUMPAD_ENTER));
  }

  @Test
  public void invalid_keys_and_sliders_are_silent()
  {
    assertEffect(-1, null, new KeyValue("", KeyValue.Kind.Placeholder,
          KeyValue.Placeholder.REMOVED.ordinal(), 0), key("cursor_left"));
  }

  @Test
  public void disabled_and_invalid_keys_do_not_contact_audio_service()
  {
    Playback playback = new Playback();
    KeypressSound sound = new KeypressSound(playback, Runnable::run);
    sound.play(key("a"), false, 50);
    sound.play(null, true, 50);
    sound.play(key("cursor_left"), true, 50);
    assertEquals(0, playback.checks);
    assertTrue(playback.effects.isEmpty());
  }

  @Test
  public void enabled_and_ringer_changes_take_effect_on_next_action()
  {
    Playback playback = new Playback();
    KeypressSound sound = new KeypressSound(playback, Runnable::run);
    sound.play(key("a"), true, 50);
    playback.ringer = AudioManager.RINGER_MODE_SILENT;
    sound.play(key("a"), true, 50);
    playback.ringer = AudioManager.RINGER_MODE_VIBRATE;
    sound.play(key("a"), true, 50);
    playback.ringer = AudioManager.RINGER_MODE_NORMAL;
    sound.play(key("space"), true, 50);
    sound.play(key("enter"), false, 50);
    assertEquals(Arrays.asList(AudioManager.FX_KEYPRESS_STANDARD,
          AudioManager.FX_KEYPRESS_SPACEBAR), playback.effects);
  }

  @Test
  public void audio_failures_are_contained_and_later_actions_recover()
  {
    Playback playback = new Playback();
    KeypressSound sound = new KeypressSound(playback, Runnable::run);
    playback.failCheck = true;
    sound.play(key("a"), true, 50);
    playback.failCheck = false;
    playback.failVolume = true;
    sound.play(key("a"), true, 50);
    playback.failVolume = false;
    playback.failPlay = true;
    sound.play(key("a"), true, 50);
    playback.failPlay = false;
    sound.play(key("a"), true, 50);
    assertEquals(Arrays.asList(AudioManager.FX_KEYPRESS_STANDARD), playback.effects);
  }

  static KeyValue keyevent(int code)
  {
    return new KeyValue("", KeyValue.Kind.Keyevent, code, 0);
  }

  @Test
  public void app_volume_is_independent_clamped_and_zero_is_silent()
  {
    Playback playback = new Playback();
    KeypressSound sound = new KeypressSound(playback, Runnable::run);
    sound.play(key("a"), true, 0);
    sound.play(key("a"), true, -10);
    sound.play(key("a"), false, 80);
    assertEquals(0, playback.checks);
    sound.play(key("a"), true, 25);
    sound.play(key("a"), true, 80);
    sound.play(key("a"), true, 200);
    playback.systemVolume = 0;
    sound.play(key("a"), true, 100);
    assertEquals(Arrays.asList(0.25f, 0.8f, 1.f), playback.volumes);
  }

  @Test
  public void preparation_is_deferred_coalesced_and_does_not_play()
  {
    Playback playback = new Playback();
    List<Runnable> work = new ArrayList<Runnable>();
    KeypressSound sound = new KeypressSound(playback, work::add);
    sound.prepare(false, 50);
    sound.prepare(true, 0);
    assertTrue(work.isEmpty());
    sound.prepare(true, 50);
    sound.prepare(true, 50);
    assertEquals(1, work.size());
    assertEquals(0, playback.loads);
    sound.play(key("a"), true, 50); // Input never waits for preparation.
    assertEquals(1, playback.effects.size());
    work.remove(0).run();
    assertEquals(1, playback.loads);
    assertEquals(1, playback.effects.size());
    sound.prepare(true, 50); // Another opening may need to reload effects.
    assertEquals(1, work.size());
  }

  @Test
  public void failed_preparation_does_not_block_playback_or_future_preparation()
  {
    Playback playback = new Playback();
    KeypressSound sound = new KeypressSound(playback, Runnable::run);
    playback.failLoad = true;
    sound.prepare(true, 50);
    sound.play(key("a"), true, 50);
    playback.failLoad = false;
    sound.prepare(true, 50);
    assertEquals(2, playback.loads);
    assertEquals(1, playback.effects.size());
    List<Runnable> work = new ArrayList<Runnable>();
    KeypressSound rejected = new KeypressSound(playback, new java.util.concurrent.Executor()
    {
      boolean reject = true;

      public void execute(Runnable task)
      {
        if (reject)
        {
          reject = false;
          throw new java.util.concurrent.RejectedExecutionException();
        }
        work.add(task);
      }
    });
    rejected.prepare(true, 50);
    rejected.play(key("a"), true, 50);
    assertEquals(2, playback.effects.size());
    rejected.prepare(true, 50);
    assertEquals(1, work.size());
    work.get(0).run();
    assertEquals(3, playback.loads);
  }

  static void assertEffect(int effect, KeyValue... keys)
  {
    for (KeyValue key : keys)
      assertEquals(effect, KeypressSound.effect_of_key(key));
  }
}
