# Kana to kanji dictionary

The Japanese input methods convert kana to kanji with a dictionary that is
**installed on demand**, like the spell checking dictionaries: it is not part of
the APK. It is published with the other dictionaries, listed in
`res/values/dictionaries.xml`, and read through `juloo.keyboard2.dict`
`Dictionaries`, which hands its entries to `KanjiDictionary`.

## Pipeline

    # 1. entries, derived from the open source Mozc dictionary (not in this repo)
    python3 -B srcs/japanese/build_dict.py DIR_WITH_MOZC_DICTIONARIES
    #    -> build/ja_dict.txt.gz, 49966 entries, 481 KB

    # 2. package them as the file the app downloads
    python3 -B srcs/japanese/build_cdict.py build/ja_dict.txt.gz ja.dict
    #    -> ja.dict, 1483828 bytes

    gzip -9 ja.dict   # the dictionaries are stored gzipped, as v1/ja.dict

Step 1 reads the `dictionary00.txt` ... `dictionary09.txt` files of
https://github.com/google/mozc/tree/master/src/data/dictionary_oss, which are
not part of this repository:

    mkdir -p /tmp/mozc
    for i in 00 01 02 03 04 05 06 07 08 09; do
      curl -Lo /tmp/mozc/d$i.txt \
        https://raw.githubusercontent.com/google/mozc/master/src/data/dictionary_oss/dictionary$i.txt
    done

Mozc spells the readings in katakana; `build_dict.py` converts them to
hiragana. Entries whose cost is above 4500 are left out: Mozc's cost has a long
tail of proper nouns, rare compounds and technical terms, and the cut keeps
about 50,000 entries covering the everyday vocabulary.

The result is published at

    https://raw.githubusercontent.com/Julow/Unexpected-Keyboard-dictionaries/refs/heads/main/v1/ja.dict

and its size, 842960 bytes gzipped, is the one written in
`res/values/dictionaries.xml` for the `ja` dictionary.

## Why the entries are cdict words of their own

The app's dictionaries are `cdict` files, written by the OCaml `cdict-tool`.
That tool is not available on every machine (there is none in the environment
this was written in), so `build_cdict.py` reimplements the writer in Python; its
output was checked against the C `libcdict` the app links, which reads every one
of the 49966 entries back with no mismatch.

libcdict stores, per word, only a 4 bit frequency and an alias to another word,
which is not enough for a kanji and an exact cost. Each entry is therefore
stored as a whole word, `reading <TAB> word <TAB> cost`, and the caller splits
it. The frequency is the cost in 16 buckets, which is what orders `suffixes()`;
the conversion itself reads the exact cost from the third field.

The dictionary is named `kanji:<number of entries>`. cdict exposes no way to
count the words of a dictionary, and `cdict_suffixes` allocates its result on
the stack, so it cannot be asked for every word either: the number is written
into the name and the app reads it from there.

## Licence

The dictionary is a derived work of the open source Mozc dictionary, which is
itself mostly IPAdic. Both are permissive but require their notices to be
carried along:

- Mozc: Copyright 2010-2024 Google LLC, BSD 3-Clause.
  https://github.com/google/mozc/blob/master/LICENSE
- IPAdic (mecab-ipadic-2.7.0-20070801): Copyright 2000, 2001, 2002, 2003 Nara
  Institute of Science and Technology. All Rights Reserved. Use, reproduction,
  and distribution of this software is permitted. Any copy of this software,
  whether in its original form or modified, must include both the above
  copyright notice and the following paragraphs:

  > This software is provided 'as is' and without any express or implied
  > warranties, including, but not limited to, the implied warranties of
  > merchantability and fitness for a particular purpose. In no event shall
  > Nara Institute of Science and Technology be liable for any damages
  > whatsoever arising from the use of this software.

- Okinawa dictionary, used by Mozc to enrich named entities:
  https://github.com/google/mozc/blob/master/src/data/dictionary_oss/README.txt

Only the `word` and `cost` columns of the Mozc dictionary are kept, and only for
the entries below the cost cut above.
