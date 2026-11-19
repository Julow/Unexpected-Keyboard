package juloo.keyboard2;

import android.content.res.Resources;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Emoji
{
  public static final int NO_SKIN_TONE = 0;

  private final KeyValue _kv;
  /** The base emoji followed by its skin tone variants, shared by all of them.
      Variants at index 1 to 5 have a uniform skin tone, from light to dark. */
  private final List<Emoji> _variants;

  protected Emoji(String bytecode, List<Emoji> variants)
  {
    this._kv = new KeyValue(bytecode, KeyValue.Kind.String, 0, 0);
    this._variants = variants;
  }

  public KeyValue kv()
  {
    return _kv;
  }

  public List<Emoji> variants()
  {
    return _variants;
  }

  public boolean hasVariants()
  {
    return _variants.size() > 1;
  }

  /** [skinTone] is between [NO_SKIN_TONE] and 5. Emojis that don't support
      skin tones are returned unchanged. */
  public Emoji withSkinTone(int skinTone)
  {
    return hasVariants() ? _variants.get(skinTone) : this;
  }


  private final static List<Emoji> _all = new ArrayList<>();
  private final static List<List<Emoji>> _groups = new ArrayList<>();
  private final static HashMap<String, Emoji> _stringMap = new HashMap<>();

  public static void init(Resources res)
  {
    if (!_all.isEmpty())
      return;

    try
    {
      InputStream inputStream = res.openRawResource(R.raw.emojis);
      load(new BufferedReader(new InputStreamReader(inputStream)));
    }
    catch (IOException e) { Logs.exn("Emoji.init() failed", e); }
  }

  static void load(BufferedReader reader) throws IOException
  {
    String line;

    // Read emoji and their variants (until empty line)
    while (!(line = reader.readLine()).isEmpty())
    {
      List<Emoji> variants = new ArrayList<>();
      for (String bytecode : line.split(" "))
      {
        Emoji e = new Emoji(bytecode, variants);
        variants.add(e);
        _stringMap.put(bytecode, e);
      }
      _all.add(variants.get(0));
    }

    // Read group indices
    if ((line = reader.readLine()) != null)
    {
      String[] tokens = line.split(" ");
      int last = 0;
      for (int i = 1; i < tokens.length; i++)
      {
        int next = Integer.parseInt(tokens[i]);
        _groups.add(_all.subList(last, next));
        last = next;
      }
      _groups.add(_all.subList(last, _all.size()));
    }
  }

  public static int getNumGroups()
  {
    return _groups.size();
  }

  public static List<Emoji> getEmojisByGroup(int groupIndex)
  {
    return _groups.get(groupIndex);
  }

  public static Emoji getEmojiByString(String value)
  {
    return _stringMap.get(value);
  }

  /** Unknown emojis, including [null], are returned unchanged. */
  public static String withSkinTone(String emoji, int skinTone)
  {
    Emoji e = getEmojiByString(emoji);
    return (e == null) ? emoji : e.withSkinTone(skinTone).kv().getString();
  }
}
