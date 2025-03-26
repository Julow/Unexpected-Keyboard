#! /bin/env python3

## Generator
## May be adoped
## Usage
## Made with Python 3.13.
##

from pathlib import Path
from xml.etree import ElementTree


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
            return None
        else:
            return val

    def __getitem__(self, index: int) -> str | None:
        return self._values[index]

    def __repr__(self) -> str:
        return f'Key{self._values}'

# TODO Postprocessor to escape chars from selected ranges.
TABLE = {
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

MAP = {
    0: 'c',
    1: 'ne',
    2: '0+shift',
    3: '1+shift',
}


# TODO Implement
# TODO Numeric
MODMAP_EXTRA: dict[str, str] = {
}


REFERENCE_LAYOUT_FILE = Path(__file__).parent / 'srcs/layouts/latn_qwerty_us.xml'


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

    def _process_key(self, key: ElementTree.Element) -> ElementTree.Element:
        char = key.get('c')
        if not char:
            return key
        new_key_entry = TABLE.get(char)
        if new_key_entry is None:
            return key

        for level, placement in MAP.items():
            if (char := new_key_entry[level]) is None:
                continue
            if '+' in placement:
                pair = placement.split('+')
                from_level, modkey = int(pair[0]), pair[1]
                key_a = new_key_entry[from_level]
                key_b = char
                if key_a is None:
                    raise RuntimeError(f'Tried to modife {key_a} to {key_b}')
                ElementTree.SubElement(self._modmap, modkey, a=key_a, b=key_b)
            else:
                key.set(placement, char)  # TODO Resolve conflicting
        return key

    def build(self) -> None:
        ref_rows = self._parse_reference_layout()
        for row in ref_rows:
            new_row = ElementTree.SubElement(self._xml_keyboard, 'row')
            for key in row:
                new_row.append(self._process_key(key))
        self._xml_keyboard.append(self._modmap)

    def get_xml(self) -> str:
        ElementTree.indent(self._xml_keyboard)
        body = ElementTree.tostring(
            self._xml_keyboard,
            xml_declaration=False,
            encoding='unicode')

        result = self.XML_DECLARATION + '\n'
        if self._comment:
            result += self._comment
        result += body

        return result


if __name__ == '__main__':
    builder = LayoutBuilder(name='සිංහල', script='sinhala', comment=COMMENT)
    builder.build()
    print(builder.get_xml())
