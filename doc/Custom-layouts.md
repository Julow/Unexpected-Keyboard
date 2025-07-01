# Custom layouts
You select a key layout for Unexpected Keyboard by calling up the Settings page (swipe the gear icon) and, at the top of the page, either tapping an existing layout or tapping _Add an alternate layout_. This displays a menu of available layouts. You can define your own layout by choosing _Custom layout_ at the bottom of this menu. Unexpected Keyboard now displays code in the XML format. You make changes by replacing this with different code and tapping OK.

We recommend you keep your work in a file outside Unexpected Keyboard (named something like `MyChanges.xml`). If you installed a new version of Unexpected from a different website (with a different signature), then the work you did solely by editing the XML inside Unexpected would be lost.

Put initial contents into your file in one of these ways:
* Copypaste the code Unexpected displays for _Custom layout_.
* Make a copy of one of the built-in layouts found in [`/srcs/layouts`](https://github.com/Julow/Unexpected-Keyboard/tree/master/srcs/layouts).
* Use the [web-based editor](https://unexpected-keyboard-layout-editor.lixquid.com/). Interact with this web page to define keys and swipes and move keys to desired positions, and it will write the XML code for you. You can make the web page put the XML in a text file or copy it to the clipboard.

When you have prepared suitable XML code in one of these ways, copy it to the clipboard and paste it into Unexpected Keyboard.

## XML language overview
A layout XML file comprises tags that start with `<` and end with `>`.
* Every layout file starts with this declaration:
  `<?xml version="1.0" encoding="utf-8"?>`
* Certain tags come in pairs—an opening tag and a closing tag—and apply to everything between them.
  * The `<keyboard>`...`</keyboard>` pair says that the material between them is the definition of your keyboard. There can be only one of these.
  * The `<row>`...`</row>` pair encloses the definition of a single row.
  * An optional `<modmap>`...`</modmap>` pair contains instructions if you want to change the behavior of a modifier key such as Shift.
* Stand-alone tags include `<key`...`/>`, which defines a single key.

A tag can have properties, defined using an equals sign and a pair of ASCII double quotes. For example, `<key c="a" />` defines the "a" key. The `c` property of the `key` tag says which key you are defining, and the tag's location inside `<row>`...`</row>` specifies where it will go in the row.

### Example
Here is a complete keyboard file with a single row containing an "a" key on the left and a "b" key on the right:

    <?xml version="1.0" encoding="utf-8"?>
    <keyboard name="Simple example" script="latin">
        <row>
            <key c="a" />
            <key c="b" />
        </row>
    </keyboard>

## Keyboard metadata

The `<keyboard>`...`</keyboard>` pair follows the declaration tag and encloses the whole keyboard. The following properties may be used (The first two appear in the example above):

* `name`: The name of the keyboard. The name you specify will appear in the Settings menu. If not present, the layout will just appear as “Custom layout”.

* `script`: The (main) writing system that the keyboard supports. The possible values are `arabic`, `armenian`, `bengali`, `cyrillic`, `devanagari`, `gujarati`, `hangul`, `hebrew`, `latin`, `persian`, `shavian`, and `urdu`. It defaults to `latin`.

* `numpad_script`: The script to use for the numpad. This is useful for scripts where a different, non-ASCII set of numerals is used, like Devanagari and Arabic. It defaults to the same as `script`.

* `bottom_row`: Whether or not to show the built-in bottom row. It accepts `true` or `false`, and defaults to `true`. If your custom layout defines the bottom row, then specify `bottom_row="false"` to disable the built-in bottom row.
  + We recommend your layout use the built-in bottom row, because it is still evolving and your layout will incorporate innovations in future versions. However, to define your own, the current definition of the bottom row is in [bottom_row.xml](https://github.com/Julow/Unexpected-Keyboard/res/xml/bottom_row.xml). You can copypaste this XML into your custom layout as a starting point.
  + Likewise, the current definition of the top (number) row is in [number_row.xml](https://github.com/Julow/Unexpected-Keyboard/res/xml/number_row.xml).

* `embedded_number_row`: Whether the layout has an embedded number row, and thus the "Show number row" setting shouldn't add another one. It accepts `true` or `false`, and defaults to `false`.

* `locale_extra_keys`: Whether Unexpected should add language-dependent extra keys from [method.xml](../res/xml/method.xml) to this layout. It accepts `true` or `false`, and defaults to `true`. To disable these automatic additions, specify `locale_extra_keys="false"`.

## Row
The `<row>`...`</row>` pair encloses one row on the keyboard. It has the following optional property:
* `height`: The height of the row: a positive floating-point value.

* `scale`: A positive floating-point value. If present, scale the width of each key so that the total is equal to the specified value, in key width unit.

A row's default height is 1.0 (one quarter of the keyboard height specified on the Settings menu). The `height` property makes the row taller or shorter than this. For example, if you define a 5-row keyboard but one row has `height="0.7"`, then the keyboard's total height is 4.7 units. If the total is different from 4.0, the keyboard will be taller or shorter than that specified in Settings.

## Key
The `<key />` tag defines a key on the keyboard. Its position in the sequence of keys inside `<row>`...`</row>` indicates its position in the row from left to right. What the key does is defined by optional properties.

### Taps
What the key does when tapped is defined by the optional `c` property. For example, `<key c="a" />` defines the "a" key. Unexpected Keyboard provides a legend in the middle of the key.

When the Shift modifier is tapped, the "a" key becomes the "A" key and the legend temporarily changes. The Fn modifier makes a different change. You can override this behavior with a modmap (see below).

### Swipes
The following optional properties define the effects of swipes:
* `n`, `ne`, `e`, `se`, `s`, `sw`, `w`, `nw`: What the key should do when it is swiped in the direction of that compass point. ("North" means upward and "East" is to the right.)
<TABLE ALIGN=CENTER>
  <TR>
    <TD STYLE="width: 6em;">nw</TD><TD>n</TD><TD>ne</TD>
  </TR>
  <TR ALIGN=CENTER>
    <TD>w</TD><TD>c</TD><TD>e</TD>
  </TR>
  <TR>
    <TD>sw</TD><TD>s</TD><TD>se</TD>
  </TR>
</TABLE>

* `key1` through `key8` is an older way to achieve the same effects. The directions are ordered as follows:
<TABLE ALIGN=CENTER>
  <TR>
    <TD>key1</TD><TD>key7</TD><TD>key2</TD>
  </TR>
  <TR>
    <TD>key5</TD><TD>key0</TD><TD>key6</TD>
  </TR>
  <TR>
    <TD>key3</TD><TD>key8</TD><TD>key4</TD>
  </TR>
</TABLE>

You can define a swipe only once with either compass-point or numeric notation. Unexpected Keyboard automatically puts a small legend in that direction from the center of the key.

* `anticircle`: The key value to send when doing an anti-clockwise gesture on the key.

### Layout
A key may have the following properties to control the row's layout:
* `width`: The width of the key, a positive floating-point value. It defaults to 1.0
* `shift`: How much empty space to add to the left of this key, a non-negative floating-point value. It defaults to 0.0

Normally, a key's width is 1.0 unit. Unexpected Keyboard occupies the full width of the screen, and the row defining the highest number of units (in widths plus shifts) is as wide as the screen. A row whose width is a smaller number of units has empty space on the right.

### Extra legend
* `indication`: An optional extra legend to show under the main label. For example, `<key c="2" indication="ABC" />` displays ABC at the bottom of the 2 key, as on a pinpad or some telephones. If the key also defines a downward swipe with `s` or `key8`, the legends overlap.

### Possible key values
Built-in strings that assign a special function to a key are described in [this page](Possible-key-values.md). For example, `se="copy"` means a southeasterly swipe produces the Copy key. If a key value does not match any of the built-in strings, it outputs that text _verbatim_. For example, `c="a"` simply outputs the letter a.

In a layout, a key value can also start with the `loc` prefix. These are place-holders; the tap or swipe does nothing unless enabled through the "Add keys to keyboard" option in the Settings menu, or implicitly enabled by the language the device is set to use. For example, `ne="loc accent_aigu"` says that a northeast swipe produces the acute accent combinatorial key—if enabled.

## Modmap
The `<modmap>`...`</modmap>` pair encloses custom mappings for modifier keys. The modmap is placed inside the `<keyboard>`...`</keyboard>` pair, but outside any row. A layout can have at most one modmap. It can contain any number of mappings. Each mapping has an `a` property and a `b` property and maps the `a` key to the `b` key. Valid values are listed in [Possible key values](Possible-key-values.md).

The following mappings are supported:

```xml
  <shift a="before" b="after" />
```
This means that when the Shift modifier is on, the key `before` is changed into `after`.

```xml
  <fn a="before" b="after" />
```
This means that when the Fn modifier is on, the key `before` is changed into `after`.

```xml
  <ctrl a="before" b="after" />
```
This means that when the Ctrl modifier is on, the key `before` is changed into `after`. The `<ctrl />` mapping is special in that the Ctrl modifier is applied to `after` after the mapping.

The clockwise circle and the round-trip gestures are affected by both `<shift />` and `<fn />` mappings. The Shift mappings are used first and if that did not modify the key, the Fn mappings are used instead.

### Examples
① Turkish keyboards use the Latin alphabet, but when "i" is shifted, it should produce "İ". This is achieved with the following mapping: 

```xml
    <shift a="i" b="İ" />
```
② Cyrillic layouts have no V key. A layout can define Ctrl-V with the following mapping:

```xml
    <ctrl a="в" b="v" />
```
This maps Ctrl+в to Ctrl+V—not to v.

## Portrait vs. landscape
Unexpected Keyboard remembers *separately* which layout has last been used in portrait and landscape orientation. So you may have one custom layout for portrait orientation, but another custom layout for landscape orientation, and Unexpected Keyboard will switch between them without your intervention.

## Contributing your layout
The Unexpected Keyboard project enthusiastically accepts user contributions, including custom layouts. (See the guidance for layouts at [CONTRIBUTING.md](https://github.com/Julow/Unexpected-Keyboard/blob/master/CONTRIBUTING.md#Adding-a-layout)).
* Submit a layout that has innovations of possible interest to other users at [Unexpected-Keyboard-layouts](https://github.com/Julow/Unexpected-Keyboard-layouts).
* Propose that your layout be included in the set of built-in layouts by making a Pull Request for an addition to [srcs/layouts](https://github.com/Julow/Unexpected-Keyboard/tree/master/srcs/layouts). Please show that such a layout is standard in your locale or has a substantial number of users.
