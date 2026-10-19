package juloo.keyboard2;

import android.content.Context;
import android.media.AudioManager;
import android.view.KeyEvent;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/** Native touch feedback with app-local gain. Android owns the effects,
    system stream volume and audio policy; no audio focus is requested. */
public final class KeypressSound
{
  private final Playback _playback;
  private final Executor _loader;
  private final AtomicBoolean _loading = new AtomicBoolean(false);

  public KeypressSound(Context context)
  {
    this(new NativePlayback(context), task -> new Thread(task, "KeyboardSoundLoader").start());
  }

  KeypressSound(Playback playback, Executor loader)
  {
    _playback = playback;
    _loader = loader;
  }

  /** Called when the view resets, including layout changes and input lifecycle
      events. Touch feedback itself does not request preparation.
      loadSoundEffects can block waiting for AudioService. Keep that work off
      the UI thread and allow only one preparation at a time per view. Repeat
      on subsequent opens because Android may unload effects in the meantime. */
  public void prepare(boolean enabled, int volume)
  {
    if (!enabled || volume <= 0 || !_loading.compareAndSet(false, true))
      return;
    try
    {
      _loader.execute(() -> {
        try { _playback.loadSoundEffects(); }
        catch (RuntimeException e) {} // Playback can still try Android's lazy loading.
        finally { _loading.set(false); }
      });
    }
    catch (RuntimeException e) { _loading.set(false); }
  }

  public void play(KeyValue key, boolean enabled, int volume)
  {
    if (!enabled || volume <= 0)
      return;
    int effect = effect_of_key(key);
    if (effect == -1)
      return;
    try
    {
      if (_playback.getRingerMode() == AudioManager.RINGER_MODE_NORMAL
          && _playback.getSystemVolume() > 0)
        _playback.playSoundEffect(effect, Math.min(volume, 100) / 100.f);
    }
    catch (RuntimeException e)
    {
      // Audio services/policy must never prevent input. Do not log key data.
    }
  }

  /** Pure selection logic; -1 means no sound. Initial contact uses the center
      value after modifiers; repeats use the then-selected value. */
  static int effect_of_key(KeyValue key)
  {
    if (key == null)
      return -1;
    switch (key.getKind())
    {
      case Placeholder:
      case Slider:
        return -1;
      case Editing:
        switch (key.getEditing())
        {
          case SPACE_BAR: return AudioManager.FX_KEYPRESS_SPACEBAR;
          case BACKSPACE:
          case DELETE_WORD:
          case FORWARD_DELETE_WORD: return AudioManager.FX_KEYPRESS_DELETE;
          default: break;
        }
        break;
      case Keyevent:
        switch (key.getKeyevent())
        {
          case KeyEvent.KEYCODE_SPACE: return AudioManager.FX_KEYPRESS_SPACEBAR;
          case KeyEvent.KEYCODE_DEL:
          case KeyEvent.KEYCODE_FORWARD_DEL: return AudioManager.FX_KEYPRESS_DELETE;
          case KeyEvent.KEYCODE_ENTER:
          case KeyEvent.KEYCODE_NUMPAD_ENTER: return AudioManager.FX_KEYPRESS_RETURN;
          default: break;
        }
        break;
      case Char:
        switch (key.getChar())
        {
          case ' ': return AudioManager.FX_KEYPRESS_SPACEBAR;
          case '\n': return AudioManager.FX_KEYPRESS_RETURN;
          default: break;
        }
        break;
      case Event:
        if (key.getEvent() == KeyValue.Event.ACTION)
          return AudioManager.FX_KEYPRESS_RETURN;
        break;
      default: break;
    }
    return AudioManager.FX_KEYPRESS_STANDARD;
  }

  interface Playback
  {
    int getRingerMode();
    int getSystemVolume();
    void loadSoundEffects();
    void playSoundEffect(int effect, float volume);
  }

  private static final class NativePlayback implements Playback
  {
    private final Context _context;
    private volatile AudioManager _audio;

    NativePlayback(Context context)
    {
      _context = context;
    }

    private AudioManager audio()
    {
      if (_audio == null)
        _audio = (AudioManager)_context.getSystemService(Context.AUDIO_SERVICE);
      return _audio;
    }

    public int getRingerMode()
    {
      AudioManager audio = audio();
      return (audio == null) ? AudioManager.RINGER_MODE_SILENT : audio.getRingerMode();
    }

    public int getSystemVolume()
    {
      return audio().getStreamVolume(AudioManager.STREAM_SYSTEM);
    }

    public void loadSoundEffects()
    {
      AudioManager audio = audio();
      if (audio != null)
        audio.loadSoundEffects();
    }

    public void playSoundEffect(int effect, float volume)
    {
      // Explicit gain bypasses SOUND_EFFECTS_ENABLED, not system stream
      // volume/mute or platform policy. Never change global audio settings.
      audio().playSoundEffect(effect, volume);
    }
  }
}
