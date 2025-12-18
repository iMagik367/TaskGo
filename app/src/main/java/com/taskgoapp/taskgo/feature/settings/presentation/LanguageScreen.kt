package com.taskgoapp.taskgo.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.taskgoapp.taskgo.core.theme.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.core.design.AppTopBar

enum class Language(val displayName: String) {
    PORTUGUESE("Português"),
    ENGLISH("English"),
    SPANISH("Español"),
    FRENCH("Français"),
    ITALIAN("Italiano"),
    GERMAN("Deutsch")
}

@Composable
fun LanguageScreen(
    onBackClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.state.collectAsState()
    var selectedLanguage by remember { mutableStateOf(
        when (settings.language) {
            "en" -> Language.ENGLISH
            "es" -> Language.SPANISH
            "fr" -> Language.FRENCH
            "it" -> Language.ITALIAN
            "de" -> Language.GERMAN
            else -> Language.PORTUGUESE
        }
    ) }
    
    // Salvar automaticamente quando sair da tela
    androidx.compose.runtime.DisposableEffect(selectedLanguage) {
        onDispose {
            val code = when (selectedLanguage) {
                Language.ENGLISH -> "en"
                Language.SPANISH -> "es"
                Language.FRENCH -> "fr"
                Language.ITALIAN -> "it"
                Language.GERMAN -> "de"
                else -> "pt"
            }
            viewModel.saveLanguage(code)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header
        AppTopBar(
            title = "Idioma",
            subtitle = "Defina o idioma exibido no aplicativo",
            onBackClick = onBackClick,
            backgroundColor = TaskGoGreen,
            titleColor = Color.White,
            backIconColor = Color.White
        )

        // Language List
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Language.values().forEach { language ->
                LanguageItem(
                    language = language,
                    isSelected = selectedLanguage == language,
                    onLanguageSelected = { selectedLanguage = language }
                )
                if (language != Language.values().last()) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(TaskGoDividerLight)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun LanguageItem(
    language: Language,
    isSelected: Boolean,
    onLanguageSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLanguageSelected() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection indicator
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    color = if (isSelected) TaskGoGreen else TaskGoDivider,
                    shape = RoundedCornerShape(4.dp)
                )
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(2.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Language name
        Text(
            text = language.displayName,
            color = TaskGoTextDarkGray,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )
    }
}


