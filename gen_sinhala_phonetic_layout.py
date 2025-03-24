#! /bin/env python3

import re

from collections import defaultdict
from xml.etree import ElementTree

# TODO Merge with default lat
# TODO Header and commens
# TODO keboard attribs


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
            res = None
        elif len(val) == 4:
            res = f'&#x{val};'
        else:
            res = val
        return res

    def __getitem__(self, index: int) -> str | None:
        return self._values[index]

    def __repr__(self) -> str:
        return f'Key{self._values}'


TABLE = {
    'row_1': {
        'q': Key('ඍ', 'ඎ', '0DD8', '0DF2'),
        'w': Key('ඇ', 'ඈ', '0DD0', '0DD1'),
        'e': Key('එ', 'ඒ', '0DD9', '0DDA'),
        'r': Key('ර', '', '', ''),  # In XKB virama was on layer 2
        't': Key('ත', 'ථ', 'ට', 'ඨ'),
        'y': Key('ය', '', '', ''),  # In XKB virama was on layer 2
        'u': Key('උ', 'ඌ', '0DD4', '0DD6'),
        'i': Key('ඉ', 'ඊ', '0DD2', '0DD3'),
        'o': Key('ඔ', 'ඕ', '0DDC', '0DDD'),
        'p': Key('ප', 'ඵ', '', ''),
    },
    'row_2': {
    },
    'row_3': {
    }
}

MAP = {
    0: 'c',
    1: 'ne',
    2: '0+shift',
    3: '1+shift',
}


BAD_NUMERIC_REFS_PATTERN = re.compile('&amp;#x')


class LayoutBuilder:
    def __init__(self) -> None:
        self._xml_root = ElementTree.Element('keyboard')
        self._modmap = ElementTree.Element('modmap')

    def _process_key(self, xml_row: ElementTree.Element, key: Key) -> None:
        xml_key = ElementTree.SubElement(xml_row, 'key')
        for level, placement in MAP.items():
            if (char := key[level]) is None:
                continue
            if '+' in placement:
                pair = placement.split('+')
                from_level, modkey = int(pair[0]), pair[1]
                key_a = key[from_level]
                key_b = char
                if key_a is None:
                    raise RuntimeError(f'Tried to modife {key_a} to {key_b}')
                ElementTree.SubElement(self._modmap, modkey, a=key_a, b=key_b)
            else:
                if char is not None:
                    if xml_key.get(placement):
                        raise Exception  # TODO
                    xml_key.set(placement, char)

    def build(self) -> None:
        for row in TABLE.values():
            self._xml_row = ElementTree.SubElement(self._xml_root, 'row')
            for key in row.values():
                self._process_key(self._xml_row, key)
        self._xml_root.append(self._modmap)

    def print(self) -> None:
        ElementTree.indent(self._xml_root)
        raw = ElementTree.tostring(self._xml_root, encoding='unicode')
        post = re.sub(BAD_NUMERIC_REFS_PATTERN, '&#x', raw)  # FIXME Ugly and fragile
        print(post)


if __name__ == '__main__':
    builder = LayoutBuilder()
    builder.build()
    builder.print()
