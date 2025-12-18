package com.taskgoapp.taskgo.core.locale

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.Locale

object LocaleManager {
    
    fun getLocale(languageCode: String): Locale {
        return when (languageCode) {
            "en" -> Locale.ENGLISH
            "es" -> Locale("es", "ES")
            "fr" -> Locale.FRENCH
            "it" -> Locale.ITALIAN
            "de" -> Locale.GERMAN
            else -> Locale("pt", "BR")
        }
    }
    
    fun updateConfiguration(context: Context, languageCode: String): Context {
        val locale = getLocale(languageCode)
        Locale.setDefault(locale)
        
        val resources: Resources = context.resources
        val configuration: Configuration = resources.configuration
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
            return context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
            return context
        }
    }
    
    fun getLanguageCode(locale: Locale): String {
        return when (locale.language) {
            "en" -> "en"
            "es" -> "es"
            "fr" -> "fr"
            "it" -> "it"
            "de" -> "de"
            else -> "pt"
        }
    }
}

