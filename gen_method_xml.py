import xml.etree.ElementTree as ET
import itertools as it

# This script generates res/xml/method.xml.

def loc(loc_name, script, default_layout, **kwargs):
    return { "name": loc_name, "script": script,
            "default_layout": default_layout, **kwargs }

# The locales are defined here. To add support for a language, add it to the
# following block:

LOCALES = [
  loc("ar", "arabic", "arab_pc_hindu"),
  loc("ar_TN", "arabic", "arab_pc"),
  loc("ay_AM", "armenian", "armenian_ph_am"),
  loc("az_AZ", "latin", "latn_qwerty_az", extra_keys="accent_trema:ü:ö@w|accent_cedille:ç:ş@s|ğ@g|ı@k|ə@l"),
  loc("be_BY", "cyrillic", "cyrl_jcuken_ru", extra_keys="ґ|є|і|ї|ў"),
  loc("bg_BG", "cyrillic", "cyrl_ueishsht", extra_keys="€"),
  loc("bn_BD", "latin", "latn_qwerty_us", extra_keys="৳"),
  loc("cs_CZ", "latin", "latn_qwertz_cz", extra_keys="accent_aigu:á:é:í:ó:ú:ý@d|accent_ring:ů@s|accent_caron:č:ě:ň:ř:š:ž:ď:ť@f"),
  loc("cy_GB", "latin", "latn_qwerty_cy"),
  loc("da_DK", "latin", "latn_qwerty_da", extra_keys="€|æ|å|ø"),
  loc("de_BE", "latin", "latn_azerty_be", extra_keys="accent_grave:è@f|accent_aigu:á:é:í:ó:ú:ý:j́@d|accent_circonflexe:ê@f|accent_cedille:ç@c|accent_trema@u|€"),
  loc("de_DE", "latin", "latn_qwertz_de", extra_keys="accent_trema:ä:ö:ü@u|ß|€"),
  loc("el", "latin", "grek_qwerty", extra_keys="£@l|€"),
  loc("en_AU", "latin", "latn_qwerty_us"),
  loc("en_CA", "latin", "latn_qwerty_us"),
  loc("en_GB", "latin", "latn_qwerty_gb", extra_keys="£@l"),
  loc("en_IN", "latin", "latn_qwerty_us"),
  loc("en_NG", "latin", "latn_qwerty_us", extra_keys="₦"),
  loc("en_US", "latin", "latn_qwerty_us"),
  loc("es_ES", "latin", "latn_qwerty_es", extra_keys="accent_aigu:á:é:í:ó:ú@d|accent_tilde:ñ@n|accent_grave@f|accent_trema@u|€"),
  loc("et_EE", "latin", "latn_qwerty_et", extra_keys="accent_trema:ä:ö:ü@u|accent_tilde:õ@o|accent_caron:š:ž@s|€"),
  loc("fa_IR", "persian", "arab_pc_ir"),
  loc("fr_BE", "latin", "latn_azerty_be", extra_keys="accent_grave:à:è:ù@f|accent_aigu:é@d|accent_circonflexe:ê:û@f|accent_cedille:ç@c|accent_trema@u|€"),
  loc("fr_CA", "latin", "latn_azerty_fr", extra_keys="accent_grave:à:è:ù@f|accent_aigu:é@d|accent_circonflexe:â:ê:ô:û@f|accent_cedille:ç@c|accent_trema:ë:ï:ü:ÿ@u"),
  loc("fr_CH", "latin", "latn_qwertz_fr_ch", extra_keys="accent_grave:à:è:ù@f|accent_aigu:é@d|accent_circonflexe:â:ê:ô:û@o|accent_cedille:ç@c|accent_trema:ë:ï:ü:ÿ@u|€"),
  loc("fr_FR", "latin", "latn_azerty_fr", extra_keys="accent_grave:à:è:ù@d|accent_aigu:é@d|accent_circonflexe:â:ê:ô:û@o|accent_cedille:ç@c|accent_trema:ë:ï:ü@l|€"),
  loc("ga_IE", "latin", "latn_qwerty_ga", extra_keys="accent_aigu:á:é:í:ó:ú@k|accent_dot_above@l"),
  loc("ha_NG", "latin", "latn_qwerty_us", extra_keys="₦|ɓ|ɗ|ƙ|’|ƴ|r̃"),
  loc("haw_US", "latin", "latn_qwerty_haw", extra_keys="ʻ@l|accent_macron:ā:ē:ī:ō:ū@m"),
  loc("he_IL", "hebrew", "default_layout=hebr_1_il,extra_keys=₪@r|€"),
  loc("hi_IN", "devanagari", "deva_inscript", extra_keys="₹"),
  loc("hu_HU", "latin", "latn_qwertz_hu", extra_keys="accent_aigu:á:é:í:ó:ú@d|accent_trema:ö:ü@u|accent_ogonek@s|accent_double_aigu:ő:ű@k|€"),
  loc("ig_NG", "latin", "latn_qwerty_us", extra_keys="₦|ṅ|ọ|ụ"),
  loc("is_IS", "latin", "latn_qwerty_is", extra_keys="ð|þ|æ|accent_trema:ö@o|accent_aigu:á:é:í:ó:ú:ý@d|accent_circonflexe|accent_ring|accent_grave"),
  loc("it_IT", "latin", "latn_qwerty_us", extra_keys="accent_grave:à:è:ì:ò:ù@f|accent_aigu:é:ó@d|accent_circonflexe:î@f|€|ə"),
  loc("ka_GE", "latin", "georgian_mes"),
  loc("kk_KZ", "latin", "cyrl_jcuken_kk"),
  loc("kn_IN", "kannada", "kann_kannada"),
  loc("ko_KR", "hangul", "latn_qwerty_us"),
  loc("lt_LT", "latin", "latn_qwerty_lt", extra_keys="accent_ogonek:ą:ę:į:ų@s|accent_caron:č:š:ž@f|accent_dot_above:ė@s|accent_macron:ū@o|€"),
  loc("lv_LV", "latin", "latn_qwerty_lv", extra_keys="accent_macron:ā:ē:ī:ū@o|accent_caron:č:š:ž@f|accent_ogonek:ķ:ļ:ņ@s|accent_cedille:ģ@c|€"),
  loc("mk", "cyrillic", "cyrl_lynyertdz_mk", extra_keys="ѕ|ѓ|ќ|ѝ|ѐ|љ|њ|џ|„|“|€"),
  loc("mn_MN", "cyrillic", "cyrl_fcuzhen_mn", extra_keys="ү|ө"),
  loc("mr_IN", "devanagari", "deva_inscript", extra_keys="₹"),
  loc("mt_MT", "latin", "latn_qwerty_mt", extra_keys="accent_grave:à:è:ì:ò:ù|accent_dot_above:ċ:ż:ġ|ħ"),
  loc("ne_NE", "devanagari", "deva_inscript", extra_keys="₹"),
  loc("nl_BE", "latin", "latn_azerty_be", extra_keys="accent_grave:è@f|accent_aigu:á:é:í:ó:ú:ý:j́@d|accent_circonflexe:ê@f|accent_cedille:ç@c|accent_trema@u|€"),
  loc("no_NO", "latin", "latn_qwerty_us", extra_keys="€|æ@a|å@a|ø@o|accent_aigu:é:ó@d|accent_grave:è:ò:ù@f|accent_circonflexe:ê:ô@f"),
  loc("pl_PL", "latin", "latn_qwerty_pl"),
  loc("pt_BR", "latin", "latn_qwerty_pt", extra_keys="accent_aigu:á:é:í:ó:ú@d|accent_cedille:ç@c|accent_circonflexe:â:ê:ô@f|accent_grave:à:ò@f|accent_tilde:ã:õ@n|€|ª|º"),
  loc("ro_RO", "latin", "latn_qwerty_ro", extra_keys="ă|â|î|ș|ț|€|$"),
  loc("ru_RU", "latin", "cyrl_jcuken_ru"),
  loc("si_LK", "sinhala", "sinhala_phonetic", extra_keys="₨"),
  loc("sk_SK", "latin", "latn_qwertz_sk", extra_keys="accent_caron:ě:ř:ž:š:č:ň:ď:ľ:ť@f|accent_ring:ů@s|accent_circonflexe:ô@f|accent_trema:ä:ü:ö@u|accent_aigu:á:é:í:ó:ú:ŕ:ś:ĺ:ý@d"),
  loc("sq_AL", "latin", "latn_qwertz_sq"),
  loc("sr_", "latin", "cyrl_lynyertz_sr"),
  loc("sv_SE", "latin", "latn_qwerty_se", extra_keys="accent_aigu:á@d|accent_trema:ä:ö@o|accent_ring:å@s|€"),
  loc("ta_IN", "tamil", "tamil_default"),
  loc("tly_AZ", "latin", "latn_qwerty_tly", extra_keys="á|ú|â|ê|ı|š|ž"),
  loc("tly_IR", "persian", "arab_hamvaj_tly"),
  loc("tr_TR", "latin", "latn_qwerty_tr", extra_keys="accent_cedille:ç:ş@c|accent_trema:ö:ü@u|accent_circonflexe:â:î:û@f|₺|ı|ğ"),
  loc("uk_UA", "cyrillic", "cyrl_jcuken_uk", extra_keys="ґ|є|і|ї|₴"),
  loc("uz_UZ", "latin", "latn_qwerty_uz", extra_keys="ʻ|ʼ"),
  loc("vi_VN", "latin", "latn_qwerty_vi"),
  loc("yo_NG", "latin", "latn_qwerty_us", extra_keys="₦|ẹ|ọ|ṣ")
]

# The locale that is at the beginning of the list
DEFAULT_LOCALE = loc("en", "latin", "latn_qwerty_us", tag="en", dictionary="en_US")

def parse_dictionaries():
    tree = ET.parse("res/values/dictionaries.xml")
    root = tree.getroot()
    return set(( it.text for it in root.findall('*[@name="dictionaries_locale"]/item') ))

# Available dictionares of the form "de" or "de_CH".
available_dictionaries = parse_dictionaries()

def subtype_elem(root, loc):
    tag = loc["tag"].replace("_", "-")
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

# Return locales in sorted order with the "tag" and "dictionary" attributes
# added.
def compute_attrs():
    locales_grouped = {} # Locales grouped by language tag
    def lang(loc):
        return loc["name"].split("_")[0]
    for loc in LOCALES:
        locales_grouped.setdefault(lang(loc), []).append(loc)
    def tag(loc):
        if "tag" in loc: return loc["tag"]
        l = lang(loc)
        if loc["name"] == f"{l}_{l.upper()}": return l # Locales like "fr_FR"
        # Return a short tag when it's not shared between several locales
        return l if len(locales_grouped[l]) == 1 else loc["name"]
    def dictionary(loc):
        if loc["name"] in available_dictionaries: return loc["name"]
        l = lang(loc)
        if l in available_dictionaries: return l
        return None
    def add_attrs(loc):
        return dict(tag=tag(loc), dictionary=dictionary(loc), **loc)
    return map(add_attrs, LOCALES)

def gen():
    locales = compute_attrs()
    root = ET.Element("input-method", attrib={
        "xmlns:android": "http://schemas.android.com/apk/res/android",
        "android:settingsActivity": "juloo.keyboard2.SettingsActivity",
        "android:supportsSwitchingToNextInputMethod": "true",
        })
    root.append(ET.Comment(text=""" This file is automatically generated. DO NOT EDIT.
       Locales definitions should go into 'gen_method_xml.py'.
       Update this file with 'gradle test'.

  """))
    subtype_elem(root, DEFAULT_LOCALE)
    for loc in sorted(locales, key=lambda loc: loc["name"]):
        subtype_elem(root, loc)
    ET.indent(root)
    print(ET.tostring(root, encoding="utf-8", xml_declaration=True).decode("UTF-8"))

gen()
