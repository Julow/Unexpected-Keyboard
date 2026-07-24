package juloo.keyboard2.doubao;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import okhttp3.HttpUrl;
import org.junit.Test;

public class DoubaoProtocolTest
{
  @Test
  public void startTaskMatchesProtobufSchema()
  {
    byte[] request = DoubaoProtocol.buildStartTask("r", "t");
    assertArrayEquals(new byte[]{
        0x12, 0x01, 't',
        0x1a, 0x03, 'A', 'S', 'R',
        0x2a, 0x09, 'S', 't', 'a', 'r', 't', 'T', 'a', 's', 'k',
        0x42, 0x01, 'r'
    }, request);
  }

  @Test
  public void sessionPayloadKeepsReferenceSettings()
  {
    JsonObject payload = JsonParser.parseString(
        DoubaoProtocol.buildSessionPayload("123")).getAsJsonObject();
    JsonObject audio = payload.getAsJsonObject("audio_info");
    JsonObject extra = payload.getAsJsonObject("extra");

    assertEquals(1, audio.get("channel").getAsInt());
    assertEquals("speech_opus", audio.get("format").getAsString());
    assertEquals(16000, audio.get("sample_rate").getAsInt());
    assertTrue(payload.get("enable_punctuation").getAsBoolean());
    assertTrue(payload.get("enable_vad").getAsBoolean());
    assertFalse(payload.get("low_latency").getAsBoolean());
    assertEquals("oime", extra.get("app_name").getAsString());
    assertEquals(8, extra.get("cell_compress_rate").getAsInt());
    assertEquals("123", extra.get("did").getAsString());
    assertEquals("tool", extra.get("input_mode").getAsString());
    assertTrue(extra.get("interim_results").getAsBoolean());
  }

  @Test
  public void audioRequestCarriesFrameAndTimestamp()
  {
    byte[] request = DoubaoProtocol.buildAudioRequest("request",
        new byte[]{1, 2, 3}, DoubaoProtocol.FRAME_LAST, 42);
    Map<Integer, byte[]> fields = decodeLengthDelimitedFields(request);

    assertEquals("ASR", utf8(fields.get(3)));
    assertEquals("TaskRequest", utf8(fields.get(5)));
    assertArrayEquals(new byte[]{1, 2, 3}, fields.get(7));
    assertEquals("request", utf8(fields.get(8)));
    assertTrue(utf8(fields.get(6)).contains("\"timestamp_ms\":42"));
    assertEquals(DoubaoProtocol.FRAME_LAST, decodeVarintField(request, 9));
  }

  @Test
  public void parsesLifecycleResponse() throws Exception
  {
    DoubaoProtocol.Response response = DoubaoProtocol.parseResponse(
        response("TaskStarted", 0, "", ""));
    assertEquals(DoubaoProtocol.ResponseType.TASK_STARTED, response.type);
  }

  @Test
  public void parsesArrayInterimResult() throws Exception
  {
    String json = "{\"results\":[{\"text\":\"你好\","
        + "\"is_interim\":true}],\"extra\":{\"packet_number\":7}}";
    DoubaoProtocol.Response response = DoubaoProtocol.parseResponse(
        response("", 0, "", json));

    assertEquals(DoubaoProtocol.ResponseType.INTERIM_RESULT, response.type);
    assertEquals("你好", response.text);
    assertFalse(response.isFinal);
  }

  @Test
  public void parsesObjectFinalWithNestedAlternative() throws Exception
  {
    String json = "{\"results\":{\"alternatives\":[{\"transcript\":\"你好。\"}],"
        + "\"is_final\":true,\"is_vad_finished\":true}}";
    DoubaoProtocol.Response response = DoubaoProtocol.parseResponse(
        response("", 0, "", json));

    assertEquals(DoubaoProtocol.ResponseType.FINAL_RESULT, response.type);
    assertEquals("你好。", response.text);
    assertTrue(response.isFinal);
    assertTrue(response.vadFinished);
  }

  @Test
  public void failureMessageTypeIsVisibleError() throws Exception
  {
    DoubaoProtocol.Response response = DoubaoProtocol.parseResponse(
        response("SessionFailed", 401, "bad token", ""));
    assertEquals(DoubaoProtocol.ResponseType.ERROR, response.type);
    assertEquals(401, response.statusCode);
    assertEquals("bad token", response.errorMessage);
  }

  @Test
  public void okStatusCodeDoesNotOverrideRecognitionPayload() throws Exception
  {
    String json = "{\"results\":[{\"text\":\"端到端测试\","
        + "\"is_interim\":true}]}";
    DoubaoProtocol.Response response = DoubaoProtocol.parseResponse(
        response("", 200, "OK", json));

    assertEquals(DoubaoProtocol.ResponseType.INTERIM_RESULT, response.type);
    assertEquals("端到端测试", response.text);
  }

  @Test
  public void okStatusWithoutPayloadIsNotAnError() throws Exception
  {
    DoubaoProtocol.Response response = DoubaoProtocol.parseResponse(
        response("", 200, "OK", ""));

    assertEquals(DoubaoProtocol.ResponseType.UNKNOWN, response.type);
  }

  @Test(expected = DoubaoProtocol.ProtocolException.class)
  public void malformedJsonFails() throws Exception
  {
    DoubaoProtocol.parseResponse(response("", 0, "", "{broken"));
  }

  @Test(expected = DoubaoProtocol.ProtocolException.class)
  public void malformedProtobufFails() throws Exception
  {
    DoubaoProtocol.parseResponse(new byte[]{0x3a, 0x05, '{'});
  }

  @Test
  public void encodesTwentyMillisecondOpusFrame() throws Exception
  {
    DoubaoVoiceInput.OpusFrameEncoder encoder =
        new DoubaoVoiceInput.OpusFrameEncoder();
    byte[] opus = encoder.encode(new short[320]);
    assertTrue(opus.length > 0);
    assertTrue(opus.length < 4000);
  }

  @Test
  public void webSocketHandshakeUsesHttpsUrlForOkHttp()
  {
    HttpUrl url = DoubaoAsrClient.Session.buildWebSocketUrl("device id");

    assertEquals("https", url.scheme());
    assertEquals("frontier-audio-ime-ws.doubao.com", url.host());
    assertEquals("401734", url.queryParameter("aid"));
    assertEquals("device id", url.queryParameter("device_id"));
  }

  private static byte[] response(String messageType, int statusCode,
      String statusMessage, String resultJson)
  {
    TestProtoWriter writer = new TestProtoWriter();
    writer.string(4, messageType);
    writer.int32(5, statusCode);
    writer.string(6, statusMessage);
    writer.string(7, resultJson);
    return writer.toByteArray();
  }

  private static Map<Integer, byte[]> decodeLengthDelimitedFields(byte[] data)
  {
    Map<Integer, byte[]> fields = new HashMap<Integer, byte[]>();
    int[] position = new int[]{0};
    while (position[0] < data.length)
    {
      long tag = readVarint(data, position);
      int field = (int)(tag >>> 3);
      int wire = (int)(tag & 7);
      if (wire == 2)
      {
        int length = (int)readVarint(data, position);
        fields.put(field, Arrays.copyOfRange(data, position[0],
            position[0] + length));
        position[0] += length;
      }
      else if (wire == 0)
        readVarint(data, position);
      else
        throw new AssertionError("Unexpected wire type " + wire);
    }
    return fields;
  }

  private static int decodeVarintField(byte[] data, int wantedField)
  {
    int[] position = new int[]{0};
    while (position[0] < data.length)
    {
      long tag = readVarint(data, position);
      int field = (int)(tag >>> 3);
      int wire = (int)(tag & 7);
      if (wire == 0)
      {
        int value = (int)readVarint(data, position);
        if (field == wantedField)
          return value;
      }
      else if (wire == 2)
      {
        int length = (int)readVarint(data, position);
        position[0] += length;
      }
      else
        throw new AssertionError("Unexpected wire type " + wire);
    }
    throw new AssertionError("Missing field " + wantedField);
  }

  private static long readVarint(byte[] data, int[] position)
  {
    long value = 0;
    for (int shift = 0; shift < 64; shift += 7)
    {
      int b = data[position[0]++] & 0xff;
      value |= (long)(b & 0x7f) << shift;
      if ((b & 0x80) == 0)
        return value;
    }
    throw new AssertionError("Malformed varint");
  }

  private static String utf8(byte[] value)
  {
    return new String(value, StandardCharsets.UTF_8);
  }

  private static final class TestProtoWriter
  {
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    void string(int field, String value)
    {
      if (value.isEmpty())
        return;
      byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
      varint((field << 3) | 2);
      varint(bytes.length);
      out.write(bytes, 0, bytes.length);
    }

    void int32(int field, int value)
    {
      if (value == 0)
        return;
      varint(field << 3);
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
}
