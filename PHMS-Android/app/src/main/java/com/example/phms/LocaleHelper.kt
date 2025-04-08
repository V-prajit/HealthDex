package com.example.phms

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale
import android.content.res.Resources
import android.util.Log
import java.lang.reflect.Field

object LocaleHelper {
    val supportedLanguages = listOf(
        SupportedLanguage("en", "English"),
        SupportedLanguage("es", "Español"),
        SupportedLanguage("fr", "Français"),
        SupportedLanguage("hi", "हिन्दी")
    )

    fun applyLanguage(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        val localeList = LocaleListCompat.create(locale)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun applyLanguageWithoutRecreation(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        val localeList = LocaleListCompat.create(locale)
        AppCompatDelegate.setApplicationLocales(localeList)

        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("selected_language", languageCode).apply()

        if (context is MainActivity) {
            context.forceLocaleRecomposition(languageCode)
        }
    }

    fun getCurrentLanguageCode(context: Context): String {
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        return if (currentLocales.isEmpty) {
            getSystemLanguageCode(context)
        } else {
            currentLocales.get(0)?.language ?: "en"
        }
    }

    fun getSystemLanguageCode(context: Context): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0).language
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale.language
        }
    }

    fun checkMissingTranslations(context: Context): List<String> {
        val currentLang = getCurrentLanguageCode(context)
        if (currentLang == "en") return emptyList()

        val missingKeys = mutableListOf<String>()
        val defaultResources = Resources(
            context.assets,
            context.resources.displayMetrics,
            context.resources.configuration
        )

        try {
            val stringFields = R.string::class.java.fields

            for (field in stringFields) {
                try {
                    val resourceId = field.getInt(null)
                    val defaultValue = defaultResources.getString(resourceId)
                    val localizedValue = context.resources.getString(resourceId)

                    if (defaultValue == localizedValue && !containsFormatSpecifier(defaultValue)) {
                        missingKeys.add(field.name)
                    }
                } catch (e: Resources.NotFoundException) {
                    missingKeys.add(field.name)
                } catch (e: Exception) {
                    Log.e(
                        "LocaleHelper",
                        "Error checking translation for ${field.name}: ${e.message}"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("LocaleHelper", "Error accessing string resources: ${e.message}")
        }
        return missingKeys
    }

    fun logMissingTranslations(context: Context) {
        val currentLang = getCurrentLanguageCode(context)
        val missingTranslations = checkMissingTranslations(context)

        if (missingTranslations.isNotEmpty()) {
            Log.w("LocaleHelper", "Missing or identical translations for $currentLang: " +
                    "${missingTranslations.size} strings")

            missingTranslations.chunked(20).forEach { chunk ->
                Log.d("LocaleHelper", "Missing translations: ${chunk.joinToString(", ")}")
            }
        } else {
            Log.i("LocaleHelper", "No missing translations detected for $currentLang")
        }
    }

    private fun containsFormatSpecifier(value: String): Boolean {
        return value.contains("%") &&
                (value.contains("%s") || value.contains("%d") || value.contains("%1$"))
    }

    fun getTotalStringCount(): Int {
        return try {
            R.string::class.java.fields.size
        } catch (e: Exception) {
            Log.e("LocaleHelper", "Error counting strings: ${e.message}")
            0
        }
    }

    fun getTranslationStatusReport(context: Context): Map<String, TranslationStatus> {
        val report = mutableMapOf<String, TranslationStatus>()
        val totalStrings = getTotalStringCount()

        for (language in supportedLanguages) {
            if (language.code == "en") {
                report[language.code] = TranslationStatus(language.displayName, totalStrings, 0, 100.0)
                continue
            }

            val originalLocale = getCurrentLanguageCode(context)
            applyLanguage(context, language.code)

            val missingCount = checkMissingTranslations(context).size
            val translatedCount = totalStrings - missingCount
            val percentage = if (totalStrings > 0) (translatedCount.toDouble() / totalStrings) * 100 else 0.0

            report[language.code] = TranslationStatus(
                language.displayName,
                translatedCount,
                missingCount,
                percentage
            )

            applyLanguage(context, originalLocale)
        }

        return report
    }
}

data class SupportedLanguage(
    val code: String,
    val displayName: String
)

data class TranslationStatus(
    val language: String,
    val translatedCount: Int,
    val missingCount: Int,
    val percentage: Double
)