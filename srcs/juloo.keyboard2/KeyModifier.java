package juloo.keyboard2;

import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import java.util.HashMap;

public final class KeyModifier
{
  /** The optional modmap takes priority over modifiers usual behaviors. Set to
      [null] to disable. */
  private static Modmap _modmap = null;
  public static void set_modmap(Modmap mm)
  {
    _modmap = mm;
  }

  /** Modify a key according to modifiers. */
  public static KeyValue modify(KeyValue k, Pointers.Modifiers mods)
  {
    if (k == null)
      return null;
    int n_mods = mods.size();
    KeyValue r = k;
    for (int i = 0; i < n_mods; i++)
      r = modify(r, mods.get(i));
    /* Keys with an empty string are placeholder keys. */
    if (r.getString().length() == 0)
      return null;
    return r;
  }

  public static KeyValue modify(KeyValue k, KeyValue mod)
  {
    switch (mod.getKind())
    {
      case Modifier:
        return modify(k, mod.getModifier());
      case Compose_pending:
        return apply_compose_pending(mod.getPendingCompose(), k);
      case Hangul_initial:
        if (k.equals(mod)) // Allow typing the initial in letter form
          return KeyValue.makeStringKey(k.getString(), KeyValue.FLAG_GREYED);
        return combine_hangul_initial(k, mod.getHangulPrecomposed());
      case Hangul_medial:
        return combine_hangul_medial(k, mod.getHangulPrecomposed());
    }
    return k;
  }

  public static KeyValue modify(KeyValue k, KeyValue.Modifier mod)
  {
    switch (mod)
    {
      case CTRL: return apply_ctrl(k);
      case ALT:
      case META: return turn_into_keyevent(k);
      case FN: return apply_fn(k);
      case GESTURE: return apply_gesture(k);
      case SHIFT: return apply_shift(k);
      case GRAVE: return apply_compose_or_dead_char(k, ComposeKeyData.accent_grave, '\u02CB');
      case AIGU: return apply_compose_or_dead_char(k, ComposeKeyData.accent_aigu, '\u00B4');
      case CIRCONFLEXE: return apply_compose_or_dead_char(k, ComposeKeyData.accent_circonflexe, '\u02C6');
      case TILDE: return apply_compose_or_dead_char(k, ComposeKeyData.accent_tilde, '\u02DC');
      case CEDILLE: return apply_compose_or_dead_char(k, ComposeKeyData.accent_cedille, '\u00B8');
      case TREMA: return apply_compose_or_dead_char(k, ComposeKeyData.accent_trema, '\u00A8');
      case CARON: return apply_compose_or_dead_char(k, ComposeKeyData.accent_caron, '\u02C7');
      case RING: return apply_compose_or_dead_char(k, ComposeKeyData.accent_ring, '\u02DA');
      case MACRON: return apply_compose_or_dead_char(k, ComposeKeyData.accent_macron, '\u00AF');
      case OGONEK: return apply_compose_or_dead_char(k, ComposeKeyData.accent_ogonek, '\u02DB');
      case DOT_ABOVE: return apply_compose_or_dead_char(k, ComposeKeyData.accent_dot_above, '\u02D9');
      case BREVE: return apply_dead_char(k, '\u02D8');
      case DOUBLE_AIGU: return apply_compose(k, ComposeKeyData.accent_double_aigu);
      case ORDINAL: return apply_compose(k, ComposeKeyData.accent_ordinal);
      case SUPERSCRIPT: return apply_compose(k, ComposeKeyData.accent_superscript);
      case SUBSCRIPT: return apply_compose(k, ComposeKeyData.accent_subscript);
      case ARROWS: return apply_compose(k, ComposeKeyData.accent_arrows);
      case BOX: return apply_compose(k, ComposeKeyData.accent_box);
      case SLASH: return apply_compose(k, ComposeKeyData.accent_slash);
      case BAR: return apply_compose(k, ComposeKeyData.accent_bar);
      case DOT_BELOW: return apply_compose(k, ComposeKeyData.accent_dot_below);
      case HORN: return apply_compose(k, ComposeKeyData.accent_horn);
      case HOOK_ABOVE: return apply_compose(k, ComposeKeyData.accent_hook_above);
      case DOUBLE_GRAVE: return apply_compose(k, ComposeKeyData.accent_double_grave);
      case ARROW_RIGHT: return apply_combining_char(k, "\u20D7");
      case SELECTION_MODE: return apply_selection_mode(k);
      default: return k;
    }
  }

  /** Modify a key after a long press. */
  public static KeyValue modify_long_press(KeyValue k)
  {
    switch (k.getKind())
    {
      case Event:
        switch (k.getEvent())
        {
          case CHANGE_METHOD_AUTO:
            return KeyValue.getKeyByName("change_method");
          case SWITCH_VOICE_TYPING:
            return KeyValue.getKeyByName("voice_typing_chooser");
        }
        break;
    }
    return k;
  }

  /** Return the compose state that modifies the numpad script. */
  public static int modify_numpad_script(String numpad_script)
  {
    if (numpad_script == null)
      return -1;
    switch (numpad_script)
    {
      case "hindu-arabic": return ComposeKeyData.numpad_hindu;
      case "bengali": return ComposeKeyData.numpad_bengali;
      case "devanagari": return ComposeKeyData.numpad_devanagari;
      case "persian": return ComposeKeyData.numpad_persian;
      case "gujarati": return ComposeKeyData.numpad_gujarati;
      case "kannada": return ComposeKeyData.numpad_kannada;
      case "tamil": return ComposeKeyData.numpad_tamil;
      default: return -1;
    }
  }

  /** Keys that do not match any sequence are greyed. */
  private static KeyValue apply_compose_pending(int state, KeyValue kv)
  {
    switch (kv.getKind())
    {
      case Char:
      case String:
        KeyValue res = ComposeKey.apply(state, kv);
        // Grey-out characters not part of any sequence.
        if (res == null)
          return kv.withFlags(kv.getFlags() | KeyValue.FLAG_GREYED);
        return res;
      /* Tapping compose again exits the pending sequence. */
      case Compose_pending:
        return KeyValue.getKeyByName("compose_cancel");
      /* These keys are not greyed. */
      case Event:
      case Modifier:
        return kv;
      /* Other keys cannot be part of sequences. */
      default:
        return kv.withFlags(kv.getFlags() | KeyValue.FLAG_GREYED);
    }
  }

  /** Apply the given compose state or fallback to the dead_char. */
  private static KeyValue apply_compose_or_dead_char(KeyValue k, int state, char dead_char)
  {
    KeyValue r = ComposeKey.apply(state, k);
    if (r != null)
      return r;
    return apply_dead_char(k, dead_char);
  }

  private static KeyValue apply_compose(KeyValue k, int state)
  {
    KeyValue r = ComposeKey.apply(state, k);
    return (r != null) ? r : k;
  }

  private static KeyValue apply_dead_char(KeyValue k, char dead_char)
  {
    switch (k.getKind())
    {
      case Char:
        char c = k.getChar();
        char modified = (char)KeyCharacterMap.getDeadChar(dead_char, c);
        if (modified != 0 && modified != c)
          return KeyValue.makeStringKey(String.valueOf(modified));
    }
    return k;
  }

  private static KeyValue apply_combining_char(KeyValue k, String combining)
  {
    switch (k.getKind())
    {
      case Char:
        return KeyValue.makeStringKey(k.getChar() + combining, k.getFlags());
    }
    return k;
  }

  private static KeyValue apply_shift(KeyValue k)
  {
    if (_modmap != null)
    {
      KeyValue mapped = _modmap.get(Modmap.M.Shift, k);
      if (mapped != null)
        return mapped;
    }
    KeyValue r = ComposeKey.apply(ComposeKeyData.shift, k);
    if (r != null)
      return r;
    switch (k.getKind())
    {
      case Char:
        char kc = k.getChar();
        char c = Character.toUpperCase(kc);
        return (kc == c) ? k : k.withChar(c);
      case String:
        String ks = k.getString();
        String s = Utils.capitalize_string(ks);
        return s.equals(ks) ? k : KeyValue.makeStringKey(s, k.getFlags());
      default: return k;
    }
  }

  private static KeyValue apply_fn(KeyValue k)
  {
    if (_modmap != null)
    {
      KeyValue mapped = _modmap.get(Modmap.M.Fn, k);
      if (mapped != null)
        return mapped;
    }
    String name = null;
    switch (k.getKind())
    {
      case Char:
      case String:
        KeyValue r = ComposeKey.apply(ComposeKeyData.fn, k);
        return (r != null) ? r : k;
      case Keyevent: name = apply_fn_keyevent(k.getKeyevent()); break;
      case Event: name = apply_fn_event(k.getEvent()); break;
      case Placeholder: name = apply_fn_placeholder(k.getPlaceholder()); break;
      case Editing: name = apply_fn_editing(k.getEditing()); break;
    }
    return (name == null) ? k : KeyValue.getKeyByName(name);
  }

  private static String apply_fn_keyevent(int code)
  {
    switch (code)
    {
      case KeyEvent.KEYCODE_DPAD_UP: return "page_up";
      case KeyEvent.KEYCODE_DPAD_DOWN: return "page_down";
      case KeyEvent.KEYCODE_DPAD_LEFT: return "home";
      case KeyEvent.KEYCODE_DPAD_RIGHT: return "end";
      case KeyEvent.KEYCODE_ESCAPE: return "insert";
      case KeyEvent.KEYCODE_TAB: return "\\t";
      case KeyEvent.KEYCODE_PAGE_UP:
      case KeyEvent.KEYCODE_PAGE_DOWN:
      case KeyEvent.KEYCODE_MOVE_HOME:
      case KeyEvent.KEYCODE_MOVE_END: return "removed";
      default: return null;
    }
  }

  private static String apply_fn_event(KeyValue.Event ev)
  {
    switch (ev)
    {
      case SWITCH_NUMERIC: return "switch_greekmath";
      default: return null;
    }
  }

  private static String apply_fn_placeholder(KeyValue.Placeholder p)
  {
    switch (p)
    {
      case F11: return "f11";
      case F12: return "f12";
      case SHINDOT: return "shindot";
      case SINDOT: return "sindot";
      case OLE: return "ole";
      case METEG: return "meteg";
      default: return null;
    }
  }

  private static String apply_fn_editing(KeyValue.Editing p)
  {
    switch (p)
    {
      case UNDO: return "redo";
      case PASTE: return "pasteAsPlainText";
      default: return null;
    }
  }

  private static KeyValue apply_ctrl(KeyValue k)
  {
    if (_modmap != null)
    {
      KeyValue mapped = _modmap.get(Modmap.M.Ctrl, k);
      // Do not return the modified character right away, first turn it into a
      // key event.
      if (mapped != null)
        k = mapped;
    }
    return turn_into_keyevent(k);
  }

  private static KeyValue turn_into_keyevent(KeyValue k)
  {
    if (k.getKind() != KeyValue.Kind.Char)
      return k;
    int e;
    switch (k.getChar())
    {
      case 'a': e = KeyEvent.KEYCODE_A; break;
      case 'b': e = KeyEvent.KEYCODE_B; break;
      case 'c': e = KeyEvent.KEYCODE_C; break;
      case 'd': e = KeyEvent.KEYCODE_D; break;
      case 'e': e = KeyEvent.KEYCODE_E; break;
      case 'f': e = KeyEvent.KEYCODE_F; break;
      case 'g': e = KeyEvent.KEYCODE_G; break;
      case 'h': e = KeyEvent.KEYCODE_H; break;
      case 'i': e = KeyEvent.KEYCODE_I; break;
      case 'j': e = KeyEvent.KEYCODE_J; break;
      case 'k': e = KeyEvent.KEYCODE_K; break;
      case 'l': e = KeyEvent.KEYCODE_L; break;
      case 'm': e = KeyEvent.KEYCODE_M; break;
      case 'n': e = KeyEvent.KEYCODE_N; break;
      case 'o': e = KeyEvent.KEYCODE_O; break;
      case 'p': e = KeyEvent.KEYCODE_P; break;
      case 'q': e = KeyEvent.KEYCODE_Q; break;
      case 'r': e = KeyEvent.KEYCODE_R; break;
      case 's': e = KeyEvent.KEYCODE_S; break;
      case 't': e = KeyEvent.KEYCODE_T; break;
      case 'u': e = KeyEvent.KEYCODE_U; break;
      case 'v': e = KeyEvent.KEYCODE_V; break;
      case 'w': e = KeyEvent.KEYCODE_W; break;
      case 'x': e = KeyEvent.KEYCODE_X; break;
      case 'y': e = KeyEvent.KEYCODE_Y; break;
      case 'z': e = KeyEvent.KEYCODE_Z; break;
      case '0': e = KeyEvent.KEYCODE_0; break;
      case '1': e = KeyEvent.KEYCODE_1; break;
      case '2': e = KeyEvent.KEYCODE_2; break;
      case '3': e = KeyEvent.KEYCODE_3; break;
      case '4': e = KeyEvent.KEYCODE_4; break;
      case '5': e = KeyEvent.KEYCODE_5; break;
      case '6': e = KeyEvent.KEYCODE_6; break;
      case '7': e = KeyEvent.KEYCODE_7; break;
      case '8': e = KeyEvent.KEYCODE_8; break;
      case '9': e = KeyEvent.KEYCODE_9; break;
      case '`': e = KeyEvent.KEYCODE_GRAVE; break;
      case '-': e = KeyEvent.KEYCODE_MINUS; break;
      case '=': e = KeyEvent.KEYCODE_EQUALS; break;
      case '[': e = KeyEvent.KEYCODE_LEFT_BRACKET; break;
      case ']': e = KeyEvent.KEYCODE_RIGHT_BRACKET; break;
      case '\\': e = KeyEvent.KEYCODE_BACKSLASH; break;
      case ';': e = KeyEvent.KEYCODE_SEMICOLON; break;
      case '\'': e = KeyEvent.KEYCODE_APOSTROPHE; break;
      case '/': e = KeyEvent.KEYCODE_SLASH; break;
      case '@': e = KeyEvent.KEYCODE_AT; break;
      case '+': e = KeyEvent.KEYCODE_PLUS; break;
      case ',': e = KeyEvent.KEYCODE_COMMA; break;
      case '.': e = KeyEvent.KEYCODE_PERIOD; break;
      case '*': e = KeyEvent.KEYCODE_STAR; break;
      case '#': e = KeyEvent.KEYCODE_POUND; break;
      case '(': e = KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN; break;
      case ')': e = KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN; break;
      case ' ': e = KeyEvent.KEYCODE_SPACE; break;
      default: return k;
    }
    return k.withKeyevent(e);
  }

  /** Modify a key affected by a round-trip or a clockwise circle gesture. */
  private static KeyValue apply_gesture(KeyValue k)
  {
    KeyValue modified = apply_shift(k);
    if (modified != null && !modified.equals(k))
      return modified;
    modified = apply_fn(k);
    if (modified != null && !modified.equals(k))
      return modified;
    String name = null;
    switch (k.getKind())
    {
      case Modifier:
        switch (k.getModifier())
        {
          case SHIFT: name = "capslock"; break;
        }
        break;
      case Keyevent:
        switch (k.getKeyevent())
        {
          case KeyEvent.KEYCODE_DEL: name = "delete_word"; break;
          case KeyEvent.KEYCODE_FORWARD_DEL: name = "forward_delete_word"; break;
        }
        break;
    }
    return (name == null) ? k : KeyValue.getKeyByName(name);
  }

  private static KeyValue apply_selection_mode(KeyValue k)
  {
    String name = null;
    switch (k.getKind())
    {
      case Char:
        switch (k.getChar())
        {
          case ' ': name = "selection_cancel"; break;
        }
        break;
      case Slider:
        switch (k.getSlider())
        {
          case Cursor_left: name = "selection_cursor_left"; break;
          case Cursor_right: name = "selection_cursor_right"; break;
        }
        break;
      case Keyevent:
        switch (k.getKeyevent())
        {
          case KeyEvent.KEYCODE_ESCAPE: name = "selection_cancel"; break;
        }
        break;
    }
    return (name == null) ? k : KeyValue.getKeyByName(name);
  }

  /** Compose the precomposed initial with the medial [kv]. */
  private static KeyValue combine_hangul_initial(KeyValue kv, int precomposed)
  {
    switch (kv.getKind())
    {
      case Char:
        return combine_hangul_initial(kv, kv.getChar(), precomposed);
      case Hangul_initial:
        // No initials are expected to compose, grey out
        return kv.withFlags(kv.getFlags() | KeyValue.FLAG_GREYED);
      default:
        return kv;
    }
  }

  private static KeyValue combine_hangul_initial(KeyValue kv, char medial,
      int precomposed)
  {
    int medial_idx;
    switch (medial)
    {
      // Vowels
      case 'ㅏ': medial_idx = 0; break;
      case 'ㅐ': medial_idx = 1; break;
      case 'ㅑ': medial_idx = 2; break;
      case 'ㅒ': medial_idx = 3; break;
      case 'ㅓ': medial_idx = 4; break;
      case 'ㅔ': medial_idx = 5; break;
      case 'ㅕ': medial_idx = 6; break;
      case 'ㅖ': medial_idx = 7; break;
      case 'ㅗ': medial_idx = 8; break;
      case 'ㅘ': medial_idx = 9; break;
      case 'ㅙ': medial_idx = 10; break;
      case 'ㅚ': medial_idx = 11; break;
      case 'ㅛ': medial_idx = 12; break;
      case 'ㅜ': medial_idx = 13; break;
      case 'ㅝ': medial_idx = 14; break;
      case 'ㅞ': medial_idx = 15; break;
      case 'ㅟ': medial_idx = 16; break;
      case 'ㅠ': medial_idx = 17; break;
      case 'ㅡ': medial_idx = 18; break;
      case 'ㅢ': medial_idx = 19; break;
      case 'ㅣ': medial_idx = 20; break;
      // Grey-out uncomposable characters
      default: return kv.withFlags(kv.getFlags() | KeyValue.FLAG_GREYED);
    }
    return KeyValue.makeHangulMedial(precomposed, medial_idx);
  }

  /** Combine the precomposed medial with the final [kv]. */
  private static KeyValue combine_hangul_medial(KeyValue kv, int precomposed)
  {
    switch (kv.getKind())
    {
      case Char:
        return combine_hangul_medial(kv, kv.getChar(), precomposed);
      case Hangul_initial:
        // Finals that can also be initials have this kind.
        return combine_hangul_medial(kv, kv.getString().charAt(0), precomposed);
      default:
        return kv;
    }
  }

  private static KeyValue combine_hangul_medial(KeyValue kv, char c,
      int precomposed)
  {
    int final_idx;
    switch (c)
    {
      case ' ': final_idx = 0; break;
      case 'ㄱ': final_idx = 1; break;
      case 'ㄲ': final_idx = 2; break;
      case 'ㄳ': final_idx = 3; break;
      case 'ㄴ': final_idx = 4; break;
      case 'ㄵ': final_idx = 5; break;
      case 'ㄶ': final_idx = 6; break;
      case 'ㄷ': final_idx = 7; break;
      case 'ㄹ': final_idx = 8; break;
      case 'ㄺ': final_idx = 9; break;
      case 'ㄻ': final_idx = 10; break;
      case 'ㄼ': final_idx = 11; break;
      case 'ㄽ': final_idx = 12; break;
      case 'ㄾ': final_idx = 13; break;
      case 'ㄿ': final_idx = 14; break;
      case 'ㅀ': final_idx = 15; break;
      case 'ㅁ': final_idx = 16; break;
      case 'ㅂ': final_idx = 17; break;
      case 'ㅄ': final_idx = 18; break;
      case 'ㅅ': final_idx = 19; break;
      case 'ㅆ': final_idx = 20; break;
      case 'ㅇ': final_idx = 21; break;
      case 'ㅈ': final_idx = 22; break;
      case 'ㅊ': final_idx = 23; break;
      case 'ㅋ': final_idx = 24; break;
      case 'ㅌ': final_idx = 25; break;
      case 'ㅍ': final_idx = 26; break;
      case 'ㅎ': final_idx = 27; break;
      // Grey-out uncomposable characters
      default: return kv.withFlags(kv.getFlags() | KeyValue.FLAG_GREYED);
    }
    return KeyValue.makeHangulFinal(precomposed, final_idx);
  }
}
