#!/bin/bash

# Style element example:
# <!-- THEMES_START -->
# ...
# <style name="AtomOneDark" parent="@style/BaseTheme">
#   <item name="android:isLightTheme">false</item>
#   <item name="colorKeyboard">#282c34</item>
#   <item name="colorKey">#3f4451</item>
#   <item name="colorKeyActivated">#5c6370</item>
#   <item name="colorLabel">#abb2bf</item>
#   <item name="colorLabelActivated">#e06c75</item>
#   <item name="colorLabelLocked">#98c379</item>
#   <item name="colorSubLabel">#61afef</item>
#   <item name="keyBorderWidth">0.0dip</item>
#   <item name="keyBorderWidthActivated">0.0dip</item>
#   <item name="keyBorderColorLeft">#00f0f0f0</item>
#   <item name="keyBorderColorTop">#00f0f0f0</item>
#   <item name="keyBorderColorRight">#00eeeeee</item>
#   <item name="keyBorderColorBottom">#00eeeeee</item>
#   <item name="emoji_button_bg">?colorKeyActivated</item>
#   <item name="emoji_color">#abb2bf</item>
# </style>
# ...
# <!-- THEMES_END -->

# Example of themes.json file:
# [
#   {
#       "name": "Solarized",
#       "display_name": "Solarized",
#       "parent_theme": "BaseTheme",
#       "is_light_theme": false,
#       "attribs": {
#           "colorKeyboard": "#282c34",
#           "colorKey": "#3f4451",
#           "colorKeyActivated": "#5c6370",
#           "colorLabel": "#abb2bf",
#           "colorLabelActivated": "#e06c75",
#           "colorLabelLocked": "#98c379",
#           "colorSubLabel": "#61afef",
#           "keyBorderWidth": "0.0dip",
#           "keyBorderWidthActivated": "0.0dip",
#           "keyBorderColorLeft": "#00f0f0f0",
#           "keyBorderColorTop": "#00f0f0f0",
#           "keyBorderColorRight": "#00eeeeee",
#           "keyBorderColorBottom": "#00eeeeee",
#           "emoji_button_bg": "?colorKeyActivated",
#           "emoji_color": "#abb2bf"
#       }
#   },
#   ...
# ]

# Example of the ../res/values/strings.xml file:
# <!-- THEMES_STRINGS_START -->
# ...
# <string name="pref_theme_e_atomonedark">Atom One Dark</string>
# ...
# <!-- THEMES_STRINGS_END -->

# Example of the ../res/values/arrays.xml file:
# <!-- THEMES_ENTRIES_ARRAY_START -->
# ...
# <item>@string/pref_theme_e_system</item>
# ...
# <!-- THEMES_ENTRIES_ARRAY_END -->
# ...
# <!-- THEMES_VALUES_ARRAY_START -->
# ...
# <item>system</item>
# ...
# <!-- THEMES_VALUES_ARRAY_END -->

# Example of the ../srcs/juloo.keyboard2/Config.java file:
# // THEMES_SWITCH_CASE_START
# ...
# case "atomonedark": return R.style.AtomOneDark;
# ...
# // THEMES_SWITCH_CASE_END

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

debug=false

# Files
themes_xml_file="$here/../res/values/themes.xml"
strings_xml_file="$here/../res/values/strings.xml"
arrays_xml_file="$here/../res/values/arrays.xml"
config_java_file="$here/../srcs/juloo.keyboard2/Config.java"

add_theme() {
    theme_config="$1"

    echo "Adding theme with attributes:" "$theme_config"

    new_theme_name="$(jq -r '.name' <<<"$theme_config")"
    new_theme_display_name="$(jq -r '.display_name' <<<"$theme_config")"
    new_theme_lower_name="${new_theme_name,,}"
    new_theme_parent_theme="$(jq -r '.parent_theme' <<<"$theme_config")"
    new_theme_is_light_theme="$(jq -r '.is_light_theme' <<<"$theme_config")"
    new_theme_pref_name="pref_theme_e_$new_theme_lower_name"
    new_theme_entry="@string/$new_theme_pref_name"
    new_theme_attribs=(
        "android:isLightTheme=$new_theme_is_light_theme"
    )

    # Pupulating the new_theme_attribs array
    for key in $(jq -r '.attribs | keys[]' <<<"$theme_config"); do
        new_theme_attribs+=("$key=$(jq -r ".attribs[\"$key\"]" <<<"$theme_config")")
    done

    # Construct the themes.xml snippet for the new theme
    new_theme_xml="\ \ <style name=\"$new_theme_name\" parent=\"$new_theme_parent_theme\">"
    for attrib in "${new_theme_attribs[@]}"; do
        new_theme_xml+="\n    <item name=\"${attrib%%=*}\">${attrib#*=}</item>"
    done
    new_theme_xml+="\n  </style>"

    # Construct the new theme's theme string item for the strings.xml file
    new_theme_string_xml="\ \ <string name=\"$new_theme_pref_name\">$new_theme_display_name</string>"

    # Construct the new theme's entry and value for the strings.xml and arrays.xml file
    new_theme_entry_xml="\ \ \ \ <item>$new_theme_entry</item>"
    new_theme_value_xml="\ \ \ \ <item>$new_theme_lower_name</item>"

    # Construct the new theme's case for the Config.java file
    new_theme_case="\ \ \ \ \ \ case \"$new_theme_lower_name\": return R.style.$new_theme_name;"

    if $debug; then
        printf "new_theme_name: %s\n\n" "$new_theme_name"
        printf "new_theme_display_name: %s\n\n" "$new_theme_display_name"
        printf "new_theme_lower_name: %s\n\n" "$new_theme_lower_name"
        printf "new_parent_theme: %s\n\n" "$new_parent_theme"
        printf "new_theme_is_light_theme: %s\n\n" "$new_theme_is_light_theme"
        printf "new_theme_pref_name: %s\n\n" "$new_theme_pref_name"
        printf "new_theme_entry: %s\n\n" "$new_theme_entry"
        printf "new_theme_attribs: %s\n\n" "${new_theme_attribs[@]}"
        printf "new_theme_xml: %s\n\n" "$new_theme_xml"
        printf "new_theme_string_xml: %s\n\n" "$new_theme_string_xml"
        printf "new_theme_entry_xml: %s\n\n" "$new_theme_entry_xml"
        printf "new_theme_value_xml: %s\n\n" "$new_theme_value_xml"
        printf "new_theme_case: %s\n\n" "$new_theme_case"
    else
        # Add the new theme to the themes.xml file
        sed -i "/<!-- THEMES_END -->/i $new_theme_xml" "$themes_xml_file"

        # Add the new theme to the strings.xml file
        sed -i "/<!-- THEMES_STRINGS_END -->/i $new_theme_string_xml" "$strings_xml_file"

        # Add the new theme's entry and value to the arrays.xml file
        sed -i "/<!-- THEMES_ENTRIES_ARRAY_END -->/i $new_theme_entry_xml" "$arrays_xml_file"
        sed -i "/<!-- THEMES_VALUES_ARRAY_END -->/i $new_theme_value_xml" "$arrays_xml_file"

        # Add the new theme to the Config.java file
        sed -i "/\/\/ THEMES_SWITCH_CASE_END/i $new_theme_case" "$config_java_file"
    fi
}

if $debug; then
    echo "Clearing the themes.xml file's content between the THEMES_START and THEMES_END comments"
    echo "Clearing the strings.xml file's content between the THEMES_STRINGS_START and THEMES_STRINGS_END comments"
    echo "Clearing the arrays.xml file's content between the THEMES_ENTRIES_ARRAY_START and THEMES_ENTRIES_ARRAY_END comments"
    echo "Clearing the Config.java file's content between the THEMES_SWITCH_CASE_START and THEMES_SWITCH_CASE_END comments"
else
    # Clear the themes.xml file's content between the THEMES_START and THEMES_END comments
    sed -i '/<!-- THEMES_START -->/,/<!-- THEMES_END -->/{//!d}' "$themes_xml_file"

    # Clear the strings.xml file's content between the THEMES_STRINGS_START and THEMES_STRINGS_END comments
    sed -i '/<!-- THEMES_STRINGS_START -->/,/<!-- THEMES_STRINGS_END -->/{//!d}' "$strings_xml_file"

    # Clear the arrays.xml file's content between the THEMES_ENTRIES_ARRAY_START and THEMES_ENTRIES_ARRAY_END comments
    sed -i '/<!-- THEMES_ENTRIES_ARRAY_START -->/,/<!-- THEMES_ENTRIES_ARRAY_END -->/{//!d}' "$arrays_xml_file"
    sed -i '/<!-- THEMES_VALUES_ARRAY_START -->/,/<!-- THEMES_VALUES_ARRAY_END -->/{//!d}' "$arrays_xml_file"

    # Clear the Config.java file's content between the THEMES_SWITCH_CASE_START and THEMES_SWITCH_CASE_END comments
    sed -i '/\/\/ THEMES_SWITCH_CASE_START/,/\/\/ THEMES_SWITCH_CASE_END/{//!d}' "$config_java_file"
fi

# Main script logic
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
themes_config_file="$here/themes.json"

# Iterate over each object in the JSON array
jq -c '.[]' "$themes_config_file" | while IFS= read -r theme_obj; do
    echo "Processing object:"
    echo "$theme_obj"    
    add_theme "$theme_obj"
done