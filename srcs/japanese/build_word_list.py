#!/usr/bin/env python3
"""Prepare the input of `cdict-tool` from the entries of `build_dict.py`.

`cdict-tool` (in the `vendor/cdict` submodule) is the writer of the `.dict`
format and the only program that writes it. This script does not write a
dictionary: it converts the entries to an AOSP word list, the format
`cdict-tool build` parses when the file name ends in `.combined`, and prints
the command that turns it into a `.dict` file.

Usage (from the repository root):

    python3 -B srcs/japanese/build_dict.py DIR_WITH_MOZC_DICTIONARIES
    python3 -B srcs/japanese/build_word_list.py build/ja_dict.txt.gz build/ja_word_list.combined

The input is the UTF-8 text file produced by `build_dict.py`, optionally
gzipped, of lines:

    reading <TAB> word <TAB> cost

The output holds one `word=` line per entry and a `dictionary=` header:

    dictionary=kanji
    word=ほん	本	3000,f=9

The stored word is the whole `reading <TAB> word <TAB> cost` line and the
frequency, after `,f=`, is a 0-15 bucket of the cost.

Why the entries are whole lines
-------------------------------

`libcdict` stores, per word, only a 4 bits frequency and an alias to another
word, which is not enough for a kanji and an exact cost, and a word is a unique
byte string, so a reading cannot have several entries whose word *is* the
reading. Each entry is therefore stored as a whole word, `reading <TAB> word
<TAB> cost`, and the caller splits it, exactly like the bundled text format. A
TAB cannot appear inside a column, so the word is unambiguous. AOSP word lists
cannot carry a comma inside a word, which is why an entry that would is
rejected.

The frequency is what orders `suffixes()`; the conversion itself reads the
exact cost from the third field. Equal costs get the same frequency: the
distinct costs of the whole dictionary are sorted and spread over 0-15, so that
a higher frequency means a cheaper (that is, more likely) candidate.

The number of entries is written into the name of the dictionary, after a
`kanji_` prefix, because `cdict` has no way to tell how many words a dictionary
holds and the app needs the number to read them all. `cdict-tool` takes the
name on its command line, in the `NAME:PATH` argument, and splits that argument
on the first colon, so the name cannot contain a colon: the count is separated
with an underscore. This script prints that argument with the right count.
"""

import argparse
import gzip
import sys


def read_entries(path):
    """Read `reading<TAB>word<TAB>cost` lines, optionally gzipped."""
    opener = gzip.open if path.endswith(".gz") else open
    out = []
    with opener(path, "rt", encoding="utf-8") as f:
        for lineno, line in enumerate(f, 1):
            line = line.rstrip("\n")
            if not line:
                continue
            fields = line.split("\t")
            if len(fields) != 3:
                sys.exit("%s:%d: expected 3 tab-separated fields"
                         % (path, lineno))
            reading, word, cost = fields
            if not reading or not word:
                continue
            out.append((reading, word, int(cost)))
    return out


def quantize_costs(costs):
    """Map a Mozc cost (lower is better) to a 0-15 frequency (higher is
    better).  Equal costs get the same frequency."""
    distinct = sorted(set(costs))
    bucket = {}
    for i, c in enumerate(distinct):
        bucket[c] = 15 - (i * 16 // len(distinct))
    return bucket


def build_entries(entries):
    """Deduplicate the entries, keeping the cheapest cost of a
    (reading, word) pair, and return the stored words, sorted, with their
    frequency.  The stored word is `reading<TAB>word<TAB>cost`."""
    costs = {}
    for reading, word, cost in entries:
        pair = reading + "\t" + word
        if pair not in costs or cost < costs[pair]:
            costs[pair] = cost
    if not costs:
        sys.exit("no entry")
    if any("," in pair for pair in costs):
        sys.exit("an entry contains a comma, which an AOSP word list cannot "
                 "carry")
    bucket = quantize_costs(costs.values())
    words = []
    for pair in sorted(costs):
        cost = costs[pair]
        words.append((pair + "\t" + str(cost), bucket[cost]))
    return words


def main():
    parser = argparse.ArgumentParser(
        description="Convert entries to the word list cdict-tool reads.")
    parser.add_argument("input", help="reading<TAB>word<TAB>cost, .gz accepted")
    parser.add_argument("output", help="the .combined word list to write")
    parser.add_argument("--name", default="kanji",
        help="name of the dictionary; the number of entries is appended to it "
             "after an underscore, as the app reads the number from there")
    args = parser.parse_args()

    if not args.output.endswith(".combined"):
        sys.exit("the output name must end in .combined, the extension that "
                 "makes cdict-tool parse an AOSP word list")

    words = build_entries(read_entries(args.input))
    with open(args.output, "w", encoding="utf-8") as f:
        f.write("dictionary=%s\n" % args.name)
        for word, freq in words:
            f.write("word=%s,f=%d\n" % (word, freq))
    print("%s: %d entries" % (args.output, len(words)))
    print("cdict-tool build -o OUT.dict \"%s_%d:%s\""
          % (args.name, len(words), args.output))


if __name__ == "__main__":
    main()
