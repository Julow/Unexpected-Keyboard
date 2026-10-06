package juloo.keyboard2;

import java.util.Locale;

/** Transform text into a different letter case.
    This is used to replace the text currently selected in the editor, see
    [KeyEventHandler.recase_selection]. Characters that have no equivalent in
    the target style are kept as-is. */
public final class Recase
{
  public static enum Style
  {
    UPPER,
    LOWER,
    SMALL_CAPS,
    /** Characters separated by spaces. */
    SPACED,
  }

  /** Return [text] with the [style] applied. */
  public static String apply(String text, Style style)
  {
    switch (style)
    {
      case UPPER: return text.toUpperCase(Locale.getDefault());
      case LOWER: return text.toLowerCase(Locale.getDefault());
      case SMALL_CAPS: return small_caps(text);
      case SPACED: return spaced(text);
    }
    return text;
  }

  /** Replace each character by its small capital variant, using the same
      substitutions as the small caps modifier. */
  static String small_caps(String text)
  {
    StringBuilder out = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); )
    {
      int c = text.codePointAt(i);
      String mapped = (Character.charCount(c) == 1) ? small_cap_of((char)c) : null;
      if (mapped != null)
        out.append(mapped);
      else
        out.appendCodePoint(c);
      i += Character.charCount(c);
    }
    return out.toString();
  }

  /** Return the small capital variant of [c] or [null] if there is none. */
  static String small_cap_of(char c)
  {
    KeyValue r = ComposeKey.apply(ComposeKeyData.accent_small_caps, c);
    // Only substitutions are used, ignore intermediate states.
    if (r == null || r.getKind() == KeyValue.Kind.Compose_pending)
      return null;
    return r.getString();
  }

  /** Separate every character by a space, without adding a space next to the
      spaces that are already in the text. */
  static String spaced(String text)
  {
    StringBuilder out = new StringBuilder(text.length() * 2);
    int prev = -1;
    for (int i = 0; i < text.length(); )
    {
      int c = text.codePointAt(i);
      if (prev != -1 && !Character.isWhitespace(prev) && !Character.isWhitespace(c))
        out.append(' ');
      out.appendCodePoint(c);
      prev = c;
      i += Character.charCount(c);
    }
    return out.toString();
  }
}
