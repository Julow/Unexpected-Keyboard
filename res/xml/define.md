This file defines the QWERTY layout.

A layout is made of keys arranged into rows. Keys can be made bigger with the
'width' attribute and blank space can be added on the left of a key with the
'shift' attribute.

'k0' assigns the symbol on the middle of the key. 'k8', 'k2', etc..
assign symbols to the corners of a key, they are arranged like this:

  8 1 2
  7 0 3
  6 5 4

Keys prefixed with 'loc ' are not visible on the keyboard. They are used to
specify a place for a key, if it needed to be added to the layout later.
(for example, by the "Add keys to keyboard" option)

See bottom_row.xml for the definition of the bottom row and neo2.xml for a
layout that re-defines it.
See srcs/juloo.keyboard2/KeyValue.java for the keys that have a special meaning.
