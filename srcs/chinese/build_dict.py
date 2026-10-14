#!/usr/bin/env python3
"""Write the AOSP word list that `cdict-tool` compiles into `zh.dict`.

The words come from the dictionary of the `jieba` Chinese word segmenter
(https://github.com/fxsjy/jieba), which is distributed under the MIT licence.
Its `dict.txt` holds one entry per line, as `word frequency part-of-speech`.
The part-of-speech is dropped and a duplicate word keeps its highest
frequency.

The tones come from `srcs/pinyin/tones.txt`, which `srcs/pinyin/from_unihan.py`
builds from the Unicode Unihan database; both licences are stated in
`README.md`.

`libcdict` stores, per word, only a frequency and an alias to another word, so
a word cannot carry the tone of its syllables beside it. The tones are stored
*inside* the word, after a tab, one digit per syllable, and the caller splits
them off, the same way the Japanese dictionary stores a reading, a word and a
cost:

    word=<word> <TAB> <one tone digit per character>,f=<frequency>

A word that has a character without a tone recorded is stored without the tab,
so it still suggests but carries no reading.

The output is the `.combined` word list format that `cdict-tool` parses for the
dictionaries built from the AOSP word lists: a `dictionary=` line, then one
`word=` line per word with its frequency in the `f` field.

Usage (from the repository root):

    python3 -B srcs/chinese/build_dict.py srcs/pinyin/tones.txt build/zh_words.combined
    vendor/cdict/_build/default/cdict-tool/main.exe build \
      -o build/zh.dict main:build/zh_words.combined

The word list is not part of this repository.
"""

import sys
import urllib.request

URL = "https://raw.githubusercontent.com/fxsjy/jieba/master/jieba/dict.txt"


def read_tones(path):
    """Return {character: tone} from the file that `from_unihan.py` writes."""
    tones = {}
    with open(path, encoding="utf-8") as f:
        for line in f:
            if line.startswith("#"):
                continue
            fields = line.split()
            if len(fields) != 2:
                continue
            tones[fields[0]] = fields[1]
    return tones


def word_tones(word, tones):
    """The tone of every character of [word], a digit per character, or [None]
       when a character has no tone recorded."""
    digits = [tones.get(char) for char in word]
    if any(digit is None for digit in digits):
        return None
    return "".join(digits)


def main():
    if len(sys.argv) != 3:
        sys.exit("usage: build_dict.py TONES OUTPUT")
    words = {}
    with urllib.request.urlopen(URL) as f:
        for lineno, line in enumerate(f, 1):
            fields = line.decode("utf-8").split()
            if not fields:
                continue
            if len(fields) < 2:
                sys.exit("line %d: expected at least 2 fields" % lineno)
            word, freq = fields[0], fields[1]
            if not word:
                continue
            freq = int(freq)
            if words.get(word, -1) < freq:
                words[word] = freq
    # The `.combined` format ends a word at a comma and separates the fields of
    # a word with a tab, so a word cannot hold either.
    bad = [w for w in words if "," in w or "\n" in w or "\t" in w]
    if bad:
        sys.exit("words that the .combined format cannot hold: %s" % bad[:5])
    tones = read_tones(sys.argv[1])
    toned = 0
    with open(sys.argv[2], "w", encoding="utf-8") as f:
        f.write("dictionary=main\n")
        # The order of the lines does not decide the order of the words in the
        # dictionary: cdict-tool puts them in an order of its own. Measured on
        # 2026-10-14: a word list sorted by decreasing frequency and one sorted
        # by word gave byte-identical dictionaries, and the same words came back
        # in the same order. Only the frequency stored per word orders the
        # results, and it is the coarse 4 bits of the format.
        for word in sorted(words):
            digits = word_tones(word, tones)
            if digits is None:
                f.write("word=%s,f=%d\n" % (word, words[word]))
            else:
                f.write("word=%s\t%s,f=%d\n" % (word, digits, words[word]))
                toned += 1
    print("%s: %d words, %d of them with tones" % (sys.argv[2], len(words), toned))


if __name__ == "__main__":
    main()
