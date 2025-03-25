#! /bin/env python3

import re

from xml.etree import ElementTree

# TODO Merge with default lat
# TODO Header and commens
# TODO keboard attribs


COMMENT = '''
<!-- This file defines Sinhala layout.

Based on XKB Sinhala (phonetic) layout.
-->
'''


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
            res = f'&#x{val.upper()};'
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
        'a': Key('අ', 'ආ', '0DCA', '0DCF'),
        's': Key('ස', 'ශ', 'ෂ', ''),
        'd': Key('ද', 'ධ', 'ඩ', 'ඪ'),
        # FIXME 'f': Key('ෆ', '', '0DDB', 'ෛ'),
        'g': Key('ග', 'ඝ', 'ඟ', ''),
        'h': Key('හ', '0D83', '0DDE', 'ඖ'),
        'j': Key('ජ', 'ඣ', 'ඦ', ''),
        'k': Key('ක', 'ඛ', 'ඦ', 'ඐ'),
        'l': Key('ල', 'ළ', '0DDF', '0DF3'),
    },
    # TODO Kunddaliya ෴
    'row_3': {
        'z': Key('ඤ', 'ඥ', '007C', '00A6'),
        'x': Key('ඳ', 'ඬ', '', ''),
        'c': Key('ච', 'ඡ', '', ''),
        'v': Key('ව', '', '', ''),
        'b': Key('බ', 'භ', '', ''),
        'n': Key('න', 'ණ', '0D82', 'ඞ'),
        'm': Key('ම', 'ඹ', '', ''),
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
            self._xml_row = ElementTree.SubElement(self._xml_keyboard, 'row')
            for key in row.values():
                self._process_key(self._xml_row, key)
        self._xml_keyboard.append(self._modmap)

    def get_xml(self) -> str:
        ElementTree.indent(self._xml_keyboard)
        raw_body = ElementTree.tostring(
            self._xml_keyboard,
            xml_declaration=False,
            encoding='unicode')

        fixed_body = re.sub(BAD_NUMERIC_REFS_PATTERN, '&#x', raw_body)  # FIXME Ugly and fragile

        result = self.XML_DECLARATION + '\n'
        if self._comment:
            result += self._comment
        result += fixed_body

        return result


if __name__ == '__main__':
    builder = LayoutBuilder(name='සිංහල', script='sinhala', comment=COMMENT)
    builder.build()
    print(builder.get_xml())
