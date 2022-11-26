import xml.etree.ElementTree as ET
from xml.sax import saxutils
import glob

# Edit every strings.xml files:
# - Add missing translation as comments
# - Remove obsolete strings
# - Sort in the same order as the baseline
# The baseline is 'values/strings.xml', which is english.

def parse_strings_file(file):
    resources = ET.parse(file).getroot()
    return [
        ((ent.get("name"), ent.get("product")), ent.text)
        for ent in resources if ent.tag == "string"
    ]

def string_entry_str(name, text, product):
    product_attr = ' product="%s"' % product if product != None else ""
    text_encoded = saxutils.escape(text)
    return '<string name="%s"%s>%s</string>' % (name, product_attr, text_encoded)

def write_updated_strings(out, baseline, strings):
    out.write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n')
    for key, default_text in baseline:
        name, product = key
        if key in strings:
            ent = string_entry_str(name, strings[key], product)
            out.write("  %s\n" % ent)
        else:
            def_ent = string_entry_str(name, default_text, product)
            out.write("  <!-- %s -->\n" % def_ent)
    out.write('</resources>\n')

baseline = parse_strings_file("res/values/strings.xml")

for strings_file in glob.glob("res/values-*/strings.xml"):
    print(strings_file)
    strings = dict(parse_strings_file(strings_file))
    with open(strings_file, "w") as out:
        write_updated_strings(out, baseline, strings)
