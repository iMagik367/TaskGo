package com.example.taskgoapp.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Cores baseadas no arquivo colors.xml existente e expandidas para Material3
object TaskGoColors {
    // Esquema claro (cores do projeto)
    // Primária: 00BD48
    val Primary = Color(0xFF00BD48)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFE1F8EA)
    val OnPrimaryContainer = Color(0xFF004319)

    // Secundária (ícones): 004319
    val Secondary = Color(0xFF004319)
    val OnSecondary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFCCE7D6)
    val OnSecondaryContainer = Color(0xFF00240E)

    // Tertiary mantém neutro suave para estados/tonalidades
    val Tertiary = Color(0xFF4CAF79)
    val OnTertiary = Color(0xFFFFFFFF)
    val TertiaryContainer = Color(0xFFE2F4EA)
    val OnTertiaryContainer = Color(0xFF0F3A22)

    val Error = Color(0xFFBA1A1A)
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFFFDAD6)
    val OnErrorContainer = Color(0xFF410002)

    // Texto: 2C2C2C | superfícies claras
    val Background = Color(0xFFFFFFFF)
    val OnBackground = Color(0xFF2C2C2C)
    val Surface = Color(0xFFFFFFFF)
    val OnSurface = Color(0xFF2C2C2C)
    val SurfaceVariant = Color(0xFFF0F0F0)
    val OnSurfaceVariant = Color(0xFF6F6F6F)

    val Outline = Color(0xFFE0E0E0)
    val OutlineVariant = Color(0xFFEAEAEA)
    val Scrim = Color(0xFF000000)
    val InverseSurface = Color(0xFF2C2C2C)
    val InverseOnSurface = Color(0xFFFFFFFF)
    val InversePrimary = Color(0xFF006D2C)
    val SurfaceTint = Primary
    
    // Esquema escuro
    val PrimaryDark = Color(0xFF5DFFA0)
    val OnPrimaryDark = Color(0xFF003316)
    val PrimaryContainerDark = Color(0xFF005827)
    val OnPrimaryContainerDark = Color(0xFFA4FFB6)

    val SecondaryDark = Color(0xFF9BD7B2)
    val OnSecondaryDark = Color(0xFF00301A)
    val SecondaryContainerDark = Color(0xFF00512B)
    val OnSecondaryContainerDark = Color(0xFFCCE7D6)

    val TertiaryDark = Color(0xFFB5E7CC)
    val OnTertiaryDark = Color(0xFF0D2E1C)
    val TertiaryContainerDark = Color(0xFF23513A)
    val OnTertiaryContainerDark = Color(0xFFE2F4EA)

    val ErrorDark = Color(0xFFFFB4AB)
    val OnErrorDark = Color(0xFF690005)
    val ErrorContainerDark = Color(0xFF93000A)
    val OnErrorContainerDark = Color(0xFFFFDAD6)

    val BackgroundDark = Color(0xFF121212)
    val OnBackgroundDark = Color(0xFFE6E6E6)
    val SurfaceDark = Color(0xFF121212)
    val OnSurfaceDark = Color(0xFFE6E6E6)
    val SurfaceVariantDark = Color(0xFF3A3A3A)
    val OnSurfaceVariantDark = Color(0xFFBDBDBD)

    val OutlineDark = Color(0xFF5A5A5A)
    val OutlineVariantDark = Color(0xFF3F3F3F)
    val ScrimDark = Color(0xFF000000)
    val InverseSurfaceDark = Color(0xFFE6E6E6)
    val InverseOnSurfaceDark = Color(0xFF2C2C2C)
    val InversePrimaryDark = Color(0xFF00BD48)
    val SurfaceTintDark = PrimaryDark
}
