package com.taskgoapp.taskgo.core.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/**
 * Modifier extension para esconder o teclado ao clicar fora dos campos de texto
 * 
 * Uso:
 * ```
 * Box(modifier = Modifier
 *     .fillMaxSize()
 *     .hideKeyboardOnClickOutside()
 * ) {
 *     // Conteúdo do formulário
 * }
 * ```
 */
@Composable
fun Modifier.hideKeyboardOnClickOutside(): Modifier {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    return this.then(
        Modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    )
}

