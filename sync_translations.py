import xml.etree.ElementTree as ET
from xml.sax import saxutils
import glob

# Edit every strings.xml files:
# - Add missing translation as comments
# - Remove obsolete strings
# - Sort in the same order as the baseline
# The baseline is 'values/strings.xml', which is english.

def parse_strings_file(file):
    def key(ent): return ent.get("name"), ent.get("product")
    resrcs = ET.parse(file).getroot()
    return { key(ent): ent for ent in resrcs if ent.tag == "string" }

def dump_entry(out, entry, comment):
    out.write("  ")
    if comment: out.write("<!-- ")
    out.write(ET.tostring(entry, "unicode").strip())
    if comment: out.write(" -->")
    out.write("\n")

def write_updated_strings(out, baseline, strings):
    out.write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n')
    for key, baseline_entry in baseline.items():
        if key in strings:
            dump_entry(out, strings[key], False)
        else:
            dump_entry(out, baseline_entry, True)
    out.write('</resources>\n')

baseline = parse_strings_file("res/values/strings.xml")

for strings_file in glob.glob("res/values-*/strings.xml"):
    print(strings_file)
    strings = dict(parse_strings_file(strings_file))
    with open(strings_file, "w", encoding="utf-8") as out:
        write_updated_strings(out, baseline, strings)
