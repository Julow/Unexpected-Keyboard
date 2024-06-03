# Custom layouts
Unexpected Keyboard includes key layouts for many natural languages. Unexpected starts out with a key layout likely to suit your device's language. You can select a different layout by calling up the Settings page (swipe the gear icon) and, at the top of the page, either tapping an existing layout or tapping _Add an alternate layout_. This displays a menu of available layouts.

If no built-in layout is what you want, you can define your own layout by choosing _Custom layout_ at the bottom of this menu. Unexpected Keyboard now displays code in the XML format. You make changes by replacing this with different code and tapping OK.

Start out in one of these ways:
* Copypaste the displayed code into a text file (named something like `MyChanges.xml`) to maintain it independently of Unexpected Keyboard.
* Make a copy of one of the built-in layouts found in [`/srcs/layouts`](https://github.com/Julow/Unexpected-Keyboard/tree/master/srcs/layouts).
* Use the [web-based editor](https://unexpected-keyboard-layout-editor.lixquid.com/). You can use touch gestures to define keys and swipes and move keys to desired positions, and it will write the XML code for you.

When you have prepared suitable XML code in one of these ways, copy it to the clipboard and paste it into Unexpected Keyboard.

## XML language overview
A layout XML file comprises tags that start with `<` and end with `>`.
* Every layout file starts with this declaration:
  `<?xml version="1.0" encoding="utf-8"?>`
* Certain tags come in pairs—an opening tag and a closing tag—and apply to everything between them.
  * The `<keyboard>`...`</keyboard>` pair says that the material between them is the definition of your keyboard. There can be only one of these.
  * The `<row>`...`</row>` pair enclose the definition of a single row.
  * An optional `<modmap>`...`</modmap>` pair contains instructions if you want to change the behavior of a modifier key such as Shift.
* Stand-alone tags include `<key`...`/>`, which defines a single key.

A tag can have properties, defined using an equals sign and a pair of ASCII double quotes. For example, `<key key0="a" />` defines the "a" key. The `key0` property of the `key` tag says which key you are defining, and the tag's location inside `<row>`...`</row>` specifies where it will go in the row.

### Example
Here is a complete keyboard file with a single row containing a single key for the letter "a":

    <?xml version="1.0" encoding="utf-8"?>
    <keyboard name="Simple example" script="latin">
        <row>
            <key key0="a" />
        </row>
    </keyboard>

## Keyboard metadata
The `<keyboard>`...`</keyboard>` pair follows the declaration tag and encloses the whole keyboard. The following properties may be used (The first two appear in the example above):
* `name`: The name of the keyboard. The name you specify will appear in the Settings menu. If not present, the layout will just appear as “Custom layout”.
* `script`: The (main) writing system that the keyboard supports. The possible values are `arabic`, `armenian`, `bengali`, `cyrillic`, `devanagari`, `gujarati`, `hangul`, `hebrew`, `latin`, `persian`, `shavian`, and `urdu`. Defaults to `latin`.
* `numpad_script`: The script to use for the numpad. This is useful for scripts where a different, non-ASCII set of numerals is used, like Devanagari and Arabic. Defaults to the same as `script`.
* `bottom_row`: Whether or not to show the common bottom row. It accepts `true` or `false`, and defaults to `true`. If your custom layout defines a bottom row, you should specify `bottom_row=false` to disable the built-in bottom row.

## Row
The `<row>`...`</row>` pair encloses one row on the keyboard. It has only one optional property:
* `height`: The height of the row: a positive floating-point value.

A row's standard height is 1.0. The `height` property makes the row taller or shorter than the others. For example, if you define a 5-row keyboard but the top row has `height="0.7"`, then the keyboard's total height is 4.7 units.

You set the physical height of the complete keyboard in Settings as a percentage of the total height of the screen, which can be different between portrait and landscape. Unexpected Keyboard scales the "units" so all the rows fit in that alloted space. Thus, defining a shorter row gives space to the other rows.

## Key

The `<key />` tag defines a key on the keyboard. It requires at least one of the following properties:

* `key0`: What the key should do when it is tapped.
* `nw`, `ne`, `sw`, `se`, `w`, `e`, `n`, `s`: What the key should do when it is swiped. They are based on cardinal directions, and uses the convention that North is up. These are the new set of keywords, and should not be used with the other set of keywords.

 nw | n    |  ne
:-: | :--: | :-:
 w  | key0 |  e
 sw | s    |  se

* `key1` through `key8`: The older set of keywords for what the key should do when it is swiped. The directions are ordered as follows:

key1 | key7 | key2
:--: | :--: | :--:
key5 | key0 | key6
key3 | key8 | key4

The following properties are optionally supported:

* `width`: The width of the key relative to the normal width. Defaults to `1` and accepts a positive floating point value.
* `shift`: How much empty space to add to the left of this key. Defaults to `0` and accepts a non-negative floating point value.
* `indication`: An extra label to show under the main label, intended to be used as a legend for 2A typing (e.g. `<key key0="2" indication="ABC" />`). Caution: if you have `key8` defined, it overlaps!
* `slider`: If set to `true`, the keys `w` and `e` are sent repeatedly when the key is being slid on. Intended to be used on the space bar, and in fact used on the default space bar.
* `anticircle`: The key value to send when doing an anti-clockwise circle gesture on the key. The clockwise circle and round-trip gestures are not configurable that way.

## Possible key values

`key0` and `nw` through `se` (`key1` through `key8`) take arbitrary strings of characters, and if they don't match any of the special values, it is printed verbatim. (This is intended behavior.)

Special values for the keys are documented in [this page](Possible-key-values).

### `loc ` prefix

Keys prefixed with `loc ` do not appear by default, and are only visible when they are enabled through the "Add keys to keyboard" option in the settings menu, or the language installed on the device is detected to require it.

## Modmap
The `<modmap>`...`</modmap>` pair encloses custom mappings for modifier keys.


A modmap can contain the following tags, each of which must have an `a` and a `b` property:
* `<shift a="`...`" b="`...`" />` —This says that, if the Shift modifier is on (or the user made an anti-clockwise gesture on a key), and if the key would normally generate the "a" sequence, it must instead generate the  "b" sequence.
* `<fn a="`...`" b="`...`" />` —This says that, if the Fn modifier is on (or the user made a round-trip gesture on a key), and if the key would normally generate the "a" sequence, it must instead generate the  "b" sequence.


There can be as many of these tags inside `<modmap>` as needed.

### Example
Turkish keyboards use the Latin alphabet, but when "i" is shifted, it should produce "İ". This is achieved with the following modmap: 


    <modmap>
        <shift a="i" b="İ" />
    </modmap>

## Portrait vs. landscape

Unexpected Keyboard remembers *separately* which layout has last been used in portrait and landscape orientation. That is to say, you may have one custom layout for portrait orientation, but another custom layout for landscape orientation, and Unexpected Keyboard will switch between them without your intervention.
