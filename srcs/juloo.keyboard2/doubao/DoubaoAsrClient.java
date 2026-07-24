package juloo.keyboard2.doubao;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/** Device registration, authentication, and WebSocket transport for Doubao ASR. */
public final class DoubaoAsrClient
{
  private static final String TAG = "Unexpected/DoubaoASR";
  private static final String REGISTER_URL =
      "https://log.snssdk.com/service/2/device_register/";
  private static final String SETTINGS_URL =
      "https://is.snssdk.com/service/settings/v3/";
  // OkHttp models a secure WebSocket handshake as an HTTPS request and
  // performs the protocol upgrade in newWebSocket().
  private static final String WEBSOCKET_URL =
      "https://frontier-audio-ime-ws.doubao.com/ocean/api/v1/ws";

  private static final int AID = 401734;
  private static final String APP_NAME = "oime";
  private static final int VERSION_CODE = 100316010;
  private static final String VERSION_NAME = "1.3.16";
  private static final String CHANNEL = "official";
  private static final String PACKAGE = "com.bytedance.android.doubaoime";
  private static final long HANDSHAKE_TIMEOUT_MS = 10000;

  private static final MediaType JSON =
      MediaType.get("application/json; charset=utf-8");

  public interface Listener
  {
    void onResponse(DoubaoProtocol.Response response);
    void onFailure(IOException failure);
  }

  private final Context context;
  private final OkHttpClient httpClient;
  private final CredentialStore credentialStore;

  public DoubaoAsrClient(Context context)
  {
    this.context = context.getApplicationContext();
    httpClient = new OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .writeTimeout(10, TimeUnit.SECONDS)
      .build();
    credentialStore = new CredentialStore(this.context, httpClient);
  }

  public Session open(Listener listener) throws IOException
  {
    if (listener == null)
      throw new IllegalArgumentException("listener must not be null");
    Credentials credentials = credentialStore.ensureCredentials();
    Log.i(TAG, "credentials_ready");
    Session session = new Session(httpClient, credentials, listener);
    session.connectAndStartTask();
    return session;
  }

  public void shutdown()
  {
    httpClient.dispatcher().executorService().shutdown();
    httpClient.connectionPool().evictAll();
  }

  public static final class Session
  {
    private final OkHttpClient httpClient;
    private final Credentials credentials;
    private final Listener listener;
    private final String requestId = UUID.randomUUID().toString();
    private final CountDownLatch openLatch = new CountDownLatch(1);
    private final CountDownLatch terminalLatch = new CountDownLatch(1);
    private final BlockingQueue<DoubaoProtocol.Response> controlResponses =
        new LinkedBlockingQueue<DoubaoProtocol.Response>();
    private final AtomicReference<IOException> failure =
        new AtomicReference<IOException>();
    private final AtomicBoolean closeRequested = new AtomicBoolean(false);

    private volatile WebSocket webSocket;
    private volatile boolean finishSent;

    Session(OkHttpClient httpClient, Credentials credentials, Listener listener)
    {
      this.httpClient = httpClient;
      this.credentials = credentials;
      this.listener = listener;
    }

    void connectAndStartTask() throws IOException
    {
      HttpUrl url = buildWebSocketUrl(credentials.deviceId);
      Request request = new Request.Builder()
        .url(url)
        .header("User-Agent", userAgent())
        .header("proto-version", "v2")
        .header("x-custom-keepalive", "true")
        .build();

      webSocket = httpClient.newWebSocket(request, new SocketListener());
      await(openLatch, HANDSHAKE_TIMEOUT_MS, "WebSocket open");
      throwIfFailed();
      send(DoubaoProtocol.buildStartTask(requestId, credentials.token));
      awaitControl(DoubaoProtocol.ResponseType.TASK_STARTED,
          HANDSHAKE_TIMEOUT_MS);
      Log.i(TAG, "task_started");
    }

    static HttpUrl buildWebSocketUrl(String deviceId)
    {
      return HttpUrl.get(WEBSOCKET_URL).newBuilder()
        .addQueryParameter("aid", Integer.toString(AID))
        .addQueryParameter("device_id", deviceId)
        .build();
    }

    public void startSession() throws IOException
    {
      send(DoubaoProtocol.buildStartSession(requestId, credentials.token,
          credentials.deviceId));
      awaitControl(DoubaoProtocol.ResponseType.SESSION_STARTED,
          HANDSHAKE_TIMEOUT_MS);
      Log.i(TAG, "session_started");
    }

    public void sendAudio(byte[] opusFrame, int frameState, long timestampMs)
        throws IOException
    {
      send(DoubaoProtocol.buildAudioRequest(requestId, opusFrame, frameState,
          timestampMs));
    }

    public void finish() throws IOException
    {
      if (finishSent)
        throw new IllegalStateException("FinishSession was already sent");
      finishSent = true;
      send(DoubaoProtocol.buildFinishSession(requestId, credentials.token));
      Log.i(TAG, "finish_session_sent");
    }

    public boolean awaitFinalOrFinished(long timeoutMs) throws IOException
    {
      await(terminalLatch, timeoutMs, "final ASR result", false);
      throwIfFailed();
      return terminalLatch.getCount() == 0;
    }

    public void close() throws IOException
    {
      closeRequested.set(true);
      WebSocket socket = webSocket;
      if (socket != null && !socket.close(1000, "ASR session complete"))
        throw new IOException("WebSocket refused the close request");
    }

    public void cancel()
    {
      closeRequested.set(true);
      WebSocket socket = webSocket;
      if (socket != null)
        socket.cancel();
      openLatch.countDown();
      terminalLatch.countDown();
    }

    private void send(byte[] message) throws IOException
    {
      throwIfFailed();
      WebSocket socket = webSocket;
      if (socket == null)
        throw new IOException("ASR WebSocket is not open");
      if (!socket.send(ByteString.of(message)))
        throw new IOException("ASR WebSocket rejected an outgoing message");
    }

    private void awaitControl(DoubaoProtocol.ResponseType expected,
        long timeoutMs) throws IOException
    {
      long deadline = System.nanoTime()
          + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
      while (true)
      {
        throwIfFailed();
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0)
          throw new IOException("Timed out waiting for " + expected);

        final DoubaoProtocol.Response response;
        try
        {
          response = controlResponses.poll(remaining, TimeUnit.NANOSECONDS);
        }
        catch (InterruptedException e)
        {
          Thread.currentThread().interrupt();
          throw new IOException("Interrupted while waiting for " + expected, e);
        }
        if (response == null)
          throw new IOException("Timed out waiting for " + expected);
        if (response.type == expected)
          return;
        if (response.type == DoubaoProtocol.ResponseType.ERROR)
          throw new IOException("Doubao ASR rejected the request: "
              + response.errorMessage);
        throw new IOException("Expected " + expected + " but received "
            + response.type);
      }
    }

    private void throwIfFailed() throws IOException
    {
      IOException current = failure.get();
      if (current != null)
        throw current;
    }

    private void fail(IOException exception)
    {
      if (!failure.compareAndSet(null, exception))
        return;
      controlResponses.offer(DoubaoProtocol.Response.error(-1,
          exception.getMessage()));
      openLatch.countDown();
      terminalLatch.countDown();
      listener.onFailure(exception);
    }

    private final class SocketListener extends WebSocketListener
    {
      @Override
      public void onOpen(WebSocket socket, Response response)
      {
        openLatch.countDown();
      }

      @Override
      public void onMessage(WebSocket socket, ByteString bytes)
      {
        final DoubaoProtocol.Response response;
        try
        {
          response = DoubaoProtocol.parseResponse(bytes.toByteArray());
        }
        catch (DoubaoProtocol.ProtocolException e)
        {
          fail(e);
          socket.cancel();
          return;
        }

        listener.onResponse(response);
        switch (response.type)
        {
          case TASK_STARTED:
          case SESSION_STARTED:
          case SESSION_FINISHED:
          case ERROR:
            controlResponses.offer(response);
            break;
          default:
            break;
        }

        if (response.type == DoubaoProtocol.ResponseType.SESSION_FINISHED
            || response.type == DoubaoProtocol.ResponseType.ERROR
            || (finishSent
                && response.type == DoubaoProtocol.ResponseType.FINAL_RESULT))
          terminalLatch.countDown();
      }

      @Override
      public void onClosing(WebSocket socket, int code, String reason)
      {
        if (!closeRequested.get()
            && terminalLatch.getCount() != 0)
          fail(new IOException("ASR WebSocket is closing: " + code + " "
              + reason));
      }

      @Override
      public void onClosed(WebSocket socket, int code, String reason)
      {
        if (!closeRequested.get()
            && terminalLatch.getCount() != 0)
          fail(new IOException("ASR WebSocket closed: " + code + " "
              + reason));
      }

      @Override
      public void onFailure(WebSocket socket, Throwable error,
          Response response)
      {
        String message = "ASR WebSocket failed";
        if (response != null)
          message += " with HTTP " + response.code();
        fail(new IOException(message, error));
      }
    }
  }

  private static void await(CountDownLatch latch, long timeoutMs,
      String operation) throws IOException
  {
    await(latch, timeoutMs, operation, true);
  }

  private static void await(CountDownLatch latch, long timeoutMs,
      String operation, boolean timeoutIsFailure) throws IOException
  {
    final boolean completed;
    try
    {
      completed = latch.await(timeoutMs, TimeUnit.MILLISECONDS);
    }
    catch (InterruptedException e)
    {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while waiting for " + operation, e);
    }
    if (!completed && timeoutIsFailure)
      throw new IOException("Timed out waiting for " + operation);
  }

  private static final class Credentials
  {
    String deviceId;
    String installId;
    String cdid;
    String openudid;
    String clientudid;
    String token;
  }

  private static final class CredentialStore
  {
    private static final String PREFS_NAME = "doubao_asr_credentials";

    private final Context context;
    private final OkHttpClient httpClient;
    private final SharedPreferences preferences;
    private final SecureRandom secureRandom = new SecureRandom();

    CredentialStore(Context context, OkHttpClient httpClient)
    {
      this.context = context;
      this.httpClient = httpClient;
      preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    synchronized Credentials ensureCredentials() throws IOException
    {
      Credentials credentials = load();
      if (credentials.cdid.isEmpty())
        credentials.cdid = UUID.randomUUID().toString();
      if (credentials.openudid.isEmpty())
        credentials.openudid = generateOpenudid();
      if (credentials.clientudid.isEmpty())
        credentials.clientudid = UUID.randomUUID().toString();

      if (credentials.deviceId.isEmpty())
      {
        registerDevice(credentials);
        save(credentials);
      }
      if (credentials.token.isEmpty())
      {
        requestToken(credentials);
        save(credentials);
      }
      return credentials;
    }

    private Credentials load()
    {
      Credentials credentials = new Credentials();
      credentials.deviceId = preferences.getString("device_id", "");
      credentials.installId = preferences.getString("install_id", "");
      credentials.cdid = preferences.getString("cdid", "");
      credentials.openudid = preferences.getString("openudid", "");
      credentials.clientudid = preferences.getString("clientudid", "");
      credentials.token = preferences.getString("token", "");
      return credentials;
    }

    private void save(Credentials credentials) throws IOException
    {
      boolean committed = preferences.edit()
        .putString("device_id", credentials.deviceId)
        .putString("install_id", credentials.installId)
        .putString("cdid", credentials.cdid)
        .putString("openudid", credentials.openudid)
        .putString("clientudid", credentials.clientudid)
        .putString("token", credentials.token)
        .commit();
      if (!committed)
        throw new IOException("Failed to persist Doubao ASR credentials");
    }

    private void registerDevice(Credentials credentials) throws IOException
    {
      long now = System.currentTimeMillis();
      DeviceIdentity identity = new DeviceIdentity(context);
      JsonObject header = identity.registrationHeader(credentials);

      JsonObject body = new JsonObject();
      body.addProperty("magic_tag", "ss_app_log");
      body.add("header", header);
      body.addProperty("_gen_time", now);

      HttpUrl url = commonUrl(REGISTER_URL, credentials, now)
        .addQueryParameter("manifest_version_code",
            Integer.toString(VERSION_CODE))
        .addQueryParameter("update_version_code",
            Integer.toString(VERSION_CODE))
        .addQueryParameter("resolution", identity.resolution)
        .addQueryParameter("dpi", identity.dpi)
        .addQueryParameter("device_type", identity.deviceType)
        .addQueryParameter("device_brand", identity.deviceBrand)
        .addQueryParameter("language", "zh")
        .addQueryParameter("os_api", identity.osApi)
        .addQueryParameter("os_version", identity.osVersion)
        .addQueryParameter("ac", "wifi")
        .build();

      Request request = new Request.Builder()
        .url(url)
        .header("User-Agent", userAgent())
        .post(RequestBody.create(body.toString(), JSON))
        .build();
      JsonObject result = executeJson(request, "device registration");
      credentials.deviceId = requiredId(result, "device_id");
      credentials.installId = requiredId(result, "install_id");
      if (credentials.deviceId.equals("0"))
        throw new IOException("Device registration returned device_id 0");
      Log.i(TAG, "device_registered");
    }

    private void requestToken(Credentials credentials) throws IOException
    {
      long now = System.currentTimeMillis();
      HttpUrl url = commonUrl(SETTINGS_URL, credentials, now)
        .addQueryParameter("device_id", credentials.deviceId)
        .build();
      String body = "body=null";
      Request request = new Request.Builder()
        .url(url)
        .header("User-Agent", userAgent())
        .header("x-ss-stub", md5Uppercase(body))
        .post(RequestBody.create(body, null))
        .build();

      JsonObject root = executeJson(request, "ASR settings");
      credentials.token = requiredString(
          requiredJsonObject(
              requiredJsonObject(
                  requiredJsonObject(root, "data"), "settings"),
              "asr_config"),
          "app_key");
      if (credentials.token.isEmpty())
        throw new IOException("ASR settings returned an empty app_key");
      Log.i(TAG, "token_received");
    }

    private HttpUrl.Builder commonUrl(String baseUrl, Credentials credentials,
        long now)
    {
      return HttpUrl.get(baseUrl).newBuilder()
        .addQueryParameter("device_platform", "android")
        .addQueryParameter("os", "android")
        .addQueryParameter("ssmix", "a")
        .addQueryParameter("_rticket", Long.toString(now))
        .addQueryParameter("cdid", credentials.cdid)
        .addQueryParameter("channel", CHANNEL)
        .addQueryParameter("aid", Integer.toString(AID))
        .addQueryParameter("app_name", APP_NAME)
        .addQueryParameter("version_code", Integer.toString(VERSION_CODE))
        .addQueryParameter("version_name", VERSION_NAME);
    }

    private JsonObject executeJson(Request request, String operation)
        throws IOException
    {
      try (Response response = httpClient.newCall(request).execute())
      {
        String body = response.body() == null ? "" : response.body().string();
        if (!response.isSuccessful())
          throw new IOException("Doubao " + operation + " failed with HTTP "
              + response.code() + ": " + body);
        try
        {
          JsonElement parsed = JsonParser.parseString(body);
          if (!parsed.isJsonObject())
            throw new IOException("Doubao " + operation
                + " returned non-object JSON");
          return parsed.getAsJsonObject();
        }
        catch (JsonParseException e)
        {
          throw new IOException("Doubao " + operation
              + " returned invalid JSON", e);
        }
      }
    }

    private JsonObject requiredJsonObject(JsonObject object, String key)
        throws IOException
    {
      JsonElement value = object.get(key);
      if (value == null)
        throw new IOException("Doubao response is missing '" + key + "'");
      if (!value.isJsonObject())
        throw new IOException("Doubao response field '" + key
            + "' is not an object");
      return value.getAsJsonObject();
    }

    private String requiredString(JsonObject object, String key)
        throws IOException
    {
      JsonElement value = object.get(key);
      if (value == null || !value.isJsonPrimitive()
          || !value.getAsJsonPrimitive().isString())
        throw new IOException("Doubao response field '" + key
            + "' is not a string");
      return value.getAsString();
    }

    private String requiredId(JsonObject object, String key) throws IOException
    {
      JsonElement value = object.get(key);
      if (value == null || !value.isJsonPrimitive())
        throw new IOException("Doubao response is missing '" + key + "'");
      String id = value.getAsString();
      if (id.isEmpty())
        throw new IOException("Doubao response field '" + key + "' is empty");
      return id;
    }

    private String generateOpenudid()
    {
      byte[] bytes = new byte[8];
      secureRandom.nextBytes(bytes);
      StringBuilder result = new StringBuilder(16);
      for (byte value : bytes)
        result.append(String.format(Locale.US, "%02x", value & 0xff));
      return result.toString();
    }
  }

  private static final class DeviceIdentity
  {
    final String osApi;
    final String osVersion;
    final String deviceType;
    final String deviceBrand;
    final String deviceModel;
    final String resolution;
    final String dpi;
    final String rom;
    final String cpuAbi;

    DeviceIdentity(Context context)
    {
      DisplayMetrics metrics = context.getResources().getDisplayMetrics();
      osApi = Integer.toString(Build.VERSION.SDK_INT);
      osVersion = Build.VERSION.RELEASE;
      deviceType = Build.MODEL;
      deviceBrand = Build.BRAND;
      deviceModel = Build.MODEL;
      resolution = metrics.widthPixels + "*" + metrics.heightPixels;
      dpi = Integer.toString(metrics.densityDpi);
      rom = Build.ID;
      cpuAbi = Build.SUPPORTED_ABIS.length == 0
          ? "unknown" : Build.SUPPORTED_ABIS[0];
    }

    JsonObject registrationHeader(Credentials credentials)
    {
      long now = System.currentTimeMillis();
      TimeZone zone = TimeZone.getDefault();
      int offsetSeconds = zone.getOffset(now) / 1000;

      JsonObject header = new JsonObject();
      header.addProperty("device_id", 0);
      header.addProperty("install_id", 0);
      header.addProperty("aid", AID);
      header.addProperty("app_name", APP_NAME);
      header.addProperty("version_code", VERSION_CODE);
      header.addProperty("version_name", VERSION_NAME);
      header.addProperty("manifest_version_code", VERSION_CODE);
      header.addProperty("update_version_code", VERSION_CODE);
      header.addProperty("channel", CHANNEL);
      header.addProperty("package", PACKAGE);
      header.addProperty("device_platform", "android");
      header.addProperty("os", "android");
      header.addProperty("os_api", osApi);
      header.addProperty("os_version", osVersion);
      header.addProperty("device_type", deviceType);
      header.addProperty("device_brand", deviceBrand);
      header.addProperty("device_model", deviceModel);
      header.addProperty("resolution", resolution);
      header.addProperty("dpi", dpi);
      header.addProperty("language", "zh");
      header.addProperty("timezone", offsetSeconds / 3600);
      header.addProperty("access", "wifi");
      header.addProperty("rom", rom);
      header.addProperty("rom_version", rom);
      header.addProperty("openudid", credentials.openudid);
      header.addProperty("clientudid", credentials.clientudid);
      header.addProperty("cdid", credentials.cdid);
      header.addProperty("region", "CN");
      header.addProperty("tz_name", zone.getID());
      header.addProperty("tz_offset", offsetSeconds);
      header.addProperty("sim_region", "cn");
      header.addProperty("carrier_region", "cn");
      header.addProperty("cpu_abi", cpuAbi);
      header.addProperty("build_serial", "unknown");
      header.addProperty("not_request_sender", 0);
      header.addProperty("sig_hash", "");
      header.addProperty("google_aid", "");
      header.addProperty("mc", "");
      header.addProperty("serial_number", "");
      return header;
    }
  }

  private static String userAgent()
  {
    return String.format(Locale.US,
        "%s/%d (Linux; U; Android %s; zh_CN; %s; Build/%s)",
        PACKAGE, VERSION_CODE, Build.VERSION.RELEASE, Build.MODEL, Build.ID);
  }

  private static String md5Uppercase(String value) throws IOException
  {
    final byte[] digest;
    try
    {
      digest = MessageDigest.getInstance("MD5")
          .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    catch (NoSuchAlgorithmException e)
    {
      throw new IOException("MD5 is unavailable", e);
    }
    StringBuilder result = new StringBuilder(digest.length * 2);
    for (byte b : digest)
      result.append(String.format(Locale.US, "%02X", b & 0xff));
    return result.toString();
  }
}
