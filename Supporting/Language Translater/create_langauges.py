# create_language_dirs.py
import os
import shutil

# Path to your resource directory
RES_DIR = "../../PHMS-Android/app/src/main/res"

# Languages to add
LANGUAGES = [
    "ar", "bn", "zh", "de", "id", "it", "ja", "ko", "pt", "ru", "sw", "ur"
]

# Create directories
for lang in LANGUAGES:
    dir_path = os.path.join(RES_DIR, f"values-{lang}")
    if not os.path.exists(dir_path):
        os.makedirs(dir_path)
        print(f"Created directory: {dir_path}")
    else:
        print(f"Directory already exists: {dir_path}")
        
    # Create an empty strings.xml file
    strings_path = os.path.join(dir_path, "strings.xml")
    if not os.path.exists(strings_path):
        with open(strings_path, 'w', encoding='utf-8') as f:
            f.write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <!-- Auto-translated placeholder -->\n</resources>')
        print(f"Created placeholder file: {strings_path}")
