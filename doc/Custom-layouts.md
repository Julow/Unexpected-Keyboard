# Custom layouts

Unexpected Keyboard allows custom layouts to be defined, loaded, and used in the app. These layouts are defined in XML.

## Existing keyboard layouts

The XML data files for the keyboard layouts that come with the app can be seen [in `/srcs/layouts`](https://github.com/Julow/Unexpected-Keyboard/tree/master/srcs/layouts).

## Structure

A complete keyboard file with a single row containing a single Turkish "i" key is provided below:

    <?xml version="1.0" encoding="utf-8"?>
    <keyboard name="Keyboard Name" script="latin">
        <modmap>
            <shift a="i" b="İ" />
        </modmap>
        <row>
            <key key0="i" />
        </row>
    </keyboard>

Shift assignments can be changed on a per-character basis.

### Crash course to XML

An XML document is made out of tags.

Paired tags start with `<` and end with `>`, and must be closed by another tag that starts with `</`. They can have other tags as children. Paired tags used in Unexpected Keyboard include `<row>`...`</row>` and `<keyboard>`...`</keyboard>`.

Auto-terminating tags start with `<` and end with `/>`, and can't have any children. Auto-terminating tags used in Unexpected Keyboard include `<key />` and `<shift />`.

An XML tag can have attributes, defined using an equals sign and a pair of ASCII double quotes.

If you do not like XML, you can also use [this third-party GUI editor](https://unexpected-keyboard-layout-editor.lixquid.com/) to create or edit a keyboard layout.

### XML declaration

Every keyboard XML starts with `<?xml version="1.0" encoding="utf-8"?>`.

### Keyboard metadata

The `<keyboard>` tag encloses the whole keyboard. The following properties may be used:

* `name`: The name of the keyboard as it appears in the settings menu. If not present, it will just be called “Custom layout”.
* `bottom_row`: Whether or not to show the common bottom row. Accepts `true` or `false`, and defaults to `true`.
* `script`: The (main) writing system that it supports. Possible values are `arabic`, `armenian`, `bengali`, `cyrillic`, `devanagari`, `gujarati`, `hangul`, `hebrew`, `latin`, `persian`, `shavian`, and `urdu`. Defaults to `latin`.
* `numpad_script`: The script to use for the numpad. This is useful for scripts where a different, non-ASCII set of numerals is used, like Devanagari and Arabic. Defaults to the same as `script`.

### Modmap

The `<modmap>` tag encloses custom mappings for the Shift and Fn key’s behavior.

Each entry contains two mandatory properties: `a` for the base character before the modifier is applied, and `b` for the modified character.

For example, to make the `I` key behave as in Turkish:

    <modmap>
        <shift a="i" b="İ" />
    </modmap>

There can be as many of these tags inside `<modmap>` as needed.

Shift and Fn modmaps also affect the clockwise circle and the roundtrip gestures.

### Row

The `<row>` tag encloses one row on the keyboard. It requires no properties, and supports the following:

* `height`: The height of the row. Defaults to 1, and accepts a positive floating point value.

The total height of the keyboard is defined in Settings as a percentage of the total height of the screen, which can be different between portrait and landscape. The height of a row is relative to the other ones, and are scaled to keep the height of the keyboard constant.

### Key

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

## Portrait vs. landscape

Unexpected Keyboard remembers *separately* which layout has last been used in portrait and landscape orientation. That is to say, you may have one custom layout for portrait orientation, but another custom layout for landscape orientation, and Unexpected Keyboard will switch between them without your intervention.
