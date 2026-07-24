package juloo.keyboard2.doubao;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import android.view.inputmethod.InputConnection;
import androidx.core.content.ContextCompat;
import io.github.jaredmdobson.concentus.OpusApplication;
import io.github.jaredmdobson.concentus.OpusEncoder;
import io.github.jaredmdobson.concentus.OpusException;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

/** Coordinates microphone capture, streaming ASR, and InputConnection updates. */
public final class DoubaoVoiceInput
{
  private static final String TAG = "Unexpected/DoubaoASR";
  private static final int SAMPLE_RATE = 16000;
  private static final int SAMPLES_PER_FRAME = 320;
  private static final int PCM_BYTES_PER_FRAME = SAMPLES_PER_FRAME * 2;
  private static final int FRAME_DURATION_MS = 20;
  private static final long FINAL_DRAIN_TIMEOUT_MS = 1500;
  private static final long INTERIM_UPDATE_INTERVAL_NS = 150000000L;

  public enum State
  {
    IDLE,
    CONNECTING,
    LISTENING,
    FINISHING
  }

  public interface Host
  {
    InputConnection getCurrentInputConnection();
    void onVoiceStateChanged(State state);
    void onVoiceFailure(String message);
  }

  private final Context context;
  private final Host host;
  private final Handler mainHandler;
  private final DoubaoAsrClient asrClient;
  private final ExecutorService executor;
  private final Object lock = new Object();

  private SessionRun activeRun;
  private boolean destroyed;

  public DoubaoVoiceInput(Context context, Host host)
  {
    this.context = context.getApplicationContext();
    this.host = host;
    mainHandler = new Handler(Looper.getMainLooper());
    asrClient = new DoubaoAsrClient(this.context);
    executor = Executors.newSingleThreadExecutor(new ThreadFactory()
    {
      @Override
      public Thread newThread(Runnable runnable)
      {
        Thread thread = new Thread(runnable, "doubao-asr");
        thread.setDaemon(true);
        return thread;
      }
    });
  }

  public boolean isActive()
  {
    synchronized (lock)
    {
      return activeRun != null;
    }
  }

  public void toggle()
  {
    requireMainThread();
    synchronized (lock)
    {
      if (activeRun == null)
      {
        startLocked();
        return;
      }
      requestStopLocked();
    }
  }

  public void stop()
  {
    requireMainThread();
    synchronized (lock)
    {
      if (activeRun != null)
        requestStopLocked();
    }
  }

  public void start()
  {
    requireMainThread();
    synchronized (lock)
    {
      if (activeRun != null)
        throw new IllegalStateException("Doubao voice input is already active");
      startLocked();
    }
  }

  public void cancel()
  {
    requireMainThread();
    final SessionRun run;
    synchronized (lock)
    {
      run = activeRun;
      if (run == null)
        return;
      activeRun = null;
      run.cancelled = true;
      run.stopRequested = true;
    }
    DoubaoAsrClient.Session session = run.session;
    if (session != null)
      session.cancel();
    if (run.hasComposingText)
      finishComposingText(run);
    host.onVoiceStateChanged(State.IDLE);
  }

  public void shutdown()
  {
    requireMainThread();
    synchronized (lock)
    {
      if (destroyed)
        return;
      destroyed = true;
    }
    cancel();
    executor.shutdownNow();
    asrClient.shutdown();
  }

  private void startLocked()
  {
    if (destroyed)
      throw new IllegalStateException("Doubao voice input was destroyed");
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED)
      throw new SecurityException("Microphone permission is not granted");

    InputConnection connection = host.getCurrentInputConnection();
    if (connection == null)
      throw new IllegalStateException("No active InputConnection for voice input");

    SessionRun run = new SessionRun(connection);
    activeRun = run;
    Log.i(TAG, "voice_start_requested");
    host.onVoiceStateChanged(State.CONNECTING);
    executor.execute(run);
  }

  private void requestStopLocked()
  {
    if (activeRun.stopRequested)
      return;
    activeRun.stopRequested = true;
    host.onVoiceStateChanged(State.FINISHING);
  }

  private void handleTranscript(SessionRun run,
      DoubaoProtocol.Response response)
  {
    mainHandler.post(() -> {
      if (!isCurrent(run) || run.cancelled || response.text.isEmpty())
        return;
      if (host.getCurrentInputConnection() != run.connection)
      {
        failInputConnection(run, "The target editor changed during voice input");
        return;
      }

      final boolean accepted;
      if (response.type == DoubaoProtocol.ResponseType.FINAL_RESULT)
      {
        accepted = run.connection.commitText(response.text, 1);
        run.hasComposingText = false;
        run.latestInterim = "";
      }
      else
      {
        accepted = run.connection.setComposingText(response.text, 1);
        run.hasComposingText = true;
        run.latestInterim = response.text;
      }
      if (!accepted)
        failInputConnection(run, "The target editor rejected voice text");
      else
        Log.i(TAG, "transcript_applied final=" + response.isFinal
            + " chars=" + response.text.length());
    });
  }

  private void failInputConnection(SessionRun run, String message)
  {
    synchronized (lock)
    {
      if (activeRun != run)
        return;
      activeRun = null;
      run.cancelled = true;
      run.stopRequested = true;
    }
    DoubaoAsrClient.Session session = run.session;
    if (session != null)
      session.cancel();
    host.onVoiceFailure(message);
    host.onVoiceStateChanged(State.IDLE);
  }

  private void finishComposingText(SessionRun run)
  {
    if (!run.connection.finishComposingText())
      host.onVoiceFailure("The target editor rejected the final voice text");
    run.hasComposingText = false;
    run.latestInterim = "";
  }

  private void complete(SessionRun run, Throwable problem)
  {
    mainHandler.post(() -> {
      synchronized (lock)
      {
        if (activeRun != run)
          return;
        activeRun = null;
      }
      if (run.hasComposingText)
        finishComposingText(run);
      if (problem != null)
      {
        Log.e(TAG, "Voice input failed", problem);
        host.onVoiceFailure(failureMessage(problem));
      }
      else
        Log.i(TAG, "voice_session_complete");
      host.onVoiceStateChanged(State.IDLE);
    });
  }

  private boolean isCurrent(SessionRun run)
  {
    synchronized (lock)
    {
      return activeRun == run;
    }
  }

  private void requireMainThread()
  {
    if (Looper.myLooper() != Looper.getMainLooper())
      throw new IllegalStateException("Voice input lifecycle must run on main thread");
  }

  private static String failureMessage(Throwable problem)
  {
    String message = problem.getMessage();
    if (message == null || message.isEmpty())
      return problem.getClass().getSimpleName();
    return message;
  }

  final class SessionRun implements Runnable, DoubaoAsrClient.Listener
  {
    final InputConnection connection;
    final AtomicReference<IOException> asyncFailure =
        new AtomicReference<IOException>();

    volatile DoubaoAsrClient.Session session;
    volatile AudioRecord recorder;
    volatile boolean stopRequested;
    volatile boolean cancelled;

    boolean hasComposingText;
    String latestInterim = "";
    long lastInterimUpdateNs;

    SessionRun(InputConnection connection)
    {
      this.connection = connection;
    }

    @Override
    public void run()
    {
      Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
      Throwable problem = null;
      try
      {
        session = asrClient.open(this);
        throwIfCancelledOrFailed();
        session.startSession();
        throwIfCancelledOrFailed();
        mainHandler.post(() -> {
          if (isCurrent(this) && !cancelled)
            host.onVoiceStateChanged(State.LISTENING);
        });

        streamAudio();
        throwIfCancelledOrFailed();
        session.finish();
        boolean receivedFinal = session.awaitFinalOrFinished(
            FINAL_DRAIN_TIMEOUT_MS);
        if (!receivedFinal)
          Log.w(TAG, "Final ASR drain timed out; keeping the latest transcript");
      }
      catch (Throwable error)
      {
        problem = error;
      }
      finally
      {
        problem = releaseRecorder(problem);
        DoubaoAsrClient.Session currentSession = session;
        if (currentSession != null)
        {
          if (cancelled || problem != null)
            currentSession.cancel();
          else
          {
            try
            {
              currentSession.close();
            }
            catch (Throwable closeError)
            {
              if (problem == null)
                problem = closeError;
              else
                problem.addSuppressed(closeError);
            }
          }
        }
        if (!cancelled)
          complete(this, problem);
      }
    }

    private void streamAudio() throws Exception
    {
      if (ContextCompat.checkSelfPermission(context,
          Manifest.permission.RECORD_AUDIO)
          != PackageManager.PERMISSION_GRANTED)
        throw new SecurityException("Microphone permission was revoked");

      int minimumBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE,
          AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
      if (minimumBuffer <= 0)
        throw new IOException("Invalid AudioRecord buffer size: "
            + minimumBuffer);

      AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
          SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
          AudioFormat.ENCODING_PCM_16BIT,
          Math.max(minimumBuffer, PCM_BYTES_PER_FRAME * 4));
      recorder = audioRecord;
      if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED)
        throw new IOException("AudioRecord initialization failed");

      OpusFrameEncoder encoder = new OpusFrameEncoder();
      short[] pcmFrame = new short[SAMPLES_PER_FRAME];
      long startedAtMs = System.currentTimeMillis();
      long frameIndex = 0;

      audioRecord.startRecording();
      if (audioRecord.getRecordingState()
          != AudioRecord.RECORDSTATE_RECORDING)
        throw new IOException("AudioRecord did not enter recording state");

      while (!stopRequested && !cancelled)
      {
        int samplesRead = readFrame(audioRecord, pcmFrame);
        if (samplesRead != SAMPLES_PER_FRAME)
          break;
        throwIfCancelledOrFailed();
        byte[] opus = encoder.encode(pcmFrame);
        int frameState = frameIndex == 0
            ? DoubaoProtocol.FRAME_FIRST : DoubaoProtocol.FRAME_MIDDLE;
        session.sendAudio(opus, frameState,
            startedAtMs + frameIndex * FRAME_DURATION_MS);
        frameIndex++;
        if (frameIndex == 1 || frameIndex % 50 == 0)
          Log.i(TAG, "audio_frames_sent=" + frameIndex);
      }

      if (!cancelled)
      {
        audioRecord.stop();
        Arrays.fill(pcmFrame, (short)0);
        byte[] finalOpus = encoder.encode(pcmFrame);
        session.sendAudio(finalOpus, DoubaoProtocol.FRAME_LAST,
            startedAtMs + frameIndex * FRAME_DURATION_MS);
        Log.i(TAG, "audio_last_sent frames=" + frameIndex);
      }
    }

    private int readFrame(AudioRecord audioRecord, short[] pcmFrame)
        throws IOException
    {
      int offset = 0;
      while (offset < pcmFrame.length && !stopRequested && !cancelled)
      {
        int read = audioRecord.read(pcmFrame, offset,
            pcmFrame.length - offset);
        if (read < 0)
          throw new IOException("AudioRecord read failed: " + read);
        if (read == 0)
          throw new IOException("AudioRecord returned zero samples");
        offset += read;
      }
      return offset;
    }

    private void throwIfCancelledOrFailed() throws IOException
    {
      if (cancelled)
        throw new IOException("Voice input was cancelled");
      IOException failure = asyncFailure.get();
      if (failure != null)
        throw failure;
    }

    private Throwable releaseRecorder(Throwable problem)
    {
      AudioRecord audioRecord = recorder;
      recorder = null;
      if (audioRecord == null)
        return problem;

      try
      {
        if (audioRecord.getRecordingState()
            == AudioRecord.RECORDSTATE_RECORDING)
          audioRecord.stop();
      }
      catch (Throwable stopError)
      {
        if (problem == null)
          problem = stopError;
        else
          problem.addSuppressed(stopError);
      }
      try
      {
        audioRecord.release();
      }
      catch (Throwable releaseError)
      {
        if (problem == null)
          problem = releaseError;
        else
          problem.addSuppressed(releaseError);
      }
      return problem;
    }

    @Override
    public void onResponse(DoubaoProtocol.Response response)
    {
      switch (response.type)
      {
        case INTERIM_RESULT:
          long now = System.nanoTime();
          if (now - lastInterimUpdateNs >= INTERIM_UPDATE_INTERVAL_NS)
          {
            lastInterimUpdateNs = now;
            handleTranscript(this, response);
          }
          if (response.vadFinished)
            stopRequested = true;
          break;
        case FINAL_RESULT:
          handleTranscript(this, response);
          if (response.vadFinished)
            stopRequested = true;
          break;
        case SESSION_FINISHED:
          stopRequested = true;
          break;
        case ERROR:
          asyncFailure.compareAndSet(null, new IOException(
              "Doubao ASR error: " + response.errorMessage));
          stopRequested = true;
          break;
        default:
          break;
      }
    }

    @Override
    public void onFailure(IOException failure)
    {
      asyncFailure.compareAndSet(null, failure);
      stopRequested = true;
    }
  }

  static final class OpusFrameEncoder
  {
    private final OpusEncoder encoder;
    private final byte[] output = new byte[4000];

    OpusFrameEncoder() throws OpusException
    {
      encoder = new OpusEncoder(SAMPLE_RATE, 1,
          OpusApplication.OPUS_APPLICATION_AUDIO);
    }

    byte[] encode(short[] pcm) throws OpusException
    {
      if (pcm.length != SAMPLES_PER_FRAME)
        throw new IllegalArgumentException("Expected " + SAMPLES_PER_FRAME
            + " PCM samples, got " + pcm.length);
      int encodedLength = encoder.encode(pcm, 0, SAMPLES_PER_FRAME,
          output, 0, output.length);
      return Arrays.copyOf(output, encodedLength);
    }
  }
}
