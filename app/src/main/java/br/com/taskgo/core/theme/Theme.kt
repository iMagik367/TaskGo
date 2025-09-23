package br.com.taskgo.taskgo.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1B5E20),            // tg_green_dark
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA5D6A7),   // tg_green_light
    onPrimaryContainer = Color(0xFF1A1A1A),
    secondary = Color(0xFF2E7D32),          // tg_green
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),         // branco
    onBackground = Color(0xFF1A1A1A),       // gray900
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF3F4F6),     // gray100
    onSurfaceVariant = Color(0xFF4D4D4D),   // gray700
    outline = Color(0xFFD1D5DB),            // gray300
    error = Color(0xFFD32F2F)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA5D6A7),
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF66BB6A),
    onSecondary = Color(0xFF1A1A1A),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFBDBDBD),
    outline = Color(0xFF2A2A2A),
    error = Color(0xFFEF9A9A)
)

@Composable
fun TaskGoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TaskGoTypography,
        shapes = TaskGoShapes,
        content = content
    )
}
