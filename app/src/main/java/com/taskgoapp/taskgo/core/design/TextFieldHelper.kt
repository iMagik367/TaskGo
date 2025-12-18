package com.taskgoapp.taskgo.core.design

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.theme.TaskGoGreen
import com.taskgoapp.taskgo.core.theme.TaskGoTextGray

/**
 * Helper para criar OutlinedTextField com área de texto aumentada
 * Aumenta o contentPadding interno para que o texto não apareça cortado
 */
@Composable
fun EnhancedOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    keyboardOptions: KeyboardOptions? = null,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = if (singleLine) 1 else 1,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        singleLine = singleLine,
        enabled = enabled,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions ?: KeyboardOptions(keyboardType = keyboardType),
        maxLines = maxLines,
        minLines = minLines,
        isError = isError,
        supportingText = supportingText,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isError) androidx.compose.material3.MaterialTheme.colorScheme.error else TaskGoGreen,
            unfocusedBorderColor = if (isError) androidx.compose.material3.MaterialTheme.colorScheme.error else TaskGoTextGray,
            focusedLabelColor = TaskGoTextGray,
            unfocusedLabelColor = TaskGoTextGray,
            cursorColor = TaskGoGreen
        ),
        textStyle = androidx.compose.ui.text.TextStyle(
            lineHeight = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp)
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
    )
}

