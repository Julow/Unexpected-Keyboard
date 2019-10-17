ocplib-endian
=============

Optimised functions to read and write int16/32/64 from strings, bytes
and bigarrays, based on primitives added in version 4.01.

The library implements three modules:
- [EndianString](src/endianString.cppo.mli) works directly on strings, and provides submodules BigEndian and LittleEndian, with their unsafe counter-parts;
- [EndianBytes](src/endianBytes.cppo.mli) works directly on bytes, and provides submodules BigEndian and LittleEndian, with their unsafe counter-parts;
- [EndianBigstring](src/endianBigstring.cppo.mli) works on bigstrings (Bigarrays of chars), and provides submodules BigEndian and LittleEndian, with their unsafe counter-parts;

