package com.taskgoapp.taskgo.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Cores extraídas diretamente do protótipo Figma
 * Fonte: https://www.figma.com/design/ESJvpooDWO2xZwFOyW0YzO/TaskGo
 * Baseado nos tokens extraídos do arquivo colors.json
 */

// === CORES PRINCIPAIS ===
// Verde principal do TaskGo (splash, vector, produtos ativo, perfil ativo)
val TaskGoGreen = Color(0xFF00BD48)           // rgb(0, 189, 72) - splash/vector
val TaskGoGreenLight = Color(0xFF49E985)      // rgb(73, 233, 134) - ellipse_1 (sucesso)
val TaskGoGreenDark = Color(0xFF005224)       // rgb(0, 82, 36) - preços destacados

// === CORES DE FUNDO ===
val TaskGoBackgroundWhite = Color(0xFFFFFFFF)           // Branco principal
val TaskGoBackgroundGray = Color(0xFFF7F7F7)          // rgb(247, 247, 247) - rectangle_11
val TaskGoBackgroundGrayLight = Color(0xFFF9F9F9)     // rgb(249, 249, 249) - rectangle_14
val TaskGoBackgroundGrayBorder = Color(0xFFFCFCFC)     // rgb(252, 252, 252) - rectangle_28

// === CORES DE SUPERFÍCIE ===
val TaskGoSurface = Color(0xFFFFFFFF)                  // Superfícies brancas
val TaskGoSurfaceGray = Color(0xFFF1F1F1)              // rgb(241, 241, 241) - rectangle_24,25,26,27
val TaskGoSurfaceGrayLight = Color(0xFFDBDBDB)         // rgb(219, 219, 219) - rectangle_39,41,42,etc

// === CORES DE TEXTO (baseadas no Figma) ===
val TaskGoTextBlack = Color(0xFF000000)                // rgb(0, 0, 0) - texto preto principal
val TaskGoTextDark = Color(0xFF383838)                // rgb(56, 56, 56) - texto escuro
val TaskGoTextGray = Color(0xFF6C6C6C)                // rgb(108, 108, 108) - texto cinza padrão
val TaskGoTextGrayMedium = Color(0xFF646464)           // rgb(100, 100, 100) - texto cinza médio
val TaskGoTextGrayLight = Color(0xFF808080)            // rgb(128, 128, 128) - texto cinza claro
val TaskGoTextGrayPlaceholder = Color(0xFFBBBBBB)     // rgb(187, 187, 187) - inicio/mensagem inativo

// === CORES DE STATUS ===
val TaskGoSuccess = Color(0xFF00BD48)                  // Verde de sucesso
val TaskGoError = Color(0xFFBD0000)                    // rgb(189, 0, 0) - texto de erro
val TaskGoWarning = Color(0xFFFFEE00)                  // Estrelas - rgb(255, 238, 0)

// === CORES DE ELEMENTOS ===
val TaskGoBorder = Color(0xFFD9D9D9)                   // rgb(217, 217, 217) - rectangle_1
val TaskGoBorderLight = Color(0xFFD9D9D9)              // rgb(217, 217, 217) - rectangle_30,39,etc
val TaskGoDivider = Color(0xFFE0E0E0)                 // rgb(224, 224, 224) - rectangle_30

// === CORES DE DESTAQUE ===
val TaskGoPrimary = TaskGoGreen
val TaskGoSecondary = Color(0xFFFFEE00)                // Estrelas/Amarelo
val TaskGoAccent = TaskGoGreenDark

// === CORES NEUTRAS ===
val TaskGoNeutralDark = Color(0xFF3F3F3F)              // rgb(63, 63, 63) - _1,_2,_3,_4,_5
val TaskGoNeutral = Color(0xFFA9A9A9)                  // rgb(169, 169, 169) - "1"

// === CORES ESPECÍFICAS DO FIGMA ===
// Cores dos botões de navegação
val TaskGoNavInactive = Color(0xFFBBBBBB)             // rgb(187, 187, 187) - inicio/mensagem inativo
val TaskGoNavActive = Color(0xFF00BD48)               // Verde ativo

// Cores de preços
val TaskGoPriceDark = Color(0xFF2C2C2C)               // rgb(44, 44, 44) - preços normais
val TaskGoPriceGreen = Color(0xFF005224)              // rgb(0, 82, 36) - preços destacados

// Cores de notificações
val TaskGoNotificationBg = Color(0xFFE0E0E0)          // rgb(224, 224, 224) - rectangle_30
val TaskGoNotificationIcon = Color(0xFFDBDBDB)        // rgb(219, 219, 219) - rectangle_39,41,42

// Cores de estrelas (avaliações)
val TaskGoStarFilled = Color(0xFFFFEE00)              // rgb(255, 238, 0) - estrelas preenchidas
val TaskGoStarEmpty = Color(0xFF6C6C6C)               // rgb(108, 108, 108) - estrelas vazias

// === MAPEAMENTO PARA MATERIAL DESIGN ===
val TaskGoSuccessGreen = Color(0xFF4CAF50)              // Verde de sucesso Material
val TaskGoErrorRed = Color(0xFFCC0000)                 // Vermelho de erro
val TaskGoTextDarkGray = Color(0xFF333333)             // Texto escuro
val TaskGoTextMediumGray = Color(0xFF666666)          // Texto médio
val TaskGoTextLightGray = Color(0xFF999999)           // Texto claro
val TaskGoStarYellow = Color(0xFFFFD700)              // Estrelas douradas
val TaskGoAmber = Color(0xFFFFC107)                   // Amarelo/âmbar
val TaskGoOrange = Color(0xFFFF9800)                  // Laranja
val TaskGoDividerLight = Color(0xFFE0E0E0)            // Divider claro
val TaskGoBackgroundLight = Color(0xFFF5F5F5)         // Background claro
val TaskGoSurfaceGrayBg = Color(0xFFE8F5E8)            // Background verde suave
