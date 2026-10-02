#!/usr/bin/env python3
"""Build the Chinese character tables from the Unicode Unihan database.

Usage:
    python3 -B srcs/pinyin/from_unihan.py Unihan_Readings.txt Unihan_Variants.txt srcs/pinyin

Writes two files into the output directory:

- `syllables.txt`: one line per toneless Hanyu Pinyin syllable, the syllable, a
  space, then the characters written with that syllable (at most 16, most
  frequent first). Characters whose reading comes from the `kHanyuPinlu` field
  of `Unihan_Readings.txt`; every other field is ignored.
- `variants.txt`: the traditional variant of the characters of `syllables.txt`,
  one `simplified+traditional` pair per line, taken from the
  `kTraditionalVariant` field of `Unihan_Variants.txt`.

Characters that are the traditional form of another character are ranked after
the simplified form when both have the same frequency, so that the tables read
naturally for Simplified Chinese. Traditional Chinese candidates are obtained by
substituting the pairs of `variants.txt`.
"""

import os
import re
import sys
import unicodedata

SYLLABLES_HEADER = (
    "# Chinese characters by toneless pinyin syllable, ordered by frequency.\n"
    "# Derived from the Unihan database (kHanyuPinlu field), (c) Unicode, Inc. See README.md.\n"
)
VARIANTS_HEADER = (
    "# Simplified character followed by its traditional variant, for the characters of syllables.txt.\n"
    "# Derived from the Unihan database (kTraditionalVariant field), (c) Unicode, Inc. See README.md.\n"
)

MAX_CHARS = 16

# "ü" and its toned forms, replaced by "v" BEFORE accents are stripped, so that
# "lǜ" becomes "lv" and not "lu".
U_UMLAUT = {
    "\u00fc": "v",  # ü
    "\u01d6": "v",  # ǖ
    "\u01d8": "v",  # ǘ
    "\u01da": "v",  # ǚ
    "\u01dc": "v",  # ǜ
    "\u00dc": "v",  # Ü
    "\u01d5": "v",  # Ǖ
    "\u01d7": "v",  # Ǘ
    "\u01d9": "v",  # Ǚ
    "\u01db": "v",  # Ǜ
}

# A reading with its frequency: "shàng(12308)".
READING_RE = re.compile(r"^(.+)\((\d+)\)$")
SYLLABLE_RE = re.compile(r"^[a-z]+$")


def toneless(reading):
    """Normalise one pinyin reading to a toneless ASCII syllable, or None."""
    for src, dst in U_UMLAUT.items():
        reading = reading.replace(src, dst)
    decomposed = unicodedata.normalize("NFD", reading)
    stripped = "".join(
        ch for ch in decomposed if unicodedata.combining(ch) == 0
    ).lower()
    if not stripped or not SYLLABLE_RE.match(stripped):
        return None
    return stripped


def parse_readings(path):
    """Return {syllable: {character: total weight}} from the Unihan file."""
    total = {}
    with open(path, encoding="utf-8") as f:
        for line in f:
            if line.startswith("#"):
                continue
            parts = line.rstrip("\n").split("\t")
            if len(parts) != 3 or parts[1] != "kHanyuPinlu":
                continue
            code, _field, value = parts
            if not code.startswith("U+"):
                continue
            char = chr(int(code[2:], 16))
            for token in value.split():
                match = READING_RE.match(token)
                if match is None:
                    continue
                syllable = toneless(match.group(1))
                if syllable is None:
                    continue
                weight = int(match.group(2))
                counts = total.setdefault(syllable, {})
                counts[char] = counts.get(char, 0) + weight
    return total


def parse_variants(path):
    """Return (simplified_of, traditional_of).

    `simplified_of[char]` is the simplified form of a traditional character,
    `traditional_of[char]` is the traditional form of a simplified character.
    Only single code point mappings are kept: the fields can also map to a
    sequence of characters, which cannot be substituted in a table.
    """
    simplified_of = {}
    traditional_of = {}
    with open(path, encoding="utf-8") as f:
        for line in f:
            if line.startswith("#"):
                continue
            parts = line.rstrip("\n").split("\t")
            if len(parts) != 3:
                continue
            code, field, value = parts
            if not code.startswith("U+"):
                continue
            # A value is a sequence of code points, each optionally followed by
            # a "<source" annotation. The variant list often contains the
            # character itself, which is not a variant. A character can have
            # several variants; the first one is used.
            targets = [v.split("<")[0] for v in value.split() if v.startswith("U+")]
            if code in targets:
                targets.remove(code)
            if len(targets) == 0:
                continue
            char = chr(int(code[2:], 16))
            target = chr(int(targets[0][2:], 16))
            if field == "kSimplifiedVariant":
                simplified_of.setdefault(char, target)
            elif field == "kTraditionalVariant":
                traditional_of.setdefault(char, target)
    return simplified_of, traditional_of


def render_syllables(total, simplified_of):
    """Rank the characters of every syllable by frequency.

    Characters that are the traditional form of another character come last
    among the ones that have the same frequency. """
    out = [SYLLABLES_HEADER]
    for syllable in sorted(total):
        counts = total[syllable]
        ranked = sorted(counts.items(),
            key=lambda kv: (-kv[1], kv[0] in simplified_of, ord(kv[0])))
        out.append(syllable + " " + "".join(c for c, _w in ranked[:MAX_CHARS]) + "\n")
    return "".join(out)


def render_variants(total, traditional_of):
    """The simple/traditional pairs of the characters that are in the table."""
    in_table = set(c for counts in total.values() for c in counts)
    out = [VARIANTS_HEADER]
    for char in sorted(in_table):
        traditional = traditional_of.get(char)
        if traditional is not None and traditional != char:
            out.append(char + traditional + "\n")
    return "".join(out)


def main():
    if len(sys.argv) != 4:
        sys.stderr.write(
            "usage: from_unihan.py Unihan_Readings.txt Unihan_Variants.txt OUTDIR\n")
        return 2
    readings_path, variants_path, out_dir = sys.argv[1:4]
    simplified_of, traditional_of = parse_variants(variants_path)
    total = parse_readings(readings_path)
    syllables = render_syllables(total, simplified_of)
    variants = render_variants(total, traditional_of)
    with open(os.path.join(out_dir, "syllables.txt"), "w", encoding="utf-8") as f:
        f.write(syllables)
    with open(os.path.join(out_dir, "variants.txt"), "w", encoding="utf-8") as f:
        f.write(variants)
    return 0


if __name__ == "__main__":
    sys.exit(main())
