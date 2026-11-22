#!/usr/bin/env python3
"""Generate a word context file ('.bigrams') from a word list and a text
corpus. The keyboard uses it to rank suggestions according to the previous
word, for example to prefer "és" over "es" after "acció" in Catalan.

Usage:
  gen_bigrams.py [options] WORDLIST.combined OUT.bigrams CORPUS...

WORDLIST.combined is the AOSP word list that the dictionary was built from.
Only the words it contains are counted. CORPUS files are plain text (or gzip
compressed text), in any number. A corpus can be given a weight by appending
':WEIGHT' to its name, for example 'subtitles.txt.gz:10'. This is useful to
balance a small corpus of informal text against a large encyclopedic one.

Requires 'numpy'.

File format (all integers are big endian):
  magic          4 bytes "UKBG"
  version        u8, equal to 1
  n              u32, number of words
  backoff        u8, cost added to the unigram cost of a word that is not
                 listed after a context word
  word_offsets   u32[n + 1], offsets into [words]
  words          UTF-8 encoded words, sorted by byte value, not separated
  unigram_costs  u8[n]
  entry_offsets  u32[n + 1], index of the first entry of each context word
  entries        u32[entry_offsets[n]], (word index << 8 | cost), sorted by
                 word index for each context word

Costs are -log2(probability) in quarters of bits, from 0 to 255. Every word
of the vocabulary can be a context word.
"""

import argparse
import gzip
import math
import re
import struct
import sys
from multiprocessing import Pool

import numpy as np

# A word: letters, possibly joined by a middle dot or an apostrophe. Digits,
# hyphens and other symbols separate words.
WORD_RE = re.compile(r"[^\W\d_]+(?:[·'][^\W\d_]+)*")
# Phrase boundaries: the previous word is not a useful context after these.
BREAK_RE = re.compile(r"[.!?;:,()\[\]{}\"«»“”…—–/|]")
# Elided articles and prepositions are not part of the next word.
ELISION_RE = re.compile(r"^(?:[ldsmnt]|qu)'", re.IGNORECASE)

COST_UNIT = 4  # Costs are in quarters of bits
MAX_COST = 255


def cost_of_prob(p):
    if p <= 0:
        return MAX_COST
    return max(0, min(MAX_COST - 1, round(-math.log2(p) * COST_UNIT)))


def read_wordlist(path):
    words = []
    with open(path, encoding="utf-8") as f:
        for line in f:
            m = re.match(r"^ word=([^,]+),", line)
            if m:
                words.append(m.group(1))
    return words


# Set in worker processes.
_lookup = None


def init_worker(lookup):
    global _lookup
    _lookup = lookup


def word_ids(phrase):
    ids = []
    for m in WORD_RE.finditer(phrase):
        w = ELISION_RE.sub("", m.group(0))
        i = _lookup.get(w)
        if i is None:
            i = _lookup.get(w.lower(), -1)
        ids.append(i)
    return ids


def count_chunk(lines):
    """Return (unigram ids, pair keys) as numpy arrays, pairs encoded as
    [context * 2^32 + word]."""
    uni = []
    pairs = []
    for line in lines:
        line = line.replace("’", "'")
        for phrase in BREAK_RE.split(line):
            ids = word_ids(phrase)
            uni.extend(i for i in ids if i >= 0)
            for a, b in zip(ids, ids[1:]):
                if a >= 0 and b >= 0:
                    pairs.append((a << 32) | b)
    u, uc = np.unique(np.array(uni, dtype=np.int64), return_counts=True)
    p, pc = np.unique(np.array(pairs, dtype=np.int64), return_counts=True)
    return u, uc, p, pc


def read_chunks(path, size=20000):
    opener = gzip.open if path.endswith(".gz") else open
    with opener(path, "rt", encoding="utf-8", errors="replace") as f:
        chunk = []
        for line in f:
            chunk.append(line)
            if len(chunk) >= size:
                yield chunk
                chunk = []
        if chunk:
            yield chunk


def merge(keys, counts):
    k = np.concatenate(keys)
    c = np.concatenate(counts)
    uk, inv = np.unique(k, return_inverse=True)
    return uk, np.bincount(inv, weights=c)


def count_corpus(path, weight, lookup, n, jobs):
    uni = np.zeros(n, dtype=np.float64)
    pk, pc = [], []
    with Pool(jobs, initializer=init_worker, initargs=(lookup,)) as pool:
        for i, (u, uc, p, c) in enumerate(
                pool.imap_unordered(count_chunk, read_chunks(path))):
            uni[u] += uc * weight
            pk.append(p)
            pc.append(c.astype(np.float64) * weight)
            if len(pk) >= 64:
                k, c = merge(pk, pc)
                pk, pc = [k], [c]
            if i % 100 == 0:
                print("%s: %d chunks" % (path, i), file=sys.stderr)
    k, c = merge(pk, pc) if pk else (np.zeros(0, np.int64), np.zeros(0))
    return uni, k, c


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("wordlist")
    ap.add_argument("output")
    ap.add_argument("corpus", nargs="+")
    ap.add_argument("--min-count", type=float, default=3,
                    help="Minimum (weighted) count of a pair of words.")
    ap.add_argument("--min-lift", type=float, default=1.5,
                    help="Keep a pair only if the word is that many times"
                    " more likely after the context word than in general."
                    " Other pairs are approximated by the backoff.")
    ap.add_argument("--max-per-context", type=int, default=400)
    ap.add_argument("--backoff", type=float, default=0.4,
                    help="Probability factor applied to unlisted pairs.")
    ap.add_argument("-j", "--jobs", type=int, default=None)
    args = ap.parse_args()

    # Sort by UTF-8 bytes, which is the order used for binary search.
    words = sorted(set(read_wordlist(args.wordlist)),
                   key=lambda w: w.encode("utf-8"))
    n = len(words)
    index = {w: i for i, w in enumerate(words)}
    # Lowercase forms map to the lowercase word if it exists, otherwise to the
    # capitalized word ("barcelona" -> "Barcelona").
    lookup = dict(index)
    for w, i in index.items():
        lookup.setdefault(w.lower(), i)

    uni = np.zeros(n, dtype=np.float64)
    pk, pc = [], []
    for spec in args.corpus:
        path, _, weight = spec.partition(":")
        u, k, c = count_corpus(path, float(weight or 1), lookup, n, args.jobs)
        uni += u
        pk.append(k)
        pc.append(c)
    keys, counts = merge(pk, pc)

    total = uni.sum()
    uni_p = uni / total
    unigram_costs = bytes(cost_of_prob(p) for p in uni_p)

    ctx = (keys >> 32).astype(np.int64)
    wrd = (keys & 0xFFFFFFFF).astype(np.int64)
    # P(w|c) is relative to the occurrences of c followed by a known word.
    ctx_total = np.bincount(ctx, weights=counts, minlength=n)
    p_cond = counts / ctx_total[ctx]
    lift = p_cond / np.maximum(uni_p[wrd], 1e-12)
    keep = (counts >= args.min_count) & (lift >= args.min_lift)
    ctx, wrd, counts, p_cond = ctx[keep], wrd[keep], counts[keep], p_cond[keep]

    # Keep the most likely words for each context.
    order = np.lexsort((-p_cond, ctx))
    ctx, wrd, p_cond = ctx[order], wrd[order], p_cond[order]
    starts = np.searchsorted(ctx, np.arange(n + 1))
    entry_offsets = [0]
    entries = []
    for c in range(n):
        s, e = starts[c], min(starts[c + 1], starts[c] + args.max_per_context)
        sel = sorted(zip(wrd[s:e].tolist(), p_cond[s:e].tolist()))
        entries.extend((w << 8) | cost_of_prob(p) for w, p in sel)
        entry_offsets.append(len(entries))

    blob = b"".join(w.encode("utf-8") for w in words)
    word_offsets = [0]
    for w in words:
        word_offsets.append(word_offsets[-1] + len(w.encode("utf-8")))

    with open(args.output, "wb") as f:
        f.write(b"UKBG")
        f.write(struct.pack(">BIB", 1, n,
                            round(-math.log2(args.backoff) * COST_UNIT)))
        f.write(struct.pack(">%dI" % (n + 1), *word_offsets))
        f.write(blob)
        f.write(unigram_costs)
        f.write(struct.pack(">%dI" % (n + 1), *entry_offsets))
        f.write(struct.pack(">%dI" % len(entries), *entries))
    print("%d words, %d pairs, %.0f tokens" % (n, len(entries), total),
          file=sys.stderr)


if __name__ == "__main__":
    main()
