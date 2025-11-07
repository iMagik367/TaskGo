package com.taskgoapp.taskgo.feature.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.theme.*

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
    onDesignReview: () -> Unit,
    onTipoConta: () -> Unit,
    onAiSupport: () -> Unit,
    onSeguranca: () -> Unit = {}
) {
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
                    containerColor = TaskGoSurface
                )
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
                            .clickable { onTipoConta() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tipo de conta",
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
                    HorizontalDivider(thickness = 0.5.dp, color = TaskGoDivider)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onIdioma() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Idioma",
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
                    HorizontalDivider(thickness = 0.5.dp, color = TaskGoDivider)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDesignReview() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Design Review",
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
