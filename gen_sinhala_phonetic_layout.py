#! /bin/env python3

"""
Script to generate a layout based on an existing.

Tuned to create Sinhala phonetic layout based on qwerty (US), but may be adoped
for other scripts. Look at dicts before the LayoutBuilder code.

Usage:
    python3 gen_sinhala_phonetic_layout [-h|--help] [-v|--verbose] [-o|--output]

By default with no args will write to corresponding file in `srcs/layouts/`.

Script uses central symbol (in direction "c") to identify a key, which may not
be appropriate for base (reference) layouts other, than qwerty (US).

Warning will be printed to stderr if new symbol overrides some symbol of the
reference layout in directions other, than "c".

Exception will be rised on other
conflicts e. g. when trying to move a symbol into occupied position.

  - Made with latn_qwerty_us.xml from commit `6b1551d`
  - Made with Python 3.13
  - Requires Python >= 3.11
"""

import argparse
import logging

from enum import StrEnum
from pathlib import Path
from xml.etree import ElementTree


class Placement(StrEnum):
    C = 'c'
    NW = 'nw'
    N = 'n'
    NE = 'ne'
    E = 'e'
    SE = 'se'
    S = 's'
    SW = 'sw'
    W = 'w'


# Based on XKB Sinhala (phonetic)
# TODO Chandrabindu
KEYS_MAP: dict[str, tuple[str, str, str, str]] = {
    # Row 1 ###########################################
    'q': ('ඍ', 'ඎ', '\u0DD8', '\u0DF2'),
    'w': ('ඇ', 'ඈ', '\u0DD0', '\u0DD1'),
    'e': ('එ', 'ඒ', '\u0DD9', '\u0DDA'),
    'r': ('ර', '', '', ''),  # In XKB virama is on layer 2
    't': ('ත', 'ථ', 'ට', 'ඨ'),
    'y': ('ය', '', '', ''),  # In XKB virama is on layer 2
    'u': ('උ', 'ඌ', '\u0DD4', '\u0DD6'),
    'i': ('ඉ', 'ඊ', '\u0DD2', '\u0DD3'),
    'o': ('ඔ', 'ඕ', '\u0DDC', '\u0DDD'),
    'p': ('ප', 'ඵ', '', ''),
    # Row 2 ###########################################
    'a': ('අ', 'ආ', '\u0DCA', '\u0DCF'),
    's': ('ස', 'ශ', 'ෂ', ''),
    'd': ('ද', 'ධ', 'ඩ', 'ඪ'),
    'f': ('ෆ', '\u0D93', '', '\u0DDB'),  # In XKB aiyanna placed otherwise
    'g': ('ග', 'ඝ', 'ඟ', ''),
    'h': ('හ', '\u0D83', '\u0DDE', 'ඖ'),
    'j': ('ජ', 'ඣ', 'ඦ', ''),
    'k': ('ක', 'ඛ', 'ඦ', 'ඐ'),
    'l': ('ල', 'ළ', '\u0DDF', '\u0DF3'),
    # Row 3 ###########################################
    'z': ('ඤ', 'ඥ', '', ''),  # In XKB contains bar, broken bar
    'x': ('ඳ', 'ඬ', '', ''),
    'c': ('ච', 'ඡ', '', ''),
    'v': ('ව', '', '', ''),
    'b': ('බ', 'භ', '', ''),
    'n': ('න', 'ණ', '\u0D82', 'ඞ'),
    'm': ('ම', 'ඹ', '', ''),
}

# How to place four levels of Key.
# Syntax: LEVEL: PLACEMENT | 'FROM_LEVEL+MODIFIER'
# The last means symbol on level FROM_LEVEL with modifier key MODIFIER gives
# key on level LEVEL
#
LEVELS_MAP = {
    0: Placement.C,
    1: Placement.NE,
    2: '0+shift',
    3: '1+shift',
}

# Additional modify keys combinations.
# Syntax:
#   MODKEY: { A: B }
#
# TODO Moar numerics!
MODMAP_EXTRA: dict[str, dict[str, str]] = {
    'shift': {
        # Astrological numbers
        '1': '෧',
        '2': '෨',
        '3': '෩',
        '4': '෪',
        '5': '෫',
        '6': '෬',
        '7': '෭',
        '8': '෮',
        '9': '෯',
        '0': '෦',
        # Kunddaliya
        '.': '෴',
        # Extra broken bar intead z key in XKB
        '\u007C': '\u00A6',
        # Special whitespaces
        'zwj': 'zwnj',
    }
}

# Table to move additional characters in reference layout.
# Format is (CENTRAL_CHAR, PLACEMENT): (CENTRAL_CHAR, PLACEMENT). E. g. to move
# char from key with central character "q", direction "se" to key with central
# character "w", direction "sw", add line:
#   ('q', Placement.SE): ('w', Placement.SW),
#
# To delete a char, use None as destination placement. E.g.:
#   ('q', Placment.SE): ('q', None)
#
# Moving of main char in central placement is not supported.
#
TRANSITIONS_MAP: dict[tuple[str, Placement], tuple[str, Placement | None]] = {
    ('q', Placement.SE): ('q', Placement.SW),  # loc esc
    ('q', Placement.NE): ('q', Placement.SE),  # 1

    ('w', Placement.NE): ('w', Placement.SE),  # 2

    ('e', Placement.SE): ('r', Placement.NW),  # loc €
    ('e', Placement.NE): ('e', Placement.SE),  # 3

    ('r', Placement.NE): ('r', Placement.SE),  # 4
    ('t', Placement.NE): ('t', Placement.SE),  # 5
    ('y', Placement.NE): ('y', Placement.SE),  # 6
    ('u', Placement.NE): ('u', Placement.SE),  # 7
    ('i', Placement.NE): ('i', Placement.SE),  # 8

    ('o', Placement.SE): ('p', Placement.SW),  # )
    ('o', Placement.NE): ('o', Placement.SE),  # 9

    ('p', Placement.NE): ('p', Placement.SE),  # 0

    ('a', Placement.NE): ('a', Placement.NW),  # `
    ('a', Placement.NW): ('a', Placement.SW),  # loc tab

    ('s', Placement.NE): ('s', Placement.NW),  # loc §

    ('g', Placement.SW): ('g', Placement.NW),  # _
    ('g', Placement.NE): ('g', Placement.SW),  # -

    ('h', Placement.SW): ('h', Placement.NW),  # +
    ('h', Placement.NE): ('h', Placement.SW),  # =

    ('l', Placement.NE): ('l', Placement.NW),  # |

    ('x', Placement.NE): ('x', Placement.NW),  # loc †
    ('c', Placement.NE): ('c', Placement.NW),  # <
    ('b', Placement.NE): ('b', Placement.NW),  # ?
    ('n', Placement.NE): ('n', Placement.NW),  # :
    ('m', Placement.NE): ('m', Placement.NW),  # "
}

# Add additional characters to arbitrary places.
# Syntax is CHAR: POSITION, where POSITION is a pari as in TRANSITIONS_MAP.
#
CHARS_EXTRA = {
    # In XKB ZWJ is on `/` key, and ZWNJ is on spacebar
    'zwj': ('m', Placement.SE),
}


# List of char unicode numbers and inclusive ranges of numbers to encode as XML
# numeric character references.
# Good for combining signs to not mess with quotes.
# Characters in line of the keyboard tag will not be escaped.
#
ESCAPE_LIST: list[int | tuple[int, int]] = [
    # Sinhalese diacritics
    (0xD81, 0xD83),
    (0xDCA, 0xDDF),
]

# Default filename. Output path can be overrided with `-o` flag also.
LAYOUT_FILENAME = 'sinhala_phonetic.xml'

# Will be placed after XML declaration. Need to have proper <!-- --> tags.
COMMENT = '''
<!-- This file defines Sinhala layout.

Based on XKB Sinhala (phonetic) layout.
-->
'''

BASE_DIR = Path(__file__).parent
REFERENCE_LAYOUT_FILE = BASE_DIR / 'srcs/layouts/latn_qwerty_us.xml'

LOGGER = logging.getLogger(__name__)
KeysMapType = list[list[ElementTree.Element]]


class LayoutGenError(RuntimeError):
    ...


def xml_elem_to_str(element: ElementTree.Element) -> str:
    return ElementTree.tostring(
        element,
        xml_declaration=False,
        encoding='unicode').strip()


def keys_map_to_str(keys_map: KeysMapType) -> str:
    """ Make laout rows map printable for debug purposes """
    result = '[\n'
    for row in keys_map:
        result += ' ' * 4
        for key in row:
            result += str(key.attrib) + ', '
        result += '\n'
    result += ']'
    return result


def is_in_escape_list(char: str | int) -> bool:
    if isinstance(char, str):
        char = ord(char)
    for item in ESCAPE_LIST:
        if isinstance(item, tuple) and char >= item[0] and char <= item[1]:
            return True
        elif isinstance(item, int):
            if char == item:
                return True
        else:
            TypeError(f'Unexpected item {item} of ESCAPE_LIST')
    return False


def xml_encode_char(ch: str | int) -> str:
    if isinstance(ch, str):
        ch = ord(ch)
    hex_val = hex(ch).split('x')[-1]
    return f'&#x{hex_val.upper().zfill(4)};'


class LayoutBuilder:
    XML_DECLARATION = "<?xml version='1.0' encoding='utf-8'?>"

    def __init__(
        self,
        name: str = '',
        script: str = '',
        numpad_script: str = '',
        comment: str = '',
    ) -> None:
        """
        :param comment: MUST be a valid XML comment wrapped in <!-- tags -->
        """
        attrs = {}
        if name:
            attrs['name'] = name
        if script:
            attrs['script'] = script
        if numpad_script:
            attrs['numpad_script'] = numpad_script
        self._comment = None
        if comment:
            self._comment = comment.strip() or None
        self._xml_keyboard = ElementTree.Element('keyboard', attrib=attrs)
        self._modmap = ElementTree.Element('modmap')

    @staticmethod
    def _parse_reference_layout() -> list[ElementTree.Element]:
        return ElementTree.parse(REFERENCE_LAYOUT_FILE).findall('row')

    @staticmethod
    def _move_untransited_to_new_map(
        ref_map: KeysMapType,
        new_map: KeysMapType
    ) -> None:
        coordinates = [
            (row_num, key_num)
            for row_num in range(len(ref_map))
            for key_num in range(len(ref_map[row_num]))
        ]

        for row_num, key_num in coordinates:
            old_key = ref_map[row_num][key_num]
            new_key = new_map[row_num][key_num]
            for k, val in old_key.attrib.items():
                if (transited := new_key.attrib.get(k)) is not None:
                    msg = (
                        f'Transition of {transited} to'
                        f' {new_key.get(Placement.C)}:{k} conflictls with'
                        f' existing value "{val}"')
                    raise LayoutGenError(msg)
                new_key.set(k, val)

    @staticmethod
    def _add_extra_chars_to_ref_map(
        coord_map: dict[str, tuple[int, int]],
        new_map: KeysMapType
    ) -> None:
        for char, (to_key_name, to_plc) in CHARS_EXTRA.items():
            if not (to_coord := coord_map.get(to_key_name)):
                msg = f'Trying to add "{char}" to missing key "{to_key_name}"'
                raise LayoutGenError(msg)
            row_num, key_num = to_coord
            key = new_map[row_num][key_num]
            if (existing := key.get(to_plc)) is not None:
                msg = f'Trying to add char to <{to_key_name}:{to_plc}>, but already contains "{existing}"'
                raise LayoutGenError(msg)
            key.set(to_plc, char)
            LOGGER.info(
                'Added "%s" to <%s:%s>',
                char, to_key_name, to_plc)

    @classmethod
    def _apply_transitions(cls, ref_map: list) -> list:
        coord_map: dict[str, tuple[int, int]] = {}

        coordinates = [
            (row_num, key_num)
            for row_num in range(len(ref_map))
            for key_num in range(len(ref_map[row_num]))
        ]

        for row_num, key_num in coordinates:
            row = ref_map[row_num]
            key = row[key_num]
            key_name = key.get(Placement.C)
            if key_name in coord_map:
                msg = f'Duplicated value "{key_name}" in central position'
                raise LayoutGenError(msg)
            coord_map[key_name] = (row_num, key_num)

        # Make new map with empty keys
        result_map = [[ElementTree.Element('key') for key in row] for row in ref_map]

        # Place by transitions map on new places
        for (from_key_name, from_plc), (to_key_name, to_plc) in TRANSITIONS_MAP.items():
            if Placement.C in (from_plc, to_plc):
                raise NotImplementedError('Transition from or to placment "c"')
            if not (from_coord := coord_map.get(from_key_name)):
                raise LayoutGenError(f'Transition from missing key {from_key_name}')
            if not (to_coord := coord_map.get(to_key_name)):
                raise LayoutGenError(f'Transition to missing key {to_key_name}')
            from_key = ref_map[from_coord[0]][from_coord[1]]
            to_key = result_map[to_coord[0]][to_coord[1]]
            try:
                val = from_key.attrib.pop(from_plc)
            except KeyError:
                msg = f'No value in key {from_key_name}, placement {from_plc} to move'
                raise LayoutGenError(msg)
            if to_plc is not None:
                if to_key.get(to_plc):
                    msg = f'Second transition to key {to_key_name}, placement {to_plc}'
                    raise LayoutGenError(msg)
                to_key.set(to_plc, val)
                LOGGER.info(
                    'Moved "%s" from <%s:%s> to <%s:%s>',
                    val, from_key_name, from_plc, to_key_name, to_plc)
            else:
                LOGGER.info(
                    'Deleted "%s" from <%s:%s>',
                    val, from_key_name, from_plc)

        # Fill new map with other values
        cls._move_untransited_to_new_map(ref_map, new_map=result_map)

        # Add additional characters
        cls._add_extra_chars_to_ref_map(coord_map, new_map=result_map)

        return result_map

    @staticmethod
    def _resolve_placement(
        key: ElementTree.Element,
        placement: Placement,
        new_char: str
    ) -> None:
        if placement != Placement.C:
            central_char = key.get(Placement.C)
            existing = key.get(placement)
            if existing:
                LOGGER.warning(
                    'Placement %s of key %s already occupied with %s',
                    placement, central_char, existing)
        key.set(placement, new_char)

    def _process_key(self, key: ElementTree.Element) -> ElementTree.Element:
        central_char = key.get(Placement.C)
        if not central_char:
            return key
        new_key_entry = KEYS_MAP.get(central_char)
        if new_key_entry is None:
            return key

        for level, placement_spec in LEVELS_MAP.items():
            if not (new_char := new_key_entry[level]):
                continue
            if '+' in placement_spec:
                pair = placement_spec.split('+')
                from_level, modkey = int(pair[0]), pair[1]
                key_a = new_key_entry[from_level]
                key_b = new_char
                if key_a is None:
                    raise LayoutGenError(f'Tried to modife {key_a} to {key_b}')
                ElementTree.SubElement(self._modmap, modkey, a=key_a, b=key_b)
            else:
                placement = Placement(placement_spec)
                self._resolve_placement(key, placement=placement, new_char=new_char)
        return key

    @staticmethod
    def _make_extra_modmap(modmap: ElementTree.Element) -> ElementTree.Element:
        for modkey, ab_map in MODMAP_EXTRA.items():
            for a, b in ab_map.items():
                LOGGER.info('Adding modmap %s "%s" -> "%s"', modkey, a, b)
                ElementTree.SubElement(modmap, modkey, a=a, b=b)
        return modmap

    @staticmethod
    def _post_escape(data: str) -> str:
        buf = ''
        lines = data.splitlines(keepends=True)
        for line in lines:
            # Skip keyboard tag line to keep attributes
            if '<keyboard ' in line:
                buf += line
                continue
            for ch in line:
                if is_in_escape_list(ch):
                    ch = xml_encode_char(ch)
                buf += ch
        return buf

    def build(self) -> None:
        raw_ref_rows = self._parse_reference_layout()
        ref_rows = self._apply_transitions(raw_ref_rows)
        for row in ref_rows:
            new_row = ElementTree.SubElement(self._xml_keyboard, 'row')
            for key in row:
                LOGGER.debug(
                    'Processing reference entry %s',
                    xml_elem_to_str(key))
                new_row.append(self._process_key(key))
        self._modmap = self._make_extra_modmap(self._modmap)
        self._xml_keyboard.append(self._modmap)

    def get_xml(self) -> str:
        ElementTree.indent(self._xml_keyboard)
        body_raw = xml_elem_to_str(self._xml_keyboard)
        body = self._post_escape(body_raw)

        result = self.XML_DECLARATION + '\n'
        if self._comment:
            result += self._comment + '\n'
        result += body + '\n'

        return result


def get_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        prog='gen_sinhala_phonetic_layout',
        description='Generate XKB-based Sinhala layout',)
    parser.add_argument(
        '-o',
        '--output',
        default=BASE_DIR / f'srcs/layouts/{LAYOUT_FILENAME}',
        help='File to write result, `-` for stdout')
    parser.add_argument(
        '-v',
        '--verbose',
        help='More verbose logging',
        action='store_true')
    return parser.parse_args()


if __name__ == '__main__':
    args = get_args()
    logging.basicConfig(
        level=logging.DEBUG if args.verbose else logging.WARNING,
        format='%(levelname)s: %(message)s')
    builder = LayoutBuilder(name='සිංහල', script='sinhala', comment=COMMENT)
    builder.build()
    content = builder.get_xml()
    if args.output == '-':
        print(content)
    else:
        with open(args.output, 'w') as file:
            file.write(content)
