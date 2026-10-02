#!/usr/bin/env python3
"""Build a `.dict` dictionary for `libcdict` **without the OCaml toolchain**.

`cdict-tool` (`vendor/cdict/cdict-tool`) is an OCaml program and the writer of
the `.dict` format.  This script reimplements that writer in Python so that a
dictionary can be built on a machine that has no OCaml compiler, from the same
input as `build_dict.py`.

Usage (from the repository root):

    python3 -B srcs/japanese/build_cdict.py res/raw/ja_dict.gz main.dict

The input is the UTF-8 text file produced by `build_dict.py`, optionally
gzipped, of lines:

    reading <TAB> word <TAB> cost

The output is a `.dict` file holding a single dictionary. It is named
`kanji:<number of entries>`: cdict has no way to tell how many words a
dictionary holds and the app needs the number to read them all, so the writer
writes it into the name.

Encoding of the entries
-----------------------

`libcdict` stores byte strings and, per word, only two pieces of metadata: a
4-bits frequency and an *alias* to another word index.  That is far too little
to carry a kanji *and* an exact cost, and a word is a unique byte string, so a
reading cannot have several entries whose word *is* the reading.  The
dictionary therefore stores one word per input line, as

    reading <TAB> word <TAB> cost

so that `word(index)` gives back all three fields and the caller parses the
exact cost, exactly like the bundled text format.  A TAB cannot appear inside a
column, so the word is unambiguous.  `find(reading)` is not an exact hit, but
`find(reading).prefix_ptr` is set and `suffixes(result, N)` lists the N
cheapest candidates of that reading -- including candidates of *longer*
readings that start with the same kana (reading "にほん" also yields
"にほんご"), so the caller must compare the first field for equality.

`freq(index)` is still a 0-15 bucket of the cost, with a higher value for a
cheaper (that is, more likely) candidate, so that `suffixes()` returns the best
candidate first and `distance()` ranks sane words first.  It is only a ranking
hint, the exact cost is in the word.

The format itself (all integers big-endian) is described in
`vendor/cdict/libcdict/libcdict_format.h`.  This script mirrors the OCaml
writer `vendor/cdict/ocaml-cdict/builder/cdict_builder.ml`: the same minimal
acyclic DFA (`vendor/cdict/ocaml-dfa/dfa.ml`), the same prefix compression, the
same 2-bytes alignment, the same sized integer arrays and the same "children
are written before their parent" ordering.
"""

import argparse
import gzip
import sys

# --------------------------------------------------------------------------
# Constants, from libcdict_format.h.  The struct offsets were checked by
# compiling vendor/cdict/ocaml-cdict/builder/gen/gen_constants.c.
# --------------------------------------------------------------------------

FORMAT_VERSION = 1
HEADER_MAGIC = b"Dic"

FORMAT_4_BITS = 1
FORMAT_8_BITS = 2
FORMAT_16_BITS = 4
FORMAT_24_BITS = 6

NODE_KIND_BIT_LENGTH = 1
BRANCHES_BRANCHES_FORMAT_OFFSET = NODE_KIND_BIT_LENGTH  # 1
BRANCHES_NUMBERS_FORMAT_OFFSET = 4
FORMAT_T_BIT_LENGTH = 3
ALIASES_KEYS_FORMAT_OFFSET = 0
ALIASES_VALUES_FORMAT_OFFSET = 3
PREFIX_LENGTH_OFFSET = 1
PREFIX_MAX_LENGTH = 0xFE  # c_PREFIX_MAX_LENGTH
# The header only has 7 bits left for the length, so a longer prefix would not
# survive the round trip.
PREFIX_LENGTH_LIMIT = 0x7F

PTR_FLAG_FINAL = 1
PTR_OFFSET_MASK = ~1

S_HEADER_T = 5
S_DICT_HEADER_T = 24
S_BRANCHES_T = 2
S_PREFIX_T = 4

O_DICT_NAME_OFF = 0
O_DICT_ROOT_PTR = 4
O_DICT_FREQ_OFF = 8
O_DICT_ALIASES_HEADER = 12
O_DICT_ALIASES_LENGTH = 13
O_DICT_ALIASES_KEYS = 16
O_DICT_ALIASES_VALUES = 20

TAG_BRANCHES = 0
TAG_PREFIX = 1


# --------------------------------------------------------------------------
# Size-optimised integer arrays (Sized_int_array.ml / libcdict_format.h)
# --------------------------------------------------------------------------


def format_of_integer(n_abs, signed):
    if signed:
        if n_abs <= 0x7:
            return FORMAT_4_BITS
        if n_abs <= 0x7F:
            return FORMAT_8_BITS
        if n_abs <= 0x7FFF:
            return FORMAT_16_BITS
        return FORMAT_24_BITS
    if n_abs <= 0xF:
        return FORMAT_4_BITS
    if n_abs <= 0xFF:
        return FORMAT_8_BITS
    if n_abs <= 0xFFFF:
        return FORMAT_16_BITS
    return FORMAT_24_BITS


def detect_format(values, signed):
    """Smallest format that holds every value.  Empty arrays use 8 bits."""
    if not values:
        return FORMAT_8_BITS
    mn = min(values)
    mx = max(values)
    # [-128] and [127] both fit on one byte.
    min_abs = -mn - 1 if mn < 0 else mn
    return format_of_integer(max(min_abs, mx), signed)


def pack_sized_int_array(values, signed):
    """Return (format_t, bytes) for [values]."""
    fmt = detect_format(values, signed)
    n = len(values)
    if fmt == FORMAT_4_BITS:
        out = bytearray((n + 1) // 2)
        for i, v in enumerate(values):
            v &= 0xF
            if i & 1:
                out[i >> 1] |= v << 4
            else:
                out[i >> 1] = v
        return fmt, bytes(out)
    width = {FORMAT_8_BITS: 1, FORMAT_16_BITS: 2, FORMAT_24_BITS: 3}[fmt]
    out = bytearray()
    for v in values:
        out += int(v).to_bytes(width, "big", signed=signed)
    return fmt, bytes(out)


def complete_tree_order(n):
    """Permutation of [0, n) that lays a sorted array out as the complete
    binary tree searched by libcdict (Complete_tree.of_sorted_array).  The
    result [o] means dst[i] = src[o[i]]."""
    if n == 0:
        return []

    def width_of_last_full_level(length):
        s = 1
        while True:
            s2 = s * 2 + 1
            if s2 > length:
                return (s // 2) + 1, length - s
            s = s2

    def pivot(length):
        width, last_level = width_of_last_full_level(length)
        if last_level <= width:
            return (length + last_level) // 2
        return width * 2 - 1

    order = [0] * n
    stack = [(0, n - 1, 0)]
    while stack:
        lo, hi, dsti = stack.pop()
        if lo > hi:
            continue
        mid = lo + pivot(hi - lo + 1)
        order[dsti] = mid
        stack.append((lo, mid - 1, dsti * 2 + 1))
        stack.append((mid + 1, hi, dsti * 2 + 2))
    return order


# --------------------------------------------------------------------------
# Output buffer (Buf / Writer in cdict_builder.ml)
# --------------------------------------------------------------------------


class Buf:
    """Byte buffer where every allocation is 2-bytes aligned, so that nodes
    can be written back to front."""

    def __init__(self):
        self.b = bytearray()
        self.end = 0

    def alloc(self, n):
        off = self.end if not (self.end & 1) else self.end + 1
        if len(self.b) < off + n:
            self.b.extend(b"\0" * (off + n - len(self.b)))
        self.end = off + n
        return off

    def put(self, off, data):
        self.b[off:off + len(data)] = data

    def getvalue(self):
        return bytes(self.b[:self.end])


class Writer:
    """Serialise Optimized nodes.  [nodes] maps an optimized node id to either
    ("prefix", prefix_bytes, next_id, final) or
    ("branches", [(byte, number, next_id, final), ...])."""

    def __init__(self, buf, nodes):
        self.buf = buf
        self.nodes = nodes

    @staticmethod
    def ptr_encode(node_off, final, address):
        offset = address - node_off
        assert (offset & PTR_OFFSET_MASK) == offset, offset
        return (PTR_FLAG_FINAL if final else 0) | offset

    def write_node(self, seen, final, nid):
        """Return the encoded pointer to node [nid], writing it if needed."""
        addr = seen.get(nid)
        if addr is None:
            node = self.nodes[nid]
            if node[0] == "prefix":
                addr = self._write_prefix(seen, node)
            else:
                addr = self._write_branches(seen, node)
            seen[nid] = addr
        return (final, addr)

    def _write_prefix(self, seen, node):
        _, prefix, nid, final = node
        assert 0 < len(prefix) <= PREFIX_LENGTH_LIMIT, len(prefix)
        off = self.buf.alloc(S_PREFIX_T + len(prefix))
        ptr_final, addr = self.write_node(seen, final, nid)
        ptr = self.ptr_encode(off, ptr_final, addr)
        self.buf.put(off, bytes([(len(prefix) << PREFIX_LENGTH_OFFSET)
                                 | TAG_PREFIX]))
        self.buf.put(off + 1, ptr.to_bytes(3, "big", signed=True))
        self.buf.put(off + 4, prefix)
        return off

    def _write_branches(self, seen, node):
        _, branches = node
        # [branches] is sorted by label; lay it out as the complete tree that
        # the C code searches.
        order = complete_tree_order(len(branches))
        items = [branches[i] for i in order]
        labels = bytes(item[0] for item in items)
        numbers = [item[1] for item in items]
        ptrs = [self.write_node(seen, item[3], item[2]) for item in items]
        assert len(labels) <= 0xFF
        off = self.buf.alloc(0)
        encoded = [self.ptr_encode(off, f, a) for (f, a) in ptrs]
        bfmt, bbytes = pack_sized_int_array(encoded, signed=True)
        nfmt, nbytes = pack_sized_int_array(numbers, signed=False)
        header = ((bfmt << BRANCHES_BRANCHES_FORMAT_OFFSET)
                  | (nfmt << BRANCHES_NUMBERS_FORMAT_OFFSET)
                  | TAG_BRANCHES)
        body = bytes([header, len(labels)]) + labels + bbytes + nbytes
        assert self.buf.alloc(len(body)) == off
        self.buf.put(off, body)
        return off


# --------------------------------------------------------------------------
# Minimal acyclic DFA (dfa.ml) and its optimized form (Optimized in
# cdict_builder.ml)
# --------------------------------------------------------------------------


class Dfa:
    """Words are added in byte order with the incremental algorithm of Daciuk
    et al., exactly as in vendor/cdict/ocaml-dfa/dfa.ml."""

    def __init__(self):
        self.trs = {0: []}  # id -> [(byte, next_id, final), ...]
        self._uniq = 0
        self._reg = {}  # state (tuple of transitions) -> id

    def _fresh(self):
        self._uniq += 1
        return self._uniq

    @staticmethod
    def _key(state):
        return tuple(state)

    def _common_prefix(self, word):
        i, sti = 0, 0
        while i < len(word):
            nxt = None
            for tr in self.trs[sti]:
                if tr[0] == word[i]:
                    nxt = tr[1]
                    break
            if nxt is None:
                break
            sti = nxt
            i += 1
        return i, sti

    def _replace_or_register(self, sti, st):
        """Minimise the states on the last-child chain of [st]."""
        if not st:
            return st
        last_c, last_next, last_final = st[-1]
        child = self._replace_or_register(last_next, self.trs[last_next])
        qi = self._reg.get(self._key(child))
        if qi is None:
            self._reg[self._key(child)] = last_next
            return st
        if qi == last_next:
            return st
        st = st[:-1] + [(last_c, qi, last_final)]
        del self.trs[last_next]
        self.trs[sti] = st
        return st

    def add_word(self, word):
        prefix_len, sti = self._common_prefix(word)
        st = self._replace_or_register(sti, self.trs[sti])
        suffix = word[prefix_len:]
        if not suffix:
            return  # A duplicate word.
        cur = None
        for i in range(len(suffix) - 1, -1, -1):
            sid = self._fresh()
            final = i + 1 == len(suffix)
            self.trs[sid] = [] if final else [cur]
            cur = (suffix[i], sid, final)
        self.trs[sti] = st + [cur]

    def numbers(self):
        """Assign the 'number' field of every transition: the number of words
        reachable through the previous transitions of the same state.  This is
        the perfect hashing scheme, see dfa.ml:numbers_state."""
        size = {}

        def state_size(sti):
            memo = size.get(sti)
            if memo is not None:
                return memo
            total = 0
            new = []
            for (c, nxt, final) in self.trs[sti]:
                new.append((c, nxt, total, final))
                total += state_size(nxt) + (1 if final else 0)
            self.trs[sti] = new
            size[sti] = total
            return total

        state_size(0)


def optimize(dfa):
    """Turn the DFA into Prefix and Branches nodes (Optimized.of_dfa)."""
    seen = {}  # dfa state id -> optimized node id
    nodes = {}
    counter = [0]

    def add(node):
        counter[0] += 1
        nodes[counter[0]] = node
        return counter[0]

    def fold_prefix(prefix, nxt, final):
        """Fold a chain of single-transition states into one Prefix node."""
        while not final and len(prefix) < PREFIX_MAX_LENGTH:
            st = dfa.trs[nxt]
            if len(st) != 1 or st[0][2] != 0:
                break
            c, nxt, _num, final = st[0]
            prefix += bytes([c])
        return prefix, nxt, final

    def uncached(sti):
        st = dfa.trs[sti]
        if len(st) == 1 and st[0][2] == 0:
            c, nxt, _num, final = st[0]
            prefix, nxt, final = fold_prefix(bytes([c]), nxt, final)
            return add(("prefix", prefix, resolve(nxt), final))
        branches = [(c, num, resolve(nxt), final)
                    for (c, nxt, num, final) in st]
        return add(("branches", branches))

    def resolve(sti):
        nid = seen.get(sti)
        if nid is None:
            nid = uncached(sti)
            seen[sti] = nid
        return nid

    return nodes, uncached(0)


# --------------------------------------------------------------------------
# The dictionary file
# --------------------------------------------------------------------------


def pack_freq(freqs):
    """4 bits per word, even index in the low nibble (Freq.of_int_array_raw)."""
    out = bytearray((len(freqs) + 1) // 2)
    for i, f in enumerate(freqs):
        f &= 0xF
        if i & 1:
            out[i >> 1] |= f << 4
        else:
            out[i >> 1] = f
    return bytes(out)


def quantize_costs(costs):
    """Map a Mozc cost (lower is better) to a 0-15 frequency (higher is
    better).  Equal costs get the same frequency."""
    distinct = sorted(set(costs))
    bucket = {}
    for i, c in enumerate(distinct):
        bucket[c] = 15 - (i * 16 // len(distinct))
    return [bucket[c] for c in costs]


def build(items, name):
    """[items] is a sorted list of (key bytes, cost).  Return (dict bytes,
    list of the stored keys, list of their frequencies)."""
    keys = [k for k, _ in items]
    costs = [c for _, c in items]

    dfa = Dfa()
    for key in keys:
        dfa.add_word(key)
    dfa.numbers()
    nodes, root_id = optimize(dfa)

    freqs = quantize_costs(costs)
    freq_bytes = pack_freq(freqs)
    name_bytes = name.encode("utf-8")

    buf = Buf()
    header_off = buf.alloc(S_HEADER_T + S_DICT_HEADER_T)
    assert header_off == 0
    dh = S_HEADER_T
    writer = Writer(buf, nodes)
    final, addr = writer.write_node({}, False, root_id)
    root_ptr = Writer.ptr_encode(0, final, addr)
    freq_off = buf.alloc(len(freq_bytes))
    name_off = buf.alloc(len(name_bytes) + 1)
    # No aliases.
    keys_off = buf.alloc(0)
    values_off = buf.alloc(0)
    aliases_header = ((FORMAT_8_BITS << ALIASES_KEYS_FORMAT_OFFSET)
                      | (FORMAT_8_BITS << ALIASES_VALUES_FORMAT_OFFSET))
    buf.put(freq_off, freq_bytes)
    buf.put(name_off, name_bytes + b"\0")
    buf.put(dh + O_DICT_ALIASES_HEADER, bytes([aliases_header]))
    buf.put(dh + O_DICT_ALIASES_LENGTH, (0).to_bytes(3, "big", signed=True))
    buf.put(dh + O_DICT_ALIASES_KEYS, keys_off.to_bytes(4, "big", signed=True))
    buf.put(dh + O_DICT_ALIASES_VALUES,
            values_off.to_bytes(4, "big", signed=True))
    buf.put(dh + O_DICT_NAME_OFF, name_off.to_bytes(4, "big", signed=True))
    buf.put(dh + O_DICT_ROOT_PTR, root_ptr.to_bytes(4, "big", signed=True))
    buf.put(dh + O_DICT_FREQ_OFF, freq_off.to_bytes(4, "big", signed=True))
    buf.put(0, HEADER_MAGIC)
    buf.put(3, bytes([FORMAT_VERSION, 1]))
    return buf.getvalue(), keys, freqs


def read_entries(path):
    opener = gzip.open if path.endswith(".gz") else open
    out = []
    with opener(path, "rt", encoding="utf-8") as f:
        for lineno, line in enumerate(f, 1):
            line = line.rstrip("\n")
            if not line:
                continue
            fields = line.split("\t")
            if len(fields) != 3:
                sys.exit("%s:%d: expected 3 tab-separated fields" % (path, lineno))
            reading, word, cost = fields
            if not reading or not word:
                continue
            out.append((reading, word, int(cost)))
    return out


def main():
    parser = argparse.ArgumentParser(
        description="Build a libcdict `.dict` file without OCaml.")
    parser.add_argument("input", help="reading<TAB>word<TAB>cost, .gz accepted")
    parser.add_argument("output", help="the .dict file to write")
    parser.add_argument("--name", default="kanji",
        help="dictionary name; the number of entries is appended to it after a "
             "colon, as the app reads the number from there")
    parser.add_argument("--key-mode", choices=("tab", "reading"), default="tab",
                        help="'tab' (default): the stored word is "
                             "reading<TAB>candidate<TAB>cost, all candidates "
                             "are kept and suffixes() lists them.  'reading': "
                             "the stored word is the reading and only its "
                             "cheapest candidate is kept -- the candidate and "
                             "the exact cost then cannot be read back.")
    args = parser.parse_args()

    entries = read_entries(args.input)
    # One entry per (reading, candidate); on a duplicate, the cheapest wins.
    best = {}
    for reading, word, cost in entries:
        pair = reading if args.key_mode == "reading" else reading + "\t" + word
        if pair not in best or cost < best[pair]:
            best[pair] = cost
    if not best:
        sys.exit("no entry")
    items = []
    for pair, cost in best.items():
        key = pair if args.key_mode == "reading" else "%s\t%d" % (pair, cost)
        items.append((key.encode("utf-8"), cost))
    data, keys, freqs = build(sorted(items),
        "%s:%d" % (args.name, len(items)))
    with open(args.output, "wb") as f:
        f.write(data)
    print("%s: %d entries, %d bytes, %d freq buckets used"
          % (args.output, len(keys), len(data), len(set(freqs))))


if __name__ == "__main__":
    main()
