package juloo.keyboard2;

import android.os.Handler;
import android.os.Message;
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

  public int getFlags()
  {
    return getFlags(false);
  }

  /* When [skip_latched] is true, don't take flags of latched keys into account. */
  private int getFlags(boolean skip_latched)
  {
    int flags = 0;
    for (Pointer p : _ptrs)
    {
      if (!(skip_latched && p.pointerId == -1 && (p.flags & KeyValue.FLAG_LOCKED) == 0))
        flags |= p.flags;
    }
    return flags;
  }

  public void clear()
  {
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
    // Comparing names because the keys might have been modified.
    // Physical equality works because names are never computed or shared.
    String name = kv.name;
    for (Pointer p : _ptrs)
      if (p.value != null && p.value.name == name)
        return p.flags;
    return -1;
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
      if ((latched.flags & KeyValue.FLAG_LOCK) != 0) // Locking key, toggle lock
      {
        latched.flags = (latched.flags & ~KeyValue.FLAG_LOCK) | KeyValue.FLAG_LOCKED;
        _handler.onPointerFlagsChanged();
      }
      else // Otherwise, unlatch
      {
        removePtr(latched);
        _handler.onPointerUp(ptr.value, ptr.modifier_flags);
      }
    }
    else if ((ptr.flags & KeyValue.FLAG_LATCH) != 0)
    {
      ptr.flags &= ~KeyValue.FLAG_LATCH;
      ptr.pointerId = -1; // Latch
      _handler.onPointerFlagsChanged();
    }
    else
    {
      clearLatched();
      removePtr(ptr);
      _handler.onPointerUp(ptr.value, ptr.modifier_flags);
    }
  }

  public void onTouchCancel(int pointerId)
  {
    Pointer ptr = getPtr(pointerId);
    if (ptr == null)
      return;
    stopKeyRepeat(ptr);
    removePtr(ptr);
    _handler.onPointerFlagsChanged();
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
    int mflags = getFlags(isOtherPointerDown());
    KeyValue value = _handler.onPointerDown(key.key0, mflags);
    Pointer ptr = new Pointer(pointerId, key, 0, value, x, y, mflags);
    _ptrs.add(ptr);
    if (value != null && (value.flags & KeyValue.FLAG_SPECIAL) == 0)
      startKeyRepeat(ptr);
  }

  public void onTouchMove(float x, float y, int pointerId)
  {
    Pointer ptr = getPtr(pointerId);
    if (ptr == null)
      return;
    float dx = x - ptr.downX;
    float dy = y - ptr.downY;
    float dist = Math.abs(dx) + Math.abs(dy);
    ptr.ptrDist = dist;
    int newIndex;
    if (dist < _config.swipe_dist_px)
    {
      newIndex = 0;
    }
    else if (ptr.key.edgekeys)
    {
      if (Math.abs(dy) > Math.abs(dx)) // vertical swipe
        newIndex = (dy < 0) ? 1 : 4;
      else // horizontal swipe
        newIndex = (dx < 0) ? 3 : 2;
    }
    else
    {
      if (dx < 0) // left side
        newIndex = (dy < 0) ? 1 : 3;
      else // right side
        newIndex = (dy < 0) ? 2 : 4;
    }
    if (newIndex != ptr.value_index)
    {
      ptr.value_index = newIndex;
      KeyValue newValue =
        _handler.onPointerSwipe(ptr.key.getValue(newIndex), ptr.modifier_flags);
      if (newValue != null)
      {
        int old_flags = ptr.flags;
        ptr.value = newValue;
        ptr.flags = newValue.flags;
        // Keep the keyrepeat going between modulated keys.
        if ((old_flags & newValue.flags & KeyValue.FLAG_PRECISE_REPEAT) == 0)
        {
          stopKeyRepeat(ptr);
          if ((newValue.flags & KeyValue.FLAG_SPECIAL) == 0)
            startKeyRepeat(ptr);
        }
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
    KeyboardData.Key k = target.key;
    KeyValue v = target.value;
    if (v == null)
      return null;
    for (Pointer p : _ptrs)
      if (p.key == k && p.pointerId == -1 && p.value != null && p.value.name == v.name)
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
      // Not latched but pressed, don't latch once released
      else if ((ptr.flags & KeyValue.FLAG_LATCH) != 0)
        ptr.flags &= ~KeyValue.FLAG_LATCH;
    }
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
        long nextInterval = _config.longPressInterval;
        if (_config.preciseRepeat && (ptr.flags & KeyValue.FLAG_PRECISE_REPEAT) != 0)
        {
          // Slower repeat for modulated keys
          nextInterval *= 2;
          // Modulate repeat interval depending on the distance of the pointer
          nextInterval = (long)((float)nextInterval / modulatePreciseRepeat(ptr));
        }
        _keyrepeat_handler.sendEmptyMessageDelayed(msg.what, nextInterval);
        _handler.onPointerHold(ptr.value, ptr.modifier_flags);
        return (true);
      }
    }
    return (false);
  }

  private static int uniqueTimeoutWhat = 0;

  private void startKeyRepeat(Pointer ptr)
  {
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

  private final class Pointer
  {
    /** -1 when latched. */
    public int pointerId;
    public final KeyboardData.Key key;
    public int value_index;
    /** Modified value. Not equal to [key.getValue(value_index)]. */
    public KeyValue value;
    public float downX;
    public float downY;
    /** Distance of the pointer to the initial press. */
    public float ptrDist;
    /** Modifier flags at the time the key was pressed. */
    public int modifier_flags;
    /** Flags of the value. Latch, lock and locked flags are updated. */
    public int flags;
    /** Identify timeout messages. */
    public int timeoutWhat;
    /** ptrDist at the first repeat, -1 otherwise. */
    public float repeatingPtrDist;

    public Pointer(int p, KeyboardData.Key k, int vi, KeyValue v, float x, float y, int mflags)
    {
      pointerId = p;
      key = k;
      value_index = vi;
      value = v;
      downX = x;
      downY = y;
      ptrDist = 0.f;
      modifier_flags = mflags;
      flags = (v == null) ? 0 : v.flags;
      timeoutWhat = -1;
      repeatingPtrDist = -1.f;
    }
  }

  public interface IPointerEventHandler
  {
    /** A key is pressed. Key can be modified or removed by returning [null].
        [getFlags()] is not uptodate. */
    public KeyValue onPointerDown(KeyValue k, int flags);

    /** Pointer swipes into a corner. Key can be modified or removed. */
    public KeyValue onPointerSwipe(KeyValue k, int flags);

    /** Key is released. [k] is the key that was returned by [onPointerDown] or
        [onPointerSwipe]. */
    public void onPointerUp(KeyValue k, int flags);

    /** Flags changed because latched or locked keys or cancelled pointers. */
    public void onPointerFlagsChanged();

    /** Key is repeating. */
    public void onPointerHold(KeyValue k, int flags);
  }
}
