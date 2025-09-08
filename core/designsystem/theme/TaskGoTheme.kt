package com.example.taskgoapp.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Cores do TaskGo
private val LightColorScheme = lightColorScheme(
    primary = TaskGoColors.Primary,
    onPrimary = TaskGoColors.OnPrimary,
    primaryContainer = TaskGoColors.PrimaryContainer,
    onPrimaryContainer = TaskGoColors.OnPrimaryContainer,
    secondary = TaskGoColors.Secondary,
    onSecondary = TaskGoColors.OnSecondary,
    secondaryContainer = TaskGoColors.SecondaryContainer,
    onSecondaryContainer = TaskGoColors.OnSecondaryContainer,
    tertiary = TaskGoColors.Tertiary,
    onTertiary = TaskGoColors.OnTertiary,
    tertiaryContainer = TaskGoColors.TertiaryContainer,
    onTertiaryContainer = TaskGoColors.OnTertiaryContainer,
    error = TaskGoColors.Error,
    onError = TaskGoColors.OnError,
    errorContainer = TaskGoColors.ErrorContainer,
    onErrorContainer = TaskGoColors.OnErrorContainer,
    background = TaskGoColors.Background,
    onBackground = TaskGoColors.OnBackground,
    surface = TaskGoColors.Surface,
    onSurface = TaskGoColors.OnSurface,
    surfaceVariant = TaskGoColors.SurfaceVariant,
    onSurfaceVariant = TaskGoColors.OnSurfaceVariant,
    outline = TaskGoColors.Outline,
    outlineVariant = TaskGoColors.OutlineVariant,
    scrim = TaskGoColors.Scrim,
    inverseSurface = TaskGoColors.InverseSurface,
    inverseOnSurface = TaskGoColors.InverseOnSurface,
    inversePrimary = TaskGoColors.InversePrimary,
    surfaceTint = TaskGoColors.SurfaceTint,
)

private val DarkColorScheme = darkColorScheme(
    primary = TaskGoColors.PrimaryDark,
    onPrimary = TaskGoColors.OnPrimaryDark,
    primaryContainer = TaskGoColors.PrimaryContainerDark,
    onPrimaryContainer = TaskGoColors.OnPrimaryContainerDark,
    secondary = TaskGoColors.SecondaryDark,
    onSecondary = TaskGoColors.OnSecondaryDark,
    secondaryContainer = TaskGoColors.SecondaryContainerDark,
    onSecondaryContainer = TaskGoColors.OnSecondaryContainerDark,
    tertiary = TaskGoColors.TertiaryDark,
    onTertiary = TaskGoColors.OnTertiaryDark,
    tertiaryContainer = TaskGoColors.TertiaryContainerDark,
    onTertiaryContainer = TaskGoColors.OnTertiaryContainerDark,
    error = TaskGoColors.ErrorDark,
    onError = TaskGoColors.OnErrorDark,
    errorContainer = TaskGoColors.ErrorContainerDark,
    onErrorContainer = TaskGoColors.OnErrorContainerDark,
    background = TaskGoColors.BackgroundDark,
    onBackground = TaskGoColors.OnBackgroundDark,
    surface = TaskGoColors.SurfaceDark,
    onSurface = TaskGoColors.SurfaceDark,
    surfaceVariant = TaskGoColors.SurfaceVariantDark,
    onSurfaceVariant = TaskGoColors.OnSurfaceVariantDark,
    outline = TaskGoColors.OutlineDark,
    outlineVariant = TaskGoColors.OutlineVariantDark,
    scrim = TaskGoColors.ScrimDark,
    inverseSurface = TaskGoColors.InverseSurfaceDark,
    inverseOnSurface = TaskGoColors.InverseOnSurfaceDark,
    inversePrimary = TaskGoColors.InversePrimaryDark,
    surfaceTint = TaskGoColors.SurfaceTintDark,
)

// Espaçamento local
data class TaskGoSpacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val huge: Dp = 48.dp,
)

// Raios locais
data class TaskGoRadii(
    val extraSmall: Dp = 8.dp,
    val small: Dp = 12.dp,
    val medium: Dp = 20.dp,
    val large: Dp = 28.dp,
    val extraLarge: Dp = 36.dp,
    val round: Dp = 100.dp,
)

// CompositionLocals para espaçamento e raios
val LocalTaskGoSpacing = staticCompositionLocalOf { TaskGoSpacing() }
val LocalTaskGoRadii = staticCompositionLocalOf { TaskGoRadii() }

@Composable
fun TaskGoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    CompositionLocalProvider(
        LocalTaskGoSpacing provides TaskGoSpacing(),
        LocalTaskGoRadii provides TaskGoRadii()
    ) {
        val radii = TaskGoRadii()
        val shapes = Shapes(
            extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(radii.extraSmall),
            small = androidx.compose.foundation.shape.RoundedCornerShape(radii.small),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(radii.medium),
            large = androidx.compose.foundation.shape.RoundedCornerShape(radii.large),
            extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(radii.extraLarge)
        )
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TaskGoTypography,
            shapes = shapes,
            content = content
        )
    }
}

// Extensões para acessar facilmente o espaçamento e raios
val MaterialTheme.taskGoSpacing: TaskGoSpacing
    @Composable
    @ReadOnlyComposable
    get() = LocalTaskGoSpacing.current

val MaterialTheme.taskGoRadii: TaskGoRadii
    @Composable
    @ReadOnlyComposable
    get() = LocalTaskGoRadii.current
