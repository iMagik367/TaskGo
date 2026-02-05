package com.taskgoapp.taskgo.feature.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.theme.*
import com.taskgoapp.taskgo.core.model.AccountType
import com.taskgoapp.taskgo.feature.profile.presentation.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracoesScreen(
    onBackClick: () -> Unit,
    onConta: () -> Unit,
    onPreferencias: () -> Unit,
    onNotificacoes: () -> Unit,
    onIdioma: () -> Unit,
    onPrivacidade: () -> Unit,
    onSuporte: () -> Unit,
    onSobre: () -> Unit,
    onAiSupport: () -> Unit,
    onSeguranca: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    if (uiState.accountType == null) {
        return
    }
    
    val accountType = uiState.accountType
    val isPartner = accountType == AccountType.PARCEIRO
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Configurações",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoBackgroundWhite
                ),
                border = BorderStroke(1.dp, TaskGoBorder)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConta() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Conta",
                            style = FigmaProductName,
                            color = TaskGoTextBlack,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = TaskGoTextGray
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = TaskGoDivider)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSeguranca() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Segurança",
                            style = FigmaProductName,
                            color = TaskGoTextBlack,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = TaskGoTextGray
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = TaskGoDivider)
                    // Preferências: remover para modo PARCEIRO
                    if (!isPartner) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPreferencias() }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Preferências",
                                style = FigmaProductName,
                                color = TaskGoTextBlack,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = TaskGoTextGray
                            )
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = TaskGoDivider)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNotificacoes() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Notificações",
                            style = FigmaProductName,
                            color = TaskGoTextBlack,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = TaskGoTextGray
                        )
                    }
                    // Idioma removido
                    HorizontalDivider(thickness = 0.5.dp, color = TaskGoDivider)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPrivacidade() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Privacidade",
                            style = FigmaProductName,
                            color = TaskGoTextBlack,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = TaskGoTextGray
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = TaskGoDivider)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSuporte() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Suporte",
                            style = FigmaProductName,
                            color = TaskGoTextBlack,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = TaskGoTextGray
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = TaskGoDivider)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAiSupport() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Suporte com IA",
                            style = FigmaProductName,
                            color = TaskGoTextBlack,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = TaskGoTextGray
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = TaskGoDivider)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSobre() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sobre",
                            style = FigmaProductName,
                            color = TaskGoTextBlack,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = TaskGoTextGray
                        )
                    }
                }
            }
        }
    }
}
