import textwrap, sys

def parse_sequences_file(fname):
    with open(fname, "r") as inp:
        return [ (s[:-2], s[-2]) for s in inp if len(s) > 1 ]

# Turn a list of sequences into a trie.
def add_sequences_to_trie(seqs, trie):
    for seq, result in seqs:
        t_ = trie
        i = 0
        while i < len(seq) - 1:
            c = seq[i]
            if c not in t_:
                t_[c] = {}
            t_ = t_[c]
            i += 1
        c = seq[i]
        t_[c] = result

# Compile the trie into a state machine.
def make_automata(tree_root):
    states = []
    def add_tree(t):
        # Index and size of the new node
        i = len(states)
        s = len(t.keys())
        # Add node header
        states.append((0, s + 1))
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
        states.append((c, 1))
    def add_node(n):
        if type(n) == str:
            add_leaf(n)
        else:
            add_tree(n)
    add_tree(tree_root)
    return states

# Print the state machine compiled by make_automata into java code that can be
# used by [ComposeKeyData.java].
def gen_java(machine):
    def gen_array(array, indent):
        return textwrap.fill(", ".join(map(str, array)), subsequent_indent=indent)
    print("""package juloo.keyboard2;

/** This file is generated, see [srcs/compose/compile.py]. */

public final class ComposeKeyData
{
  public static final char[] states = {
    %s
  };

  public static final short[] edges = {
    %s
  };
}""" % (
    gen_array(map(lambda s: repr(s[0]), machine), '    '),
    gen_array(map(lambda s: s[1], machine), '    '),
))

total_sequences = 0
trie = {}
for fname in sys.argv[1:]:
    sequences = parse_sequences_file(fname)
    add_sequences_to_trie(sequences, trie)
    total_sequences += len(sequences)
gen_java(make_automata(trie))
print("Compiled %d sequences" % total_sequences, file=sys.stderr)
