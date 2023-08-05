import xml.etree.ElementTree as ET
import glob

# Edit every strings.xml files:
# - Add missing translation as comments
# - Remove obsolete strings
# - Sort in the same order as the baseline
# The baseline is 'values/strings.xml', which is english.

# Dict of strings. Key is the pair string name and product field (often None).
def parse_strings_file(file):
    def key(ent): return ent.get("name"), ent.get("product")
    resrcs = ET.parse(file).getroot()
    return { key(ent): ent for ent in resrcs if ent.tag == "string" }

# Print the XML file back autoformatted. Takes the output of [sync].
def write_updated_strings(out, strings):
    out.write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n')
    for key, string, comment in strings:
        out.write("  ")
        if comment: out.write("<!-- ")
        out.write(ET.tostring(string, "unicode").strip())
        if comment: out.write(" -->")
        out.write("\n")
    out.write('</resources>\n')

# Print whether string file is uptodate.
def print_status(fname, strings):
    # Number of commented-out strings
    c = sum(1 for _, _, comment in strings if comment)
    status = "uptodate" if c == 0 else "missing %d strings" % c
    print("%s: %s" % (fname, status))

# Returns a list of tuples (key, string, commented).
def sync(baseline, strings):
    return [
            (key, strings[key], False) if key in strings else
            (key, base_string, True)
            for key, base_string in baseline.items() ]

baseline = parse_strings_file("res/values/strings.xml")

for strings_file in glob.glob("res/values-*/strings.xml"):
    strings = sync(baseline, dict(parse_strings_file(strings_file)))
    with open(strings_file, "w", encoding="utf-8") as out:
        write_updated_strings(out, strings)
    print_status(strings_file, strings)
