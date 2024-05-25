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

  enum Name
  {
    None,
    Swipe,
    Roundtrip,
    Circle,
    Anticircle
  }

  /** Angle to travel before a rotation gesture starts. A threshold too low
      would be too easy to reach while doing back and forth gestures, as the
      quadrants are very small. In the same unit as [current_dir] */
  static final int ROTATION_THRESHOLD = 2;

  /** Return the currently recognized gesture. Return [null] if no gesture is
      recognized. Might change everytime [changed_direction] return [true]. */
  public Name get_gesture()
  {
    switch (state)
    {
      case Cancelled:
        return Name.None;
      case Swiped:
      case Ended_swipe:
        return Name.Swipe;
      case Ended_center:
        return Name.Roundtrip;
      case Rotating_clockwise:
      case Ended_clockwise:
        return Name.Circle;
      case Rotating_anticlockwise:
      case Ended_anticlockwise:
          return Name.Anticircle;
    }
    return Name.None; // Unreachable
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

  public int current_direction() { return current_dir; }

  /** The pointer changed direction. Return [true] if the gesture changed
      state and [get_gesture] return a different value. */
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

  /** Return [true] if [get_gesture] will return a different value. */
  public boolean moved_to_center()
  {
    switch (state)
    {
      case Swiped: state = State.Ended_center; return true;
      case Rotating_clockwise: state = State.Ended_clockwise; return false;
      case Rotating_anticlockwise: state = State.Ended_anticlockwise; return false;
    }
    return false;
  }

  /** Will not change the gesture state. */
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
