package com.example.taskgoapp.core.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.taskgoapp.core.designsystem.theme.TaskGoColors
import com.example.taskgoapp.core.designsystem.theme.TaskGoTypography

// Estados do campo de texto
enum class TgTextFieldState {
    DEFAULT,    // Estado padrão
    FOCUSED,    // Campo focado
    ERROR,      // Campo com erro
    DISABLED    // Campo desabilitado
}

// Configurações do campo de texto
data class TgTextFieldConfig(
    val state: TgTextFieldState = TgTextFieldState.DEFAULT,
    val fullWidth: Boolean = true,
    val cornerRadius: Int = 20,
    val showLabel: Boolean = true,
    val showError: Boolean = false,
    val helperText: String? = null,
    val errorText: String? = null,
    val maxLines: Int = 1,
    val singleLine: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TgTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    config: TgTextFieldConfig = TgTextFieldConfig(),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = config.state == TgTextFieldState.ERROR,
    enabled: Boolean = config.state != TgTextFieldState.DISABLED
) {
    val fieldModifier = modifier
        .then(
            if (config.fullWidth) Modifier.fillMaxWidth() else Modifier
        )
        .padding(horizontal = 4.dp)
    
    val shape = RoundedCornerShape(config.cornerRadius.dp)
    
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = fieldModifier,
            label = if (config.showLabel && label != null) {
                { Text(text = label, style = TaskGoTypography.bodyMedium) }
            } else null,
            placeholder = if (placeholder != null) {
                { Text(text = placeholder, style = TaskGoTypography.bodyMedium) }
            } else null,
            enabled = enabled,
            isError = isError,
            shape = shape,
            maxLines = config.maxLines,
            singleLine = config.singleLine,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = TaskGoColors.Surface,
                unfocusedContainerColor = TaskGoColors.Surface,
                disabledContainerColor = TaskGoColors.Surface,
                focusedTextColor = TaskGoColors.OnSurface,
                unfocusedTextColor = TaskGoColors.OnSurface,
                disabledTextColor = TaskGoColors.Outline.copy(alpha = 0.38f),
                cursorColor = TaskGoColors.Primary,
                focusedIndicatorColor = TaskGoColors.Primary,
                unfocusedIndicatorColor = TaskGoColors.Outline,
                disabledIndicatorColor = TaskGoColors.Outline.copy(alpha = 0.12f)
            ),
            textStyle = TextStyle(
                fontFamily = TaskGoTypography.bodyMedium.fontFamily,
                fontSize = TaskGoTypography.bodyMedium.fontSize,
                lineHeight = TaskGoTypography.bodyMedium.lineHeight,
                letterSpacing = TaskGoTypography.bodyMedium.letterSpacing
            )
        )
        
        // Texto de ajuda
        if (config.helperText != null && !isError) {
            Text(
                text = config.helperText,
                style = TaskGoTypography.bodySmall.copy(
                    color = TaskGoColors.OnSurfaceVariant
                ),
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
        
        // Texto de erro
        if (config.showError && isError && config.errorText != null) {
            Text(
                text = config.errorText,
                style = TaskGoTypography.bodySmall.copy(
                    color = TaskGoColors.Error
                ),
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

// Campos de texto pré-configurados para uso comum
@Composable
fun TgPrimaryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    config: TgTextFieldConfig = TgTextFieldConfig(),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    TgTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        config = config,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation
    )
}

@Composable
fun TgErrorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    errorText: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    config: TgTextFieldConfig = TgTextFieldConfig(
        state = TgTextFieldState.ERROR,
        showError = true,
        errorText = errorText
    ),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    TgTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        config = config,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        isError = true
    )
}

@Composable
fun TgDisabledTextField(
    value: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    config: TgTextFieldConfig = TgTextFieldConfig(
        state = TgTextFieldState.DISABLED
    )
) {
    TgTextField(
        value = value,
        onValueChange = {},
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        config = config,
        enabled = false
    )
}
