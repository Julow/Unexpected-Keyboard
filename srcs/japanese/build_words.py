#!/usr/bin/env python3
"""Build the Japanese word list from the FrequencyWords corpus.

Usage (from the repository root):

    python3 -B srcs/japanese/build_words.py ja_full.txt [OUT]

`ja_full.txt` is the Japanese frequency list of
https://github.com/hermitdave/FrequencyWords, a file of lines:

    word SPACE count

It is not part of this repository; see README.md for how to fetch it and for
its licence.

The output, `build/ja_words.combined` by default, is an AOSP word list, the
format `cdict-tool build` reads:

    dictionary=main
    word=<word>,f=<count>

`f` is the count of the corpus. `cdict-tool` reduces it to the 16 frequencies
that the dictionary format stores, with the most frequent words getting the
highest values, and the app orders the suggestions of a word by that
frequency.

Only the words that the Japanese layouts write are kept: the kana of the
layouts and the kanji that the conversion produces. The punctuation, the
Latin words, the numbers and the tokens that mix scripts are left out, as are
the entries that carry a character the `.combined` format cannot hold.
"""

import gzip
import os
import sys


def is_japanese(c):
    """The kana of the layouts, the kanji of the conversion, and the
    iteration marks that are part of a word such as 人々 or 〆切."""
    o = ord(c)
    return (
        (0x3041 <= o <= 0x3096)         # hiragana
        or (0x30A1 <= o <= 0x30FA)      # katakana
        or o == 0x30FC                  # the prolonged sound mark
        or o == 0x309D or o == 0x309E   # the hiragana iteration marks
        or o == 0x30FD or o == 0x30FE   # the katakana iteration marks
        or o == 0x3005 or o == 0x3006   # 々 and 〆
        or (0x4E00 <= o <= 0x9FFF)      # CJK unified ideographs
        or (0x3400 <= o <= 0x4DBF)      # CJK extension A
        or (0xF900 <= o <= 0xFAFF))     # CJK compatibility ideographs


def is_japanese_word(word):
    return all(is_japanese(c) for c in word)


def read_counts(path):
    """Return {word: count} over the words of the corpus."""
    best = {}
    opener = gzip.open if path.endswith(".gz") else open
    with opener(path, "rt", encoding="utf-8") as f:
        for lineno, line in enumerate(f, 1):
            fields = line.rstrip("\n").split(" ")
            if len(fields) != 2:
                sys.exit("%s:%d: expected 'word count'" % (path, lineno))
            word, count = fields
            if not word or not is_japanese_word(word):
                continue
            count = int(count)
            if word not in best or count > best[word]:
                best[word] = count
    return best


def main():
    if len(sys.argv) < 2 or len(sys.argv) > 3:
        sys.exit("usage: build_words.py JA_FULL_TXT [OUT]")
    counts = read_counts(sys.argv[1])
    # The `.combined` format ends a word at a comma, so a word cannot hold one.
    bad = [w for w in counts if "," in w]
    if bad:
        sys.exit("words that the .combined format cannot hold: %s" % bad[:5])
    out_path = sys.argv[2] if len(sys.argv) > 2 else os.path.join("build",
        "ja_words.combined")
    parent = os.path.dirname(out_path)
    if parent:
        os.makedirs(parent, exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as out:
        out.write("dictionary=main\n")
        for word in sorted(counts):
            out.write("word=%s,f=%d\n" % (word, counts[word]))
    print("%d words, %s is %d bytes"
          % (len(counts), out_path, os.path.getsize(out_path)))


if __name__ == "__main__":
    main()
