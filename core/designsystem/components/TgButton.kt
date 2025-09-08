package com.example.taskgoapp.core.designsystem.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.taskgoapp.core.designsystem.theme.TaskGoColors
import com.example.taskgoapp.core.designsystem.theme.TaskGoTypography

// Variantes do botão TaskGo
enum class TgButtonVariant {
    FILLED,    // Botão preenchido (primário)
    TONAL,     // Botão tonal (secundário)
    TEXT       // Botão de texto (terciário)
}

// Estados do botão
enum class TgButtonState {
    ENABLED,   // Botão habilitado
    DISABLED,  // Botão desabilitado
    LOADING    // Botão carregando
}

// Configurações do botão
data class TgButtonConfig(
    val variant: TgButtonVariant = TgButtonVariant.FILLED,
    val state: TgButtonState = TgButtonState.ENABLED,
    val fullWidth: Boolean = false,
    val height: Int = 52,
    val cornerRadius: Int = 24
)

@Composable
fun TgButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    config: TgButtonConfig = TgButtonConfig(),
    enabled: Boolean = config.state != TgButtonState.DISABLED,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
) {
    val buttonModifier = modifier
        .then(
            if (config.fullWidth) Modifier.fillMaxWidth() else Modifier
        )
        .height(config.height.dp)
        .padding(horizontal = 4.dp)
    
    val shape = RoundedCornerShape(config.cornerRadius.dp)
    
    when (config.variant) {
        TgButtonVariant.FILLED -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                shape = shape,
                contentPadding = contentPadding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TaskGoColors.Primary,
                    contentColor = TaskGoColors.OnPrimary,
                    disabledContainerColor = TaskGoColors.Outline.copy(alpha = 0.12f),
                    disabledContentColor = TaskGoColors.Outline.copy(alpha = 0.38f)
                )
            ) {
                TgButtonContent(
                    text = text,
                    state = config.state,
                    textAlign = if (config.fullWidth) TextAlign.Center else TextAlign.Start
                )
            }
        }
        
        TgButtonVariant.TONAL -> {
            FilledTonalButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                shape = shape,
                contentPadding = contentPadding,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = TaskGoColors.SecondaryContainer,
                    contentColor = TaskGoColors.OnSecondaryContainer,
                    disabledContainerColor = TaskGoColors.Outline.copy(alpha = 0.12f),
                    disabledContentColor = TaskGoColors.Outline.copy(alpha = 0.38f)
                )
            ) {
                TgButtonContent(
                    text = text,
                    state = config.state,
                    textAlign = if (config.fullWidth) TextAlign.Center else TextAlign.Start
                )
            }
        }
        
        TgButtonVariant.TEXT -> {
            TextButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                shape = shape,
                contentPadding = contentPadding,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = TaskGoColors.Primary,
                    disabledContentColor = TaskGoColors.Outline.copy(alpha = 0.38f)
                )
            ) {
                TgButtonContent(
                    text = text,
                    state = config.state,
                    textAlign = if (config.fullWidth) TextAlign.Center else TextAlign.Start
                )
            }
        }
    }
}

@Composable
private fun TgButtonContent(
    text: String,
    state: TgButtonState,
    textAlign: TextAlign
) {
    when (state) {
        TgButtonState.LOADING -> {
            // Aqui você pode adicionar um indicador de carregamento
            Text(
                text = "Carregando...",
                style = TaskGoTypography.labelLarge,
                textAlign = textAlign
            )
        }
        else -> {
            Text(
                text = text,
                style = TaskGoTypography.labelLarge,
                textAlign = textAlign
            )
        }
    }
}

// Botões pré-configurados para uso comum
@Composable
fun TgPrimaryButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    config: TgButtonConfig = TgButtonConfig(variant = TgButtonVariant.FILLED),
    enabled: Boolean = true
) {
    TgButton(
        onClick = onClick,
        text = text,
        modifier = modifier,
        config = config.copy(variant = TgButtonVariant.FILLED),
        enabled = enabled
    )
}

@Composable
fun TgSecondaryButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    config: TgButtonConfig = TgButtonConfig(variant = TgButtonVariant.TONAL),
    enabled: Boolean = true
) {
    TgButton(
        onClick = onClick,
        text = text,
        modifier = modifier,
        config = config.copy(variant = TgButtonVariant.TONAL),
        enabled = enabled
    )
}

@Composable
fun TgTextButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    config: TgButtonConfig = TgButtonConfig(variant = TgButtonVariant.TEXT),
    enabled: Boolean = true
) {
    TgButton(
        onClick = onClick,
        text = text,
        modifier = modifier,
        config = config.copy(variant = TgButtonVariant.TEXT),
        enabled = enabled
    )
}
