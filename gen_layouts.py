#!/usr/bin/env python

# Generates the list of layouts in res/values/layouts.xml from the layout files
# in res/xml. Every layouts must have a 'name' attribute to be listed.

import itertools as it
import sys, os, glob
import xml.etree.ElementTree as XML

# Layouts first in the list (these are the programming layouts). Other layouts
# are sorted alphabetically.
FIRST_LAYOUTS = [ "latn_qwerty_us", "latn_colemak", "latn_dvorak" ]

# File names that are known not to be layouts. Avoid warning about them.
KNOWN_NOT_LAYOUT = set([
    "number_row", "numpad", "pin", "bottom_row", "settings", "method",
    "greekmath", "numeric" ])

# Read a layout from a file. Returns [None] if [fname] is not a layout.
def read_layout(fname):
    root = XML.parse(fname).getroot()
    if root.tag != "keyboard":
        return None
    return { "name": root.get("name") }

# Yields the id (based on the file name) and the display name for every layouts
def read_layouts(files):
    for layout_file in files:
        layout_id, _ = os.path.splitext(os.path.basename(layout_file))
        layout = read_layout(layout_file)
        if layout_id in KNOWN_NOT_LAYOUT:
            continue
        elif layout == None:
            print("Not a layout file: %s" % layout_file)
        elif layout["name"] == None:
            print("Layout doesn't have a name: %s" % layout_id)
        else:
            yield (layout_id, layout["name"])

# Sort layouts alphabetically, except for layouts in FIRST_LAYOUTS, which are
# placed at the top.
# Returns a list. 'layouts' can be an iterator.
def sort_layouts(layouts):
    layouts = dict(layouts)
    head = [ (lid, layouts.pop(lid)) for lid in FIRST_LAYOUTS ]
    return head + sorted(layouts.items())

# Write the XML arrays used in the preferences.
def generate_arrays(out, layouts):
    def mk_array(tag, name, strings_items):
        elem = XML.Element(tag, name=name)
        for s in strings_items:
            item = XML.Element("item")
            item.text = s
            elem.append(item)
        return elem
    system_item = [ ("system", "@string/pref_layout_e_system") ]
    custom_item = [ ("custom", "@string/pref_layout_e_custom") ]
    values_items, entries_items = zip(*(system_item + layouts + custom_item)) # unzip
    ids_items = map(lambda s: "@xml/%s" % s if s not in ["system", "custom"] else "-1", values_items)
    root = XML.Element("resources")
    root.append(XML.Comment(text="DO NOT EDIT. This file is generated, see gen_layouts.py."))
    root.append(mk_array("string-array", "pref_layout_values", values_items))
    root.append(mk_array("string-array", "pref_layout_entries", entries_items))
    root.append(mk_array("integer-array", "layout_ids", ids_items))
    XML.indent(root)
    XML.ElementTree(element=root).write(out, encoding="unicode", xml_declaration=True)

layouts = sort_layouts(read_layouts(glob.glob("src/main/res/xml/*.xml")))
with open("src/main/res/values/layouts.xml", "w") as out:
    generate_arrays(out, layouts)
