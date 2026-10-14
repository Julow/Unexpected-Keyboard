# Korean word list

Korean is typed with a 두벌식 (dubeolsik) layout whose keys are jamo, and the
composer turns them into syllables. A word list is still needed to suggest and
autocorrect the words that are being typed: it is **installed on demand**, like
the spell checking dictionaries, not part of the APK. It is published with the
other dictionaries, listed in `res/values/dictionaries.xml` as the `ko`
dictionary, and read through `juloo.keyboard2.dict` `Dictionaries`, which hands
it to the suggestion code as a `cdict` file.

## Pipeline

    # 1. words and counts, from the FrequencyWords Korean list (not in this repo)
    python3 -B srcs/korean/build_dict.py ko_50k.txt
    #    -> build/ko_wordlist.combined, 49313 words, 1082503 bytes

    # 2. package them as the file the app downloads, with the cdict writer
    vendor/cdict/cdict-tool build -o ko.dict "main:build/ko_wordlist.combined"
    #    -> ko.dict, 305386 bytes

    gzip -9 -n ko.dict   # the dictionaries are stored gzipped, as v1/ko.dict
    #    -> ko.dict.gz, 221823 bytes

`ko_50k.txt` is the Korean list of
https://github.com/hermitdave/FrequencyWords, which is not part of this
repository:

    curl -Lo ko_50k.txt \
      https://raw.githubusercontent.com/hermitdave/FrequencyWords/master/content/2018/ko/ko_50k.txt

It holds the 50,000 most frequent tokens of the OpenSubtitles2018 corpus, as
`word SPACE count`, and it is a snapshot: the file that was used to build the
published dictionary has SHA-256
`8d00401b6728c8d6feeaa5455583c1a33408dd020aae4aa55609b25004d1d99d`.

The layout writes precomposed Hangul syllables, so `build_dict.py` keeps a
token only when all of its characters are syllables (U+AC00..U+D7A3). That
drops 687 of the 50000 tokens: the numbers, the Latin words, the punctuation
and a few entries that mix a syllable with a word joiner or with a jamo that no
key of the layout produces.

The dictionary is published as

    https://raw.githubusercontent.com/Julow/Unexpected-Keyboard-dictionaries/refs/heads/main/v1/ko.dict

and its size, 221823 bytes gzipped (305386 bytes raw), is the one written in
`res/values/dictionaries.xml` for the `ko` dictionary. The published file has
SHA-256
`b51934e4865ab9af6798f236a1f7f273365de2bb72bffa7a43820557771c285f` gzipped and
`cd602131dee3da0c110303aacdaa9841556966a5a0fe10d22f681293d6286901` raw; GitHub
reports the digest of the uploaded asset as the first of those, which is what
ties the published file to these numbers and to the size the app declares.

`cdict-tool` is **not byte-reproducible**: measured at `c69e3a1`, two runs on the
same word list produced different bytes, a few tens of bytes apart in size. The
SHA-256 values above therefore identify the published file, but a rebuild will
not reproduce it, and two rebuilds will not match each other. What is stable is
the *order* the tool assigns, which is what the suggestions use; a rebuild is
checked with `query` on a few words (the frequencies and their order), never by
bytes. The size in `res/values/dictionaries.xml` is the published file's.

Until it is copied there, the file is carried by the `ko-dict-v1` pre-release
of the fork, with the word list `build_dict.py` produces:

    https://github.com/bqv/Unexpected-Keyboard/releases/download/ko-dict-v1/ko.dict.gz
    https://github.com/bqv/Unexpected-Keyboard/releases/download/ko-dict-v1/ko_wordlist.combined.gz

## How the words are stored

The word list is built with `cdict-tool`, the writer of the `cdict` format, so
that the dictionary uses `libcdict` end to end like the other dictionaries of
the app. `cdict-tool` is built from the `vendor/cdict` submodule, which is also
what the app links:

    (cd vendor/cdict && dune build -p dfa,cdict,cdict-tool)

`vendor/cdict` is pinned at `c69e3a1` ("Logarithmic partitioning"), which spreads
the frequencies instead of putting every frequent word in the same bucket; the
dictionary above is built with the tool from that commit. Because the tool is not
byte-reproducible (see above), the pin and the file are tied together by the
`query` order rather than by a matching hash.

`build_dict.py` writes the AOSP word list that `cdict-tool build` reads, with
one `word=` line per word and `f=` set to its count in the corpus. The writer
reduces that count to the 16 frequencies the format stores, and the app lists
the words that start with what is being typed, the most frequent first.

The file holds a single dictionary, named `main`, the name the app looks up for
word suggestions. The words are the Hangul syllables of the corpus, which is
what the layout writes.

## Emoji

No `emoji` dictionary is built. The dictionaries repository has emoji word
lists for English, French, Russian and Ukrainian only, and the Korean one it
holds (`emoji_cldr_signal_wordlists/emoji_ko.combined`) has broken entries, a
few of its `word=` lines carrying unbalanced quotes. The emoji dictionary can
be added later, once that list is fixed; the file can also simply hold the two
dictionaries `main` and `emoji`, as the other languages do.

## Licence

The word list is the Korean content of
[FrequencyWords](https://github.com/hermitdave/FrequencyWords), which its
repository licences as "MIT License for code. CC-by-sa-4.0 for content." The
content is the word lists, so this dictionary is a derived work of
CC-BY-SA-4.0 material:

- FrequencyWords, Copyright (c) 2016 Hermit Dave, CC-BY-SA-4.0.
  https://github.com/hermitdave/FrequencyWords
- The corpus it was generated from, OpenSubtitles2018, distributed by OPUS.
  http://opus.nlpl.eu/OpenSubtitles2018.php

Only the words that the layout can type are kept, and only their counts are
used, to rank them.
