import xml.etree.ElementTree as ET
import itertools as it
import sys

# This script generates res/xml/method.xml.

def warn(msg):
    print("Warning: " + msg, file=sys.stderr)

def loc(loc_name, script, default_layout, **kwargs):
    return { "name": loc_name, "script": script,
             "default_layout": default_layout, **kwargs }

# The locales are defined here. To add support for a language, add it to the
# following block:

LOCALES = [
  loc("ar", "arabic", "arab_pc_hindu"),
  loc("ar_TN", "arabic", "arab_pc"),
  loc("as", "beng", "beng_assamese"),
  loc("az_AZ", "latin", "latn_qwerty_az", extra_keys="accent_trema:ü:ö@w|accent_cedille:ç:ş@s|ğ@g|ı@k|ə@l"),
  loc("be_BY", "cyrillic", "cyrl_jcuken_ru", extra_keys="ґ|є|і|ї|ў"),
  loc("bg_BG", "cyrillic", "cyrl_ueishsht", extra_keys="€"),
  loc("bn_BD", "latin", "latn_qwerty_us", extra_keys="৳"),
  loc("bs", "latin", "latn_qwerty_us", extra_keys="đ|ž|lj|nj|ć|č|dž|š"),
  loc("ca", "latin", "latn_qwerty_us", extra_keys="accent_grave:à:ò:è|accent_cedille:ç@c|accent_aigu:é:í:ú:ó|accent_trema:ï:ü|ŀl|€"),
  loc("cs_CZ", "latin", "latn_qwertz_cz", extra_keys="accent_aigu:á:é:í:ó:ú:ý@d|accent_ring:ů@s|accent_caron:č:ě:ň:ř:š:ž:ď:ť@f"),
  loc("cy_GB", "latin", "latn_qwerty_cy"),
  loc("da_DK", "latin", "latn_qwerty_da", extra_keys="€|æ|å|ø"),
  loc("de_BE", "latin", "latn_azerty_be", extra_keys="accent_grave:è@f|accent_aigu:á:é:í:ó:ú:ý:j́@d|accent_circonflexe:ê@f|accent_cedille:ç@c|accent_trema@u|€"),
  loc("de_CH", "latin", "latn_qwertz_de", extra_keys="accent_trema:ä:ö:ü@u|ß"),
  loc("de_DE", "latin", "latn_qwertz_de", extra_keys="accent_trema:ä:ö:ü@u|ß|€"),
  loc("el", "latin", "grek_qwerty", extra_keys="£@l|€"),
  loc("en", "latin", "latn_qwerty_us", dictionary="en_GB"),
  loc("en_AU", "latin", "latn_qwerty_us"),
  loc("en_CA", "latin", "latn_qwerty_us"),
  loc("en_GB", "latin", "latn_qwerty_gb", extra_keys="£@l"),
  loc("en_IN", "latin", "latn_qwerty_us"),
  loc("en_NG", "latin", "latn_qwerty_us", extra_keys="₦"),
  loc("en_US", "latin", "latn_qwerty_us"),
  loc("es_ES", "latin", "latn_qwerty_es", extra_keys="accent_aigu:á:é:í:ó:ú@d|accent_tilde:ñ@n|accent_grave@f|accent_trema@u|€"),
  loc("et_EE", "latin", "latn_qwerty_et", extra_keys="accent_trema:ä:ö:ü@u|accent_tilde:õ@o|accent_caron:š:ž@s|€"),
  loc("eu", "latin", "latn_qwerty_us", extra_keys="ñ|ç|ü|dd|ll|rr|ts|tt|tx|tz"),
  loc("fa_IR", "persian", "arab_pc_ir"),
  loc("fi", "latin", "latn_qwerty_fi", extra_keys="å|accent_ring|accent_aigu|accent_trema|ö|ä|€"),
  loc("fr_BE", "latin", "latn_azerty_be", extra_keys="accent_grave:à:è:ù@f|accent_aigu:é@d|accent_circonflexe:ê:û@f|accent_cedille:ç@c|accent_trema@u|€"),
  loc("fr_CA", "latin", "latn_azerty_fr", extra_keys="accent_grave:à:è:ù@f|accent_aigu:é@d|accent_circonflexe:â:ê:ô:û@f|accent_cedille:ç@c|accent_trema:ë:ï:ü:ÿ@u"),
  loc("fr_CH", "latin", "latn_qwertz_fr_ch", extra_keys="accent_grave:à:è:ù@f|accent_aigu:é@d|accent_circonflexe:â:ê:ô:û@o|accent_cedille:ç@c|accent_trema:ë:ï:ü:ÿ@u|€"),
  loc("fr_FR", "latin", "latn_azerty_fr", extra_keys="accent_grave:à:è:ù@d|accent_aigu:é@d|accent_circonflexe:â:ê:ô:û@o|accent_cedille:ç@c|accent_trema:ë:ï:ü@l|€"),
  loc("ga_IE", "latin", "latn_qwerty_ga", extra_keys="accent_aigu:á:é:í:ó:ú@k|accent_dot_above@l"),
  loc("gl", "latin", "latn_qwerty_us"),
  loc("ha_NG", "latin", "latn_qwerty_us", extra_keys="₦|ɓ|ɗ|ƙ|’|ƴ|r̃"),
  loc("haw_US", "latin", "latn_qwerty_haw", extra_keys="ʻ@l|accent_macron:ā:ē:ī:ō:ū@m"),
  loc("he_IL", "hebrew", "hebr_1_il", extra_keys="₪@r"),
  loc("hi_IN", "devanagari", "deva_inscript", extra_keys="₹"),
  loc("hr", "latin", "latn_qwerty_us", extra_keys="č|ć|dž|đ|lj|nj|š|ž"),
  loc("hu_HU", "latin", "latn_qwertz_hu", extra_keys="accent_aigu:á:é:í:ó:ú@d|accent_trema:ö:ü@u|accent_ogonek@s|accent_double_aigu:ő:ű@k|€"),
  loc("hy", "armenian", "armenian_ph_am"),
  loc("ig_NG", "latin", "latn_qwerty_us", extra_keys="₦|ṅ|ọ|ụ"),
  loc("is_IS", "latin", "latn_qwerty_is", extra_keys="ð|þ|æ|accent_trema:ö@o|accent_aigu:á:é:í:ó:ú:ý@d|accent_circonflexe|accent_ring|accent_grave"),
  loc("it_IT", "latin", "latn_qwerty_us", extra_keys="accent_grave:à:è:ì:ò:ù@f|accent_aigu:é:ó@d|accent_circonflexe:î@f|€|ə"),
  loc("ka_GE", "latin", "georgian_mes"),
  loc("kk_KZ", "latin", "cyrl_jcuken_kk"),
  loc("kn_IN", "kannada", "kann_kannada"),
  loc("ko_KR", "hangul", "hang_dubeolsik_kr"),
  loc("lb", "latin", "latn_qwerty_us", extra_keys="é|ä|ë|accent_grave|accent_cedille@c|accent_aigu|accent_trema|€"),
  loc("lt_LT", "latin", "latn_qwerty_lt", extra_keys="accent_ogonek:ą:ę:į:ų@s|accent_caron:č:š:ž@f|accent_dot_above:ė@s|accent_macron:ū@o|€"),
  loc("lv_LV", "latin", "latn_qwerty_lv", extra_keys="accent_macron:ā:ē:ī:ū@o|accent_caron:č:š:ž@f|accent_ogonek:ķ:ļ:ņ@s|accent_cedille:ģ@c|€"),
  loc("mk", "cyrillic", "cyrl_lynyertdz_mk", extra_keys="ѕ|ѓ|ќ|ѝ|ѐ|љ|њ|џ|„|“|€"),
  loc("mn_MN", "cyrillic", "cyrl_fcuzhen_mn", extra_keys="ү|ө"),
  loc("mr_IN", "devanagari", "deva_inscript", extra_keys="₹"),
  loc("mt_MT", "latin", "latn_qwerty_mt", extra_keys="accent_grave:à:è:ì:ò:ù|accent_dot_above:ċ:ż:ġ|ħ"),
  loc("nb", "latin", "latn_qwerty_us", extra_keys="€|æ@a|å@a|ø@o|accent_aigu:é:ó@d|accent_grave:è:ò:ù@f|accent_circonflexe:ê:ô@f"),
  loc("ne_NE", "devanagari", "deva_inscript", extra_keys="₹"),
  loc("nl_BE", "latin", "latn_azerty_be", extra_keys="accent_grave:è@f|accent_aigu:á:é:í:ó:ú:ý:j́@d|accent_circonflexe:ê@f|accent_cedille:ç@c|accent_trema@u|€"),
  loc("no_NO", "latin", "latn_qwerty_us", extra_keys="€|æ@a|å@a|ø@o|accent_aigu:é:ó@d|accent_grave:è:ò:ù@f|accent_circonflexe:ê:ô@f"),
  loc("pl_PL", "latin", "latn_qwerty_pl"),
  loc("pt_BR", "latin", "latn_qwerty_pt", extra_keys="accent_aigu:á:é:í:ó:ú@d|accent_cedille:ç@c|accent_circonflexe:â:ê:ô@f|accent_grave:à:ò@f|accent_tilde:ã:õ@n|€|ª|º"),
  loc("pt_PT", "latin", "latn_qwerty_pt", extra_keys="accent_aigu:á:é:í:ó:ú@d|accent_cedille:ç@c|accent_circonflexe:â:ê:ô@f|accent_grave:à:ò@f|accent_tilde:ã:õ@n|€|ª|º"),
  loc("ro_RO", "latin", "latn_qwerty_ro", extra_keys="ă|â|î|ș|ț|€|$"),
  loc("ru_RU", "latin", "cyrl_jcuken_ru"),
  loc("si_LK", "sinhala", "sinhala_phonetic", extra_keys="₨"),
  loc("sk_SK", "latin", "latn_qwertz_sk", extra_keys="accent_caron:ě:ř:ž:š:č:ň:ď:ľ:ť@f|accent_ring:ů@s|accent_circonflexe:ô@f|accent_trema:ä:ü:ö@u|accent_aigu:á:é:í:ó:ú:ŕ:ś:ĺ:ý@d"),
  loc("sl", "latin", "latn_qwerty_us", extra_keys="accent_caron:Č:Š:Ž|€"),
  loc("sq_AL", "latin", "latn_qwertz_sq"),
  loc("sr_", "latin", "cyrl_lynyertz_sr"),
  loc("sv_SE", "latin", "latn_qwerty_se", extra_keys="accent_aigu:á@d|accent_trema:ä:ö@o|accent_ring:å@s|€"),
  loc("ta_IN", "tamil", "tamil_default"),
  loc("tly_AZ", "latin", "latn_qwerty_tly", extra_keys="á|ú|â|ê|ı|š|ž"),
  loc("tly_IR", "persian", "arab_hamvaj_tly"),
  loc("tr_TR", "latin", "latn_qwerty_tr", extra_keys="accent_cedille:ç:ş@c|accent_trema:ö:ü@u|accent_circonflexe:â:î:û@f|₺|ı|ğ"),
  loc("uk_UA", "cyrillic", "cyrl_jcuken_uk", extra_keys="ґ|є|і|ї|₴"),
  loc("ur", "persian", "arab_pc_ir"),
  loc("uz_UZ", "latin", "latn_qwerty_uz", extra_keys="ʻ|ʼ"),
  loc("vi_VN", "latin", "latn_qwerty_vi"),
  loc("yo_NG", "latin", "latn_qwerty_us", extra_keys="₦|ẹ|ọ|ṣ")
]

# The locale that is at the beginning of the list.
DEFAULT_LOCALE = "en_GB"

# The default attributes for incomplete locales
DEFAULT_LOC_FOR_COPY = loc("en", "latin", "latn_qwerty_us")

def parse_dictionaries():
    tree = ET.parse("res/values/dictionaries.xml")
    root = tree.getroot()
    return set(( it.text for it in root.findall('*[@name="dictionaries_locale"]/item') ))

# Available dictionares of the form "de" or "de_CH".
available_dictionaries = parse_dictionaries()

def subtype_elem(root, loc):
    tag = loc["name"].replace("_", "-")
    extra_keys = ",extra_keys=" + loc["extra_keys"] if "extra_keys" in loc else ""
    dictionaries = ",dictionary=" + loc["dictionary"] if loc["dictionary"] != None else ""
    extra_value = f'script={loc["script"]},default_layout={loc["default_layout"]}{dictionaries}{extra_keys}'
    ET.SubElement(root, "subtype", attrib={
        "android:label": "%s",
        "android:languageTag": tag,
        "android:imeSubtypeLocale": loc["name"],
        "android:imeSubtypeMode": "keyboard",
        "android:isAsciiCapable": "true",
        "android:imeSubtypeExtraValue": extra_value
        })

# Add default locales for each languages and pair dictionaries.
def process_locales(locales):
    def lang(loc):
        return loc["name"].split("_")[0]
    def find_locale(locs, name):
        for loc in locs:
            if loc["name"] == name: return loc
        return None
    def find_default_locale_in_group(l, locs):
        for loc in locs:
            if "default_for_lang" in loc and loc["default_for_lang"]:
                return loc
        # Set the language with a country code equal to the lang as the first
        # in order (eg. "de_DE" comes before "de_BE")
        def_loc = find_locale(locs, f"{l}_{l.upper()}")
        if def_loc is not None:
            def_loc["default_for_lang"] = True
        else:
            def_loc = find_locale(locs, l)
            if def_loc is None and len(locs) == 1:
                def_loc = locs[0]
        return def_loc
    def dictionary(loc, def_loc):
        if loc is None: return None
        if "dictionary" in loc: return loc["dictionary"]
        if loc["name"] in available_dictionaries: return loc["name"]
        l = lang(loc)
        if l in available_dictionaries: return l
        if def_loc is not None and "dictionary" in def_loc: return def_loc["dictionary"]
        return None
    locales_grouped = {} # Locales grouped by language tag
    for loc in locales:
        locales_grouped.setdefault(lang(loc), []).append(dict(**loc))
    # Set "default_for_lang" and "dictionary"
    used_dicts = set()
    for l, locs in locales_grouped.items():
        def_loc = find_default_locale_in_group(l, locs)
        for loc in locs:
            loc["dictionary"] = dictionary(loc, def_loc)
            used_dicts.add(loc["dictionary"])
    # Add locales for dictionaries not yet attached
    for dict_ in available_dictionaries:
        if dict_ not in used_dicts:
            l = dict_.split("_")[0]
            locs = locales_grouped.setdefault(l, [])
            if find_locale(locs, dict_) is None:
                def_loc = find_default_locale_in_group(l, locs) or DEFAULT_LOC_FOR_COPY
                locs.append({ **def_loc, "name": dict_, "dictionary": dict_ })
    for _l, locs in locales_grouped.items():
        yield from locs

def sort_locales(locales):
    # The default locale for a language (eg. "en") might shadow the exact
    # locale (eg. "en_US"). Makes sure the default locale sorts after the exact
    # ones.
    def key(l):
        s = l["name"].split("_")
        return (l["name"] != DEFAULT_LOCALE), s[0], (not l.get("default_for_lang", False)), (len(s) == 1), s[1:]
    return sorted(locales, key=key)

def gen():
    locales = sort_locales(process_locales(LOCALES))
    root = ET.Element("input-method", attrib={
        "xmlns:android": "http://schemas.android.com/apk/res/android",
        "android:settingsActivity": "juloo.keyboard2.SettingsActivity",
        "android:supportsInlineSuggestions": "true",
        "android:supportsSwitchingToNextInputMethod": "true",
        })
    root.append(ET.Comment(text=""" This file is automatically generated. DO NOT EDIT.
       Locales definitions should go into 'gen_method_xml.py'.
       Update this file with 'gradle test'.

  """))
    for loc in locales:
        subtype_elem(root, loc)
    ET.indent(root)
    print(ET.tostring(root, encoding="utf-8", xml_declaration=True).decode("UTF-8"))

gen()
