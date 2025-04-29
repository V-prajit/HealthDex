# HealthDex/Supporting/Language Translater/translate_strings.py
import os
import re
import sys
import json
import argparse
import xml.etree.ElementTree as ET
import requests
from urllib.parse import urljoin
from xml.dom import minidom
import copy # Needed for deep copying attributes

LIBRETRANSLATE_URL = "http://localhost:5003"

SUPPORTED_LANGUAGES = {
    "en": "English",
    "es": "Spanish",
    "fr": "French",
    "hi": "Hindi",
    "ar": "Arabic",
    "bn": "Bengali",
    "zh": "Chinese",
    "de": "German",
    "id": "Indonesian",
    "it": "Italian",
    "ja": "Japanese",
    "ko": "Korean",
    "pt": "Portuguese",
    "ru": "Russian",
    "sw": "Swahili",
    "ur": "Urdu"
}

# Define the keys that need special format correction for the slash '/'
SPECIAL_FORMAT_KEYS_SLASH = {
    "calories_progress_short",
    "protein_progress_short",
    "fat_progress_short",
    "carb_progress_short",
    "bp_value_display", # e.g., %1$d/%2$d mmHg
    "note_image_viewer_title" # e.g., Image %1$d/%2$d
}

# Regex to find Android format specifiers
# Handles %s, %d, %f, %% and positional variants like %1$s, %2$d
ANDROID_PLACEHOLDER_REGEX = re.compile(r'%(\d+\$)?[sdfe%]')
# Regex to find our temporary internal placeholders
INTERNAL_PLACEHOLDER_REGEX = re.compile(r'__PHMSPH(\d+)__')
# Regex to detect potentially problematic placeholder text like "key_name"
PLACEHOLDER_TEXT_REGEX = re.compile(r'^(\s*)"([a-zA-Z0-9_]+)"')

def extract_resources(xml_file):
    """Extracts both <string> and <string-array> resources with attributes."""
    try:
        tree = ET.parse(xml_file)
        root = tree.getroot()
        resources = {}
        for string_elem in root.findall(".//string"):
            name = string_elem.get('name')
            if name:
                resources[name] = {
                    'value': string_elem.text if string_elem.text else "",
                    'attrib': dict(string_elem.attrib) # Store all attributes
                }
        for array_elem in root.findall(".//string-array"):
            name = array_elem.get('name')
            if name:
                items = [
                    {'value': item.text if item.text else "", 'attrib': dict(item.attrib)}
                    for item in array_elem.findall("item")
                ]
                resources[name] = {'items': items, 'attrib': dict(array_elem.attrib)}
        return resources
    except ET.ParseError as e:
        print(f"Error parsing XML file {xml_file}: {e}")
        sys.exit(1)
    except Exception as e:
        print(f"Error extracting resources from {xml_file}: {e}")
        sys.exit(1)

def load_existing_translations(file_path):
    """Loads existing translations, including attributes."""
    if not os.path.exists(file_path): return {}
    try: return extract_resources(file_path)
    except Exception as e:
        print(f"Warning: Error loading existing translations from {file_path}: {e}")
        return {}

def check_libretranslate():
    """Checks if LibreTranslate is running and gets available languages."""
    try:
        response = requests.get(urljoin(LIBRETRANSLATE_URL, "/languages"), timeout=10)
        response.raise_for_status()
        return {lang["code"]: lang["name"] for lang in response.json()}
    except requests.exceptions.RequestException as e:
        print(f"Error connecting to LibreTranslate at {LIBRETRANSLATE_URL}: {e}")
        print("Please ensure LibreTranslate is running (./setup_libretranslate.sh) and has had time to initialize.")
        sys.exit(1)
    except Exception as e:
        print(f"An unexpected error occurred while checking LibreTranslate: {e}")
        sys.exit(1)

def escape_android_string(text):
    """Escapes characters for Android strings.xml."""
    if not isinstance(text, str): return ""
    # Escape backslashes first, then other characters
    text = text.replace('\\', '\\\\')
    text = text.replace("'", "\\'")
    text = text.replace('"', '\\"')
    # Basic XML needs - often handled by XML library, but let's be safe for '&' within text
    text = text.replace('&', '&amp;')
    # Escape newlines and tabs
    text = text.replace('\n', '\\n')
    text = text.replace('\t', '\\t')
    # Escape @ and ? at the beginning of a string
    if text.startswith('@'):
        text = '\\' + text
    if text.startswith('?'):
        text = '\\' + text
    return text

def translate_text(original_text, target_lang, source_lang="en"):
    """Translates a single string, handling placeholders and attempting error correction."""
    if not original_text or original_text.strip() == "":
        return ""

    original_placeholders = ANDROID_PLACEHOLDER_REGEX.findall(original_text)
    original_placeholder_count = len(original_placeholders)

    # If no placeholders, translate directly
    if original_placeholder_count == 0:
        data = {"q": original_text, "source": source_lang, "target": target_lang, "format": "text"}
        try:
            response = requests.post(urljoin(LIBRETRANSLATE_URL, "/translate"), json=data, timeout=20)
            response.raise_for_status()
            translated = response.json().get("translatedText", "")
            # Basic check for placeholder-like text in non-placeholder strings
            if PLACEHOLDER_TEXT_REGEX.match(translated):
                 print(f"    Warning: Translation for non-placeholder string '{original_text[:30]}...' resulted in suspicious text: '{translated[:30]}...'. May need manual review.")
            return escape_android_string(translated)
        except requests.exceptions.RequestException as e:
            print(f"    Error translating simple text: '{original_text[:30]}...' ({e})")
            return escape_android_string(original_text) # Fallback to original
        except Exception as e:
            print(f"    Unexpected error during simple translation: {e}")
            return escape_android_string(original_text)

    # --- Placeholder Handling ---
    internal_placeholder_template = "__PHMSPH{}__"
    modified_text = original_text
    placeholders_map = {}
    original_specifiers_list = [match[0] for match in original_placeholders]

    # Replace standard placeholders with internal ones
    temp_marker_template = "__TEMP_MARKER_{}__"
    current_modified_text = modified_text
    for i in reversed(range(original_placeholder_count)):
        spec = original_specifiers_list[i]
        temp_marker = temp_marker_template.format(i)
        last_occurrence_index = current_modified_text.rfind(spec)
        if last_occurrence_index != -1:
            internal_placeholder = internal_placeholder_template.format(i)
            current_modified_text = current_modified_text[:last_occurrence_index] + temp_marker + current_modified_text[last_occurrence_index + len(spec):]
            placeholders_map[internal_placeholder] = spec
        else:
             print(f"    Warning: Specifier '{spec}' not found for replacement in '{original_text[:50]}...'")

    text_to_translate = current_modified_text
    for i in reversed(range(original_placeholder_count)):
         internal_placeholder = internal_placeholder_template.format(i)
         temp_marker = temp_marker_template.format(i)
         text_to_translate = text_to_translate.replace(temp_marker, internal_placeholder)

    # Translate text containing internal placeholders
    data = {"q": text_to_translate, "source": source_lang, "target": target_lang, "format": "text"}
    try:
        response = requests.post(urljoin(LIBRETRANSLATE_URL, "/translate"), json=data, timeout=20)
        response.raise_for_status()
        translated_with_internal = response.json().get("translatedText", "")

        # Restore original Android placeholders
        restored_translation = translated_with_internal
        found_internal_placeholders = INTERNAL_PLACEHOLDER_REGEX.findall(restored_translation)
        mapped_placeholders_sorted = sorted(placeholders_map.keys(), key=lambda x: int(re.search(r'\d+', x).group()), reverse=True)

        for internal_placeholder in mapped_placeholders_sorted:
            original_spec = placeholders_map[internal_placeholder]
            # Use regex for precise replacement
            restored_translation = re.sub(re.escape(internal_placeholder), lambda _: original_spec, restored_translation, count=1)

        # --- Validation and Fallback ---
        final_placeholders = ANDROID_PLACEHOLDER_REGEX.findall(restored_translation)
        if len(final_placeholders) != original_placeholder_count:
            print(f"    Warning: Placeholder count mismatch for '{original_text[:30]}...' (Expected {original_placeholder_count}, Got {len(final_placeholders)} in '{restored_translation[:50]}...'). Falling back to original English.")
            return escape_android_string(original_text) # Fallback to original

        return escape_android_string(restored_translation)

    except requests.exceptions.RequestException as e:
        print(f"    Error translating placeholder text: '{original_text[:30]}...' ({e})")
        return escape_android_string(original_text) # Fallback to original
    except Exception as e:
        print(f"    Unexpected error during placeholder translation: {e}")
        return escape_android_string(original_text) # Fallback to original

def correct_special_format(name, translated_text, original_text):
    """Applies specific format corrections for known problematic keys."""
    if name not in SPECIAL_FORMAT_KEYS_SLASH:
        return translated_text # No correction needed for this key

    # Find all original placeholders to ensure they are preserved
    original_placeholders = ANDROID_PLACEHOLDER_REGEX.findall(original_text)
    if len(original_placeholders) != 2: # Expecting two placeholders for these keys
        print(f"    Warning: Expected 2 placeholders for '{name}', found {len(original_placeholders)}. Skipping special correction.")
        return translated_text

    # Extract the actual specifiers (like %1$d, %2$d)
    spec1 = original_placeholders[0][0]
    spec2 = original_placeholders[1][0]

    # Attempt to extract the translated prefix robustly
    # Find the first placeholder in the translated text
    first_placeholder_match = ANDROID_PLACEHOLDER_REGEX.search(translated_text)
    if first_placeholder_match:
        prefix_end_index = first_placeholder_match.start()
        translated_prefix = translated_text[:prefix_end_index].strip()
    else:
        # Fallback: try to get prefix from original if translation lost placeholders
        original_prefix_match = re.match(r'^([^%]+)', original_text)
        translated_prefix = original_prefix_match.group(1).strip() if original_prefix_match else ""
        print(f"    Warning: Placeholders lost in translation for '{name}'. Using prefix '{translated_prefix}'.")


    # Reconstruct with the correct format and original specifiers
    corrected_format = f"{translated_prefix} {spec1}/{spec2}"
    print(f"    Correction Applied for '{name}': '{translated_text}' -> '{corrected_format}'")
    return corrected_format

def generate_xml(source_resources, existing_resources, lang_dir, lang_code):
    """Generates the translated strings.xml file."""
    output_file = os.path.join(lang_dir, "strings.xml")

    root = ET.Element("resources")
    comment = ET.Comment(f" Translated by script - {SUPPORTED_LANGUAGES.get(lang_code, lang_code)} ")
    root.append(comment)

    processed_names = set()

    # Prioritize existing keys to maintain order and attributes
    for name, existing_data in existing_resources.items():
        processed_names.add(name)
        source_data = source_resources.get(name)
        element_type = 'string' if 'value' in existing_data else 'string-array' if 'items' in existing_data else None

        if element_type == 'string':
            string_elem = ET.SubElement(root, element_type)
            # Restore attributes, ensuring 'name' is present
            current_attribs = copy.deepcopy(existing_data.get('attrib', {}))
            current_attribs['name'] = name # Ensure name is always there
            for k, v in current_attribs.items():
                string_elem.set(k, v)

            original_value = source_data.get('value') if source_data else None
            existing_value = existing_data.get('value')

            # Translate if source exists and differs from existing, or if it's a new source entry for an existing name
            should_translate = source_data and (original_value != existing_value)

            if should_translate:
                translated = translate_text(original_value, lang_code)
                corrected = correct_special_format(name, translated, original_value)
                # Final cleanup for escaped quotes that might come from translator
                final_text = corrected.replace('&quot;', '\\"')
                string_elem.text = final_text
            else:
                 # Keep existing, but ensure proper escaping
                 string_elem.text = escape_android_string(existing_value)

        elif element_type == 'string-array':
            array_elem = ET.SubElement(root, element_type)
            current_attribs = copy.deepcopy(existing_data.get('attrib', {}))
            current_attribs['name'] = name
            for k, v in current_attribs.items():
                array_elem.set(k, v)

            source_items = source_data.get('items', []) if source_data else []
            existing_items = existing_data.get('items', [])

            max_len = max(len(source_items), len(existing_items))
            for i in range(max_len):
                 item_elem = ET.SubElement(array_elem, "item")
                 existing_item_data = existing_items[i] if i < len(existing_items) else None
                 source_item_data = source_items[i] if i < len(source_items) else None

                 # Restore attributes
                 item_attribs = existing_item_data.get('attrib', {}) if existing_item_data else {}
                 for k, v in item_attribs.items():
                      item_elem.set(k,v)

                 original_item_value = source_item_data.get('value') if source_item_data else None
                 existing_item_value = existing_item_data.get('value') if existing_item_data else None

                 should_translate_item = source_item_data and (original_item_value != existing_item_value)

                 if should_translate_item:
                     item_elem.text = translate_text(original_item_value, lang_code).replace('&quot;', '\\"')
                 else:
                     # Keep existing, but ensure proper escaping
                     item_elem.text = escape_android_string(existing_item_value)

    # Add new keys from source
    for name, source_data in source_resources.items():
        if name not in processed_names:
            element_type = 'string' if 'value' in source_data else 'string-array' if 'items' in source_data else None
            if element_type == 'string':
                 string_elem = ET.SubElement(root, element_type)
                 current_attribs = copy.deepcopy(source_data.get('attrib', {}))
                 current_attribs['name'] = name
                 for k, v in current_attribs.items():
                     string_elem.set(k, v)
                 original_value = source_data.get('value')
                 translated = translate_text(original_value, lang_code)
                 corrected = correct_special_format(name, translated, original_value)
                 final_text = corrected.replace('&quot;', '\\"')
                 string_elem.text = final_text

            elif element_type == 'string-array':
                 array_elem = ET.SubElement(root, element_type)
                 current_attribs = copy.deepcopy(source_data.get('attrib', {}))
                 current_attribs['name'] = name
                 for k, v in current_attribs.items():
                     array_elem.set(k, v)
                 for item_data in source_data.get('items', []):
                     item_elem = ET.SubElement(array_elem, "item")
                     item_attribs = item_data.get('attrib', {})
                     for k, v in item_attribs.items():
                          item_elem.set(k,v)
                     item_elem.text = translate_text(item_data.get('value', ""), lang_code).replace('&quot;', '\\"')


    # --- Write XML ---
    os.makedirs(lang_dir, exist_ok=True)
    try:
        # Use ElementTree to write, which handles basic XML entities but not Android specifics
        tree = ET.ElementTree(root)
        # Write to a temporary string first to replace escaped characters
        # ET automatically escapes &, <, > but we need specific handling for ' and "
        xml_string_bytes = ET.tostring(root, encoding='utf-8', method='xml')
        xml_string = xml_string_bytes.decode('utf-8')

        # Use minidom for pretty printing (optional, but nice)
        try:
            reparsed = minidom.parseString(xml_string)
            pretty_xml = reparsed.toprettyxml(indent="    ", encoding='utf-8').decode('utf-8')

            # Clean up extra newlines potentially added by minidom
            cleaned_lines = [line for line in pretty_xml.split('\n') if line.strip()]
            final_xml_lines = []
            if cleaned_lines and cleaned_lines[0].startswith("<?xml"):
                final_xml_lines.append(cleaned_lines[0])
                final_xml_lines.extend(cleaned_lines[1:])
            else:
                final_xml_lines.extend(cleaned_lines)

            if len(final_xml_lines) > 1 and final_xml_lines[-1].strip() == "</resources>":
                final_xml_lines.insert(len(final_xml_lines) - 1, "") # Add empty line before

            final_xml_string = '\n'.join(final_xml_lines)

        except Exception as parse_err:
             print(f"    Warning: minidom pretty printing failed ({parse_err}). Writing raw XML.")
             final_xml_string = xml_string # Fallback to raw string

        with open(output_file, 'w', encoding='utf-8') as f:
            f.write(final_xml_string)
        return output_file

    except Exception as e:
        print(f"  Error generating XML for {lang_code}: {e}")
        return None


def main():
    parser = argparse.ArgumentParser(
        description="Translate Android string resources using a local LibreTranslate instance"
    )
    parser.add_argument(
        "--source",
        default="PHMS-Android/app/src/main/res/values/strings.xml",
        help="Path to source (English) strings.xml file"
    )
    parser.add_argument(
        "--res-dir",
        default="PHMS-Android/app/src/main/res",
        help="Root resource directory (e.g., app/src/main/res)"
    )
    parser.add_argument(
        "--languages",
        nargs="+",
        default=list(SUPPORTED_LANGUAGES.keys() - {'en'}),
        help="Languages to translate to (language codes)"
    )

    args = parser.parse_args()

    # Determine the absolute paths based on the script's location if needed
    script_dir = os.path.dirname(os.path.realpath(__file__))
    default_source_path = os.path.join(script_dir, '..', '..', 'PHMS-Android', 'app', 'src', 'main', 'res', 'values', 'strings.xml')
    default_res_dir = os.path.join(script_dir, '..', '..', 'PHMS-Android', 'app', 'src', 'main', 'res')

    source_path = args.source if os.path.isabs(args.source) else os.path.normpath(os.path.join(script_dir, args.source))
    res_dir_path = args.res_dir if os.path.isabs(args.res_dir) else os.path.normpath(os.path.join(script_dir, args.res_dir))

    # Use default paths if the provided ones don't exist, but check defaults too
    if not os.path.isfile(source_path):
        print(f"Warning: Specified source '{args.source}' not found at '{source_path}'. Trying default path.")
        source_path = default_source_path
        if not os.path.isfile(source_path):
             print(f"Error: Default source file '{source_path}' not found either.")
             sys.exit(1)
        else:
             print(f"Using default source path: '{source_path}'")


    if not os.path.isdir(res_dir_path):
        print(f"Warning: Specified res-dir '{args.res_dir}' not found at '{res_dir_path}'. Trying default path.")
        res_dir_path = default_res_dir
        if not os.path.isdir(res_dir_path):
             print(f"Error: Default resource directory '{res_dir_path}' not found either.")
             sys.exit(1)
        else:
            print(f"Using default resource directory: '{res_dir_path}'")

    available_languages = check_libretranslate()
    print(f"LibreTranslate is running with {len(available_languages)} available languages.")

    source_resources = extract_resources(source_path)
    print(f"Extracted {len(source_resources)} resource entries from {source_path}")

    valid_target_languages = [lang for lang in args.languages if lang in available_languages and lang != 'en']

    if not valid_target_languages:
         print("No valid target languages available/selected in LibreTranslate instance. Exiting.")
         sys.exit(0)

    print(f"Target languages: {', '.join(valid_target_languages)}")

    for lang in valid_target_languages:
        lang_dir = os.path.join(res_dir_path, f"values-{lang}")
        existing_file = os.path.join(lang_dir, "strings.xml")
        existing_resources = load_existing_translations(existing_file)

        print(f"\nProcessing language: {lang} ({SUPPORTED_LANGUAGES.get(lang, 'Unknown')})...")
        if existing_resources:
            print(f"  Found {len(existing_resources)} existing resource entries in {existing_file}")

        output_file = generate_xml(source_resources, existing_resources, lang_dir, lang)
        if output_file:
            print(f"  Generated/Updated {output_file}")
        else:
            print(f"  Failed to generate file for language {lang}")

    print("\nTranslation process completed!")

if __name__ == "__main__":
    main()