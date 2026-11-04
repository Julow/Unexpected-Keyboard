package android.os;

import java.util.LinkedHashMap;
import java.util.Map;

/** JVM-only queue for pointer tests. fireNext dispatches in insertion order,
    ignoring delays; it does not simulate Android looper timing. */
public class Handler implements juloo.keyboard2.TestUtils.Scheduler
{
  public interface Callback { boolean handleMessage(Message message); }

  private final Callback callback;
  private final Map<Integer, Long> pending = new LinkedHashMap<Integer, Long>();

  public Handler(Callback c)
  {
    callback = c;
    juloo.keyboard2.TestUtils.scheduler = this;
  }

  public boolean sendEmptyMessageDelayed(int what, long delay)
  {
    pending.put(what, delay);
    return true;
  }

  public void removeMessages(int what) { pending.remove(what); }

  public boolean fireNext()
  {
    if (pending.isEmpty())
      return false;
    int what = pending.keySet().iterator().next();
    pending.remove(what);
    Message message = new Message();
    message.what = what;
    return callback.handleMessage(message);
  }
}
