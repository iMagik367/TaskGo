package com.taskgoapp.taskgo.core.theme

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.taskgoapp.taskgo.core.locale.LocaleManager
import java.util.Locale

/**
 * Esquema de cores baseado no protótipo Figma
 * Cores extraídas diretamente do design
 */
private val LightColorScheme = lightColorScheme(
    primary = TaskGoGreen,                    // Verde principal #00BD48
    onPrimary = TaskGoBackgroundWhite,        // Branco sobre verde
    primaryContainer = TaskGoGreenLight,      // Verde claro
    onPrimaryContainer = TaskGoTextBlack,
    
    secondary = TaskGoGreenDark,              // Verde escuro
    onSecondary = TaskGoBackgroundWhite,
    secondaryContainer = TaskGoBackgroundGray,
    onSecondaryContainer = TaskGoTextBlack,
    
    tertiary = TaskGoSecondary,               // Amarelo das estrelas
    onTertiary = TaskGoBackgroundWhite,
    
    background = TaskGoBackgroundWhite,
    onBackground = TaskGoTextBlack,
    
    surface = TaskGoSurface,
    onSurface = TaskGoTextBlack,
    
    surfaceVariant = TaskGoBackgroundGray,
    onSurfaceVariant = TaskGoTextGray,
    
    error = TaskGoError,
    onError = TaskGoBackgroundWhite,
    errorContainer = TaskGoSurfaceGrayLight,
    onErrorContainer = TaskGoError,
    
    outline = TaskGoBorder,
    outlineVariant = TaskGoDivider,
    
    surfaceTint = TaskGoGreen,
    inverseSurface = TaskGoNeutralDark,
    inverseOnSurface = TaskGoBackgroundWhite,
    inversePrimary = TaskGoGreenLight
)

private val DarkColorScheme = darkColorScheme(
    primary = TaskGoGreenLight,
    onPrimary = TaskGoTextBlack,
    primaryContainer = TaskGoGreenDark,
    onPrimaryContainer = TaskGoBackgroundWhite,
    
    secondary = TaskGoGreen,
    onSecondary = TaskGoBackgroundWhite,
    secondaryContainer = TaskGoNeutralDark,
    onSecondaryContainer = TaskGoGreenLight,
    
    tertiary = TaskGoSecondary,
    onTertiary = TaskGoTextBlack,
    
    background = TaskGoTextBlack,
    onBackground = TaskGoBackgroundWhite,
    
    surface = TaskGoNeutralDark,
    onSurface = TaskGoBackgroundWhite,
    
    surfaceVariant = TaskGoNeutralDark,
    onSurfaceVariant = TaskGoTextGrayMedium,
    
    error = TaskGoError,
    onError = TaskGoBackgroundWhite,
    errorContainer = TaskGoError,
    onErrorContainer = TaskGoBackgroundWhite,
    
    outline = TaskGoBorder,
    outlineVariant = TaskGoBorderLight,
    
    surfaceTint = TaskGoGreen,
    inverseSurface = TaskGoBackgroundWhite,
    inverseOnSurface = TaskGoTextBlack,
    inversePrimary = TaskGoGreenDark
)

@Composable
fun TaskGoTheme(
    darkTheme: Boolean = false, // Sempre usar tema claro
    languageCode: String = "pt",
    content: @Composable () -> Unit
) {
    // Sempre usar tema claro - tema escuro desabilitado
    val colorScheme = LightColorScheme

    val view = LocalView.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    
    // Aplicar locale dinamicamente
    val locale = LocaleManager.getLocale(languageCode)
    Locale.setDefault(locale)
    
    val updatedConfiguration = Configuration(configuration).apply {
        setLocale(locale)
    }
    
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    // Prover configuração atualizada para os composables filhos
    CompositionLocalProvider(
        LocalConfiguration provides updatedConfiguration
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TaskGoTypography,
            shapes = TaskGoShapes,
            content = content
        )
    }
}
