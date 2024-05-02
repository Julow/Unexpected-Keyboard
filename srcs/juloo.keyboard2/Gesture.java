package juloo.keyboard2;

public final class Gesture
{
  /** The pointer direction that caused the last state change.
      Integer from 0 to 15 (included). */
  int current_dir;

  State state;

  public Gesture(int starting_direction)
  {
    current_dir = starting_direction;
    state = State.Swiped;
  }

  enum State
  {
    Cancelled,
    Swiped,
    Rotating_clockwise,
    Rotating_anticlockwise,
    Ended_swipe,
    Ended_center,
    Ended_clockwise,
    Ended_anticlockwise
  }

  /** Angle to travel before a rotation gesture starts. A threshold too low
      would be too easy to reach while doing back and forth gestures, as the
      quadrants are very small. In the same unit as [current_dir] */
  static final int ROTATION_THRESHOLD = 2;

  /** Modify the key depending on the current gesture. Return [null] if the
      gesture is invalid or if [KeyModifier] returned [null]. */
  public KeyValue modify_key(KeyValue selected_val, KeyboardData.Key key)
  {
    switch (state)
    {
      case Cancelled:
        return null;
      case Swiped:
      case Ended_swipe:
        return selected_val;
      case Ended_center:
        return KeyModifier.modify_round_trip(selected_val);
      case Rotating_clockwise:
      case Ended_clockwise:
        return KeyModifier.modify_circle(key.keys[0], true);
      case Rotating_anticlockwise:
      case Ended_anticlockwise:
        return KeyModifier.modify_circle(key.keys[0], false);
    }
    return null; // Unreachable
  }

  public boolean is_in_progress()
  {
    switch (state)
    {
      case Swiped:
      case Rotating_clockwise:
      case Rotating_anticlockwise:
        return true;
    }
    return false;
  }

  /** The pointer changed direction. Return [true] if the gesture changed state. */
  public boolean changed_direction(int direction)
  {
    int d = dir_diff(current_dir, direction);
    boolean clockwise = d > 0;
    switch (state)
    {
      case Swiped:
        if (Math.abs(d) < ROTATION_THRESHOLD)
          return false;
        // Start a rotation
        state = (clockwise) ?
          State.Rotating_clockwise : State.Rotating_anticlockwise;
        current_dir = direction;
        return true;
      // Check that rotation is not reversing
      case Rotating_clockwise:
      case Rotating_anticlockwise:
        current_dir = direction;
        if ((state == State.Rotating_clockwise) == clockwise)
          return false;
        state = State.Cancelled;
        return true;
    }
    return false;
  }

  public void moved_to_center()
  {
    switch (state)
    {
      case Swiped: state = State.Ended_center; break;
      case Rotating_clockwise: state = State.Ended_clockwise; break;
      case Rotating_anticlockwise: state = State.Ended_anticlockwise; break;
    }
  }

  public void pointer_up()
  {
    switch (state)
    {
      case Swiped: state = State.Ended_swipe; break;
      case Rotating_clockwise: state = State.Ended_clockwise; break;
      case Rotating_anticlockwise: state = State.Ended_anticlockwise; break;
    }
  }

  static int dir_diff(int d1, int d2)
  {
    final int n = 16;
    // Shortest-path in modulo arithmetic
    if (d1 == d2)
      return 0;
    int left = (d1 - d2 + n) % n;
    int right = (d2 - d1 + n) % n;
    return (left < right) ? -left : right;
  }
}
