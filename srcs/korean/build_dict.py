#!/usr/bin/env python3
"""Build the Korean word list from the FrequencyWords corpus.

Usage (from the repository root):

    python3 -B srcs/korean/build_dict.py ko_50k.txt [OUT]

`ko_50k.txt` is the Korean frequency list of
https://github.com/hermitdave/FrequencyWords, a file of lines:

    word SPACE count

It is not part of this repository; see README.md for how to fetch it and for
its licence.

The output, `build/ko_wordlist.combined` by default, is an AOSP word list, the
format the other dictionaries of
https://github.com/Julow/Unexpected-Keyboard-dictionaries are built from and
the one `cdict-tool build` reads:

    dictionary=main:ko
     word=<word>,f=<count>

`f` is the count of the corpus. `cdict-tool` reduces it to the 16 frequencies
that the dictionary format stores, with the most frequent words getting the
highest values, and the app orders the candidates of a word by that frequency.

Only the words that the 두벌식 (dubeolsik) layout can type are kept: the
layout writes precomposed Hangul syllables, so a word is kept only when all of
its characters are syllables (U+AC00..U+D7A3). That leaves out the numbers,
the Latin words and the punctuation that the corpus also counts, and it drops
the few entries that mix a syllable with a word joiner or a jamo.
"""

import gzip
import os
import sys

# The Hangul Syllables block. The layout composes these characters.
SYLLABLES = (0xAC00, 0xD7A3)


def is_hangul_syllable(c):
    return SYLLABLES[0] <= ord(c) <= SYLLABLES[1]


def read_counts(path):
    """Return {word: count} over the words the layout can type."""
    best = {}
    opener = gzip.open if path.endswith(".gz") else open
    with opener(path, "rt", encoding="utf-8") as f:
        for lineno, line in enumerate(f, 1):
            fields = line.rstrip("\n").split(" ")
            if len(fields) != 2:
                sys.exit("%s:%d: expected 'word count'" % (path, lineno))
            word, count = fields
            if not word or not all(is_hangul_syllable(c) for c in word):
                continue
            count = int(count)
            if word not in best or count > best[word]:
                best[word] = count
    return best


def main():
    if len(sys.argv) < 2 or len(sys.argv) > 3:
        sys.exit("usage: build_dict.py KO_50K_TXT [OUT]")
    counts = read_counts(sys.argv[1])
    out_path = sys.argv[2] if len(sys.argv) > 2 else os.path.join("build",
        "ko_wordlist.combined")
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as out:
        out.write("dictionary=main:ko,description=Korean,locale=ko\n")
        for word in sorted(counts):
            out.write(" word=%s,f=%d\n" % (word, counts[word]))
    print("%d words, %s is %d bytes"
          % (len(counts), out_path, os.path.getsize(out_path)))


if __name__ == "__main__":
    main()
