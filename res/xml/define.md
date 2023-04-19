# How to define a layout

A layout is made of keys arranged into rows. The width of keys can be defined with the 'width' attribute; a space can be added to the left side of a key with the 'shift' attribute.

'k0' assigns the symbol in the middle of the key. Other symbols are assigned with 'k1', 'k2', etc.:
┌─────┐
│8 1 2│
│7 0 3│
│6 5 4│
└─────┘

Keys prefixed with 'loc ' are not visible on the keyboard. They are used to
specify a place for a key, if it needed to be added to the layout later.
(for example, by the 'Add keys to keyboard' option)

See any XML that defines a layout.

To define diacritics and other combinations, see
* srcs/juloo.keyboard2/KeyModifier.java
* srcs/juloo.keyboard2/KeyValue.java

To define extra keys, see
* srcs/juloo.keyboard2/ExtraKeyCheckBoxPreference.java