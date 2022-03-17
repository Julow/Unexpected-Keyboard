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
    int flags = 0;
    for (Pointer p : _ptrs)
      flags |= p.flags;
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
    for (Pointer p : _ptrs)
      if (p.value != null && p.value.name == kv.name) // Physical equality
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
    Pointer latched = getLatched(ptr.value);
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
        _handler.onPointerUp(ptr.value);
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
      _handler.onPointerUp(ptr.value);
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

  public void onTouchDown(float x, float y, int pointerId, KeyboardData.Key key)
  {
    // Ignore new presses while a modulated key is active. On some devices,
    // ghost touch events can happen while the pointer travels on top of other
    // keys.
    if (isModulatedKeyPressed())
      return;
    KeyValue value = key.key0;
    Pointer ptr = new Pointer(pointerId, key, value, x, y);
    _ptrs.add(ptr);
    if (value != null && (value.flags & KeyValue.FLAG_NOREPEAT) == 0)
      startKeyRepeat(ptr);
    _handler.onPointerDown(value);
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
    KeyValue newValue;
    if (dist < _config.swipe_dist_px)
    {
      newValue = ptr.key.key0;
    }
    else if (ptr.key.edgekeys)
    {
      if (Math.abs(dy) > Math.abs(dx)) // vertical swipe
        newValue = (dy < 0) ? ptr.key.key1 : ptr.key.key4;
      else // horizontal swipe
        newValue = (dx < 0) ? ptr.key.key3 : ptr.key.key2;
    }
    else
    {
      if (dx < 0) // left side
        newValue = (dy < 0) ? ptr.key.key1 : ptr.key.key3;
      else // right side
        newValue = (dy < 0) ? ptr.key.key2 : ptr.key.key4;
    }
    if (newValue != null && newValue != ptr.value)
    {
      int old_flags = (ptr.value != null) ? ptr.value.flags : 0;
      ptr.value = newValue;
      ptr.flags = newValue.flags;
      if ((old_flags & newValue.flags & KeyValue.FLAG_PRECISE_REPEAT) != 0)
      {
        // Keep the keyrepeat going between modulated keys.
      }
      else
      {
        stopKeyRepeat(ptr);
        if ((newValue.flags & KeyValue.FLAG_NOREPEAT) == 0)
          startKeyRepeat(ptr);
        _handler.onPointerSwipe(newValue);
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

  private Pointer getLatched(KeyValue kv)
  {
    for (Pointer p : _ptrs)
      if (p.value == kv && p.pointerId == -1)
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
        _handler.onPointerHold(ptr.value);
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
    public KeyboardData.Key key;
    public KeyValue value;
    public float downX;
    public float downY;
    /** Distance of the pointer to the initial press. */
    public float ptrDist;
    public int flags;
    /** Identify timeout messages. */
    public int timeoutWhat;
    /** ptrDist at the first repeat, -1 otherwise. */
    public float repeatingPtrDist;

    public Pointer(int p, KeyboardData.Key k, KeyValue v, float x, float y)
    {
      pointerId = p;
      key = k;
      value = v;
      downX = x;
      downY = y;
      ptrDist = 0.f;
      flags = (v == null) ? 0 : v.flags;
      timeoutWhat = -1;
      repeatingPtrDist = -1.f;
    }
  }

  public interface IPointerEventHandler
  {
    public void onPointerDown(KeyValue k);
    public void onPointerSwipe(KeyValue k);
    public void onPointerUp(KeyValue k);
    public void onPointerFlagsChanged();
    public void onPointerHold(KeyValue k);
  }
}
