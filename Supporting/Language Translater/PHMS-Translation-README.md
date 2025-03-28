PHMS Translation System
This system allows you to automatically translate your PHMS app's string resources using LibreTranslate, a free and open-source machine translation API.
Overview
The translation system consists of:

A local LibreTranslate server running in Docker
A Python script that extracts strings, translates them, and generates resource files
Enhancements to the app's LocaleHelper class to detect missing translations

Setup Instructions
Prerequisites

Docker installed on your system
Python 3.6+ with the requests module installed
Your PHMS project code

Step 1: Set up LibreTranslate
Run the provided setup script to start LibreTranslate in a Docker container:
bashCopychmod +x setup_libretranslate.sh
./setup_libretranslate.sh
This will:

Check if Docker is installed
Pull the LibreTranslate Docker image (if not already present)
Start LibreTranslate on port 5000

You can verify it's running by visiting http://localhost:5000 in your browser.
Step 2: Translate String Resources
Run the translation script to translate your app's string resources:
bashCopy# Install required Python packages
pip install requests

# Run the script with default settings
python translate_strings.py

# Or specify custom paths and languages
python translate_strings.py --source path/to/strings.xml --res-dir path/to/res --languages fr es hi
The script will:

Extract strings from your default (English) strings.xml file
Preserve any existing translations from previous runs
Translate only new or changed strings
Generate or update the language-specific string resource files

Key Features
Preserving Format Specifiers
The translation script properly handles Android string format specifiers (like %1$s or %d), ensuring they remain intact after translation.
Preserving Existing Translations
If you've manually edited any translations, the script will preserve those edits and only translate new or modified strings.
Missing Translation Detection
The enhanced LocaleHelper includes methods to detect and log missing translations at runtime. To use this feature, add the following code to your app's startup:
kotlinCopy// Add this in MainActivity.onCreate() or Application.onCreate()
LocaleHelper.logMissingTranslations(context)
This will log any missing translations for the current locale to help you identify strings that need translation.
Translation Status UI
To view the status of your translations in the app, you can add a translation status screen to your settings:
kotlinCopy@Composable
fun TranslationStatusScreen(context: Context) {
    val status = remember { LocaleHelper.getTranslationStatusReport(context) }
    
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Translation Status", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        for ((code, stats) in status) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "${stats.language} (${code})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    LinearProgressIndicator(
                        progress = (stats.percentage / 100).toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                    Text(
                        "${stats.translatedCount}/${stats.translatedCount + stats.missingCount} " +
                        "strings translated (${stats.percentage.toInt()}%)"
                    )
                }
            }
        }
    }
}
Best Practices

Review Machine Translations: While LibreTranslate provides decent translations, always review machine-translated strings, especially for important UI elements.
Regular Updates: Run the translation script regularly as you add new strings to keep all language resources up-to-date.
Monitor Missing Translations: Use the logMissingTranslations method during development to identify untranslated strings.
Testing: Test your app with different languages to ensure layouts work well with translated text (which may be longer or shorter than English).

Troubleshooting
LibreTranslate Issues
If LibreTranslate isn't working:

Verify it's running: docker ps | grep libretranslate
Check logs: docker logs libretranslate
Restart it: docker restart libretranslate

Translation Script Issues

Make sure your source paths are correct
Check for XML syntax errors in your strings.xml file
Ensure proper permissions for writing to the output directories

Extending the System
Add More Languages
To add support for more languages:

Update the SUPPORTED_LANGUAGES in both the translation script and LocaleHelper.kt
Add new language resource directories (e.g., values-de for German)
Run the translation script with the new language code

Integration with CI/CD
You can integrate the translation process into your CI/CD pipeline:

Add the translation script to your repository
Create a GitHub Action or other CI job that runs the script when strings.xml changes
Commit the generated translations automatically

Notes on Translation Quality
LibreTranslate is based on the open-source Argos Translate library, which provides good but not perfect translations. For professional-grade translations, consider:

Using the machine translations as a starting point
Having native speakers review and correct the translations
For critical text, using a professional translation service