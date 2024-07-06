# Key values

This is an exhaustive list of special values accepted for the `key0` through `key8` or `nw` through `se` attributes on a key.

Any string that does not exactly match these will be printed verbatim.
A key can output multiple characters, but cannot combine multiple built-in key values.

## Escape codes
Value | Escape code for
:---- | :------
`\?`  | `?`
`\#`  | `#`
`\@`  | `@`
`\n`  | Literal newline character. This is different from `enter` and `action` in certain apps.
`\t`  | Literal tab character. This is different from `tab` in certain apps.
`\\`  | `\`

XML escape codes also work, including:

Value    | Escape code for
:------- | :------
`&amp;`  | `&`
`&lt;`   | `<`
`&gt;`   | `>`
`&quot;` | `"`

## Modifiers
System modifiers are sent to the app, which is free to do whatever they want in response.
The other modifiers only exist within the keyboard.
Value       | Meaning
:---------- | :------
`shift`     | System modifier.
`ctrl`      | System modifier.
`alt`       | System modifier.
`meta`      | System modifier. Equivalent to the Windows key.
`fn`        | Activates Fn mode, which assigns letters and symbols to special characters. e.g. `fn` `!` = `¡`
`compose`   | Compose key. Enables composing characters using Linux-like shortcuts; e.g. `Compose` `A` `single quote` types `Á` (A with acute accent).
`capslock`  | Actives and locks Shift

## Special keys
These keys are sent to apps, which are free to ignore them. The keyboard does not perform editing in response to these keys.

`esc`, `enter`,
`up`, `right`,
`down`, `left`,
`page_up`, `page_down`,
`home`, `end`,
`backspace`, `delete`,
`insert`, `f1`-`f12`,
`tab`, `copy`,
`paste`, `cut`,
`selectAll`, `pasteAsPlainText`,
`undo`, `redo`

## Whitespace
Value   | Meaning
:------ | :------
`space` | Space bar.
`nbsp`  | Non-breaking space.
`nnbsp` | Narrow non-breaking space.
`zwj`   | Zero-width joiner.
`zwnj`  | Zero-width non-joiner.

## Keyboard editing actions
These keys perform editing on the text without sending keys that the app can interpret differently or ignore.
Value              | Meaning
:----------------- | :------
`cursor_left`      | Moves the cursor position to the left directly, without sending a `left` key event.
`cursor_right`     | Moves the cursor position to the right directly, without sending a `right` key event.

## Other modifiers and diacritics
Value                | Meaning
:------------------- | :------
`accent_aigu`        | Acute accent. `á`
`accent_caron`       | Háček. `č`
`accent_cedille`     | Cedilla. `ç`
`accent_circonflexe` | Circumflex. `â`
`accent_grave`       | Grave accent. `à`
`accent_macron`      | Macron. `ā`
`accent_ring`        | Ring accent. `å`
`accent_tilde`       | Tilde. `ã`
`accent_trema`       | Dieresis/umlaut. `ä`
`accent_ogonek`      | Ogonek. `ą`
`accent_dot_above`   | Dot accent. `ż` If applied to the lowercase `i`, removes the dot instead for Turkish. `ı`
`accent_double_aigu` | Double acute accent. `ő`
`accent_slash`       | Slash through. `ø`
`accent_arrow_right` | Right arrow above, used to denote a vector. `a⃗`
`accent_breve`       | Breve. `ă`
`accent_bar`         | Bar/strikethrough. `ɨ`
`accent_dot_below`   | Dot below. `ạ`
`accent_horn`        | Horn accent. `ơ`
`accent_hook_above`  | Hook accent. `ả`
`superscript`        | Superscript. `ᵃ`
`subscript`          | Subscript. `ₐ`
`ordinal`            | Turns `a` and `o` into `ª` and `º`.
`arrows`             | Turns `1`-`4` and `6`-`9` into arrows.
`box`                | Turns `1`-`9`, `0`, and `.` into single-line, thin box-drawing characters.

## Bidirectional
Value   | Meaning
:------ | :------
`lrm`   | Left-to-right mark.
`rlm`   | Right-to-left mark.
`b(`, `b)`, `b[`, `b]`, `b{`, `b}`, `blt`, `bgt` | Sends the bracket characters, but with mirrored key legends for right-to-left languages. (`blt` and `bgt` print `<` and `>` respectively.)

## Hebrew
Keys ending in `_placeholder` are normally hidden unless the Fn key is pressed.

`qamats`, `patah`,
`sheva`, `dagesh`,
`hiriq`, `segol`,
`tsere`, `holam`,
`qubuts`, `hataf_patah`,
`hataf_qamats`, `hataf_segol`,
`shindot`, `shindot_placeholder`,
`sindot`, `sindot_placeholder`,
`geresh`, `gershayim`,
`maqaf`, `rafe`,
`ole`, `ole_placeholder`,
`meteg`, `meteg_placeholder`

## Unexpected Keyboard specific
Value                  | Meaning
:--------------------- | :------
`config`               | Gear icon; opens Unexpected Keyboard settings.
`switch_text`          | Switch to the text layer (main layer).
`switch_numeric`       | Switch to the numeric layer.
`switch_emoji`         | Switch to the emoji layer.
`switch_back_emoji`    | Switch to the text layer from the emoji layer.
`switch_forward`       | Change the keyboard layout, as long as Unexpected Keyboard has multiple keyboard layouts enabled in the settings.
`switch_backward`      | Change the keyboard layout to the previous one in the list.
`switch_greekmath`     | Switch to the Greek & Math Symbols layer.
`change_method`        | Open the input method picker dialog.
`change_method_prev`   | Switch to the previously used input method.
`action`               | Performs a special context-sensitive operation related to the Enter key. For example, in the Twitter (X) app, `enter` adds a new line, while `action` posts.
`voice_typing`         | Begin voice typing.
`voice_typing_chooser` | Shows a menu where you can choose which voice typing provider to use, then begins voice typing when you make a selection.
`shareText`            | Emit a share Intent for the selected text. **Oddity:** This is in CamelCase.

## Unused
These keys are known to do nothing.

`replaceText`, `textAssist`,
`autofill`, `removed`

## Placeholders
These keys are normally hidden unless the Fn modifier is activated.

`f11_placeholder` | `f12_placeholder`
