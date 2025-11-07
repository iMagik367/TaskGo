package com.taskgoapp.taskgo.feature.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Info
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
fun SuporteScreen(
    onBackClick: () -> Unit,
    onChatAi: () -> Unit,
    onSobre: () -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Suporte",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Opção: Chat AI TaskGo
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = TaskGoGreen
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Chat AI TaskGo",
                            style = FigmaProductName,
                            color = TaskGoTextBlack,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Tire dúvidas com nossa IA",
                            style = FigmaProductDescription,
                            color = TaskGoTextGray
                        )
                    }
                    TextButton(onClick = onChatAi) { Text("Abrir") }
                }
            }

            // Opção: Sobre o TaskGo
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TaskGoGreen
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sobre o TaskGo",
                            style = FigmaProductName,
                            color = TaskGoTextBlack,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Informações do aplicativo",
                            style = FigmaProductDescription,
                            color = TaskGoTextGray
                        )
                    }
                    TextButton(onClick = onSobre) { Text("Abrir") }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Contato
            Button(
                onClick = onChatAi,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = TaskGoGreen)
            ) { Text("Entrar em Contato", style = FigmaButtonText, color = TaskGoBackgroundWhite) }
        }
    }
}
