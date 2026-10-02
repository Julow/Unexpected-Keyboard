#!/usr/bin/env python3
"""Build `res/raw/ja_dict.gz`, the dictionary that converts kana to kanji.

Usage (from the repository root):

    python3 -B srcs/japanese/build_dict.py DIR_WITH_MOZC_DICTIONARIES [OUT]

`DIR_WITH_MOZC_DICTIONARIES` must contain the `dictionary00.txt` ...
`dictionary09.txt` files of the open source Mozc dictionary
(https://github.com/google/mozc/tree/master/src/data/dictionary_oss). They are
not part of this repository; see README.md for how to fetch them.

The output, `build/ja_dict.txt.gz` by default, is a gzipped text file, sorted
by reading, of:

    reading <TAB> word <TAB> cost

where `reading` is hiragana, `word` is what the reading is converted to and
`cost` is the Mozc cost, a lower value meaning a more likely word.

Only the entries whose cost is at most [MAX_COST] are kept. Mozc's cost of a
word is roughly the negative log of its frequency, and the distribution has a
long tail: the cut keeps the everyday vocabulary (about 50,000 entries, 1.5 MB)
and leaves out the proper nouns, rare compounds and technical terms that make up
the other million entries.
"""

import glob
import gzip
import os
import sys

# Entries above this cost are left out. Measured against a list of everyday
# words, 4500 is where the last of them (コンピュータ, 天気, 新聞) enters.
MAX_COST = 4500

# Hiragana block. Mozc spells the readings in katakana, the keyboard types
# hiragana.
HIRAGANA = (0x3041, 0x3096)
KATAKANA = (0x30A1, 0x30F6)
PROLONGED_SOUND_MARK = "\u30fc"


def to_hiragana(reading):
    """Mozc spells readings in katakana, the keyboard types hiragana."""
    return "".join(
        chr(ord(c) - 0x60) if KATAKANA[0] <= ord(c) <= KATAKANA[1] else c
        for c in reading)


def is_hiragana(reading):
    return all(HIRAGANA[0] <= ord(c) <= HIRAGANA[1]
               or c == PROLONGED_SOUND_MARK for c in reading)


def read_entries(directory):
    """Return {(reading, word): cost} over every Mozc dictionary file."""
    best = {}
    files = sorted(glob.glob(os.path.join(directory, "dictionary*.txt")))
    if not files:
        # The files are also distributed split as d00.txt ... d09.txt.
        files = sorted(glob.glob(os.path.join(directory, "d[0-9][0-9].txt")))
    if not files:
        sys.exit("No dictionary*.txt file in " + directory)
    for path in files:
        with open(path, encoding="utf-8") as f:
            for line in f:
                fields = line.rstrip("\n").split("\t")
                if len(fields) != 5:
                    continue
                reading = to_hiragana(fields[0])
                cost = int(fields[3])
                word = fields[4]
                if not reading or not word or not is_hiragana(reading):
                    continue
                key = (reading, word)
                if key not in best or cost < best[key]:
                    best[key] = cost
    return best


def main():
    if len(sys.argv) < 2 or len(sys.argv) > 3:
        sys.exit("usage: build_dict.py DIR_WITH_MOZC_DICTIONARIES [OUT]")
    entries = read_entries(sys.argv[1])
    kept = sorted((r, w, c) for (r, w), c in entries.items() if c <= MAX_COST)
    out_path = sys.argv[2] if len(sys.argv) > 2 else os.path.join("build",
        "ja_dict.txt.gz")
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    with gzip.GzipFile(out_path, "wb", mtime=0) as out:
        for reading, word, cost in kept:
            out.write(("%s\t%s\t%d\n" % (reading, word, cost)).encode("utf-8"))
    print("%d entries kept out of %d, %s is %.2f MB"
          % (len(kept), len(entries), out_path,
             os.path.getsize(out_path) / 1e6))


if __name__ == "__main__":
    main()
