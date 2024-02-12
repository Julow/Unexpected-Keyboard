package juloo.keyboard2;

import java.util.Arrays;

public final class ComposeKey
{
  /** Apply the pending compose sequence to [kv]. Returns [null] if [kv] is not
      part of the pending sequence. */
  public static KeyValue apply(int state, KeyValue kv)
  {
    switch (kv.getKind())
    {
      case Char:
        KeyValue res = apply(state, kv.getChar());
        // Dim characters not part of any sequence instead of removing them.
        if (res == null)
          return kv.withFlags(kv.getFlags() | KeyValue.FLAG_SECONDARY);
        return res;
      /* These keys must not be removed. */
      case Event:
      case Modifier:
        return kv;
      /* These keys cannot be part of sequences. */
      case String:
      case Keyevent:
      case Editing:
      case Placeholder:
        return kv.withFlags(kv.getFlags() | KeyValue.FLAG_SECONDARY);
      case Compose_pending: return null;
    }
    return null;
  }

  /** Apply the pending compose sequence to char [c]. */
  static KeyValue apply(int state, char c)
  {
    char[] states = ComposeKeyData.states;
    char[] edges = ComposeKeyData.edges;
    int length = edges[state];
    int next = Arrays.binarySearch(states, state + 1, state + length, c);
    if (next < 0)
      return null;
    next = edges[next];
    // The next state is the end of a sequence, show the result.
    if (edges[next] == 1)
      return KeyValue.makeCharKey(states[next]);
    return KeyValue.makeComposePending(String.valueOf(c), next, 0);
  }

  /** The [states] array represents the different states and their transition.
      A state occupies one or several cells of the array:
      - The first cell is the result of the compose sequence if the state is of
        size 1, unspecified otherwise.
      - The remaining cells are the transitions, sorted alphabetically.

      The [edges] array represents the transition state corresponding to each
      accepted inputs.
      Id [states[i]] is the first cell of a state, [edges[i]] is the number of
      cells occupied by the state [i].
      If [states[i]] is a transition, [edges[i]] is the index of the state to
      jump into. */
}
