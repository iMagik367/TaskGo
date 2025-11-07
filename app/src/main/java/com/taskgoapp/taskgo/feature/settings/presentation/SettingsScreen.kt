package com.taskgoapp.taskgo.feature.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.res.painterResource
import com.taskgoapp.taskgo.core.design.TGIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.design.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import com.taskgoapp.taskgo.core.theme.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToAccount: () -> Unit,
    onNavigateToPreferences: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToAiSupport: () -> Unit,
    onNavigateToSecurity: () -> Unit = {}
) {
    val settingsOptions = listOf(
        SettingsOption(
            icon = TGIcons.Profile,
            title = "Conta",
            subtitle = "Dados pessoais e segurança",
            onClick = onNavigateToAccount
        ),
        SettingsOption(
            icon = TGIcons.Settings,
            title = "Segurança",
            subtitle = "Biometria e autenticação",
            onClick = onNavigateToSecurity
        ),
        SettingsOption(
            icon = TGIcons.Edit,
            title = "Preferências",
            subtitle = "Configurações do app",
            onClick = onNavigateToPreferences
        ),
        SettingsOption(
            icon = TGIcons.Bell,
            title = "Notificações",
            subtitle = "Configurar alertas",
            onClick = onNavigateToNotifications
        ),
        SettingsOption(
            icon = TGIcons.Language,
            title = "Idioma",
            subtitle = "Português (Brasil)",
            onClick = onNavigateToLanguage
        ),
        SettingsOption(
            icon = TGIcons.Privacy,
            title = "Privacidade",
            subtitle = "Configurações de privacidade",
            onClick = onNavigateToPrivacy
        ),
        SettingsOption(
            icon = TGIcons.Support,
            title = "Suporte",
            subtitle = "Central de ajuda",
            onClick = onNavigateToSupport
        ),
        SettingsOption(
            icon = TGIcons.Messages,
            title = "AI TaskGo",
            subtitle = "Suporte com inteligência artificial",
            onClick = onNavigateToAiSupport
        ),
        SettingsOption(
            icon = TGIcons.Info,
            title = "Sobre",
            subtitle = "Versão 1.0.0",
            onClick = onNavigateToAbout
        )
    )
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Configurações",
                onBackClick = { /* TODO: Implementar navegação de volta */ }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(settingsOptions) { option ->
                SettingsOptionCard(option = option)
            }
            
            // Logout Button
            item {
                Spacer(modifier = Modifier.height(32.dp))
                OutlinedButton(
                    onClick = { /* TODO: Implementar logout */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(1.dp, TaskGoError)
                ) {
                    Icon(
                        painter = painterResource(TGIcons.Edit),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = TaskGoError
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sair da Conta", color=TaskGoError)
                }
            }
        }
    }
}

@Composable
fun SettingsOptionCard(
    option: SettingsOption,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { option.onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(option.icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = TaskGoGreen
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = option.title,
                    style = FigmaProductName,
                    color = TaskGoTextBlack,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = option.subtitle,
                    style = FigmaStatusText,
                    color = TaskGoTextGray
                )
            }
            
            Icon(
                painter = painterResource(TGIcons.Back),
                contentDescription = "Acessar",
                tint = TaskGoTextGray
            )
        }
    }
}

data class SettingsOption(
    val icon: Int,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)
