# Chinese word dictionary

The Chinese input methods (Pinyin and Zhuyin) suggest words with a dictionary
that is **installed on demand**, like the spell checking dictionaries: it is
not part of the APK. It is published with the other dictionaries, listed in
`res/values/dictionaries.xml` and read through `juloo.keyboard2.dict`
`Dictionaries`, which looks up the dictionary named `main`.

## Pipeline

    # 1. the tone of every character, next to the other tables
    #    (see srcs/pinyin/README.md for the Unihan download)
    python3 -B srcs/pinyin/from_unihan.py Unihan_Readings.txt Unihan_Variants.txt srcs/pinyin
    #    -> srcs/pinyin/tones.txt, 3799 characters

    # 2. the words, from the jieba word segmenter (not in this repo)
    python3 -B srcs/chinese/build_dict.py srcs/pinyin/tones.txt build/zh_words.combined
    #    -> build/zh_words.combined, 349045 words, 269021 of them with tones,
    #       7746639 bytes

    # 3. package them as the file the app downloads
    vendor/cdict/_build/default/cdict-tool/main.exe build \
      -o build/zh.dict main:build/zh_words.combined
    #    -> build/zh.dict, 4281146 bytes

    gzip -9 -c build/zh.dict > zh.dict.gz   # the dictionaries are stored gzipped
    #    -> zh.dict.gz, 3046233 bytes

Step 1 and step 3 are the same as for the character tables and the other
languages. Step 1 adds `tones.txt` to the tables built by
`srcs/pinyin/from_unihan.py`: one line per character with the tone of its
heaviest reading, derived from the same `kHanyuPinlu` field. Step 3 is
`cdict-tool`, built from the `vendor/cdict` submodule. It compresses the
frequencies to 4 bits with a k-medians clustering, like for the other
languages.

Step 2 reads `jieba/dict.txt` from https://github.com/fxsjy/jieba, which is not
part of this repository. It holds one entry per line, `word frequency
part-of-speech`; only the word and its frequency are kept, and a duplicate word
keeps its highest frequency. The output is the `.combined` word list format, the
same one the dictionaries of the other languages are built from.

## The tone of a word, and the rubytext it shows

`libcdict` stores, per word, only a frequency and an alias to another word, so a
word cannot carry its reading beside it. The tones are therefore stored *inside*
the word, after a tab, one digit per character, in the order of the syllables -
the same way the Japanese dictionary stores a reading, a word and a cost:

    word=中国	12,f=129470          (zhōng guó: 1 then 2)
    word=中国人民政治协商会议	1222442144,f=616

A word that has a character whose tone is not known is stored without the tab,
so it is still suggested but carries no reading. 269021 of the 349045 words have
one (77%).

A word is looked up by its first characters, which is what the dictionary is
queried with once a composition is committed, and the caller splits the tab off
before showing the word. **The rubytext shown for a multi-syllable word is a
single mark: the tone of the first character the suggestion adds after the text
the user has typed.** Reasons:

- Every suggestion of that row starts with the text the user has typed, so the
  part they have in common says nothing about how to choose between them; the
  first character that differs is the one the user is about to gain, which is
  what the tone explains.
- One mark always fits a cell, whatever the width; one mark per syllable does
  not, as a ten-character word would need ten.
- It is the same rule as the composing row, where the annotation is the tone of
  the syllable the candidate corrects.
- When the suggestion adds nothing, its whole text is already there and the last
  syllable's tone is shown instead.

The asset stores every syllable's tone, not just the one that is displayed: the
display rule can be changed without rebuilding it.

A character read with several tones keeps the tone of the reading it is the most
frequent with, so the tone of a character inside a word is an approximation:
a word that uses a character with its less common reading shows that character's
common tone.

## Why an ordinary dictionary

An ordinary cdict dictionary holds one word per entry, with a frequency, and
the app looks up the dictionary named `main` for the dictionary of the
current layout. It is the same shape as the dictionaries built from the AOSP
word lists for the other languages; only the tone field inside the word is
specific to Chinese.

## Licence

The dictionary is a derived work of two sources:

- the `jieba` dictionary, of the word and frequency columns of `dict.txt`:
  Copyright (c) 2013 Sun Junyi, MIT.
  https://github.com/fxsjy/jieba/blob/master/LICENSE
- the Unicode Unihan database, of the tone of every character, through
  `srcs/pinyin/tones.txt`: Unicode License v3.
  https://www.unicode.org/license.txt

The result is published at

    https://raw.githubusercontent.com/Julow/Unexpected-Keyboard-dictionaries/refs/heads/main/v1/zh.dict

as 3046233 bytes gzipped, the size written in `res/values/dictionaries.xml` for
the `zh` dictionary. The uncompressed file is 4281146 bytes, with sha256
`b026b4ea3aae8b8f238775beb8cded9bda02c7873fe8afb7bff352b0c5a22ea4`; the gzipped
file has sha256
`069a21b5cdf8d02439c1e9a2628a03aeccfba1ecbb99ba0b451f529121c26c27`.

Those hashes are a snapshot, not a reproducible value: `cdict-tool build` is not
deterministic. Two runs of the same tool on the same word list, both at
`c69e3a1`, produced different files (measured 2026-10-14: `6bb9e747...` and
`65c129cf...` from `build/det_a.dict` and `build/det_b.dict`). The *order* it
gives is stable, which is what the suggestions depend on, and so are the
frequencies of every word sampled; only the bytes are not.

Where the variation is *not*, measured rather than assumed:

- not the frequency partitioning: after `31259d2` it is a pure function of the
  frequency array, `int_of_float (log (1. +. f) *. 15. /. max_log)`, with no
  seed, no initialisation and nothing iterated;
- not `Random`, which no module of the library or the tool mentions (only
  `tests/ocaml/freq.ml` does), and not the `Hashtbl.fold` of the count path,
  which is the newline-separated one and whose order is the same across runs
  here anyway;
- not visible with a small input: forty words built three times are identical,
  which is consistent with the Japanese word list being stable over three runs.

What the two `zh` runs do show is a *structural* difference: the same total size
(4281146 bytes) with the first differing byte at 74, and different node and
branch counts in `cdict-tool stats`. The variation is therefore in the dictionary
encoding rather than in the frequencies, and the frequencies and their order are
what the app consumes.
