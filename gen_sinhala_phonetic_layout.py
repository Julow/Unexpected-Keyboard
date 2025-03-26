#! /bin/env python3

# TODO Fill the doc
"""
Generator
May be adoped
Usage
Requires Python >= 3.8.
Made with Python 3.13.
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


class Key:
    def __init__(
        self,
        l1: str | None,
        l2: str | None,
        l3: str | None,
        l4: str | None
    ) -> None:
        self._values = (
            self._clean_val(l1),
            self._clean_val(l2),
            self._clean_val(l3),
            self._clean_val(l4),
        )

    @staticmethod
    def _clean_val(val: None | str) -> None | str:
        if not val:
            return None
        else:
            return val

    def __getitem__(self, index: int) -> str | None:
        return self._values[index]

    def __repr__(self) -> str:
        return f'Key{self._values}'


# TODO Postprocessor to escape chars from selected ranges.
KEYS_MAP = {
    # TODO Add ZWJ and ZWNJ (200D, 200C)
    # Row 1 ###########################################
    'q': Key('ඍ', 'ඎ', '\u0DD8', '\u0DF2'),
    'w': Key('ඇ', 'ඈ', '\u0DD0', '\u0DD1'),
    'e': Key('එ', 'ඒ', '\u0DD9', '\u0DDA'),
    'r': Key('ර', '', '', ''),  # In XKB virama was on layer 2
    't': Key('ත', 'ථ', 'ට', 'ඨ'),
    'y': Key('ය', '', '', ''),  # In XKB virama was on layer 2
    'u': Key('උ', 'ඌ', '\u0DD4', '\u0DD6'),
    'i': Key('ඉ', 'ඊ', '\u0DD2', '\u0DD3'),
    'o': Key('ඔ', 'ඕ', '\u0DDC', '\u0DDD'),
    'p': Key('ප', 'ඵ', '', ''),
    # Row 2 ###########################################
    'a': Key('අ', 'ආ', '\u0DCA', '\u0DCF'),
    's': Key('ස', 'ශ', 'ෂ', ''),
    'd': Key('ද', 'ධ', 'ඩ', 'ඪ'),
    # TODO Swap letter and sigh?
    # FIXME
    'f': Key('ෆ', '\u0DDB', 'ෛ', ''), # In XKB aiyanna is 1 level higher
    'g': Key('ග', 'ඝ', 'ඟ', ''),
    'h': Key('හ', '\u0D83', '\u0DDE', 'ඖ'),
    'j': Key('ජ', 'ඣ', 'ඦ', ''),
    'k': Key('ක', 'ඛ', 'ඦ', 'ඐ'),
    'l': Key('ල', 'ළ', '\u0DDF', '\u0DF3'),
    # Row 3 ###########################################
    # TODO Kunddaliya ෴
    'z': Key('ඤ', 'ඥ', '\u007C', '\u00A6'),
    'x': Key('ඳ', 'ඬ', '', ''),
    'c': Key('ච', 'ඡ', '', ''),
    'v': Key('ව', '', '', ''),
    'b': Key('බ', 'භ', '', ''),
    'n': Key('න', 'ණ', '\u0D82', 'ඞ'),
    'm': Key('ම', 'ඹ', '', ''),
}

LAYERS_MAP = {
    0: 'c',
    1: 'ne',
    2: '0+shift',
    3: '1+shift',
}


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
    }
}

# TODO Doc not Placement.C
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

    # FIXME dup
    ('l', Placement.NE): ('l', Placement.NW),  # |

    ('x', Placement.NE): ('x', Placement.NW),  # loc †
    ('c', Placement.NE): ('c', Placement.NW),  # <
    ('b', Placement.NE): ('b', Placement.NW),  # ?
    ('n', Placement.NE): ('n', Placement.NW),  # :
    ('m', Placement.NE): ('m', Placement.NW),  # "
}

COMMENT = '''
<!-- This file defines Sinhala layout.

Based on XKB Sinhala (phonetic) layout.
-->
'''

BASE_DIR = Path(__file__).parent
REFERENCE_LAYOUT_FILE = BASE_DIR / 'srcs/layouts/latn_qwerty_us.xml'

LOGGER = logging.getLogger(__name__)

KeysMapType = list[list[ElementTree.Element]]


def xml_elem_to_str(element: ElementTree.Element) -> str:
    return ElementTree.tostring(
        element,
        xml_declaration=False,
        encoding='unicode').strip()


def keys_map_to_str(keys_map: KeysMapType) -> str:
    result = '[\n'
    for row in keys_map:
        result += ' ' * 4
        for key in row:
            result += str(key.attrib) + ', '
        result += '\n'
    result += ']'
    return result


class LayoutBuilder:
    XML_DECLARATION = "<?xml version='1.0' encoding='utf-8'?>"

    def __init__(
        self,
        name: str | None = None,
        script: str | None = None,
        numpad_script: str | None = None,
        comment: str | None = None,
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
                    raise RuntimeError(msg)
                new_key.set(k, val)

    @classmethod
    def _apply_transitions(cls, ref_map: list) -> list:
        coord_map = {}

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
                raise RuntimeError(msg)
            coord_map[key_name] = (row_num, key_num)

        # Make new map with empty keys
        result_map = [[ElementTree.Element('key') for key in row] for row in ref_map]

        ## Place by transitions map on new places
        for (from_key_name, from_plc), (to_key_name, to_plc) in TRANSITIONS_MAP.items():
            if Placement.C in (from_plc, to_plc):
                raise NotImplementedError('Transition from or to placment "c"')
            if not (from_coord := coord_map.get(from_key_name)):
                raise RuntimeError(f'Transition from missing key {from_key_name}')
            if not (to_coord := coord_map.get(to_key_name)):
                raise RuntimeError(f'Transition to missing key {to_key_name}')
            from_key = ref_map[from_coord[0]][from_coord[1]]
            to_key = result_map[to_coord[0]][to_coord[1]]
            try:
                val = from_key.attrib.pop(from_plc)
            except KeyError:
                msg = f'No value in key {from_key_name}, placement {from_plc} to move'
                raise RuntimeError(msg)
            if to_key.get(to_plc):
                msg = f'Second transition to key {to_key_name}, placement {to_plc}'
                raise RuntimeError(msg)
            if to_plc is not None:
                to_key.set(to_plc, val)
                LOGGER.info(
                    'Moved "%s" from %s:%s to %s:%s',
                    val, from_key_name, from_plc, to_key_name, to_plc)
            else:
                LOGGER.info(
                    'Deleted "%s" from %s:%s',
                    val, from_key_name, from_plc)

        # Fill new map with other values
        cls._move_untransited_to_new_map(ref_map, new_map=result_map)

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

        for level, placement_spec in LAYERS_MAP.items():
            if (new_char := new_key_entry[level]) is None:
                continue
            if '+' in placement_spec:
                pair = placement_spec.split('+')
                from_level, modkey = int(pair[0]), pair[1]
                key_a = new_key_entry[from_level]
                key_b = new_char
                if key_a is None:
                    raise RuntimeError(f'Tried to modife {key_a} to {key_b}')
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
        body = xml_elem_to_str(self._xml_keyboard)

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
        default=BASE_DIR / 'srcs/layouts/sinhala_phonetic.xml',
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
