package com.taskgoapp.taskgo.core.design

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.theme.TaskGoGreen
import com.taskgoapp.taskgo.core.theme.TaskGoTextGray

@Composable
private fun OutlinedTextFieldString(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    label: @Composable (() -> Unit)?,
    placeholder: @Composable (() -> Unit)?,
    singleLine: Boolean,
    enabled: Boolean,
    readOnly: Boolean,
    keyboardOptions: KeyboardOptions?,
    maxLines: Int,
    minLines: Int,
    isError: Boolean,
    supportingText: @Composable (() -> Unit)?,
    visualTransformation: VisualTransformation,
    trailingIcon: @Composable (() -> Unit)?,
    colors: TextFieldColors,
    shape: Shape,
    @Suppress("UNUSED_PARAMETER") contentPadding: PaddingValues
) {
    val interactionSource = remember { MutableInteractionSource() }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        label = label,
        placeholder = placeholder,
        trailingIcon = trailingIcon,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions ?: KeyboardOptions.Default,
        interactionSource = interactionSource,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        shape = shape,
        colors = colors
    )
}

@Composable
private fun OutlinedTextFieldValue(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier,
    label: @Composable (() -> Unit)?,
    placeholder: @Composable (() -> Unit)?,
    singleLine: Boolean,
    enabled: Boolean,
    readOnly: Boolean,
    keyboardOptions: KeyboardOptions?,
    maxLines: Int,
    minLines: Int,
    isError: Boolean,
    supportingText: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    colors: TextFieldColors,
    shape: Shape,
    @Suppress("UNUSED_PARAMETER") contentPadding: PaddingValues
) {
    val interactionSource = remember { MutableInteractionSource() }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        label = label,
        placeholder = placeholder,
        trailingIcon = trailingIcon,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = VisualTransformation.None,
        keyboardOptions = keyboardOptions ?: KeyboardOptions.Default,
        interactionSource = interactionSource,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        shape = shape,
        colors = colors
    )
}

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
    supportingText: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextFieldString(
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
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else TaskGoGreen,
            unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else TaskGoTextGray,
            focusedLabelColor = TaskGoTextGray,
            unfocusedLabelColor = TaskGoTextGray,
            cursorColor = TaskGoGreen
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp)
    )
}

/**
 * Helper para criar OutlinedTextField com TextFieldValue (para campos formatados)
 */
@Composable
fun OutlinedTextFieldWithValue(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions? = null,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = if (singleLine) 1 else 1,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(8.dp),
    colors: TextFieldColors? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 20.dp)
) {
    OutlinedTextFieldValue(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        singleLine = singleLine,
        enabled = enabled,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        maxLines = maxLines,
        minLines = minLines,
        isError = isError,
        supportingText = supportingText,
        trailingIcon = trailingIcon,
        colors = colors ?: OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else TaskGoGreen,
            unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else TaskGoTextGray,
            focusedLabelColor = TaskGoTextGray,
            unfocusedLabelColor = TaskGoTextGray,
            cursorColor = TaskGoGreen
        ),
        shape = shape,
        contentPadding = contentPadding
    )
}
