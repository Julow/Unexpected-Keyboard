package juloo.keyboard2;

import android.os.Handler;
import android.os.Message;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

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

  public int getKeyFlags(KeyboardData.Key key, KeyValue kv)
  {
    Pointer ptr = getLatched(key, kv);
    if (ptr == null) return -1;
    return ptr.flags;
  }

  /** Fake pointers are latched and not lockable. */
  public void add_fake_pointer(KeyValue kv, KeyboardData.Key key, boolean locked)
  {
    Pointer ptr = getLatched(key, kv);
    if (ptr != null)
      removePtr(ptr); // Already latched, replace pointer.
    ptr = new Pointer(-1, key, kv, 0.f, 0.f, Modifiers.EMPTY);
    ptr.flags &= ~(KeyValue.FLAG_LATCH | KeyValue.FLAG_LOCK);
    ptr.flags |= KeyValue.FLAG_FAKE_PTR;
    if (locked)
      ptr.flags |= KeyValue.FLAG_LOCKED;
    _ptrs.add(ptr);
    _handler.onPointerFlagsChanged(false);
  }

  public void remove_fake_pointer(KeyValue kv, KeyboardData.Key key)
  {
    Pointer ptr = getLatched(key, kv);
    if (ptr != null && (ptr.flags & KeyValue.FLAG_FAKE_PTR) != 0)
      removePtr(ptr);
    _handler.onPointerFlagsChanged(false);
  }

  // Receiving events

  public void onTouchUp(int pointerId)
  {
    Pointer ptr = getPtr(pointerId);
    if (ptr == null)
      return;
    if (ptr.sliding)
    {
      clearLatched();
      onTouchUp_sliding(ptr);
      return;
    }
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
    // Ignore new presses while a sliding key is active. On some devices, ghost
    // touch events can happen while the pointer travels on top of other keys.
    if (isSliding())
      return;
    // Don't take latched modifiers into account if an other key is pressed.
    // The other key already "own" the latched modifiers and will clear them.
    Modifiers mods = getModifiers(isOtherPointerDown());
    KeyValue value = _handler.modifyKey(key.keys[0], mods);
    Pointer ptr = new Pointer(pointerId, key, value, x, y, mods);
    _ptrs.add(ptr);
    startKeyRepeat(ptr);
    _handler.onPointerDown(value, false);
  }

  static final int[] DIRECTION_TO_INDEX = new int[]{
    7, 2, 2, 6, 6, 4, 4, 8, 8, 3, 3, 5, 5, 1, 1, 7
  };

  /**
   * [direction] is an int between [0] and [15] that represent 16 sections of a
   * circle, clockwise, starting at the top.
   */
  KeyValue getKeyAtDirection(KeyboardData.Key k, int direction)
  {
    return k.keys[DIRECTION_TO_INDEX[direction]];
  }

  /*
   * Get the KeyValue at the given direction. In case of swipe (direction !=
   * null), get the nearest KeyValue that is not key0.
   * Take care of applying [_handler.onPointerSwipe] to the selected key, this
   * must be done at the same time to be sure to treat removed keys correctly.
   * Return [null] if no key could be found in the given direction or if the
   * selected key didn't change.
   */
  private KeyValue getNearestKeyAtDirection(Pointer ptr, Integer direction)
  {
    if (direction == null)
      return _handler.modifyKey(ptr.key.keys[0], ptr.modifiers);
    KeyValue k;
    // [i] is [0, -1, 1, -2, 2, ...]
    for (int i = 0; i > -4; i = (~i>>31) - i)
    {
      int d = (direction + i + 16) % 16;
      // Don't make the difference between a key that doesn't exist and a key
      // that is removed by [_handler]. Triggers side effects.
      k = _handler.modifyKey(getKeyAtDirection(ptr.key, d), ptr.modifiers);
      if (k != null)
        return k;
    }
    return null;
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

    if (ptr.sliding)
    {
      onTouchMove_sliding(ptr, dx);
      return;
    }

    float dist = Math.abs(dx) + Math.abs(dy);
    Integer direction;
    if (dist < _config.swipe_dist_px)
    {
      direction = null;
    }
    else
    {
      // See [getKeyAtDirection()] for the meaning. The starting point on the
      // circle is the top direction.
      double a = Math.atan2(dy, dx) + Math.PI;
      // a is between 0 and 2pi, 0 is pointing to the left
      // add 12 to align 0 to the top
      direction = ((int)(a * 8 / Math.PI) + 12) % 16;
    }

    if (direction != ptr.selected_direction)
    {
      ptr.selected_direction = direction;
      KeyValue newValue = getNearestKeyAtDirection(ptr, direction);
      if (newValue != null && !newValue.equals(ptr.value))
      {
        ptr.value = newValue;
        ptr.flags = newValue.getFlags();
        // Sliding mode is entered when key5 or key6 is down on a slider key.
        if (ptr.key.slider &&
            (newValue.equals(ptr.key.getKeyValue(5))
             || newValue.equals(ptr.key.getKeyValue(6))))
        {
          startSliding(ptr, dy);
        }
        _handler.onPointerDown(newValue, true);
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

  boolean isSliding()
  {
    for (Pointer ptr : _ptrs)
      if (ptr.sliding)
        return true;
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
          _keyrepeat_handler.sendEmptyMessageDelayed(msg.what,
              _config.longPressInterval);
        else
          ptr.timeoutWhat = -1;
        return true;
      }
    }
    return false;
  }

  private static int uniqueTimeoutWhat = 0;

  private void startKeyRepeat(Pointer ptr)
  {
    int what = (uniqueTimeoutWhat++);
    ptr.timeoutWhat = what;
    _keyrepeat_handler.sendEmptyMessageDelayed(what, _config.longPressTimeout);
  }

  private void stopKeyRepeat(Pointer ptr)
  {
    if (ptr.timeoutWhat != -1)
    {
      _keyrepeat_handler.removeMessages(ptr.timeoutWhat);
      ptr.timeoutWhat = -1;
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
    // Stop repeating: Latched key, no key
    if (ptr.pointerId == -1 || ptr.value == null)
      return false;
    KeyValue kv = KeyModifier.modify_long_press(ptr.value);
    if (!kv.equals(ptr.value))
    {
      ptr.value = kv;
      ptr.flags = kv.getFlags();
      _handler.onPointerDown(kv, true);
      return true;
    }
    // Stop repeating: Special keys
    if (kv.hasFlags(KeyValue.FLAG_SPECIAL))
      return false;
    _handler.onPointerHold(kv, ptr.modifiers);
    return true;
  }

  // Sliding

  void startSliding(Pointer ptr, float initial_dy)
  {
    stopKeyRepeat(ptr);
    ptr.sliding = true;
    ptr.sliding_count = (int)(initial_dy / _config.slide_step_px);
  }

  /** Handle a sliding pointer going up. Latched modifiers are not cleared to
      allow easy adjustments to the cursors. The pointer is cancelled. */
  void onTouchUp_sliding(Pointer ptr)
  {
    removePtr(ptr);
    _handler.onPointerFlagsChanged(false);
  }

  /** Handle move events for sliding pointers. [dx] is distance travelled from
      [downX]. */
  void onTouchMove_sliding(Pointer ptr, float dx)
  {
    int count = (int)(dx / _config.slide_step_px);
    if (count == ptr.sliding_count)
      return;
    int key_index = (count < ptr.sliding_count) ? 5 : 6;
    KeyValue newValue = _handler.modifyKey(ptr.key.keys[key_index], ptr.modifiers);
    ptr.sliding_count = count;
    ptr.value = newValue;
    if (newValue != null)
      _handler.onPointerHold(newValue, ptr.modifiers);
  }

  private static final class Pointer
  {
    /** -1 when latched. */
    public int pointerId;
    /** The Key pressed by this Pointer */
    public final KeyboardData.Key key;
    /** Current direction. [null] means not swiping. */
    public Integer selected_direction;
    /** Selected value with [modifiers] applied. */
    public KeyValue value;
    public float downX;
    public float downY;
    /** Modifier flags at the time the key was pressed. */
    public Modifiers modifiers;
    /** Flags of the value. Latch, lock and locked flags are updated. */
    public int flags;
    /** Identify timeout messages. */
    public int timeoutWhat;
    /** Whether the pointer is "sliding" laterally on a key. */
    public boolean sliding;
    /** Number of event already caused by sliding. */
    public int sliding_count;

    public Pointer(int p, KeyboardData.Key k, KeyValue v, float x, float y, Modifiers m)
    {
      pointerId = p;
      key = k;
      selected_direction = null;
      value = v;
      downX = x;
      downY = y;
      modifiers = m;
      flags = (v == null) ? 0 : v.getFlags();
      timeoutWhat = -1;
      sliding = false;
      sliding_count = 0;
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
    public boolean has(KeyValue.Modifier m)
    {
      return (Arrays.binarySearch(_mods, 0, _size, m) >= 0);
    }

    /** Returns the activated modifiers that are not in [m2]. */
    public Iterator<KeyValue.Modifier> diff(Modifiers m2)
    {
      return new ModifiersDiffIterator(this, m2);
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

    /** Returns modifiers that are in [m1_] but not in [m2_]. */
    static final class ModifiersDiffIterator
        implements Iterator<KeyValue.Modifier>
    {
      Modifiers m1;
      int i1 = 0;
      Modifiers m2;
      int i2 = 0;

      public ModifiersDiffIterator(Modifiers m1_, Modifiers m2_)
      {
        m1 = m1_;
        m2 = m2_;
        advance();
      }

      public boolean hasNext()
      {
        return i1 < m1._size;
      }

      public KeyValue.Modifier next()
      {
        if (i1 >= m1._size)
          throw new NoSuchElementException();
        KeyValue.Modifier m = m1._mods[i1];
        i1++;
        advance();
        return m;
      }

      /** Advance to the next element if [i1] is not a valid element. The end
          is reached when [i1 = m1.size()].  */
      void advance()
      {
        while (i1 < m1.size())
        {
          KeyValue.Modifier m = m1._mods[i1];
          while (true)
          {
            if (i2 >= m2._size)
              return;
            int d = m.compareTo(m2._mods[i2]);
            if (d < 0)
              return;
            i2++;
            if (d == 0)
              break;
          }
          i1++;
        }
      }
    }
  }

  public interface IPointerEventHandler
  {
    /** Key can be modified or removed by returning [null]. */
    public KeyValue modifyKey(KeyValue k, Modifiers flags);

    /** A key is pressed. [getModifiers()] is uptodate. Might be called after a
        press or a swipe to a different value. Down events are not paired with
        up events. */
    public void onPointerDown(KeyValue k, boolean isSwipe);

    /** Key is released. [k] is the key that was returned by
        [modifySelectedKey] or [modifySelectedKey]. */
    public void onPointerUp(KeyValue k, Modifiers flags);

    /** Flags changed because latched or locked keys or cancelled pointers. */
    public void onPointerFlagsChanged(boolean shouldVibrate);

    /** Key is repeating. */
    public void onPointerHold(KeyValue k, Modifiers flags);
  }
}
