import os
import re
import sys
import json
import argparse
import xml.etree.ElementTree as ET
import requests
from urllib.parse import urljoin
from xml.dom import minidom

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

def extract_resources(xml_file):
    """Extracts both <string> and <string-array> resources."""
    try:
        tree = ET.parse(xml_file)
        root = tree.getroot()

        resources = {}

        for string_elem in root.findall(".//string"):
            name = string_elem.get('name')
            value = string_elem.text if string_elem.text else ""
            if name:
                resources[name] = value

        for array_elem in root.findall(".//string-array"):
            name = array_elem.get('name')
            items = [item.text if item.text else "" for item in array_elem.findall("item")]
            if name:
                resources[name] = items

        return resources
    except ET.ParseError as e:
        print(f"Error parsing XML file {xml_file}: {e}")
        sys.exit(1)
    except Exception as e:
        print(f"Error extracting resources from {xml_file}: {e}")
        sys.exit(1)

def load_existing_translations(file_path):
    """Loads existing translations, including string arrays."""
    if not os.path.exists(file_path):
        return {}

    try:
        return extract_resources(file_path)
    except Exception as e:
        print(f"Warning: Error loading existing translations from {file_path}: {e}")
        return {}


def check_libretranslate():
    """Checks if LibreTranslate is running and gets available languages."""
    try:
        response = requests.get(urljoin(LIBRETRANSLATE_URL, "/languages"))
        response.raise_for_status()
        available_languages = {lang["code"]: lang["name"] for lang in response.json()}
        return available_languages
    except requests.exceptions.RequestException as e:
        print(f"Error connecting to LibreTranslate at {LIBRETRANSLATE_URL}: {e}")
        print("Please ensure LibreTranslate is running. You might need to run setup_libretranslate.sh")
        sys.exit(1)
    except Exception as e:
        print(f"An unexpected error occurred while checking LibreTranslate: {e}")
        sys.exit(1)

def translate_text(text, target_lang, source_lang="en"):
    """Translates a single string, handling placeholders and escaping."""
    if not text or text.strip() == "":
        return ""

    # Simple check to avoid translating only placeholders like %1$s
    if text.strip().startswith("%") and not any(c.isalpha() for c in text):
         return escape_xml_apostrophes(text)

    # --- More robust placeholder ---
    placeholder_template = "__PHMSPH{}__" # Placeholder format like __PHMSPH0__, __PHMSPH1__

    # Find format specifiers like %1$s, %d, etc.
    specifiers = re.findall(r'(%(\d+\$)?[sdfe%])', text)
    original_specifiers = [match[0] for match in specifiers] # Store the original specifiers found

    modified_text = text
    placeholders_map = {} # To map placeholder back to original specifier

    # Replace specifiers with unique placeholders from end to start
    for i, spec in reversed(list(enumerate(original_specifiers))):
        placeholder = placeholder_template.format(i)
        # Use a temporary unique marker to avoid replacing already replaced parts
        temp_marker = f"__TEMP_MARKER_{i}__"
        modified_text = modified_text.replace(spec, temp_marker, 1)
        placeholders_map[placeholder] = spec # Store mapping

    # Now replace temporary markers with final placeholders
    for i, spec in reversed(list(enumerate(original_specifiers))):
         placeholder = placeholder_template.format(i)
         temp_marker = f"__TEMP_MARKER_{i}__"
         modified_text = modified_text.replace(temp_marker, placeholder)


    # --- Send text with robust placeholders to API ---
    data = {
        "q": modified_text,
        "source": source_lang,
        "target": target_lang,
        "format": "text",
        "api_key": ""
    }

    try:
        response = requests.post(urljoin(LIBRETRANSLATE_URL, "/translate"), json=data)
        response.raise_for_status()

        translated = response.json().get("translatedText", "")

        # --- Restore original specifiers using the map ---
        # Iterate through placeholders found in the translated text
        # Use regex to find placeholders like __PHMSPH<number>__
        # Use sorted to replace higher index placeholders first, avoid conflicts
        found_placeholders = sorted(
            re.findall(r'(__PHMSPH(\d+)__)', translated),
            key=lambda x: int(x[1]), # Sort by the number inside placeholder
            reverse=True
        )

        for full_placeholder, index_str in found_placeholders:
            original_spec = placeholders_map.get(full_placeholder)
            if original_spec:
                # Replace the found placeholder (e.g., __PHMSPH0__) with its original specifier (e.g., %1$s)
                translated = translated.replace(full_placeholder, original_spec, 1)
            else:
                 print(f"Warning: Found placeholder {full_placeholder} in translation but no original specifier mapped.")


        return escape_xml_apostrophes(translated)

    except requests.exceptions.RequestException as e:
        print(f"Error translating text: '{text[:50]}...' ({e})")
        return escape_xml_apostrophes(text)
    except Exception as e:
        print(f"Unexpected error during translation: {e}")
        return escape_xml_apostrophes(text)

def escape_xml_apostrophes(text):
    """Escapes apostrophes and other necessary XML characters."""
    if not isinstance(text, str): return ""
    text = text.replace("&", "&amp;")
    text = text.replace("<", "&lt;")
    text = text.replace(">", "&gt;")
    text = text.replace("\"", "&quot;")
    text = text.replace("'", "\\'")
    return text

def generate_xml(source_resources, existing_resources, lang_dir, lang_code):
    """Generates the translated strings.xml file, handling both strings and arrays."""
    output_file = os.path.join(lang_dir, "strings.xml")

    root = ET.Element("resources")
    comment = ET.Comment(f" Auto-translated with LibreTranslate - {SUPPORTED_LANGUAGES.get(lang_code, lang_code)} ")
    root.append(comment)

    for name, value in source_resources.items():
        if isinstance(value, str): # It's a <string>
            string_elem = ET.SubElement(root, "string")
            string_elem.set("name", name)
            if name in existing_resources and isinstance(existing_resources[name], str):
                 # Use existing translation if it's also a string
                string_elem.text = escape_xml_apostrophes(existing_resources[name])
            else:
                 # Translate if new or type mismatch in existing
                string_elem.text = translate_text(value, lang_code)

        elif isinstance(value, list): # It's a <string-array>
            array_elem = ET.SubElement(root, "string-array")
            array_elem.set("name", name)
            existing_items = existing_resources.get(name) if isinstance(existing_resources.get(name), list) else []

            for index, item_text in enumerate(value):
                item_elem = ET.SubElement(array_elem, "item")
                if index < len(existing_items):
                    # Use existing item translation if available at the same index
                    item_elem.text = escape_xml_apostrophes(existing_items[index])
                else:
                    # Translate new item
                    item_elem.text = translate_text(item_text, lang_code)

    os.makedirs(lang_dir, exist_ok=True)

    # Use minidom for pretty printing
    try:
        rough_string = ET.tostring(root, encoding='utf-8')
        reparsed = minidom.parseString(rough_string)
        pretty_string = reparsed.toprettyxml(indent="    ", encoding='utf-8').decode('utf-8')

        # Basic cleanup of minidom's extra newlines
        cleaned_lines = [line for line in pretty_string.split('\n') if line.strip()]
        # Ensure <?xml...> is first if present, then add newline
        if cleaned_lines and cleaned_lines[0].startswith("<?xml"):
            cleaned_xml = cleaned_lines[0] + '\n' + '\n'.join(cleaned_lines[1:])
        else:
             cleaned_xml = '\n'.join(cleaned_lines)


        with open(output_file, 'w', encoding='utf-8') as f:
            f.write(cleaned_xml)
        return output_file

    except Exception as e:
        print(f"Error generating XML for {lang_code}: {e}")
        # Fallback to writing the unformatted string if pretty print fails
        try:
             with open(output_file, 'wb') as f: # Write bytes directly
                f.write(rough_string)
             print(f"Wrote unformatted XML to {output_file} due to formatting error.")
             return output_file
        except Exception as write_e:
             print(f"FATAL: Could not write XML file {output_file}: {write_e}")
             return None


def main():
    parser = argparse.ArgumentParser(
        description="Translate Android string resources (including arrays) using a local LibreTranslate instance"
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
        default=list(SUPPORTED_LANGUAGES.keys() - {'en'}), # Default to all supported except English
        help="Languages to translate to (language codes)"
    )

    args = parser.parse_args()

    if not os.path.isfile(args.source):
        print(f"Error: Source file '{args.source}' not found or is not a file.")
        sys.exit(1)
    if not os.path.isdir(args.res_dir):
        print(f"Error: Resource directory '{args.res_dir}' not found or is not a directory.")
        sys.exit(1)

    available_languages = check_libretranslate()
    print(f"LibreTranslate is running with {len(available_languages)} available languages.")

    source_resources = extract_resources(args.source)
    print(f"Extracted {len(source_resources)} resources (strings and arrays) from {args.source}")

    valid_target_languages = [lang for lang in args.languages if lang in available_languages and lang != 'en']

    if not valid_target_languages:
         print("No valid target languages available in LibreTranslate instance. Exiting.")
         sys.exit(0)

    print(f"Target languages: {', '.join(valid_target_languages)}")

    for lang in valid_target_languages:
        lang_dir = os.path.join(args.res_dir, f"values-{lang}")
        existing_file = os.path.join(lang_dir, "strings.xml")
        existing_resources = load_existing_translations(existing_file)

        print(f"\nProcessing language: {lang} ({SUPPORTED_LANGUAGES.get(lang, 'Unknown')})...")
        if existing_resources:
            print(f"Found {len(existing_resources)} existing resources in {existing_file}")

        output_file = generate_xml(source_resources, existing_resources, lang_dir, lang)
        if output_file:
            print(f"Generated/Updated {output_file}")
        else:
            print(f"Failed to generate file for language {lang}")

    print("\nTranslation process completed!")

if __name__ == "__main__":
    main()