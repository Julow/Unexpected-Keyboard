package juloo.keyboard2.doubao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Wire-level implementation of the Doubao IME ASR protobuf protocol. */
public final class DoubaoProtocol
{
  public static final int FRAME_UNSPECIFIED = 0;
  public static final int FRAME_FIRST = 1;
  public static final int FRAME_MIDDLE = 3;
  public static final int FRAME_LAST = 9;

  private static final String SERVICE_NAME = "ASR";
  private static final String APP_NAME = "oime";

  public enum ResponseType
  {
    TASK_STARTED,
    SESSION_STARTED,
    SESSION_FINISHED,
    VAD_START,
    INTERIM_RESULT,
    FINAL_RESULT,
    HEARTBEAT,
    ERROR,
    UNKNOWN
  }

  public static final class Response
  {
    public final ResponseType type;
    public final String text;
    public final boolean isFinal;
    public final boolean vadStart;
    public final boolean vadFinished;
    public final int packetNumber;
    public final int statusCode;
    public final String errorMessage;
    public final String rawJson;

    private Response(ResponseType type, String text, boolean isFinal,
        boolean vadStart, boolean vadFinished, int packetNumber,
        int statusCode, String errorMessage, String rawJson)
    {
      this.type = type;
      this.text = text;
      this.isFinal = isFinal;
      this.vadStart = vadStart;
      this.vadFinished = vadFinished;
      this.packetNumber = packetNumber;
      this.statusCode = statusCode;
      this.errorMessage = errorMessage;
      this.rawJson = rawJson;
    }

    static Response simple(ResponseType type)
    {
      return new Response(type, "", false, false, false, -1, 0, "", null);
    }

    static Response error(int statusCode, String message)
    {
      return new Response(ResponseType.ERROR, "", false, false, false, -1,
          statusCode, message, null);
    }
  }

  public static final class ProtocolException extends IOException
  {
    ProtocolException(String message)
    {
      super(message);
    }

    ProtocolException(String message, Throwable cause)
    {
      super(message, cause);
    }
  }

  private DoubaoProtocol() {}

  public static byte[] buildStartTask(String requestId, String token)
  {
    requireNonEmpty(requestId, "requestId");
    requireNonEmpty(token, "token");
    return request(requestId, token, "StartTask", "", null, FRAME_UNSPECIFIED);
  }

  public static byte[] buildStartSession(String requestId, String token,
      String deviceId)
  {
    requireNonEmpty(requestId, "requestId");
    requireNonEmpty(token, "token");
    requireNonEmpty(deviceId, "deviceId");
    return request(requestId, token, "StartSession",
        buildSessionPayload(deviceId), null, FRAME_UNSPECIFIED);
  }

  public static byte[] buildFinishSession(String requestId, String token)
  {
    requireNonEmpty(requestId, "requestId");
    requireNonEmpty(token, "token");
    return request(requestId, token, "FinishSession", "", null,
        FRAME_UNSPECIFIED);
  }

  public static byte[] buildAudioRequest(String requestId, byte[] opusFrame,
      int frameState, long timestampMs)
  {
    requireNonEmpty(requestId, "requestId");
    if (opusFrame == null || opusFrame.length == 0)
      throw new IllegalArgumentException("opusFrame must not be empty");
    if (frameState != FRAME_FIRST && frameState != FRAME_MIDDLE
        && frameState != FRAME_LAST)
      throw new IllegalArgumentException("Invalid frameState: " + frameState);

    JsonObject payload = new JsonObject();
    payload.add("extra", new JsonObject());
    payload.addProperty("timestamp_ms", timestampMs);
    return request(requestId, "", "TaskRequest", payload.toString(),
        opusFrame, frameState);
  }

  public static String buildSessionPayload(String deviceId)
  {
    requireNonEmpty(deviceId, "deviceId");

    JsonObject audioInfo = new JsonObject();
    audioInfo.addProperty("channel", 1);
    audioInfo.addProperty("format", "speech_opus");
    audioInfo.addProperty("sample_rate", 16000);

    JsonObject extra = new JsonObject();
    extra.addProperty("app_name", APP_NAME);
    extra.addProperty("cell_compress_rate", 8);
    extra.addProperty("did", deviceId);
    extra.addProperty("enable_asr_threepass", false);
    extra.addProperty("enable_asr_twopass", false);
    extra.addProperty("input_mode", "tool");
    extra.addProperty("interim_results", true);

    JsonObject config = new JsonObject();
    config.add("audio_info", audioInfo);
    config.addProperty("enable_punctuation", true);
    config.addProperty("enable_speech_rejection", false);
    config.addProperty("enable_vad", true);
    config.addProperty("low_latency", false);
    config.add("extra", extra);
    return config.toString();
  }

  public static Response parseResponse(byte[] data) throws ProtocolException
  {
    if (data == null || data.length == 0)
      throw new ProtocolException("Empty ASR protobuf response");

    ProtoReader reader = new ProtoReader(data);
    String messageType = "";
    int statusCode = 0;
    String statusMessage = "";
    String resultJson = "";

    while (reader.hasRemaining())
    {
      int tag = reader.readTag();
      int field = tag >>> 3;
      int wireType = tag & 7;
      switch (field)
      {
        case 4:
          messageType = reader.readString(wireType, field);
          break;
        case 5:
          statusCode = reader.readInt32(wireType, field);
          break;
        case 6:
          statusMessage = reader.readString(wireType, field);
          break;
        case 7:
          resultJson = reader.readString(wireType, field);
          break;
        default:
          reader.skip(wireType);
          break;
      }
    }

    switch (messageType)
    {
      case "TaskStarted":
        return Response.simple(ResponseType.TASK_STARTED);
      case "SessionStarted":
        return Response.simple(ResponseType.SESSION_STARTED);
      case "SessionFinished":
        return Response.simple(ResponseType.SESSION_FINISHED);
      case "TaskFailed":
      case "SessionFailed":
        return Response.error(statusCode, nonEmptyStatus(statusCode, statusMessage));
      default:
        break;
    }

    if (resultJson.isEmpty())
      return Response.simple(ResponseType.UNKNOWN);

    final JsonObject root;
    try
    {
      JsonElement parsed = JsonParser.parseString(resultJson);
      if (!parsed.isJsonObject())
        throw new ProtocolException("ASR result_json is not an object");
      root = parsed.getAsJsonObject();
    }
    catch (JsonParseException e)
    {
      throw new ProtocolException("Invalid ASR result_json", e);
    }

    JsonObject extra = optionalObject(root, "extra");
    JsonElement resultsElement = root.get("results");
    if (resultsElement == null || resultsElement.isJsonNull())
    {
      int packetNumber = optionalInt(extra, "packet_number", -1);
      return new Response(ResponseType.HEARTBEAT, "", false, false, false,
          packetNumber, 0, "", resultJson);
    }

    if (optionalBoolean(extra, "vad_start", false))
      return new Response(ResponseType.VAD_START, "", false, true, false,
          -1, 0, "", resultJson);

    List<JsonObject> results = resultObjects(resultsElement);
    String text = "";
    boolean explicitFinal = false;
    boolean vadFinished = false;
    boolean nonstreamResult = false;

    for (JsonObject result : results)
    {
      String candidate = extractResultText(result);
      if (!candidate.isEmpty())
        text = candidate;

      Boolean interim = firstBoolean(result, "is_interim", "interim");
      if (Boolean.FALSE.equals(interim))
        explicitFinal = true;
      if (optionalBoolean(result, "is_final", false))
        explicitFinal = true;
      if (firstBooleanValue(result, false, "is_vad_finished", "vad_finished"))
        vadFinished = true;

      JsonObject resultExtra = optionalObject(result, "extra");
      if (optionalBoolean(resultExtra, "nonstream_result", false))
        nonstreamResult = true;
    }

    boolean isFinal = nonstreamResult || (explicitFinal && vadFinished);
    return new Response(
        isFinal ? ResponseType.FINAL_RESULT : ResponseType.INTERIM_RESULT,
        text, isFinal, false, vadFinished, -1, 0, "", resultJson);
  }

  private static byte[] request(String requestId, String token,
      String methodName, String payload, byte[] audioData, int frameState)
  {
    ProtoWriter writer = new ProtoWriter();
    writer.string(2, token);
    writer.string(3, SERVICE_NAME);
    writer.string(5, methodName);
    writer.string(6, payload);
    writer.bytes(7, audioData);
    writer.string(8, requestId);
    writer.int32(9, frameState);
    return writer.toByteArray();
  }

  private static String nonEmptyStatus(int statusCode, String statusMessage)
  {
    if (!statusMessage.isEmpty())
      return statusMessage;
    return "ASR returned status " + statusCode;
  }

  private static List<JsonObject> resultObjects(JsonElement results)
      throws ProtocolException
  {
    List<JsonObject> objects = new ArrayList<JsonObject>();
    if (results.isJsonObject())
    {
      objects.add(results.getAsJsonObject());
      return objects;
    }
    if (!results.isJsonArray())
      throw new ProtocolException("ASR results must be an object or array");

    JsonArray array = results.getAsJsonArray();
    for (JsonElement item : array)
    {
      if (!item.isJsonObject())
        throw new ProtocolException("ASR results array contains a non-object");
      objects.add(item.getAsJsonObject());
    }
    return objects;
  }

  private static String extractResultText(JsonObject result)
  {
    String direct = firstString(result, "text", "utterance", "transcript");
    if (!direct.isEmpty())
      return direct;

    for (String key : new String[]{"alternatives", "words", "utterances"})
    {
      JsonElement nested = result.get(key);
      if (nested == null || !nested.isJsonArray())
        continue;
      for (JsonElement item : nested.getAsJsonArray())
      {
        if (!item.isJsonObject())
          continue;
        String text = extractResultText(item.getAsJsonObject());
        if (!text.isEmpty())
          return text;
      }
    }
    return "";
  }

  private static String firstString(JsonObject object, String... keys)
  {
    for (String key : keys)
    {
      JsonElement value = object.get(key);
      if (value != null && value.isJsonPrimitive())
      {
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        if (primitive.isString() && !primitive.getAsString().isEmpty())
          return primitive.getAsString();
      }
    }
    return "";
  }

  private static Boolean firstBoolean(JsonObject object, String... keys)
  {
    for (String key : keys)
    {
      JsonElement value = object.get(key);
      if (value != null && value.isJsonPrimitive()
          && value.getAsJsonPrimitive().isBoolean())
        return value.getAsBoolean();
    }
    return null;
  }

  private static boolean firstBooleanValue(JsonObject object,
      boolean defaultValue, String... keys)
  {
    Boolean value = firstBoolean(object, keys);
    return value == null ? defaultValue : value.booleanValue();
  }

  private static boolean optionalBoolean(JsonObject object, String key,
      boolean defaultValue)
  {
    if (object == null)
      return defaultValue;
    return firstBooleanValue(object, defaultValue, key);
  }

  private static int optionalInt(JsonObject object, String key, int defaultValue)
  {
    if (object == null)
      return defaultValue;
    JsonElement value = object.get(key);
    if (value == null || !value.isJsonPrimitive()
        || !value.getAsJsonPrimitive().isNumber())
      return defaultValue;
    return value.getAsInt();
  }

  private static JsonObject optionalObject(JsonObject parent, String key)
      throws ProtocolException
  {
    if (parent == null)
      return null;
    JsonElement value = parent.get(key);
    if (value == null || value.isJsonNull())
      return null;
    if (!value.isJsonObject())
      throw new ProtocolException("ASR JSON field '" + key + "' is not an object");
    return value.getAsJsonObject();
  }

  private static void requireNonEmpty(String value, String name)
  {
    if (value == null || value.isEmpty())
      throw new IllegalArgumentException(name + " must not be empty");
  }

  private static final class ProtoWriter
  {
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    void string(int field, String value)
    {
      if (value == null || value.isEmpty())
        return;
      bytes(field, value.getBytes(StandardCharsets.UTF_8));
    }

    void bytes(int field, byte[] value)
    {
      if (value == null || value.length == 0)
        return;
      varint((field << 3) | 2);
      varint(value.length);
      out.write(value, 0, value.length);
    }

    void int32(int field, int value)
    {
      if (value == 0)
        return;
      varint((field << 3) | 0);
      varint(value);
    }

    void varint(long value)
    {
      while ((value & ~0x7fL) != 0)
      {
        out.write(((int)value & 0x7f) | 0x80);
        value >>>= 7;
      }
      out.write((int)value);
    }

    byte[] toByteArray()
    {
      return out.toByteArray();
    }
  }

  private static final class ProtoReader
  {
    private final byte[] data;
    private int position;

    ProtoReader(byte[] data)
    {
      this.data = data;
    }

    boolean hasRemaining()
    {
      return position < data.length;
    }

    int readTag() throws ProtocolException
    {
      long tag = readVarint();
      if (tag == 0 || tag > Integer.MAX_VALUE)
        throw new ProtocolException("Invalid protobuf tag: " + tag);
      return (int)tag;
    }

    String readString(int wireType, int field) throws ProtocolException
    {
      byte[] value = readBytes(wireType, field);
      return new String(value, StandardCharsets.UTF_8);
    }

    byte[] readBytes(int wireType, int field) throws ProtocolException
    {
      requireWireType(wireType, 2, field);
      long lengthValue = readVarint();
      if (lengthValue > Integer.MAX_VALUE)
        throw new ProtocolException("Protobuf field is too large");
      int length = (int)lengthValue;
      if (length < 0 || position + length > data.length)
        throw new ProtocolException("Truncated protobuf length-delimited field");
      byte[] value = new byte[length];
      System.arraycopy(data, position, value, 0, length);
      position += length;
      return value;
    }

    int readInt32(int wireType, int field) throws ProtocolException
    {
      requireWireType(wireType, 0, field);
      return (int)readVarint();
    }

    long readVarint() throws ProtocolException
    {
      long value = 0;
      for (int shift = 0; shift < 64; shift += 7)
      {
        if (position >= data.length)
          throw new ProtocolException("Truncated protobuf varint",
              new EOFException());
        int b = data[position++] & 0xff;
        value |= (long)(b & 0x7f) << shift;
        if ((b & 0x80) == 0)
          return value;
      }
      throw new ProtocolException("Malformed protobuf varint");
    }

    void skip(int wireType) throws ProtocolException
    {
      switch (wireType)
      {
        case 0:
          readVarint();
          return;
        case 1:
          skipBytes(8);
          return;
        case 2:
          long length = readVarint();
          if (length > Integer.MAX_VALUE)
            throw new ProtocolException("Protobuf field is too large");
          skipBytes((int)length);
          return;
        case 5:
          skipBytes(4);
          return;
        default:
          throw new ProtocolException("Unsupported protobuf wire type: "
              + wireType);
      }
    }

    void skipBytes(int length) throws ProtocolException
    {
      if (length < 0 || position + length > data.length)
        throw new ProtocolException("Truncated protobuf field");
      position += length;
    }

    void requireWireType(int actual, int expected, int field)
        throws ProtocolException
    {
      if (actual != expected)
        throw new ProtocolException("Unexpected wire type " + actual
            + " for protobuf field " + field);
    }
  }
}
