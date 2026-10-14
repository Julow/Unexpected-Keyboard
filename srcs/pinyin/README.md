# Chinese character tables

These tables hold the characters that the Pinyin and Zhuyin input methods offer
(see `srcs/juloo.keyboard2/staged/`).

## Files

- `syllables.txt` — one line per toneless Hanyu Pinyin syllable: the syllable, a
  space, then the characters written with that syllable, most frequent first (at
  most 16 per syllable). Characters that are the traditional form of another
  character come after it when both have the same frequency, so that the table
  reads naturally for Simplified Chinese.
- `variants.txt` — one line per character: a simplified character followed by
  its traditional variant. The Zhuyin input method uses it to offer the
  traditional characters.
- `from_unihan.py` — builds the two files above from the Unicode Unihan
  database.
- `compile.py` — generates `srcs/juloo.keyboard2/staged/PinyinData.java`,
  including the Zhuyin spelling of every syllable, which it computes.

## Regenerating

The generated Java file only depends on the two committed tables and is checked
by CI:

    python3 -B srcs/pinyin/compile.py

Rebuilding the tables needs the Unihan database, which is not part of the
repository:

    curl -O https://www.unicode.org/Public/UCD/latest/ucd/Unihan.zip
    unzip Unihan.zip Unihan_Readings.txt Unihan_Variants.txt
    python3 -B srcs/pinyin/from_unihan.py Unihan_Readings.txt Unihan_Variants.txt srcs/pinyin

Only the `kHanyuPinlu` (readings with a frequency, from the *Hanyu Pinlu
Cidian*) and `kTraditionalVariant` fields are used. Readings are made toneless
by replacing the `ü` family with `v` and then stripping combining marks, so
e.g. `lǜ` becomes `lv`. Tones are not typed: the tables are ordered by the
frequency of every reading and the candidates are offered in that order.

Unicode data files are distributed under the Unicode License v3
(https://www.unicode.org/license.txt). The tables are a derived, reduced subset
of that data.
