This is an exhaustive list of special values accepted for the `key0` through `key8` or `nw` through `se` attributes on a key. Any string that does not exactly match these will be printed verbatim.

**Escape codes**
Value | Meaning
:---- | :------
`\?`  | Escape code for `?` (question mark).
`\#`  | Escape code for `#` (number sign).
`\@`  | Escape code for `@` (at mark).
`\n`  | Escape code for newline. This has inconsistent compatibility (sometimes acts as the Enter key, sometimes inserts a literal line-break character, even into text areas where it doesn’t belong). What you really want is probably `enter` or `action`.
`\\`  | Escape code for `\` (backslash).

XML escape codes also work, including:

Value    | Meaning
:------- | :------
`&amp;`  | Escape code for `&` (ampersand).
`&lt;`   | Escape code for `<` (less-than sign).
`&gt;`   | Escape code for `>` (greater-than sign).
`&quot;` | Escape code for `"` (straight double quote).

**Virtual keys**
Value       | Meaning
:---------- | :------
`shift`     | Shift key.
`ctrl`      | Control key.
`alt`       | Alt key.
`meta`      | Meta key. Equivalent to the Windows key.
`capslock`  | Caps lock key.
`esc`       | Escape key.
`enter`     | Enter key. Known as Return on Mac OS.
`up`        | Cursor up key.
`right`     | Cursor right key.
`down`      | Cursor down key.
`left`      | Cursor left key.
`page_up`   | Page up key.
`page_down` | Page down key.
`home`      | Home key.
`end`       | End key.
`backspace` | Backspace key.
`delete`    | Delete key.
`insert`    | Insert key.
`f1`-`f12`  | The function keys from F1 to F12. F13 throught F24 are not supported, because no Android apps actually use those.
`tab`       | Tab key.

**Whitespace and bidirectional text**
Value   | Meaning
:------ | :------
`\\t`   | Literal tab character. This may yield different behavior from `tab` in certain text-editor apps.
`space` | Space bar.
`nbsp`  | No-break space. Note: To input the narrow no-break space recommended for French, use `\u202F`.
`lrm`   | Left-to-right mark.
`rlm`   | Right-to-left mark.
`b(`, `b)`, `b[`, `b]`, `b{`, `b}`, `blt`, `bgt` | Sends the bracket characters, but with mirrored key legends for right-to-left languages. (`blt` and `bgt` print `<` and `>` respectively.)

**Virtual shortcuts**
Value              | Meaning
:----------------- | :------
`copy`             | Copy. Equivalent to Ctrl+C.
`paste`            | Paste. Equivalent to Ctrl+V.
`cut`              | Cut. Equivalent to Ctrl+X.
`selectAll`        | Select all. Equivalent to Ctrl+A. **Oddity:** This is in CamelCase.
`pasteAsPlainText` | Paste as plain text. Equivalent to Ctrl+Shift+V. ***TODO: Verify if this is true.*** **Oddity:** This is in CamelCase.
`undo`             | Undo. Equivalent to Ctrl+Z.
`redo`             | Redo. Equivalent to Ctrl+Shift+Z. ***TODO: Or is it Ctrl+Y?***
`cursor_left`      | Moves the cursor position to the left directly, without sending a `left` key event.
`cursor_right`     | Moves the cursor position to the right directly, without sending a `right` key event.

**Diacritics**
Value                | Meaning
:------------------- | :------
`compose`            | Compose key. Enables composing characters using Linux-like shortcuts; e.g. `Compose` `A` `single quote` types `Á` (A with acute accent).
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
`accent_bar`         | Bar/strikethrough. `ᵢ`
`accent_dot_below`   | Dot below. `ạ`
`accent_horn`        | Horn accent. `ơ`
`accent_hook_above`  | Hook accent. `ả`
`superscript`        | Superscript. `ᵃ`
`subscript`          | Subscript. `ₐ`
`ordinal`            | Turns `a` and `o` into `ª` and `º`.
`arrows`             | Turns `1`-`4` and `6`-`9` into arrows.
`box`                | Turns `1`-`9`, `0`, and `.` into single-line, thin box-drawing characters.
`fn`                 | Activates Fn mode, which assigns letters and symbols to special characters. e.g. `fn` `!` = `¡`

***TODO: I don't know Hebrew, so I can't describe these.***

**Hebrew**
Value                 | Meaning
:-------------------- | :------
`qamats`              | Kamatz.
`patah`               | Patach.
`sheva`               | Sheva
`dagesh`              | Dagesh or mapiq.
`hiriq`               | Hiriq.
`segol`               | Segol.
`tsere`               | Tsere.
`holam`               | Holam.
`qubuts`              | Kubuts.
`hataf_patah`         | Reduced patach.
`hataf_qamats`        | Reduced kamatz.
`hataf_segol`         | Reduced segol.
`shindot`             | Shin dot.
`shindot_placeholder` | Shin dot placeholder. ***TODO: What does "placeholder" mean here?*
`sindot`              | Sin dot.
`sindot_placeholder`  | Sin dot placeholder.
`geresh`              | Geresh.
`gershayim`           | Gershayim.
`maqaf`               | Maqaf.
`rafe`                | Rafe.
`ole`                 | Ole.
`ole_placeholder`     | Ole placeholder.
`meteg`               | Meteg, siluq, or sof-pasuq.
`meteg_placeholder`   | Meteg placeholder.

**Ligature control**
Value  | Meaning
:----- | :------
`zwj`  | Zero-width joiner.
`zwnj` | Zero-width non-joiner.

**Unexpected Keyboard specific**
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
`change_method`        | Switch from Unexpected Keyboard to the next input method in the list.
`change_method_prev`   | Switch from Unexpected Keyboard to the previous input method in the list.
`action`               | Performs a special context-sensitive operation related to the Enter key. For example, in the Twitter (X) app, `enter` adds a new line, while `action` posts.
`voice_typing`         | Begin voice typing.
`voice_typing_chooser` | Shows a menu where you can choose which voice typing provider to use, then begins voice typing when you make a selection.
`shareText`            | Emit a share Intent for the selected text. **Oddity:** This is in CamelCase.

**Unused**
Value         | Meaning
:------------ | :------
`replaceText` | This is in the code, but marked as "not used".
`textAssist`  | This is in the code, but marked as "not used".
`autofill`    | This is in the code, but marked as "not used".

**Placeholders**
Value             | Meaning
:---------------- | :------
`removed`         | ***TODO: What does this do?***
`f11_placeholder` | ***TODO: What does this do?***
`f12_placeholder` | ***TODO: What does this do?***
