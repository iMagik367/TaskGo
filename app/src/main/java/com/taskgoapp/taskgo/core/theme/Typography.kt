package com.taskgoapp.taskgo.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Tipografia baseada no protótipo Figma
 * Font: Open Sans (conforme arquivo OpenSans.ttf no projeto)
 * Baseado nos estilos extraídos do Figma
 */

// === TIPOGRAFIA PRINCIPAL ===
val TaskGoTypography = Typography(
    // Display styles - para títulos principais
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    
    // Headline styles - para títulos de seção
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 28.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    
    // Title styles - para títulos de cards e elementos
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    
    // Body styles - para texto principal
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    
    // Label styles - para labels e botões
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
)

// === ESTILOS ESPECÍFICOS DO FIGMA ===
// Baseados nos estilos extraídos do protótipo

// Títulos principais das telas
val FigmaTitleLarge = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 24.sp,
    fontWeight = FontWeight.Bold,
    lineHeight = 32.sp,
    letterSpacing = 0.sp
)

// Títulos de seção (ex: "Produtos em Destaque")
val FigmaSectionTitle = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 20.sp,
    fontWeight = FontWeight.SemiBold,
    lineHeight = 28.sp,
    letterSpacing = 0.sp
)

// Nomes de produtos/serviços
val FigmaProductName = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 16.sp,
    fontWeight = FontWeight.Medium,
    lineHeight = 22.sp,
    letterSpacing = 0.sp
)

// Descrições de produtos
val FigmaProductDescription = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 20.sp,
    letterSpacing = 0.sp
)

// Preços
val FigmaPrice = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 16.sp,
    fontWeight = FontWeight.Bold,
    lineHeight = 22.sp,
    letterSpacing = 0.sp
)

// Texto de navegação (bottom nav)
val FigmaNavLabel = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 12.sp,
    fontWeight = FontWeight.Medium,
    lineHeight = 16.sp,
    letterSpacing = 0.sp
)

// Placeholders de input
val FigmaPlaceholder = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 20.sp,
    letterSpacing = 0.sp
)

// Texto de botões
val FigmaButtonText = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 16.sp,
    fontWeight = FontWeight.SemiBold,
    lineHeight = 22.sp,
    letterSpacing = 0.sp
)

// Texto de status (ex: "Em andamento", "Concluído")
val FigmaStatusText = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 12.sp,
    fontWeight = FontWeight.Medium,
    lineHeight = 16.sp,
    letterSpacing = 0.sp
)

// Texto de avaliação/rating
val FigmaRatingText = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 14.sp,
    fontWeight = FontWeight.Medium,
    lineHeight = 20.sp,
    letterSpacing = 0.sp
)
