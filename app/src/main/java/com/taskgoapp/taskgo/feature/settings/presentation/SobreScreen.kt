package com.taskgoapp.taskgo.feature.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SobreScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Sobre",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // App logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = TaskGoGreen,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "TaskGo",
                    style = FigmaTitleLarge,
                    color = TaskGoTextBlack,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = "Versão 1.0.0",
                style = FigmaProductDescription,
                color = TaskGoTextGray
            )
            
            Text(
                text = "TaskGo é uma plataforma que conecta pessoas que precisam de serviços com profissionais qualificados. Nossa missão é facilitar a contratação de serviços de qualidade de forma rápida e segura.",
                style = FigmaProductDescription,
                color = TaskGoTextBlack,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Text(
                text = "© 2024 TaskGo. Todos os direitos reservados.",
                style = FigmaStatusText,
                color = TaskGoTextGray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
