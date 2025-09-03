import xml.etree.ElementTree as ET
import sys, os, glob, re

layout_file_name = 0
warnings = []

KNOWN_NOT_LAYOUT = set([
    "number_row", "numpad", "pin",
    "bottom_row", "settings", "method",
    "greekmath", "numeric", "emoji_bottom_row",
    "clipboard_bottom_row" ])

KEY_ATTRIBUTES = set([
    "key0", "key1", "key2", "key3", "key4", "key5", "key6", "key7", "key8",
    "c", "nw", "ne", "sw", "se", "w", "e", "n", "s"
    ])

# Keys defined in KeyValue.java
known_keys = set()

def warn(msg):
    global warnings
    warnings.append("%s: %s" % (layout_file_name, msg))

def key_list_str(keys):
    return ", ".join(sorted(list(keys)))

def missing_some_of(keys, symbols, class_name=None):
    if class_name is None:
        class_name = "of [" + ", ".join(symbols) + "]"
    missing = set(symbols).difference(keys)
    if len(missing) > 0 and len(missing) != len(symbols):
        warn("Layout includes some %s but not all, missing: %s" % (
            class_name, key_list_str(missing)))

def missing_required(keys, symbols, msg):
    missing = set(symbols).difference(keys)
    if len(missing) > 0:
        warn("%s, missing: %s" % (msg, key_list_str(missing)))

def unexpected_keys(keys, symbols, msg):
    unexpected = set(symbols).intersection(keys)
    if len(unexpected) > 0:
        warn("%s, unexpected: %s" % (msg, key_list_str(unexpected)))

# Write to [keys] and [dup].
def parse_row_from_et(row, keys, dup):
    for key in row:
        for attr in key.keys():
            if attr in KEY_ATTRIBUTES:
                k = key.get(attr).removeprefix("\\")
                if k in keys: dup.add(k)
                keys.add(k)

def parse_layout(fname):
    keys = set()
    dup = set()
    root = ET.parse(fname).getroot()
    if root.tag != "keyboard":
        return None
    for row in root:
        parse_row_from_et(row, keys, dup)
    return root, keys, dup

def parse_row(fname):
    keys = set()
    dup = set()
    root = ET.parse(fname).getroot()
    if root.tag != "row":
        return None
    parse_row_from_et(root, keys, dup)
    return root, keys, dup

def check_layout(layout):
    root, keys, dup = layout
    if len(dup) > 0: warn("Duplicate keys: " + key_list_str(dup))
    missing_some_of(keys, "~!@#$%^&*(){}`[]=\\-_;:/.,?<>'\"+|", "ASCII punctuation")
    missing_some_of(keys, "0123456789", "digits")
    missing_required(keys, ["backspace", "delete"], "Layout doesn't define some important keys")
    unexpected_keys(keys,
                    ["copy", "paste", "cut", "selectAll", "shareText",
                     "pasteAsPlainText", "undo", "redo" ],
                    "Layout contains editing keys")
    unexpected_keys(keys,
                    [ "f1", "f2", "f3", "f4", "f5", "f6", "f7", "f8", "f9",
                     "f10", "f11", "f12" ],
                    "Layout contains function keys")
    unexpected_keys(keys, [""], "Layout contains empty strings")
    unexpected_keys(keys, ["loc"], "Special keyword cannot be a symbol")
    unexpected_keys(keys, filter(lambda k: k.strip()!=k, keys), "Some keys contain whitespaces")
    unexpected_keys(keys, ["f11_placeholder", "f12_placeholder"], "These keys are now added automatically")

    if root.get("script", "latin") == "latin":
        missing_required(keys, ["shift", "loc capslock"], "Missing important key")
        missing_required(keys, ["loc esc", "loc tab"], "Missing programming keys")

    _, bottom_row_keys, _ = parse_row("res/xml/bottom_row.xml")

    if root.get("bottom_row") == "false":
        missing_required(keys, bottom_row_keys,
                         "Layout redefines the bottom row but some important keys are missing")
    else:
        unexpected_keys(keys, bottom_row_keys,
                        "Layout contains keys present in the bottom row")

    if root.get("script") == None:
        warn("Layout doesn't specify a script.")

    keys_without_loc = set(( k.removeprefix("loc ") for k in keys ))
    # Keys with a len under 3 are often composed characters
    special_keys = set(( k for k in keys_without_loc if len(k) > 3 and ":" not in k ))
    unknown = special_keys.difference(known_keys)
    if len(unknown) > 0:
        warn("Layout contains unknown keys: %s" % key_list_str(unknown))

# Fill 'known_keys', which is used for some checks
def parse_known_keys():
    global known_keys
    with open("srcs/juloo.keyboard2/KeyValue.java", "r", encoding="utf-8") as f:
        known_keys = set(
                ( m.group(1) for m in re.finditer('case "([^"]+)":', f.read()) )
                )

parse_known_keys()

for fname in sorted(glob.glob("srcs/layouts/*.xml")):
    layout_id, _ = os.path.splitext(os.path.basename(fname))
    if layout_id in KNOWN_NOT_LAYOUT:
        continue
    layout_file_name = layout_id
    layout = parse_layout(fname)
    if layout == None:
        warn("Not a layout file")
    else:
        check_layout(layout)

with open("check_layout.output", "w", encoding="utf-8") as out:
    for w in warnings:
        print(w, file=out)
