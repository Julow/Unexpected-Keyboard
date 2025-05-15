import textwrap, sys, re, string, json, os, string
from array import array

# Compile compose sequences from Xorg's format or from JSON files into an
# efficient state machine.
# See [ComposeKey.java] for the interpreter.
#
# Takes input files as arguments and generate a Java file.
# The initial state for each input is generated as a constant named after the
# input file.

# Parse symbol names from keysymdef.h. Many compose sequences in
# en_US_UTF_8_Compose.pre reference theses. For example, all the sequences on
# the Greek, Cyrillic and Hebrew scripts need these symbols.
def parse_keysymdef_h(fname):
    with open(fname, "r") as inp:
        keysym_re = re.compile(r'^#define XK_(\S+)\s+\S+\s*/\*.U\+([0-9a-fA-F]+)\s')
        for line in inp:
            m = re.match(keysym_re, line)
            if m != None:
                yield (m.group(1), chr(int(m.group(2), 16)))

dropped_sequences = 0
warning_count = 0

# [s] is a list of strings
def seq_to_str(s, result=None):
    msg = "+".join(s)
    return msg if result is None else msg + " = " + result

# Print a warning. If [seq] is passed, it is prepended to the message.
def warn(msg, seq=None, result=None):
    global warning_count
    if seq is not None:
        msg = f"Sequence {seq_to_str(seq, result=result)} {msg}"
    print(f"Warning: {msg}", file=sys.stderr)
    warning_count += 1

# Parse XKB's Compose.pre files
def parse_sequences_file_xkb(fname, xkb_char_extra_names):
    # Parse a line of the form:
    #     <Multi_key> <minus> <space>		: "~"	asciitilde # TILDE
    # Sequences not starting with <Multi_key> are ignored.
    line_re = re.compile(r'^((?:\s*<[^>]+>)+)\s*:\s*"((?:[^"\\]+|\\.)+)"\s*(\S+)?\s*(?:#.+)?$')
    char_re = re.compile(r'\s*<(?:U([a-fA-F0-9]{4,6})|([^>]+))>')
    def parse_seq_line(line):
        global dropped_sequences
        prefix = "<Multi_key>"
        if not line.startswith(prefix):
            return None
        m = re.match(line_re, line[len(prefix):])
        if m == None:
            return None
        def_ = m.group(1)
        try:
            def_ = parse_seq_chars(def_)
            result = parse_seq_result(m.group(2))
        except Exception as e:
            # print(str(e) + ". Sequence dropped: " + line.strip(), file=sys.stderr)
            dropped_sequences += 1
            return None
        return def_, result
    char_names = { **xkb_char_extra_names }
    # Interpret character names of the form "U0000" or using [char_names].
    def parse_seq_char(sc):
        uchar, named_char = sc
        if uchar != "":
            c = chr(int(uchar, 16))
        elif len(named_char) == 1:
            c = named_char
        else:
            if not named_char in char_names:
                raise Exception("Unknown char: " + named_char)
            c = char_names[named_char]
        # The state machine can't represent sequence characters that do not fit
        # in a 16-bit char.
        if len(c) > 1 or ord(c[0]) > 65535:
            raise Exception("Char out of range: " + r)
        return c
    # Interpret the left hand side of a sequence.
    def parse_seq_chars(def_):
        return list(map(parse_seq_char, re.findall(char_re, def_)))
    # Interpret the result of a sequence, as outputed by [line_re].
    def parse_seq_result(r):
        if len(r) == 2 and r[0] == '\\':
            return r[1]
        return r
    # Populate [char_names] with the information present in the file.
    with open(fname, "r") as inp:
        for line in inp:
            m = re.match(line_re, line)
            if m == None or m.group(3) == None:
                continue
            try:
                char_names[m.group(3)] = parse_seq_result(m.group(2))
            except Exception:
                pass
    # Parse the sequences
    with open(fname, "r") as inp:
        seqs = []
        for line in inp:
            s = parse_seq_line(line)
            if s != None:
                seqs.append(s)
        return seqs

# Basic support for comments in json files. Reads a file
def strip_cstyle_comments(inp):
    def strip_line(line):
        i = line.find("//")
        return line[:i] + "\n" if i >= 0 else line
    return "".join(map(strip_line, inp))

# Parse from a json file containing a dictionary sequence → result string.
def parse_sequences_file_json(fname):
    def tree_to_seqs(tree, prefix):
        for c, r in tree.items():
            if isinstance(r, str):
                yield prefix + [c], r
            else:
                yield from tree_to_seqs(r, prefix + [c])
    try:
        with open(fname, "r") as inp:
            tree = json.loads(strip_cstyle_comments(inp))
        return list(tree_to_seqs(tree, []))
    except Exception as e:
        warn("Failed parsing '%s': %s" % (fname, str(e)))

# Format of the sequences file is determined by its extension
def parse_sequences_file(fname, xkb_char_extra_names={}):
    if fname.endswith(".pre"):
        return parse_sequences_file_xkb(fname, xkb_char_extra_names)
    if fname.endswith(".json"):
        return parse_sequences_file_json(fname)
    raise Exception(fname + ": Unsupported format")

# A sequence directory can contain several sequence files as well as
# 'keysymdef.h'.
def parse_sequences_dir(dname):
    compose_files = []
    xkb_char_extra_names = {}
    # Parse keysymdef.h first if present
    for fbasename in os.listdir(dname):
        fname = os.path.join(dname, fbasename)
        if fbasename == "keysymdef.h":
            xkb_char_extra_names = dict(parse_keysymdef_h(fname))
        else:
            compose_files.append(fname)
    sequences = []
    for fname in compose_files:
        sequences.extend(parse_sequences_file(fname, xkb_char_extra_names))
    return sequences

# Turn a list of sequences into a trie.
def add_sequences_to_trie(seqs, trie):
    global dropped_sequences
    def add_seq_to_trie(seq, result):
        t_ = trie
        for c in seq[:-1]:
            t_ = t_.setdefault(c, {})
            if isinstance(t_, str):
                return False
        c = seq[-1]
        if c in t_:
            return False
        t_[c] = result
        return True
    def existing_sequence_to_str(seq): # Used in error message
        i = 0
        t_ = trie
        while i < len(seq):
            if seq[i] not in t_: break # No collision ?
            t_ = t_[seq[i]]
            i += 1
            if isinstance(t_, str): break
        return "".join(seq[:i]) + " = " + str(t_)
    for seq, result in seqs:
        if not add_seq_to_trie(seq, result):
            dropped_sequences += 1
            warn("Sequence collide: '%s' and '%s = %s'" % (
                existing_sequence_to_str(seq),
                "".join(seq), result))

# Compile the trie into a state machine.
def make_automata(tries):
    previous_leafs = {} # Deduplicate leafs
    states = []
    def add_tree(t):
        this_node_index = len(states)
        # Index and size of the new node
        i = len(states)
        s = len(t.keys())
        # Add node header
        states.append(("\0", s + 1))
        i += 1
        # Reserve space for the current node in both arrays
        for c in range(s):
            states.append((None, None))
        # Add nested nodes and fill the current node
        for c in sorted(t.keys()):
            states[i] = (c, add_node(t[c]))
            i += 1
        return this_node_index
    def add_leaf(c):
        if c in previous_leafs:
            return previous_leafs[c]
        this_node_index = len(states)
        previous_leafs[c] = this_node_index
        # There are two encoding for leafs: character final state for 15-bit
        # characters and string final state for the rest.
        if len(c) > 1 or ord(c[0]) > 32767: # String final state
            # A ':' can be added to the result of a sequence to force a string
            # final state. For example, to go through KeyValue lookup.
            if c.startswith(":"): c = c[1:]
            javachars = array('H', c.encode("UTF-16-LE"))
            states.append((-1, len(javachars) + 1))
            for c in javachars:
                states.append((c, 0))
        else: # Character final state
            states.append((c, 1))
        return this_node_index
    def add_node(n):
        if type(n) == str:
            return add_leaf(n)
        else:
            return add_tree(n)
    states.append((1, 1)) # Add an empty state at the beginning.
    entry_states = { n: add_tree(root) for n, root in tries.items() }
    return entry_states, states

# Debug
def print_automata(automata):
    i = 0
    for (s, e) in automata:
        s = "%#06x" % s if isinstance(s, int) else '"%s"' % str(s)
        print("%3d %8s %d" % (i, s, e), file=sys.stderr)
        i += 1

# Report warnings about the compose sequences
def check_for_warnings(tries):
    def get(seq):
        t = tries
        for c in seq:
            if c not in t:
                return None
            t = t[c]
        return t if type(t) == str else None
    # Check that compose+Upper+Upper have an equivalent compose+Upper+Lower or compose+Lower+Lower
    for c1 in string.ascii_uppercase:
        for c2 in string.ascii_uppercase:
            seq = [c1, c2]
            seq_l = [c1, c2.lower()]
            seq_ll = [c1.lower(), c2.lower()]
            r = get(seq)
            r_l = get(seq_l)
            r_ll = get(seq_ll)
            if r is not None:
                ll_warning = f" (but {seq_to_str(seq_ll)} = {r_ll} exists)" if r_ll is not None else ""
                if r_l is None:
                    if r != r_ll:
                        warn(f"has no lower case equivalent {seq_to_str(seq_l)}{ll_warning}", seq=seq, result=r)
                elif r != r_l:
                    warn(f"is not the same as {seq_to_str(seq_l)} = {r_l}{ll_warning}", seq=seq, result=r)

def batched(ar, n):
    i = 0
    while i + n < len(ar):
        yield ar[i:i+n]
        i += n
    if i < len(ar):
        yield ar[i:]

# Print the state machine compiled by make_automata into java code that can be
# used by [ComposeKeyData.java].
def gen_java(entry_states, machine):
    chars_map = {
            # These characters cannot be used in unicode form as Java's parser
            # unescape unicode sequences before parsing.
            -1: "\\uFFFF",
            "\"": "\\\"",
            "\\": "\\\\",
            "\n": "\\n",
            "\r": "\\r",
            ord("\""): "\\\"",
            ord("\\"): "\\\\",
            ord("\n"): "\\n",
            ord("\r"): "\\r",
            }
    def char_repr(c):
        if c in chars_map:
            return chars_map[c]
        if type(c) == int: # The edges array contains ints
            return "\\u%04x" % c
        if c in string.printable:
            return c
        return "\\u%04x" % ord(c)
    def gen_array(array):
        chars = list(map(char_repr, array))
        return "\" +\n    \"".join(map(lambda b: "".join(b), batched(chars, 72)))
    def gen_entry_state(s):
        name, state = s
        return "  public static final int %s = %d;" % (name, state)
    print("""package juloo.keyboard2;

/** This file is generated, see [srcs/compose/compile.py]. */

public final class ComposeKeyData
{
  public static final char[] states =
    ("%s").toCharArray();

  public static final char[] edges =
    ("%s").toCharArray();

%s
}""" % (
    # Break the edges array every few characters using string concatenation.
    gen_array(map(lambda s: s[0], machine)),
    gen_array(map(lambda s: s[1], machine)),
    "\n".join(map(gen_entry_state, entry_states.items())),
))

total_sequences = 0
tries = {} # Orderred dict
for fname in sorted(sys.argv[1:]):
    tname, _ = os.path.splitext(os.path.basename(fname))
    if os.path.isdir(fname):
        sequences = parse_sequences_dir(fname)
    else:
        sequences = parse_sequences_file(fname)
    add_sequences_to_trie(sequences, tries.setdefault(tname, {}))
    total_sequences += len(sequences)

check_for_warnings(tries["compose"])
entry_states, automata = make_automata(tries)
gen_java(entry_states, automata)

print("Compiled %d sequences into %d states. Dropped %d sequences. Generated %d warnings." % (total_sequences, len(automata), dropped_sequences, warning_count), file=sys.stderr)
# print_automata(automata)
