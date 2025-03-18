package com.example.phms

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper{
    val supportedLanguages = listOf(
        SupportedLanguage("en", "English"),
        SupportedLanguage("es", "Español"),
        SupportedLanguage("fr", "Français"),
        SupportedLanguage("hi", "हिन्दी")
    )

    fun applyLanguage(context: Context, languageCode: String){
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        val localeList = LocaleListCompat.create(locale)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun getCurrentLanguageCode(context: Context): String{
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        return if (currentLocales.isEmpty) {
            getSystemLanguageCode(context)
        } else {
            currentLocales.get(0)?.language ?: "en"
        }
    }

    fun getSystemLanguageCode(context: Context): String{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0).language
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale.language
        }
    }
}

data class SupportedLanguage(
    val code: String,
    val displayName: String
)