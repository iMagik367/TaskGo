package com.taskgoapp.taskgo.core.locale

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

val LocalAppLanguage = compositionLocalOf<String> { "pt" }

@Composable
fun ProvideAppLanguage(
    languageCode: String,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    
    val updatedContext = remember(languageCode) {
        updateLocale(context, languageCode)
    }
    
    val updatedConfiguration = remember(languageCode, configuration) {
        Configuration(configuration).apply {
            setLocale(LocaleManager.getLocale(languageCode))
        }
    }
    
    CompositionLocalProvider(
        LocalAppLanguage provides languageCode
    ) {
        content()
    }
}

private fun updateLocale(context: Context, languageCode: String): Context {
    val locale = LocaleManager.getLocale(languageCode)
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

