package com.taskgoapp.taskgo.core.theme

import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Formas baseadas no protótipo Figma
 * Baseado nos componentes de bordas arredondadas dos cards e botões
 */
val TaskGoShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
)

/**
 * Espaçamentos padrão extraídos do Figma
 */
object TaskGoSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}
