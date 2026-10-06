# Create a substitution file for cdict from the various compose mappings.
# This is used when building the dictionaries and when making word suggestions
# disregarding case and diacritics.

import sys, os, json, glob, unicodedata

OUTPUT_FILE = "srcs/compose/substitutions.json"

def warn(msg):
    print("Warning: " + msg, file=sys.stderr)

# From srcs/compose/compile.py
def strip_cstyle_comments(inp):
    def strip_line(line):
        i = line.find("//")
        return line[:i] + "\n" if i >= 0 else line
    return "".join(map(strip_line, inp))

def parse(fname):
    with open(fname, "r") as inp:
        return json.loads(strip_cstyle_comments(inp))

def is_char16(c):
    return len(c) == 1 and ord(c) < 65536

def get_mappings(tree):
    for c, r in tree.items():
        # Remove deep compose sequences and remove mappings to non-char keys or
        # to characters that do not fit in a Java 16-bit char.
        if isinstance(r, str) and is_char16(r) and is_char16(c):
            yield c, r

def mappings_from_compose_files():
    for f in glob.glob("srcs/compose/*.json"):
        if f == OUTPUT_FILE:
            continue
        yield from get_mappings(parse(f))

# The definition of shift doesn't contain any letters as shift is implemented
# using Java's API so we generate it using Python's API. It's not important if
# both are not equivalent.
def add_case_variants(mappings):
    for c in "abcdefghijklmnopqrstuvwxyz":
        yield c, c.upper()
    for c, r in mappings:
        c_low = c.lower()
        if c_low != c and is_char16(c_low): yield c_low, r
        r_up = r.upper()
        if r_up != r and is_char16(r_up): yield c, r_up
        yield c, r

# Remove unecessary characters to reduce the lookup time
ALLOWED_CAT = [ "Ll", "Lu", "Lt", "Lo" ]
def remove_non_letters(mappings):
    for c, r in mappings:
        cat = unicodedata.category(c)
        if cat in ALLOWED_CAT:
            yield c, r

def resolve_mappings(mappings):
    m = {}
    # Sort mappings to keep the lowest char in case of a conflict
    for c, r in sorted(mappings, key=lambda it: it[1]):
        if r in m:
            if m[r] != c:
                warn("Conflicting mapping '%s -> %s' and '%s -> %s'" %
                      (c, r, m[r], r))
            continue
        m[r] = c
    def resolve(c, trace=None):
        if c in m:
            if trace is None:
                trace = set()
            elif c in trace:
                return c
            trace.add(c)
            return resolve(m[c], trace=trace)
        return c
    return { r: resolve(c) for r, c in m.items() }


with open(OUTPUT_FILE, "w") as out:
    json.dump(
            resolve_mappings(
                add_case_variants(
                    remove_non_letters(
                        mappings_from_compose_files()))),
              out, ensure_ascii=False, indent=2)

print("Generated " + OUTPUT_FILE)
