import textwrap, sys, re, string, json, os

# Parse symbol names from keysymdef.h. Many compose sequences in
# en_US_UTF_8_Compose.pre reference theses. For example, all the sequences on
# the Greek, Cyrillic and Hebrew scripts need these symbols.
def parse_keysymdef_h():
    with open(os.path.join(os.path.dirname(__file__), "keysymdef.h"), "r") as inp:
        keysym_re = re.compile(r'^#define XK_(\S+)\s+\S+\s*/\*.U\+([0-9a-fA-F]+)\s')
        for line in inp:
            m = re.match(keysym_re, line)
            if m != None:
                yield (m.group(1), chr(int(m.group(2), 16)))

xkb_char_extra_names = dict(parse_keysymdef_h())

dropped_sequences = 0

# Parse XKB's Compose.pre files
def parse_sequences_file_xkb(fname):
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
    def parse_seq_char(c):
        uchar, named_char = c
        if uchar != "":
            return chr(int(uchar, 16))
        # else is a named char
        if len(named_char) == 1:
            return named_char
        if not named_char in char_names:
            raise Exception("Unknown char: " + named_char)
        return char_names[named_char]
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

# Parse from a json file containing a dictionary sequence → result string.
def parse_sequences_file_json(fname):
    with open(fname, "r") as inp:
        seqs = json.load(inp)
    return list(seqs.items())

# Format of the sequences file is determined by its extension
def parse_sequences_file(fname):
    if fname.endswith(".pre"):
        return parse_sequences_file_xkb(fname)
    if fname.endswith(".json"):
        return parse_sequences_file_json(fname)
    raise Exception(fname + ": Unsupported format")

# Turn a list of sequences into a trie.
def add_sequences_to_trie(seqs, trie):
    def add_seq_to_trie(t_, seq, result):
        t_ = trie
        i = 0
        while i < len(seq) - 1:
            c = seq[i]
            if c not in t_:
                t_[c] = {}
            if isinstance(t_[c], str):
                global dropped_sequences
                dropped_sequences += 1
                print("Sequence collide: '%s = %s' '%s = %s'" % (
                    seq[:i+1], t_[c], seq, result),
                      file=sys.stderr)
                return
            t_ = t_[c]
            i += 1
        c = seq[i]
        t_[c] = result
    for seq, result in seqs:
        add_seq_to_trie(trie, seq, result)

# Compile the trie into a state machine.
def make_automata(tree_root):
    states = []
    def add_tree(t):
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
            node_i = len(states)
            add_node(t[c])
            states[i] = (c, node_i)
            i += 1
    def add_leaf(c):
        # There are two encoding for leafs: character final state for 15-bit
        # characters and string final state for the rest.
        if len(c) > 1 or ord(c[0]) > 32767: # String final state
            cb = c.encode("UTF-16")
            states.append((-1, len(cb) + 1))
            for c in cb:
                states.append((c, 0))
        else: # Character final state
            states.append((c, 1))
    def add_node(n):
        if type(n) == str:
            add_leaf(n)
        else:
            add_tree(n)
    add_tree(tree_root)
    return states

def batched(ar, n):
    i = 0
    while i + n < len(ar):
        yield ar[i:i+n]
        i += n
    if i < len(ar):
        yield ar[i:]

# Print the state machine compiled by make_automata into java code that can be
# used by [ComposeKeyData.java].
def gen_java(machine):
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
    print("""package juloo.keyboard2;

/** This file is generated, see [srcs/compose/compile.py]. */

public final class ComposeKeyData
{
  public static final char[] states =
    ("%s").toCharArray();

  public static final char[] edges =
    ("%s").toCharArray();
}""" % (
    # Break the edges array every few characters using string concatenation.
    gen_array(map(lambda s: s[0], machine)),
    gen_array(map(lambda s: s[1], machine)),
))

total_sequences = 0
trie = {}
for fname in sys.argv[1:]:
    sequences = parse_sequences_file(fname)
    add_sequences_to_trie(sequences, trie)
    total_sequences += len(sequences)
automata = make_automata(trie)
gen_java(automata)
print("Compiled %d sequences into %d states. Dropped %d sequences." % (total_sequences, len(automata), dropped_sequences), file=sys.stderr)
