import xml.etree.ElementTree as ET
import sys, os, glob

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

def duplicates(keys):
    dup = [ k for k, key_elts in keys.items() if len(key_elts) >= 2 ]
    if len(dup) > 0:
        warn("Duplicate keys: " + key_list_str(dup))

def should_have_role(keys_map, role, keys):
    def is_center_key(key):
        def center(key_elt): return key_elt.get("key0", key_elt.get("c"))
        return any(( center(key_elt) == key for key_elt in keys_map.get(key, []) ))
    def key_roles(key):
        return ( key_elt.get("role", "normal") for key_elt in keys_map[key] )
    for key in keys:
        if is_center_key(key) and role not in key_roles(key):
            warn("Key '%s' is not on a key with role=\"%s\"" % (key, role))

# Write to [keys], dict of keyvalue to the key elements they appear in
def parse_row_from_et(row, keys):
    for key_elt in row:
        for attr in key_elt.keys():
            if attr in KEY_ATTRIBUTES:
                k = key_elt.get(attr).removeprefix("\\")
                keys.setdefault(k, []).append(key_elt)

def parse_layout(fname):
    keys = {}
    root = ET.parse(fname).getroot()
    if root.tag != "keyboard":
        return None
    for row in root:
        parse_row_from_et(row, keys)
    return root, keys

def parse_row(fname):
    keys = {}
    root = ET.parse(fname).getroot()
    if root.tag != "row":
        return None
    parse_row_from_et(root, keys)
    return root, keys

def check_layout(layout):
    root, keys_map = layout
    keys = set(keys_map.keys())
    duplicates(keys_map)
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

    _, bottom_row_keys_map = parse_row("res/xml/bottom_row.xml")
    bottom_row_keys = set(bottom_row_keys_map.keys())
    if root.get("bottom_row") == "false":
        missing_required(keys, bottom_row_keys,
                         "Layout redefines the bottom row but some important keys are missing")
    else:
        unexpected_keys(keys, bottom_row_keys,
                        "Layout contains keys present in the bottom row")

    if root.get("script") == None:
        warn("Layout doesn't specify a script.")

    should_have_role(keys_map, "action",
                     [ "shift", "ctrl", "fn", "backspace", "enter" ])
    should_have_role(keys_map, "space_bar", [ "space" ])

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

with open("check_layout.output", "w") as out:
    for w in warnings:
        print(w, file=out)
