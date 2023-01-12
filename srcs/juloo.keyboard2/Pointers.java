package juloo.keyboard2;

import android.os.Handler;
import android.os.Message;
import java.util.Arrays;
import java.util.ArrayList;

/**
 * Manage pointers (fingers) on the screen and long presses.
 * Call back to IPointerEventHandler.
 */
public final class Pointers implements Handler.Callback
{
  private Handler _keyrepeat_handler;
  private ArrayList<Pointer> _ptrs = new ArrayList<Pointer>();
  private IPointerEventHandler _handler;
  private Config _config;

  public Pointers(IPointerEventHandler h, Config c)
  {
    _keyrepeat_handler = new Handler(this);
    _handler = h;
    _config = c;
  }

  /** Return the list of modifiers currently activated. */
  public Modifiers getModifiers()
  {
    return getModifiers(false);
  }

  /** When [skip_latched] is true, don't take flags of latched keys into account. */
  private Modifiers getModifiers(boolean skip_latched)
  {
    int n_ptrs = _ptrs.size();
    KeyValue.Modifier[] mods = new KeyValue.Modifier[n_ptrs];
    int n_mods = 0;
    for (int i = 0; i < n_ptrs; i++)
    {
      Pointer p = _ptrs.get(i);
      if (p.value != null && p.value.getKind() == KeyValue.Kind.Modifier
          && !(skip_latched && p.pointerId == -1
            && (p.flags & KeyValue.FLAG_LOCKED) == 0))
        mods[n_mods++] = p.value.getModifier();
    }
    return Modifiers.ofArray(mods, n_mods);
  }

  public void clear()
  {
    for (Pointer p : _ptrs)
      stopKeyRepeat(p);
    _ptrs.clear();
  }

  public boolean isKeyDown(KeyboardData.Key k)
  {
    for (Pointer p : _ptrs)
      if (p.key == k)
        return true;
    return false;
  }

  /**
   * These flags can be different:
   *  FLAG_LOCK   Removed when the key is locked
   *  FLAG_LOCKED Added when the key is locked
   *  FLAG_LATCH  Removed when the key is latched (released but not consumed yet)
   * Returns [-1] if not found.
   */
  public int getKeyFlags(KeyValue kv)
  {
    for (Pointer p : _ptrs)
      if (p.value != null && p.value.equals(kv))
        return p.flags;
    return -1;
  }

  /** Fake pointers are latched and not lockable. */
  public void add_fake_pointer(KeyValue kv, KeyboardData.Key key, boolean locked)
  {
    if (getLatched(key, kv) != null)
      return; // Already latched, don't add an other pointer.
    Pointer ptr = new Pointer(-1, key, kv, 0.f, 0.f, Modifiers.EMPTY);
    ptr.flags &= ~(KeyValue.FLAG_LATCH | KeyValue.FLAG_LOCK);
    ptr.flags |= KeyValue.FLAG_FAKE_PTR;
    if (locked)
      ptr.flags |= KeyValue.FLAG_LOCKED;
    _ptrs.add(ptr);
  }

  public void remove_fake_pointer(KeyValue kv, KeyboardData.Key key)
  {
    Pointer ptr = getLatched(key, kv);
    if (ptr != null && (ptr.flags & KeyValue.FLAG_FAKE_PTR) != 0)
      removePtr(ptr);
  }

  // Receiving events

  public void onTouchUp(int pointerId)
  {
    Pointer ptr = getPtr(pointerId);
    if (ptr == null)
      return;
    stopKeyRepeat(ptr);
    Pointer latched = getLatched(ptr);
    if (latched != null) // Already latched
    {
      removePtr(ptr); // Remove dupplicate
      if ((latched.flags & KeyValue.FLAG_LOCK) != 0) // Toggle lockable key
        lockPointer(latched, false);
      else // Otherwise, unlatch
      {
        removePtr(latched);
        _handler.onPointerUp(ptr.value, ptr.modifiers);
      }
    }
    else if ((ptr.flags & KeyValue.FLAG_LATCH) != 0)
    {
      ptr.flags &= ~KeyValue.FLAG_LATCH;
      ptr.pointerId = -1; // Latch
      _handler.onPointerFlagsChanged(false);
    }
    else
    {
      clearLatched();
      removePtr(ptr);
      _handler.onPointerUp(ptr.value, ptr.modifiers);
    }
  }

  public void onTouchCancel()
  {
    clear();
    _handler.onPointerFlagsChanged(true);
  }

  /* Whether an other pointer is down on a non-special key. */
  private boolean isOtherPointerDown()
  {
    for (Pointer p : _ptrs)
      if (p.pointerId != -1 && (p.flags & KeyValue.FLAG_SPECIAL) == 0)
        return true;
    return false;
  }

  public void onTouchDown(float x, float y, int pointerId, KeyboardData.Key key)
  {
    // Ignore new presses while a modulated key is active. On some devices,
    // ghost touch events can happen while the pointer travels on top of other
    // keys.
    if (isModulatedKeyPressed())
      return;
    // Don't take latched modifiers into account if an other key is pressed.
    // The other key already "own" the latched modifiers and will clear them.
    Modifiers mods = getModifiers(isOtherPointerDown());
    KeyValue value = handleKV(key.key0, mods);
    Pointer ptr = new Pointer(pointerId, key, value, x, y, mods);
    _ptrs.add(ptr);
    startKeyRepeat(ptr);
    _handler.onPointerDown(false);
  }

  /*
   * Get the KeyValue at the given direction. In case of swipe (!= 0), get the
   * nearest KeyValue that is not key0.
   * Take care of applying [_handler.onPointerSwipe] to the selected key, this
   * must be done at the same time to be sure to treat removed keys correctly.
   * Return [null] if no key could be found in the given direction or if the
   * selected key didn't change.
   */
  private KeyValue getKeyAtDirection(Pointer ptr, int direction)
  {
    if (direction == 0)
      return handleKV(ptr.key.key0, ptr.modifiers);
    KeyValue k;
    for (int i = 0; i > -3; i = (~i>>31) - i)
    {
      int d = (direction + i + 8 - 1) % 8 + 1;
      // Don't make the difference between a key that doesn't exist and a key
      // that is removed by [_handler]. Triggers side effects.
      k = _handler.modifyKey(ptr.key.getAtDirection(d), ptr.modifiers);
      if (k != null)
        return k;
    }
    return null;
  }

  private KeyValue handleKV(KeyboardData.Corner c, Modifiers modifiers)
  {
    if (c == null)
      return null;
    return _handler.modifyKey(c.kv, modifiers);
  }

  public void onTouchMove(float x, float y, int pointerId)
  {
    Pointer ptr = getPtr(pointerId);
    if (ptr == null)
      return;

    // The position in a IME windows is clampled to view.
    // For a better up swipe behaviour, set the y position to a negative value when clamped.
    if (y == 0.0) y = -400;

    float dx = x - ptr.downX;
    float dy = y - ptr.downY;
    float dist = Math.abs(dx) + Math.abs(dy);
    ptr.ptrDist = dist;

    int direction;
    if (dist < _config.swipe_dist_px)
    {
      direction = 0;
    }
    else
    {
      // One of the 8 directions:
      // |\2|3/|
      // |1\|/4|
      // |-----|
      // |8/|\5|
      // |/7|6\|
      direction = 1;
      if (dx > 0) direction += 2;
      if (dx > Math.abs(dy) || (dx < 0 && dx > -Math.abs(dy))) direction += 1;
      if (dy > 0) direction = 9 - direction;
    }

    if (direction != ptr.selected_direction)
    {
      ptr.selected_direction = direction;
      KeyValue newValue = getKeyAtDirection(ptr, direction);
      if (newValue != null && (ptr.value == null || !newValue.equals(ptr.value)))
      {
        int old_flags = ptr.flags;
        ptr.value = newValue;
        ptr.flags = newValue.getFlags();
        // Keep the keyrepeat going between modulated keys.
        if ((old_flags & ptr.flags & KeyValue.FLAG_PRECISE_REPEAT) == 0)
        {
          stopKeyRepeat(ptr);
          startKeyRepeat(ptr);
        }
        _handler.onPointerDown(true);
      }
    }
  }

  // Pointers management

  private Pointer getPtr(int pointerId)
  {
    for (Pointer p : _ptrs)
      if (p.pointerId == pointerId)
        return p;
    return null;
  }

  private void removePtr(Pointer ptr)
  {
    _ptrs.remove(ptr);
  }

  private Pointer getLatched(Pointer target)
  {
    return getLatched(target.key, target.value);
  }

  private Pointer getLatched(KeyboardData.Key k, KeyValue v)
  {
    if (v == null)
      return null;
    for (Pointer p : _ptrs)
      if (p.key == k && p.pointerId == -1 && p.value != null && p.value.equals(v))
        return p;
    return null;
  }

  private void clearLatched()
  {
    for (int i = _ptrs.size() - 1; i >= 0; i--)
    {
      Pointer ptr = _ptrs.get(i);
      // Latched and not locked, remove
      if (ptr.pointerId == -1 && (ptr.flags & KeyValue.FLAG_LOCKED) == 0)
        _ptrs.remove(i);
      // Not latched but pressed, don't latch once released and stop long press.
      else if ((ptr.flags & KeyValue.FLAG_LATCH) != 0)
        ptr.flags &= ~KeyValue.FLAG_LATCH;
    }
  }

  /** Make a pointer into the locked state. */
  private void lockPointer(Pointer ptr, boolean shouldVibrate)
  {
    ptr.flags = (ptr.flags & ~KeyValue.FLAG_LOCK) | KeyValue.FLAG_LOCKED;
    _handler.onPointerFlagsChanged(shouldVibrate);
  }

  private boolean isModulatedKeyPressed()
  {
    for (Pointer ptr : _ptrs)
    {
      if ((ptr.flags & KeyValue.FLAG_PRECISE_REPEAT) != 0)
        return true;
    }
    return false;
  }

  // Key repeat

  /** Message from [_keyrepeat_handler]. */
  @Override
  public boolean handleMessage(Message msg)
  {
    for (Pointer ptr : _ptrs)
    {
      if (ptr.timeoutWhat == msg.what)
      {
        if (handleKeyRepeat(ptr))
          _keyrepeat_handler.sendEmptyMessageDelayed(msg.what, nextRepeatInterval(ptr));
        else
          ptr.timeoutWhat = -1;
        return true;
      }
    }
    return false;
  }

  private long nextRepeatInterval(Pointer ptr)
  {
    long t = _config.longPressInterval;
    if (_config.preciseRepeat && (ptr.flags & KeyValue.FLAG_PRECISE_REPEAT) != 0)
    {
      // Modulate repeat interval depending on the distance of the pointer
      t = (long)((float)t * 2.f / modulatePreciseRepeat(ptr));
    }
    return t;
  }

  private static int uniqueTimeoutWhat = 0;

  private void startKeyRepeat(Pointer ptr)
  {
    if (ptr.value == null)
      return;
    int what = (uniqueTimeoutWhat++);
    ptr.timeoutWhat = what;
    long timeout = _config.longPressTimeout;
    // Faster repeat timeout for modulated keys
    if ((ptr.flags & KeyValue.FLAG_PRECISE_REPEAT) != 0)
      timeout /= 2;
    _keyrepeat_handler.sendEmptyMessageDelayed(what, timeout);
  }

  private void stopKeyRepeat(Pointer ptr)
  {
    if (ptr.timeoutWhat != -1)
    {
      _keyrepeat_handler.removeMessages(ptr.timeoutWhat);
      ptr.timeoutWhat = -1;
      ptr.repeatingPtrDist = -1.f;
    }
  }

  /** A pointer is repeating. Returns [true] if repeat should continue. */
  private boolean handleKeyRepeat(Pointer ptr)
  {
    // Long press toggle lock on modifiers
    if ((ptr.flags & KeyValue.FLAG_LATCH) != 0)
    {
      lockPointer(ptr, true);
      return false;
    }
    // Stop repeating: Latched key, special keys
    if (ptr.pointerId == -1 || (ptr.flags & KeyValue.FLAG_SPECIAL) != 0)
      return false;
    _handler.onPointerHold(ptr.value, ptr.modifiers);
    return true;
  }

  private float modulatePreciseRepeat(Pointer ptr)
  {
    if (ptr.repeatingPtrDist < 0.f)
      ptr.repeatingPtrDist = ptr.ptrDist; // First repeat
    if (ptr.ptrDist > ptr.repeatingPtrDist * 2.f)
      ptr.repeatingPtrDist = ptr.ptrDist / 2.f; // Large swipe, move the middle point
    float left = ptr.repeatingPtrDist / 2.f;
    float accel = (ptr.ptrDist - left) / (ptr.repeatingPtrDist - left);
    return Math.min(8.f, Math.max(0.1f, accel));
  }

  private static final class Pointer
  {
    /** -1 when latched. */
    public int pointerId;
    /** The Key pressed by this Pointer */
    public final KeyboardData.Key key;
    /** Current direction. */
    public int selected_direction;
    /** Selected value with [modifiers] applied. */
    public KeyValue value;
    public float downX;
    public float downY;
    /** Distance of the pointer to the initial press. */
    public float ptrDist;
    /** Modifier flags at the time the key was pressed. */
    public Modifiers modifiers;
    /** Flags of the value. Latch, lock and locked flags are updated. */
    public int flags;
    /** Identify timeout messages. */
    public int timeoutWhat;
    /** ptrDist at the first repeat, -1 otherwise. */
    public float repeatingPtrDist;

    public Pointer(int p, KeyboardData.Key k, KeyValue v, float x, float y, Modifiers m)
    {
      pointerId = p;
      key = k;
      selected_direction = 0;
      value = v;
      downX = x;
      downY = y;
      ptrDist = 0.f;
      modifiers = m;
      flags = (v == null) ? 0 : v.getFlags();
      timeoutWhat = -1;
      repeatingPtrDist = -1.f;
    }
  }

  /** Represent modifiers currently activated.
      Sorted in the order they should be evaluated. */
  public static final class Modifiers
  {
    private final KeyValue.Modifier[] _mods;
    private final int _size;

    private Modifiers(KeyValue.Modifier[] m, int s)
    {
      _mods = m; _size = s;
    }

    public KeyValue.Modifier get(int i) { return _mods[_size - 1 - i]; }
    public int size() { return _size; }
    public boolean contains(KeyValue.Modifier mod)
    {
      for (int i = 0; i < _mods.length; i++)
      {
        if (_mods[i] == mod) return true;
      }
      return false;
    }

    @Override
    public int hashCode() { return Arrays.hashCode(_mods); }
    @Override
    public boolean equals(Object obj)
    {
      return Arrays.equals(_mods, ((Modifiers)obj)._mods);
    }

    public static final Modifiers EMPTY =
      new Modifiers(new KeyValue.Modifier[0], 0);

    protected static Modifiers ofArray(KeyValue.Modifier[] mods, int size)
    {
      // Sort and remove duplicates and nulls.
      if (size > 1)
      {
        Arrays.sort(mods, 0, size);
        int j = 0;
        for (int i = 0; i < size; i++)
        {
          KeyValue.Modifier m = mods[i];
          if (m != null && (i + 1 >= size || m != mods[i + 1]))
          {
            mods[j] = m;
            j++;
          }
        }
        size = j;
      }
      return new Modifiers(mods, size);
    }
  }

  public interface IPointerEventHandler
  {
    /** Key can be modified or removed by returning [null]. */
    public KeyValue modifyKey(KeyValue k, Modifiers flags);

    /** A key is pressed. [getModifiers()] is uptodate. Might be called after a
        press or a swipe to a different value. */
    public void onPointerDown(boolean isSwipe);

    /** Key is released. [k] is the key that was returned by
        [modifySelectedKey] or [modifySelectedKey]. */
    public void onPointerUp(KeyValue k, Modifiers flags);

    /** Flags changed because latched or locked keys or cancelled pointers. */
    public void onPointerFlagsChanged(boolean shouldVibrate);

    /** Key is repeating. */
    public void onPointerHold(KeyValue k, Modifiers flags);
  }
}
