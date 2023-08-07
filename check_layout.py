import xml.etree.ElementTree as ET
import sys

warning_count = 0

KNOWN_NOT_LAYOUT = set([
    "res/xml/number_row.xml", "res/xml/numpad.xml", "res/xml/pin.xml",
    "res/xml/bottom_row.xml", "res/xml/settings.xml", "res/xml/method.xml",
    "res/xml/greekmath.xml", "res/xml/numeric.xml" ])

def warn(msg):
    global warning_count
    print(msg)
    warning_count += 1

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

def parse_layout(fname):
    keys = set()
    dup = set()
    root = ET.parse(fname).getroot()
    if root.tag != "keyboard":
        return None
    for row in root:
        for key in row:
            for attr in key.keys():
                if attr.startswith("key"):
                    k = key.get(attr).removeprefix("\\")
                    if k in keys: dup.add(k)
                    keys.add(k)
    return root, keys, dup

def check_layout(layout):
    root, keys, dup = layout
    if len(dup) > 0: warn("Duplicate keys: " + key_list_str(dup))
    missing_some_of(keys, "~!@#$%^&*(){}`[]=\\-_;:/.,?<>'\"+|", "ASCII punctuation")
    missing_some_of(keys, "0123456789", "digits")
    missing_required(keys,
                     ["esc", "tab", "backspace", "delete",
                      "f11_placeholder", "f12_placeholder"],
                     "Layout doesn't define some important keys")
    unexpected_keys(keys,
                    ["copy", "paste", "cut", "selectAll", "shareText",
                     "pasteAsPlainText", "undo", "redo", "replaceText",
                     "textAssist", "autofill" ],
                    "Layout contains editing keys")
    unexpected_keys(keys,
                    [ "f1", "f2", "f3", "f4", "f5", "f6", "f7", "f8", "f9",
                     "f10", "f11", "f12" ],
                    "Layout contains function keys")

    bottom_row_keys = [
            "ctrl", "fn", "switch_numeric", "change_method", "switch_emoji",
            "config", "switch_forward", "switch_backward", "enter", "action",
            "left", "up", "right", "down", "space"
            ]

    if root.get("bottom_row") == "false":
        missing_required(keys, bottom_row_keys,
                         "Layout redefines the bottom row but some important keys are missing")
    else:
        unexpected_keys(keys, bottom_row_keys,
                        "Layout contains keys present in the bottom row")

    if root.get("script") == None:
        warn("Layout doesn't specify a script.")

for fname in sys.argv[1:]:
    if fname in KNOWN_NOT_LAYOUT:
        continue
    layout = parse_layout(fname)
    if layout == None:
        print("Not a layout file: %s" % fname)
    else:
        print("# %s" % fname)
        warning_count = 0
        check_layout(layout)
        print("%d warnings" % warning_count)
