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
  public static final int NON_SPECIFIC_GENDER = 0;
  public static final int MALE = 1;
  public static final int FEMALE = 2;
  /** Width of the rows containing the 3 genders. */
  public static final int GENDERS = 3;

  private final KeyValue _kv;
  /** The base emoji and its variants, shared by all of them. Variants are
      organized in rows of [_rowWidth], one row per skin tone: no skin tone,
      the 5 uniform skin tones from light to dark, then mixed skin tones.
      Rows of [GENDERS] contain the non-specific, male and female versions.
      Wider rows are groups of people (couples, families). */
  private final List<Emoji> _variants;
  private final int _rowWidth;

  protected Emoji(String bytecode, List<Emoji> variants, int rowWidth)
  {
    this._kv = new KeyValue(bytecode, KeyValue.Kind.String, 0, 0);
    this._variants = variants;
    this._rowWidth = rowWidth;
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

  public int rowWidth()
  {
    return _rowWidth;
  }

  public boolean hasSkinTones()
  {
    return _variants.size() > _rowWidth;
  }

  /** Apply the variations that this emoji doesn't already specify.
      [skinTone] is between [NO_SKIN_TONE] and 5. [gender] is one of
      [NON_SPECIFIC_GENDER], [MALE] or [FEMALE] and only applies to emojis with
      exactly these 3 versions. Unsupported variations are ignored. */
  public Emoji withSkinToneAndGender(int skinTone, int gender)
  {
    int index = _variants.indexOf(this);
    int row = index / _rowWidth;
    int column = index % _rowWidth;
    if (row == NO_SKIN_TONE && hasSkinTones())
      row = skinTone;
    if (column == NON_SPECIFIC_GENDER && _rowWidth == GENDERS)
      column = gender;
    return _variants.get(row * _rowWidth + column);
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

    // Read emoji and their variants (until empty line). Rows are separated by
    // spaces and the emojis in a row by commas.
    while (!(line = reader.readLine()).isEmpty())
    {
      List<Emoji> variants = new ArrayList<>();
      String[] rows = line.split(" ");
      int rowWidth = rows[0].split(",").length;
      for (String row : rows)
        for (String bytecode : row.split(","))
        {
          Emoji e = new Emoji(bytecode, variants, rowWidth);
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
  public static String withSkinToneAndGender(String emoji, int skinTone, int gender)
  {
    Emoji e = getEmojiByString(emoji);
    return (e == null) ? emoji : e.withSkinToneAndGender(skinTone, gender).kv().getString();
  }
}
