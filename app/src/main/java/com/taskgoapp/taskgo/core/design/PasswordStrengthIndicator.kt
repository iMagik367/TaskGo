package com.taskgoapp.taskgo.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskgoapp.taskgo.core.theme.TaskGoTextGray
import com.taskgoapp.taskgo.core.validation.PasswordStrength
import com.taskgoapp.taskgo.core.validation.PasswordValidator

/**
 * Componente visual para indicar a força da senha
 * Exibe uma barra de progresso colorida e texto descritivo
 */
@Composable
fun PasswordStrengthIndicator(
    password: String,
    modifier: Modifier = Modifier
) {
    val validator = PasswordValidator()
    val strength = validator.calculateStrength(password)
    
    if (password.isEmpty()) {
        return // Não exibir nada se a senha estiver vazia
    }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Barra de progresso
        LinearProgressIndicator(
            progress = { getStrengthProgress(strength) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = getStrengthColor(strength),
            trackColor = Color(0xFFE0E0E0)
        )
        
        // Texto descritivo
        Text(
            text = getStrengthText(strength),
            color = getStrengthColor(strength),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Obtém a cor baseada na força da senha
 */
private fun getStrengthColor(strength: PasswordStrength): Color {
    return when (strength) {
        PasswordStrength.MUITO_FRACA -> Color(0xFFBD0000) // Vermelho (TaskGoError)
        PasswordStrength.FRACA -> Color(0xFFFF9800) // Laranja
        PasswordStrength.MEDIA -> Color(0xFFFFEE00) // Amarelo (TaskGoWarning)
        PasswordStrength.FORTE -> Color(0xFF49E985) // Verde claro (TaskGoGreenLight)
        PasswordStrength.MUITO_FORTE -> Color(0xFF00BD48) // Verde (TaskGoGreen)
    }
}

/**
 * Obtém o progresso (0.0 a 1.0) baseado na força da senha
 */
private fun getStrengthProgress(strength: PasswordStrength): Float {
    return when (strength) {
        PasswordStrength.MUITO_FRACA -> 0.2f
        PasswordStrength.FRACA -> 0.4f
        PasswordStrength.MEDIA -> 0.6f
        PasswordStrength.FORTE -> 0.8f
        PasswordStrength.MUITO_FORTE -> 1.0f
    }
}

/**
 * Obtém o texto descritivo baseado na força da senha
 */
private fun getStrengthText(strength: PasswordStrength): String {
    return when (strength) {
        PasswordStrength.MUITO_FRACA -> "Senha muito fraca"
        PasswordStrength.FRACA -> "Senha fraca"
        PasswordStrength.MEDIA -> "Senha média"
        PasswordStrength.FORTE -> "Senha forte"
        PasswordStrength.MUITO_FORTE -> "Senha muito forte"
    }
}

