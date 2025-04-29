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
import time # For rate limiting requests

LIBRETRANSLATE_URL = "http://localhost:5003"
# Add a small delay between translation requests to avoid overloading the server
REQUEST_DELAY = 0.5  # seconds

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
    "note_image_viewer_title", # e.g., Image %1$d/%2$d
    "macro_progress_g",  # Added these additional keys that might contain slashes
    "appointment_time_duration",
    "calories_progress",
    "vital_value_display",
    "time_dose_display"
}

# Add keys that should not be translated at all (brand names, technical terms, etc.)
UNTRANSLATED_KEYS = {
    "app_name",  # Keep app name consistent
    "doctor_prefix",  # Medical prefix
    "health_chat",  # Character name
    "test_dr_name"  # Test doctor name
}

# Android format specifier regex - more robust version
# Handles %s, %d, %f, %% and positional variants like %1$s, %2$d
ANDROID_PLACEHOLDER_REGEX = re.compile(r'(%(?:\d+\$)?[sdfe%])')

# Regex to find our temporary internal placeholders
INTERNAL_PLACEHOLDER_REGEX = re.compile(r'__PHMSPH(\d+)__')

# Regex to detect potentially problematic placeholder text like "key_name"
PLACEHOLDER_TEXT_REGEX = re.compile(r'^(\s*)"([a-zA-Z0-9_]+)"')

# Map certain terms to their preferred translations to ensure consistency
TERM_TRANSLATIONS = {
    "de": {  # German
        "Login": "Anmelden",
        "Biometric": "Biometrisch",
        "Doctor": "Arzt",
        "Error": "Fehler",
        "Heart Rate": "Herzfrequenz",
        "Blood Pressure": "Blutdruck",
        "Password": "Passwort",
        "Email": "E-Mail"
    },
    "es": {  # Spanish
        "Login": "Iniciar sesión",
        "Biometric": "Biométrico",
        "Doctor": "Médico",
        "Error": "Error",
        "Heart Rate": "Frecuencia cardíaca",
        "Blood Pressure": "Presión arterial",
        "Password": "Contraseña",
        "Email": "Correo electrónico"
    }
    # Add more languages as needed
}

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

def extract_placeholders(text):
    """Extract all Android placeholders from text, maintaining order."""
    if not isinstance(text, str) or not text:
        return []
    
    # Find all placeholder matches
    matches = ANDROID_PLACEHOLDER_REGEX.finditer(text)
    placeholders = []
    
    for match in matches:
        placeholders.append({
            'original': match.group(0),
            'position': match.start(),
            'length': len(match.group(0))
        })
    
    return placeholders

def apply_term_translations(text, target_lang):
    """Applies known term translations to ensure consistency."""
    if target_lang not in TERM_TRANSLATIONS:
        return text
    
    result = text
    for source_term, target_term in TERM_TRANSLATIONS[target_lang].items():
        # Case-insensitive replacement
        pattern = re.compile(re.escape(source_term), re.IGNORECASE)
        result = pattern.sub(target_term, result)
    
    return result

def translate_text(original_text, target_lang, source_lang="en", key=None):
    """Translates a single string, handling placeholders and ensuring correct format."""
    # Skip translation for untranslated keys or empty strings
    if key in UNTRANSLATED_KEYS:
        return original_text
        
    if not original_text or original_text.strip() == "":
        return ""
    
    # Extract placeholders before translation
    placeholders = extract_placeholders(original_text)
    
    # If no placeholders, translate directly
    if not placeholders:
        try:
            # Small delay to avoid overloading server
            time.sleep(REQUEST_DELAY)
            
            data = {"q": original_text, "source": source_lang, "target": target_lang, "format": "text"}
            response = requests.post(urljoin(LIBRETRANSLATE_URL, "/translate"), json=data, timeout=20)
            response.raise_for_status()
            translated = response.json().get("translatedText", "")
            
            # Apply known term translations for consistency
            translated = apply_term_translations(translated, target_lang)
            
            # Check for placeholder-like text in non-placeholder strings
            if PLACEHOLDER_TEXT_REGEX.match(translated):
                print(f"    Warning: Translation for '{key}' resulted in suspicious text: '{translated[:30]}...'. May need manual review.")
            
            return escape_android_string(translated)
        except requests.exceptions.RequestException as e:
            print(f"    Error translating simple text: '{original_text[:30]}...' ({e})")
            return escape_android_string(original_text)  # Fallback to original
        except Exception as e:
            print(f"    Unexpected error during simple translation: {e}")
            return escape_android_string(original_text)

    # ----- Handling text with placeholders -----
    
    # Replace placeholders with unique markers
    modified_text = original_text
    placeholder_map = {}
    
    # Sort placeholders by position in reverse order to avoid position shifts
    sorted_placeholders = sorted(placeholders, key=lambda p: p['position'], reverse=True)
    
    for i, ph in enumerate(sorted_placeholders):
        marker = f"__PHMSPH{i}__"
        start_pos = ph['position']
        end_pos = start_pos + ph['length']
        
        modified_text = modified_text[:start_pos] + marker + modified_text[end_pos:]
        placeholder_map[marker] = ph['original']
    
    # Translate the text with markers
    try:
        # Small delay to avoid overloading server
        time.sleep(REQUEST_DELAY)
        
        data = {"q": modified_text, "source": source_lang, "target": target_lang, "format": "text"}
        response = requests.post(urljoin(LIBRETRANSLATE_URL, "/translate"), json=data, timeout=20)
        response.raise_for_status()
        translated_with_markers = response.json().get("translatedText", "")
        
        # Apply known term translations for consistency
        translated_with_markers = apply_term_translations(translated_with_markers, target_lang)
        
        # Restore placeholders
        restored_translation = translated_with_markers
        for marker, original_ph in placeholder_map.items():
            if marker in restored_translation:
                restored_translation = restored_translation.replace(marker, original_ph)
            else:
                print(f"    Warning: Marker {marker} not found in translated text for key '{key}'. Adding placeholder at the end.")
                restored_translation += f" {original_ph}"
        
        # Special handling for specific format types (e.g., with slashes)
        if key in SPECIAL_FORMAT_KEYS_SLASH:
            restored_translation = correct_special_format(key, restored_translation, original_text, placeholders)
        
        # Final validation - count placeholders
        final_placeholders = extract_placeholders(restored_translation)
        if len(final_placeholders) != len(placeholders):
            print(f"    Warning: Placeholder count mismatch for key '{key}' (Expected {len(placeholders)}, Got {len(final_placeholders)})")
            print(f"    Original: '{original_text}'")
            print(f"    Translation: '{restored_translation}'")
            print(f"    Falling back to more conservative translation approach...")
            return conservative_placeholder_translation(original_text, target_lang, key, placeholders)
        
        return escape_android_string(restored_translation)
        
    except requests.exceptions.RequestException as e:
        print(f"    Error translating placeholder text: '{original_text[:30]}...' ({e})")
        return escape_android_string(original_text)  # Fallback to original
    except Exception as e:
        print(f"    Unexpected error during placeholder translation: {e}")
        return escape_android_string(original_text)  # Fallback to original

def conservative_placeholder_translation(original_text, target_lang, key, placeholders):
    """
    A more conservative approach for translating text with placeholders
    when the normal translation fails to preserve them correctly.
    """
    try:
        # Find text segments between placeholders
        segments = []
        last_end = 0
        
        sorted_placeholders = sorted(placeholders, key=lambda p: p['position'])
        
        for ph in sorted_placeholders:
            start_pos = ph['position']
            # Add text before placeholder if any
            if start_pos > last_end:
                segments.append({
                    'type': 'text',
                    'content': original_text[last_end:start_pos]
                })
            
            # Add placeholder
            segments.append({
                'type': 'placeholder',
                'content': ph['original']
            })
            
            last_end = start_pos + ph['length']
        
        # Add any remaining text after the last placeholder
        if last_end < len(original_text):
            segments.append({
                'type': 'text',
                'content': original_text[last_end:]
            })
        
        # Translate only the text segments
        for i, segment in enumerate(segments):
            if segment['type'] == 'text' and segment['content'].strip():
                # Small delay to avoid overloading server
                time.sleep(REQUEST_DELAY)
                
                data = {"q": segment['content'], "source": "en", "target": target_lang, "format": "text"}
                response = requests.post(urljoin(LIBRETRANSLATE_URL, "/translate"), json=data, timeout=20)
                response.raise_for_status()
                
                translated_segment = response.json().get("translatedText", "")
                translated_segment = apply_term_translations(translated_segment, target_lang)
                segments[i]['content'] = translated_segment
        
        # Reconstruct the full string
        result = ''.join(segment['content'] for segment in segments)
        
        return escape_android_string(result)
    
    except Exception as e:
        print(f"    Conservative translation failed for key '{key}': {e}")
        return escape_android_string(original_text)  # Ultimate fallback

def correct_special_format(key, translated_text, original_text, placeholders):
    """Applies specific format corrections for known problematic keys."""
    if key not in SPECIAL_FORMAT_KEYS_SLASH:
        return translated_text  # No correction needed
    
    # For blood pressure and similar format with slashes
    if len(placeholders) >= 2:
        # Find the placeholders in the original and translated text
        original_placeholders = extract_placeholders(original_text)
        translated_placeholders = extract_placeholders(translated_text)
        
        if len(original_placeholders) >= 2 and len(translated_placeholders) >= 2:
            # For the format like "%1$d/%2$d %3$s"
            # Get the first two placeholders
            ph1 = original_placeholders[0]['original']
            ph2 = original_placeholders[1]['original']
            
            # Check if they appear in the correct order in the translation
            if (translated_placeholders[0]['original'] == ph1 and 
                translated_placeholders[1]['original'] == ph2):
                
                # Make sure they have a slash between them
                first_pos = translated_text.find(ph1) + len(ph1)
                second_pos = translated_text.find(ph2, first_pos)
                
                if second_pos > first_pos:
                    between_text = translated_text[first_pos:second_pos]
                    if '/' not in between_text:
                        # Add slash if missing
                        corrected = (
                            translated_text[:first_pos] + 
                            '/' + 
                            translated_text[second_pos:]
                        )
                        print(f"    Corrected slash format for '{key}': Added missing slash")
                        return corrected
            
            # If the order is wrong or the format can't be easily corrected
            # Create a safe format based on the original
            if key == "bp_value_display" or "progress" in key:
                # Extract text before first placeholder
                prefix_end = original_text.find(original_placeholders[0]['original'])
                if prefix_end > 0:
                    prefix = original_text[:prefix_end].strip()
                else:
                    prefix = ""
                
                # Extract text after second placeholder
                suffix_start = original_text.find(original_placeholders[1]['original']) + len(original_placeholders[1]['original'])
                if suffix_start < len(original_text):
                    suffix = original_text[suffix_start:].strip()
                else:
                    suffix = ""
                
                # Translate just the prefix and suffix
                translated_prefix = ""
                if prefix:
                    try:
                        data = {"q": prefix, "source": "en", "target": target_lang, "format": "text"}
                        response = requests.post(urljoin(LIBRETRANSLATE_URL, "/translate"), json=data, timeout=10)
                        translated_prefix = response.json().get("translatedText", prefix).strip()
                    except:
                        translated_prefix = prefix
                
                translated_suffix = ""
                if suffix:
                    try:
                        data = {"q": suffix, "source": "en", "target": target_lang, "format": "text"}
                        response = requests.post(urljoin(LIBRETRANSLATE_URL, "/translate"), json=data, timeout=10)
                        translated_suffix = response.json().get("translatedText", suffix).strip()
                    except:
                        translated_suffix = suffix
                
                # Reconstruct with correct slash format
                if len(original_placeholders) == 2:
                    # Format: prefix PH1/PH2 suffix
                    corrected = f"{translated_prefix} {ph1}/{ph2}"
                    if translated_suffix:
                        corrected += f" {translated_suffix}"
                elif len(original_placeholders) > 2:
                    # Handle formats with more than 2 placeholders
                    ph3 = original_placeholders[2]['original']
                    corrected = f"{translated_prefix} {ph1}/{ph2} {ph3}"
                    if translated_suffix:
                        corrected += f" {translated_suffix}"
                
                print(f"    Applied special correction for '{key}'")
                return corrected
    
    # Return original if no correction could be applied
    return translated_text

def generate_xml(source_resources, existing_resources, lang_dir, lang_code):
    """Generates the translated strings.xml file."""
    output_file = os.path.join(lang_dir, "strings.xml")

    root = ET.Element("resources")
    comment = ET.Comment(f" Auto-translated with LibreTranslate - {SUPPORTED_LANGUAGES.get(lang_code, lang_code)} ")
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
                translated = translate_text(original_value, lang_code, key=name)
                # Final cleanup for escaped quotes that might come from translator
                final_text = translated.replace('&quot;', '\\"')
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
                 translated = translate_text(original_value, lang_code, key=name)
                 final_text = translated.replace('&quot;', '\\"')
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

        # Use minidom for pretty printing
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
    parser.add_argument(
        "--retranslate-all",
        action="store_true",
        help="Retranslate all strings, ignoring existing translations"
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
        
        # Load existing translations if we're not forcing retranslation
        existing_resources = {} if args.retranslate_all else load_existing_translations(existing_file)

        print(f"\nProcessing language: {lang} ({SUPPORTED_LANGUAGES.get(lang, 'Unknown')})...")
        if existing_resources:
            print(f"  Found {len(existing_resources)} existing resource entries in {existing_file}")
        elif args.retranslate_all:
            print(f"  Retranslating all strings (ignoring existing translations)")
        else:
            print(f"  No existing translations found, will create new file")

        output_file = generate_xml(source_resources, existing_resources, lang_dir, lang)
        if output_file:
            print(f"  Generated/Updated {output_file}")
        else:
            print(f"  Failed to generate file for language {lang}")

    print("\nTranslation process completed!")

if __name__ == "__main__":
    main()