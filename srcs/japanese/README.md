# Japanese dictionaries

The Japanese input methods suggest words and convert kana to kanji with a
dictionary that is **installed on demand**, like the spell checking
dictionaries: it is not part of the APK. It is published with the other
dictionaries, listed in `res/values/dictionaries.xml`, and read through
`juloo.keyboard2.dict` `Dictionaries`.

One file holds two `cdict` dictionaries:

- `main`, the word list the app suggests words from, the dictionary of the
  current layout;
- `kanji_<number of entries>`, the kana to kanji conversion table that
  `Dictionaries` hands to `KanjiDictionary`.

Both are built with `cdict-tool` and packed in the same file, so the app
downloads one file per language, like for the other languages.

## Pipeline

    # 1. the conversion entries, derived from the open source Mozc dictionary
    python3 -B srcs/japanese/build_dict.py DIR_WITH_MOZC_DICTIONARIES
    #    -> build/ja_dict.txt.gz, 49966 entries, 481 KB

    # 2. the conversion word list that cdict-tool reads
    python3 -B srcs/japanese/build_word_list.py \
      build/ja_dict.txt.gz build/ja_word_list.combined
    #    -> build/ja_word_list.combined, 49966 entries

    # 3. the word list, from the FrequencyWords corpus (not in this repo)
    curl -Lo ja_full.txt \
      https://raw.githubusercontent.com/hermitdave/FrequencyWords/master/content/2018/ja/ja_full.txt
    python3 -B srcs/japanese/build_words.py ja_full.txt
    #    -> build/ja_words.combined, 33563 words, 626122 bytes

    # 4. package both dictionaries in the file the app downloads
    (cd vendor/cdict && dune build -p dfa,cdict,cdict-tool)
    vendor/cdict/_build/install/default/bin/cdict-tool build -o build/ja.dict \
      "main:build/ja_words.combined" \
      "kanji_49966:build/ja_word_list.combined"
    #    -> ja.dict, 1764980 bytes

    gzip -9 -n build/ja.dict   # the dictionaries are stored gzipped, as v1/ja.dict
    #    -> ja.dict.gz, 1049325 bytes

The file is built with the `vendor/cdict` submodule at `c69e3a1` or later. The
bump added `31259d2` "Logarithmic partitioning" (Julow's PR #10), which changes
how the 4 bit frequency is partitioned without changing the format: before it,
the frequent words all landed in the top bucket and `suffixes()` ordered them
arbitrarily. Rebuilt with the bump, 日本 (count 1429) comes out at 9, 中国 (834)
at 8 and 東京 (378) at 7, where all three used to be 15.

Steps 1 and 2 read the `dictionary00.txt` ... `dictionary09.txt` files of
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

Step 3 reads `content/2018/ja/ja_full.txt` of
https://github.com/hermitdave/FrequencyWords, which is not part of this
repository. It holds the most frequent tokens of the OpenSubtitles2018 corpus,
as `word SPACE count`, and it is a snapshot: the file used to build the
published dictionary has SHA-256
`581dd106a134ff9f2706e23cde9fe95cd24d96ead6357cd3bd3bba9e166a764e`.
`build_words.py` keeps a token only when every character is a kana the layouts
write or a kanji the conversion can produce, which leaves out the punctuation,
the Latin words, the numbers and the tokens that mix scripts. The count is the
frequency `cdict-tool` stores.

The file belongs at

    https://raw.githubusercontent.com/Julow/Unexpected-Keyboard-dictionaries/refs/heads/main/v1/ja.dict

and its size, 1049325 bytes gzipped, is the one written in
`res/values/dictionaries.xml` for the `ja` dictionary. The uncompressed file is
1764980 bytes, with SHA-256
`515e84bda403e0ab00d620427968fb33d62220509cf1bbac66c5b5b2005aeec3`; the
gzipped file has SHA-256
`ffa21d2bfed2f68712cd437134f33b65ad32d81fc2f4aa5059eb6c7fb7f66010`. Until
the file is published in that repository, `DictionaryListView` falls back to
the pre-release copy on the fork, `ja-dict-v1`; remove that entry once
`v1/ja.dict` exists.

## The two dictionaries

### `main`, the word list

`Dictionaries` sets `config.current_dictionary` to the dictionary named `main`,
and that is the one `Suggestions` queries for the word being typed: `find()`
for an exact match, `suffixes()` for the words that start with it and
`distance()` for the near misses. Japanese therefore gets word suggestions once
a composition is committed, between characters, and whenever the kana composer
has no candidate to show, which is the case of a katakana word. While the kana
composer has candidates, the row belongs to it.

The words are the everyday vocabulary of the corpus, in the script the corpus
writes them in: compound kanji words such as `日本橋`, kana-and-kanji words,
and the katakana loanwords. A query that matches nothing simply shows no
suggestion, like for the other languages.

### `kanji_<count>`, the conversion table

libcdict stores, per word, only a 4 bit frequency and an alias to another word,
which is not enough for a kanji and an exact cost. Each entry is therefore
stored as a whole word, `reading <TAB> word <TAB> cost`, and the caller splits
it. The frequency is the cost in 16 buckets, which is what orders `suffixes()`;
the conversion itself reads the exact cost from the third field. `cdict-tool`
compresses the frequencies to 4 bits with k-medians, which keeps that order.

The dictionary is named `kanji_<number of entries>`. cdict exposes no way to
count the words of a dictionary, and `cdict_suffixes` allocates its result on
the stack, so it cannot be asked for every word either: the number is written
into the name and the app reads it from there. The separator is an underscore
and not a colon because `cdict-tool` splits its `NAME:PATH` argument on the
first colon.

Both word lists are prepared by a script that writes the AOSP format
(`word=<word>,f=<frequency>`) and nothing else; only `cdict-tool`, from the
`vendor/cdict` submodule, writes the `.dict` file.

## Licence

The file holds two derived works, each with its own notice.

### The conversion table

The conversion table is a derived work of the open source Mozc dictionary,
which is itself mostly IPAdic. Both are permissive but require their notices to
be carried along:

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

### The word list

The word list is the Japanese content of
[FrequencyWords](https://github.com/hermitdave/FrequencyWords), which its
repository licences as "MIT License for code. CC-by-sa-4.0 for content". The
content is the word lists, so this part is a derived work of CC-BY-SA-4.0
material:

- FrequencyWords, Copyright (c) 2016 Hermit Dave, CC-BY-SA-4.0.
  https://github.com/hermitdave/FrequencyWords
- The corpus it was generated from, OpenSubtitles2018, distributed by OPUS.
  http://opus.nlpl.eu/OpenSubtitles2018.php

Only the words that the layouts write are kept, and only their counts are used,
to rank them.
