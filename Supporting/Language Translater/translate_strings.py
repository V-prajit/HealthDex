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
    # Adding more major world languages
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

def extract_strings(xml_file):
    try:
        tree = ET.parse(xml_file)
        root = tree.getroot()

        strings = {}

        for string_elem in root.findall(".//string"):
            name = string_elem.get('name')
            value = string_elem.text if string_elem.text else ""
            strings[name] = value
        
        return strings
    except Exception as e:
        print(f"error extracting from {xml_file}: {e}")
        sys.exit(1)

def check_libretranslate():
    try:
        response = requests.get(urljoin(LIBRETRANSLATE_URL, "/languages"))

        if response.status_code != 200:
            print("LibreTranslate doesn't seem to be running correctly")
            sys.exit(1)   

        available_languages = {lang["code"]: lang["name"] for lang in response.json()}

        return available_languages
    
    except Exception as e:
        print(f"Error connecting to LibreTranslate: {e}")
        print(f"Please make sure LibreTranslate is running at {LIBRETRANSLATE_URL}")
        sys.exit(1)  

def translate_text(text, target_lang):
    if not text:
        return ""
    
    if text.startswith("%") and len(text.strip()) < 5:
        return text
    
    format_specifiers = []
    for match in re.finditer(r'(%\d+\$[a-z]|%[a-z])', text):
        format_specifiers.append((match.start(), match.end(), match.group()))

    modified_text = text
    for i in range(len(format_specifiers)-1, -1, -1):
        start, end, spec = format_specifiers[i]
        modified_text = modified_text[:start] + f"PLACEHOLDER_{i}" + modified_text[end:]

    data = {
        "q": modified_text,
        "source": "en",
        "target": target_lang,
        "format": "text",
        "api_key": ""       
    }

    try:
        response = requests.post(urljoin(LIBRETRANSLATE_URL, "/translate"), json=data)

        if response.status_code == 200:
            translated = response.json()["translatedText"]

            for i, (_, _, spec) in enumerate(format_specifiers):
                translated = translated.replace(f"PLACEHOLDER_{i}", spec)

            # Escape apostrophes for XML
            translated = escape_xml_apostrophes(translated)
            
            return translated
        
        else:
            print(f"Translation failed: {response.text}")
            return text 
    except Exception as e:
        print(f"Error translating text: {e}")
        return text
    
def escape_xml_apostrophes(text):
    """Escape apostrophes in text for XML compatibility"""
    # Replace unescaped apostrophes with escaped ones
    # But don't double-escape already escaped apostrophes
    result = ""
    i = 0
    while i < len(text):
        if text[i] == '\\' and i + 1 < len(text) and text[i+1] == "'":
            # Already escaped apostrophe
            result += "\\'"
            i += 2
        elif text[i] == "'":
            # Unescaped apostrophe
            result += "\\'"
            i += 1
        else:
            # Regular character
            result += text[i]
            i += 1
    return result
    
def load_existing_translations(file_path):
    if not os.path.exists(file_path):
        return {}
    
    try:
        tree = ET.parse(file_path)
        root = tree.getroot()
        
        existing = {}
        for string_elem in root.findall(".//string"):
            name = string_elem.get('name')
            value = string_elem.text if string_elem.text else ""
            existing[name] = value
        
        return existing
    except Exception as e:
        print(f"Warning: Error loading existing translations from {file_path}: {e}")
        return {}

def generate_xml(strings, existing_strings, lang_dir, lang_code):
    output_file = os.path.join(lang_dir, "strings.xml")
    
    # Create the XML structure
    root = ET.Element("resources")
    
    comment = ET.Comment(f" Auto-translated with LibreTranslate - {SUPPORTED_LANGUAGES.get(lang_code, lang_code)} ")
    root.append(comment)
    
    for name, value in strings.items():
        string_elem = ET.SubElement(root, "string")
        string_elem.set("name", name)
        
        if name in existing_strings:
            string_elem.text = escape_xml_apostrophes(existing_strings[name])
        else:
            string_elem.text = translate_text(value, lang_code)
    
    os.makedirs(lang_dir, exist_ok=True)
    
    # First create the XML as a string
    rough_string = ET.tostring(root, encoding='utf-8').decode('utf-8')
    
    # Create the pretty-formatted XML string 
    # We'll use minidom for formatting
    reparsed = minidom.parseString(rough_string)
    pretty_string = reparsed.toprettyxml(indent="    ", encoding='utf-8').decode('utf-8')
    
    # But minidom adds some unwanted newlines, let's clean them up
    lines = pretty_string.split('\n')
    cleaned_lines = []
    
    for line in lines:
        stripped = line.strip()
        if stripped and not (stripped.startswith('<?xml') and len(cleaned_lines) > 0):
            cleaned_lines.append(line)
    
    # Join the lines back together
    cleaned_xml = '\n'.join(cleaned_lines)
    
    # Write the formatted XML to file
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(cleaned_xml)
    
    return output_file

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
        help="Root resource directory"
    )
    parser.add_argument(
        "--languages", 
        nargs="+", 
        default=["fr", "es", "hi", "ar", "de", "zh", "pt", "ru"],
        help="Languages to translate to (language codes)"
    )
    
    args = parser.parse_args()
    
    if not os.path.exists(args.source):
        print(f"Error: Source file {args.source} does not exist.")
        sys.exit(1)
    
    available_languages = check_libretranslate()
    print(f"LibreTranslate is running with {len(available_languages)} available languages.")
    
    source_strings = extract_strings(args.source)
    print(f"Extracted {len(source_strings)} strings from {args.source}")
    
    for lang in args.languages:
        if lang not in available_languages:
            print(f"Warning: Language '{lang}' is not available in this LibreTranslate instance.")
            continue
        
        lang_dir = os.path.join(args.res_dir, f"values-{lang}")
        existing_file = os.path.join(lang_dir, "strings.xml")
        existing_strings = load_existing_translations(existing_file)
        
        print(f"Translating to {lang} ({available_languages[lang]})...")
        if existing_strings:
            print(f"Found {len(existing_strings)} existing translations in {existing_file}")
        
        output_file = generate_xml(source_strings, existing_strings, lang_dir, lang)
        print(f"Generated {output_file}")
    
    print("Translation completed!")

if __name__ == "__main__":
    main()
