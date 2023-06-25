#!/usr/bin/env python

# Generates the list of layouts in res/values/layouts.xml from the layout files
# in res/xml. Every layouts must have a 'name' attribute to be listed.

import itertools as it
import sys, os, glob
import xml.etree.ElementTree as XML

# Layouts first in the list (these are the programming layouts). Other layouts
# are sorted alphabetically.
FIRST_LAYOUTS = [ "latn_qwerty_us", "latn_colemak", "latn_dvorak" ]

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
        if layout == None:
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
    def add_items(parent, strings):
        for s in strings:
            item = XML.Element("item")
            item.text = s
            parent.append(item)
    none_item = [ ("none", "None") ]
    custom_item = [ ("custom", "@string/pref_layout_e_custom") ]
    lids, names = zip(*(none_item + layouts + custom_item)) # unzip
    values = XML.Element("string-array", name="pref_layout_values")
    add_items(values, lids)
    entries = XML.Element("string-array", name="pref_layout_entries")
    add_items(entries, names)
    root = XML.Element("resources")
    root.append(XML.Comment(text="DO NOT EDIT. This file is generated, see gen_layouts.py."))
    root.append(values)
    root.append(entries)
    XML.indent(root)
    XML.ElementTree(element=root).write(out, encoding="unicode", xml_declaration=True)

layouts = sort_layouts(read_layouts(glob.glob("res/xml/*.xml")))
with open("res/values/layouts.xml", "w") as out:
    generate_arrays(out, layouts)
